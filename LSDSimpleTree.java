package skylinebreaker;


import preference.csv.optimize.LevelManager;
import preference.exception.PreferenceException;
import flatlc.levels.FlatLevelCombination;

public final class LSDSimpleTree extends LSDAbstractTree {


	public final static class LSDSimpleTreeFactory implements LSDAbstractTreeFactory
	{
		@Override
		public LSDAbstractTree get(final LevelManager manager, final FlatLevelCombination firstElement) throws PreferenceException {
			return new LSDSimpleTree(manager, firstElement);
		}	
	}
	
	
	public LSDSimpleTree(final LevelManager manager, final FlatLevelCombination firstElement) throws PreferenceException
	{
		super(manager, firstElement);
	}
	@Override
	public void add(final FlatLevelCombination element)
	{
		Node n = root;
		int depth = 0;
		while( !(n instanceof BucketNode))
		{
			final DirectoryNode nc = (DirectoryNode) n;
			final int compared = Integer.compare(getLevel(element,getSplitAxis(depth++)), nc.location);
			n = compared > 0 ? nc.right : nc.left;
		}
		final BucketNode bn = (BucketNode) n;
		if(bn.isFull())
		{
			bn.add(element);
			final int location = bn.splitPoint();
			final BucketNode lesser = new BucketNode(getSplitAxis(depth+1));
			final BucketNode higher = new BucketNode(getSplitAxis(depth+1));
			for(int i = 0; i < location; ++i) lesser.add(bn.data[i]);
			for(int i = location; i < bn.data.length; ++i) higher.add(bn.data[i]);
			final DirectoryNode dn = lesser.parent = higher.parent = new DirectoryNode(bn.data[location].getLevel(getSplitAxis(depth)), lesser, higher, bn.parent);
			if(bn.parent == null) root = dn;
			else
			{
				if(bn.parent.left == bn) bn.parent.left = dn;
				else bn.parent.right = dn;
			}
		}
		else
			bn.add(element);
	}
	

	
}
