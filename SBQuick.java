package skylinebreaker;

import java.util.LinkedList;
import java.util.Queue;

import preference.csv.optimize.LevelManager;
import preference.exception.PreferenceException;
import skylinebreaker.LSDAbstractTree.LSDAbstractTreeFactory;
import flatlc.inputrelations.FlatLCResultSetWrapper;
import flatlc.levels.FlatLevelCombination;

public final class SBQuick {
	
	public static FlatLevelCombination[] evaluate(final int processes, final LSDAbstractTreeFactory treeFactory, final FlatLCResultSetWrapper anti, final LevelManager manager) throws PreferenceException, InterruptedException
	{
		final Queue<Integer> notification = new LinkedList<Integer>();
		
		final class LSDWorker extends Thread
		{
			
			FlatLevelCombination[] data = null;
			private final FlatLevelCombination init_value;
			final int number;
			public LSDWorker(final int number, final FlatLevelCombination init_value) throws PreferenceException
			{
				this.number = number;
				this.init_value = init_value;
			}
			
	    	@Override
	    	public void run() 
	    	{
	    		try 
	    		{
		    		final LSDAbstractTree tree = treeFactory.get(manager, init_value);
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
		    		
						data = new SBSingle(tree).computeSkyline();
				}
	    		catch (PreferenceException | InterruptedException e1) 
	    		{
					e1.printStackTrace();
	    		}
	    		synchronized(notification) { notification.add(number); notification.notifyAll(); }
	    	}
		}
		
		//final int processes = Runtime.getRuntime().availableProcessors();
		final LSDWorker[] threads = new LSDWorker[processes];
		for(int i = 0; i < processes; ++i) threads[i] = new LSDWorker(i, (FlatLevelCombination) anti.next());
		for(int i = 0; i < processes; ++i) threads[i].start();
		final SkylineMergeQueue skylineMergeQueue = new SkylineMergeQueue(processes);
		int finishedThreads = 0;
		while(finishedThreads != threads.length)
		{
			int threadNum;
			synchronized(notification) 
			{ 
				while(notification.isEmpty()) notification.wait(); 
				threadNum = notification.poll();
			}
			++finishedThreads;
			final LSDWorker thread = threads[threadNum];
			thread.join();
			final FlatLevelCombination[] localSkyline = thread.data;
			if(localSkyline != null) skylineMergeQueue.add(localSkyline);
		}
		skylineMergeQueue.stop();
		
		for(SkylineMergeQueue.SkylineMergeWorker worker : skylineMergeQueue.threads)
		{
			worker.interrupt();
			worker.join();
		}
		assert skylineMergeQueue.computedLocalSkylines.size() == 1;
		return skylineMergeQueue.computedLocalSkylines.poll();
	}

	final static class SkylineMergeQueue
	{
		final Queue<FlatLevelCombination[]> computedLocalSkylines = new LinkedList<FlatLevelCombination[]>();
		final SkylineMergeWorker[] threads;
		private boolean running = true;
		public SkylineMergeQueue(final int maxThreads)
		{
			this.threads = new SkylineMergeWorker[Math.max(maxThreads/2, 1)];
			for(int i = 0; i < threads.length; ++i)
			{
				threads[i] = new SkylineMergeWorker();
				threads[i].start();
			}

		}
		public void stop()
		{
			running = false;
		}
		public void add(final FlatLevelCombination[] node)
		{
		       synchronized(computedLocalSkylines) {
		    	   computedLocalSkylines.add(node);
		    	   computedLocalSkylines.notify();
		        }
		}
		
		 class SkylineMergeWorker extends Thread 
		 {
		    	@Override
		    	public void run() 
		    	{
		    		try
		    		{
		    			while(running || !computedLocalSkylines.isEmpty())
		    			{
		    				FlatLevelCombination[] a;
		    				FlatLevelCombination[] b;
			    			synchronized(computedLocalSkylines) 
			    			{
					    		while(computedLocalSkylines.size() < 2) 
					    			computedLocalSkylines.wait();
					    		a = computedLocalSkylines.poll();
					    		b = computedLocalSkylines.poll();
			    			}
			    			
			    			FlatLevelCombination[] c = SBBase.combineLocalSkyline(a, b);
			    			add(c);
		    			}
		    		}
		            catch (InterruptedException ignored)
		            {
		            }
		    	}
		    }
		
		
		
	}
	
	
}
