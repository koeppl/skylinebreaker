package skylinebreaker;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

import preference.csv.optimize.LevelManager;
import preference.exception.PreferenceException;
import skylinebreaker.LSDAbstractTree.LSDAbstractTreeFactory;
import skylinebreaker.SBBase.NearestNeighborOfZero;
import flatlc.inputrelations.FlatLCResultSetWrapper;
import flatlc.levels.FlatLevelCombination;

public class SBFork {

	final private ForkJoinPool forkPool;
	
	final LSDAbstractTreeFactory treeFactory;
	final LevelManager manager;
	final FlatLCResultSetWrapper anti;
	final int processes;
	public SBFork(final int processes, final LSDAbstractTreeFactory treeFactory, final FlatLCResultSetWrapper anti, final LevelManager manager)
	{
		forkPool = new ForkJoinPool(processes);
		this.processes = processes;
		this.anti = anti;
		this.manager = manager;
		this.treeFactory = treeFactory;

	}
	public FlatLevelCombination[] compute() throws InterruptedException
	{
		
		final LSDWorker[] threads = new LSDWorker[processes];
		for(int i = 0; i < processes; ++i) threads[i] = new LSDWorker(i, (FlatLevelCombination) anti.next());
		for(int i = 0; i < processes; ++i) forkPool.submit(threads[i]);
		
		while(forkPool.getQueuedSubmissionCount() > 0 || !forkPool.isQuiescent()) 
		{
			Thread.sleep(10);
			FlatLevelCombination[] a = null, b = null;
			
			int queueSize;
			do
			{
				
			    synchronized(localSkylineQueue) 
			    {
			    	queueSize = localSkylineQueue.size();
			    	if(queueSize > 1) System.out.println("Found Queue-Size: " + queueSize);
			    	if(queueSize > 1)
			    	{
			    		a = localSkylineQueue.poll();
			    		b = localSkylineQueue.poll();
			    	}
			    	
			    }
			    
			    if(a != null) forkPool.submit(new SkylineMergeWorker(a, b));
			}
			while(queueSize > 3);
		}
		forkPool.shutdown();
		forkPool.awaitTermination(1, TimeUnit.MINUTES);
		
		System.out.println("Final Queue-Size: " + localSkylineQueue.size());
		if(localSkylineQueue.size() == 2) return SBBase.combineLocalSkyline(localSkylineQueue.poll(), localSkylineQueue.poll());
		
		
		//forkPool.awaitTermination(2, TimeUnit.MINUTES);
		//forkPool.getRunningThreadCount();
		//forkPool.
		assert localSkylineQueue.size() == 1;
		return localSkylineQueue.poll();
	}

	

	NearestNeighborOfZero nnzero = new NearestNeighborOfZero();
	
	
	final class LSDWorker implements Runnable
	{
		FlatLevelCombination[] data = null;
		private final FlatLevelCombination init_value;
		LSDAbstractTree tree;
		final int number;
		
		
		
		public LSDWorker(final int number, final FlatLevelCombination init_value)
		{
			this.number = number;
			this.init_value = init_value;
		}

		@Override
		public void run()
    	{
			try {
				tree = treeFactory.get(manager, init_value);
				while(true)
	    		{
	    			FlatLevelCombination element;
	    			synchronized(anti)
	    			{
	    				if(!anti.hasNext()) break;
	    				element = (FlatLevelCombination) anti.next();
	    			}
	    			tree.add(element);
	    		}
				final LSDAbstractTree.Node root = tree.getRoot();
				LSDAbstractTree.Node n = root;
				int depth = 0;
				
				while( !(n instanceof LSDAbstractTree.BucketNode))
				{
					LSDAbstractTree.DirectoryNode nc = (LSDAbstractTree.DirectoryNode) n;
					n = nc.left;
					++depth;
				}
				final LSDAbstractTree.BucketNode bn = (LSDAbstractTree.BucketNode) n;
				FlatLevelCombination[] zeroLocalSkyline = bn.computeLocalSkyline();
				{
					int nearestToZeroNorm = Integer.MAX_VALUE;
					FlatLevelCombination nearestToZero = null;
					for(final FlatLevelCombination element : zeroLocalSkyline)
					{
						final int l1n = tree.getOverallLevel(element);
						if(nearestToZeroNorm > l1n) { nearestToZero = element; nearestToZeroNorm = l1n; }
					}
				    synchronized(localSkylineQueue) { localSkylineQueue.add(zeroLocalSkyline); }
					synchronized(nnzero)
					{
						if(nnzero.norm > nearestToZeroNorm) 
						{
							nnzero.norm = nearestToZeroNorm;
							nnzero.neighbor = nearestToZero;
						}
						++nnzero.threadCount;
						nnzero.notifyAll();
						while(nnzero.threadCount < processes)
							nnzero.wait();
					}
					System.out.println("nn: " + nnzero.neighbor);
				}
				
			
				for(int i = 0; i < tree.dimensions-1; ++i)
				{
					final LSDAbstractTree.DirectoryNode dn = n.getParent(); --depth;
					if(dn == null) break;
					if(dn.right instanceof LSDAbstractTree.BucketNode) 
						submit(new BucketNodeWorker(true, (LSDAbstractTree.BucketNode) dn.right));
					else submit(new DirectoryNodeWorker(true, (LSDAbstractTree.DirectoryNode) dn.right));
					n = dn;
				}
				
				
				if(n != root)
				{
					while(n != root)
					{
						final LSDAbstractTree.DirectoryNode dn = n.getParent(); --depth;
						assert (dn.right != n) : "Upward going not on the left side";
						
						if(dn.right instanceof LSDAbstractTree.BucketNode) submit(new BucketNodeWorker(true, (LSDAbstractTree.BucketNode) dn.right));
						else
						{
							final int[] minlevels = new int[tree.dimensions];
							final int dim = tree.getSplitAxis(depth);
							minlevels[dim] = dn.location;
							submit(new PruneBucketWorker( (LSDAbstractTree.DirectoryNode)dn.right, depth+1, minlevels));
						}
						n = dn;				
					}
				}
				
			} 
			catch (PreferenceException | InterruptedException e) 
			{
				e.printStackTrace();
			}
    	}
		

		

