package skylinebreaker;

import java.util.ArrayList;

import skylinebreaker.LSDAbstractTree.BucketNode;
import skylinebreaker.LSDAbstractTree.DirectoryNode;
import skylinebreaker.LSDAbstractTree.Node;
import flatlc.levels.FlatLevelCombination;

public class SBSingleCNN {
	
	
	private final LSDAbstractTree tree;
	static SBBase.NearestNeighborOfZero nnzero = null;
	final int processes;
	public SBSingleCNN(final LSDAbstractTree tree, int processes)
	{
		this.tree = tree;
		this.processes = processes;
		
	}
	
	public FlatLevelCombination[] computeSkyline() throws InterruptedException
	{
		final LSDAbstractTree.Node root = tree.getRoot();
		Node n = root;
		int depth = 0;
		
		while( !(n instanceof BucketNode))
		{
			DirectoryNode nc = (DirectoryNode) n;
			n = nc.left;
			++depth;
		}
		final BucketNode bn = (BucketNode) n;
		FlatLevelCombination[] zeroLocalSkyline = bn.computeLocalSkyline();
		{
			int nearestToZeroNorm = Integer.MAX_VALUE;
			FlatLevelCombination nearestToZero = null;
			for(final FlatLevelCombination element : zeroLocalSkyline)
			{
				final int l1n = tree.getOverallLevel(element);
				if(nearestToZeroNorm > l1n) { nearestToZero = element; nearestToZeroNorm = l1n; }
			}
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
			//	if(tree instanceof LSDPruningTree) ((LSDPruningTree)tree).computeOverallLevel(nnzero.neighbor); // hash the value of nnzero
			}
		}
		
		ArrayList<FlatLevelCombination[]> localSkylines = new ArrayList<FlatLevelCombination[]>();

		for(int i = 0; i < tree.dimensions-1; ++i)
		{
			final DirectoryNode dn = n.getParent(); --depth;
			if(dn == null) break;
			localSkylines.add(computeBucketSkyline(dn.left == n ? dn.right : dn.left));
			n = dn;
			
		}
		
		if(n != root)
		{
			while(n != root)
			{
				final DirectoryNode dn = n.getParent(); --depth;
				assert (dn.right != n) : "Upward going not on the left side";
				
				if(dn.right instanceof BucketNode) localSkylines.add(computeBucketSkyline(dn.right));
				else
				{
					final int[] minlevels = new int[tree.dimensions];
					final int dim = tree.getSplitAxis(depth);
					minlevels[dim] = dn.location;
					pruneBuckets( (DirectoryNode)dn.right, depth+1, minlevels, localSkylines);
				}
				
				n = dn;
				//minlevels[getSplitAxis(depth)] = 0;
				
			}
		}
		for(final FlatLevelCombination[] localSkyline : localSkylines)
			zeroLocalSkyline = SBBase.combineLocalSkyline(localSkyline, zeroLocalSkyline);
		return zeroLocalSkyline;
	}
	
	private void pruneBuckets(final LSDAbstractTree.DirectoryNode dn, final int depth, final int[] minlevels, ArrayList<FlatLevelCombination[]> localSkylines)
	{
		final int dim = tree.getSplitAxis(depth);
		final int oldvalue = minlevels[dim]; 
		minlevels[dim] = dn.location;
		boolean greater = true;
		for(int i = 0; i < tree.dimensions; ++i) if(minlevels[i] <= tree.getLevel(nnzero.neighbor, i)) { greater = false; break; }
		minlevels[dim] = oldvalue;
		if(dn.left instanceof LSDAbstractTree.BucketNode) localSkylines.add( ((LSDAbstractTree.BucketNode) dn.left).computeLocalSkyline());
		else pruneBuckets( ((LSDAbstractTree.DirectoryNode)dn.left), depth+1, minlevels, localSkylines);
		
		if(!greater){
			final int[] rightminlevels = minlevels.clone();
			rightminlevels[dim] = dn.location;
			if(dn.right instanceof LSDAbstractTree.BucketNode) localSkylines.add( ((LSDAbstractTree.BucketNode) dn.right).computeLocalSkyline());
			else pruneBuckets( ((LSDAbstractTree.DirectoryNode)dn.right), depth+1, rightminlevels, localSkylines);
		}
	
	}
	
	
	static FlatLevelCombination[] computeBucketSkyline(final Node n)
	{
		if(n instanceof BucketNode) return ((BucketNode)n).computeLocalSkyline();
		else 
		{
			final DirectoryNode dn = (DirectoryNode) n;
			return SBBase.combineLocalSkyline(computeBucketSkyline(dn.left),computeBucketSkyline(dn.right));
		}
	}

}