		class PruneBucketWorker extends RecursiveAction
		{
			private static final long serialVersionUID = 3191824942893677558L;
			final LSDAbstractTree.DirectoryNode dn;
			final int depth;
			final int[] minlevels;
			private PruneBucketWorker(final LSDAbstractTree.DirectoryNode dn, final int depth, final int[] minlevels)
			{
				this.dn = dn;
				this.depth = depth;
				this.minlevels = minlevels;
			}

			@Override
			protected void compute() {
				final int dim = tree.getSplitAxis(depth);
				final int oldvalue = minlevels[dim]; 
				minlevels[dim] = dn.location;
				boolean greater = true;
				for(int i = 0; i < tree.dimensions; ++i) if(minlevels[i] <= tree.getLevel(nnzero.neighbor,i)) { greater = false; break; }
				minlevels[dim] = oldvalue;
				if(dn.left instanceof LSDAbstractTree.BucketNode) submit(new BucketNodeWorker(true, (LSDAbstractTree.BucketNode) dn.left));
				else submit(new PruneBucketWorker( ((LSDAbstractTree.DirectoryNode)dn.left), depth+1, minlevels));
				
				if(!greater)
				{
					final int[] rightminlevels = minlevels.clone();
					rightminlevels[dim] = dn.location;
					if(dn.right instanceof LSDAbstractTree.BucketNode) submit(new BucketNodeWorker(true, (LSDAbstractTree.BucketNode) dn.right));
					else submit(new PruneBucketWorker( ((LSDAbstractTree.DirectoryNode)dn.right), depth+1, rightminlevels));
				}
			}
		}
		
		
		
	}				
	private final Queue<FlatLevelCombination[]> localSkylineQueue = new LinkedList<FlatLevelCombination[]>();

	public void addLocalSkyline(final FlatLevelCombination[] localSkyline)
	{
		FlatLevelCombination[] a = null;
	    synchronized(localSkylineQueue) 
	    {
	    	if(localSkylineQueue.isEmpty()) localSkylineQueue.add(localSkyline);
	    	else a = localSkylineQueue.poll();
	    }
	    if(a != null) submit(new SkylineMergeWorker(a, localSkyline));
	}
	
	
	private void submit(ForkJoinTask<?> task) {
		synchronized(forkPool) { forkPool.submit(task); }
		
	}
	private void submit(Runnable task) {
		synchronized(forkPool) { forkPool.submit(task); }
	}


	class SkylineMergeWorker implements Runnable
	{
		
		final FlatLevelCombination[] a;
		final FlatLevelCombination[] b;
		
		public SkylineMergeWorker(final FlatLevelCombination[] a, final FlatLevelCombination[] b) 
		{
			this.a = a;
			this.b = b;
		}

		@Override
		public void run() 
		{
			addLocalSkyline(SBBase.combineLocalSkyline(a, b));
		}
		
	}
	
class BucketNodeWorker extends ForkJoinTask<FlatLevelCombination[]> 
{
	private static final long serialVersionUID = 3788728179104943691L;
	final LSDAbstractTree.BucketNode node;
	private FlatLevelCombination[] result = null;
	private final boolean notify; 
	BucketNodeWorker(final boolean notify, LSDAbstractTree.BucketNode node)
	{
		this.notify = notify;
		this.node = node;
	}
	
	@Override
	protected boolean exec() {
		result = node.computeLocalSkyline();
		if(notify) addLocalSkyline(result);
		return true;
	}

	@Override
	public FlatLevelCombination[] getRawResult() 
	{
		return result;
	}

	@Override
	protected void setRawResult(FlatLevelCombination[] flv) {
		result = flv;
	}
}	

	
	private final class DirectoryNodeWorker extends RecursiveTask<FlatLevelCombination[]> 
	{
		private static final long serialVersionUID = 1L;
		private final LSDAbstractTree.DirectoryNode node;
		private final boolean notify; 
		DirectoryNodeWorker(boolean notify, final LSDAbstractTree.DirectoryNode node)
		{
			this.notify = notify;
			this.node = node;
		}
		@Override
		protected FlatLevelCombination[] compute() 
		{
			final ForkJoinTask<FlatLevelCombination[]> leftWorker 
				= (node.left instanceof LSDAbstractTree.DirectoryNode) 
				? new DirectoryNodeWorker(false, (LSDAbstractTree.DirectoryNode) node.left) 
				: new BucketNodeWorker(false, (LSDAbstractTree.BucketNode) node.left); 
			leftWorker.fork();
			final ForkJoinTask<FlatLevelCombination[]> rightWorker 
			= (node.right instanceof LSDAbstractTree.DirectoryNode) 
			? new DirectoryNodeWorker(false, (LSDAbstractTree.DirectoryNode) node.right) 
			: new BucketNodeWorker(false, (LSDAbstractTree.BucketNode) node.right); 
			
			FlatLevelCombination[] result = SBBase.combineLocalSkyline(leftWorker.join(), rightWorker.invoke());
			if(notify) addLocalSkyline(result);
			return result;
		   }
	 }
	 
}
