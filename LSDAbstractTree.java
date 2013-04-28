package skylinebreaker;

import java.util.Arrays;
import java.util.Comparator;

import preference.csv.optimize.LevelManager;
import preference.exception.PreferenceException;
import flatlc.levels.FlatLevelCombination;

public abstract class LSDAbstractTree {
	final static int BUCKETSIZE = 10;
	
	static interface Node
	{
		DirectoryNode getParent();
	}
	
	static public interface LSDAbstractTreeFactory
	{
		public abstract LSDAbstractTree get(final LevelManager manager, final FlatLevelCombination firstElement) throws PreferenceException;
	}
	static class DirectoryNode implements Node
	{
		final int location;
		Node left;
		Node right;
		final DirectoryNode parent;
		
		DirectoryNode(final int location, final Node left, final Node right, final DirectoryNode parent)
		{
			this.location = location;
			this.left = left;	
			this.right = right;
			this.parent = parent;
		}

		@Override
		public DirectoryNode getParent() {
			return parent;
		}
	}
	class BucketNode implements Node
	{
		final FlatLevelCombination[] data = new FlatLevelCombination[BUCKETSIZE];
		int reserved = 0;
		final int axis;
		DirectoryNode parent = null;
		
		BucketNode(final int axis)
		{
			this.axis = axis;
		}
		void add(FlatLevelCombination el)
		{
			data[reserved++] = el;
		}
		boolean isFull()
		{
			return reserved+1 == data.length;
		}
		int splitPoint()
		{
			Arrays.sort(data, 0, reserved, new AxisProjectionComparator(axis));
			return data.length/2; //[data.length/2].getLevel(axis);
		}
		public FlatLevelCombination[] computeLocalSkyline()
		{
			for(int i = 0; i < reserved; ++i)
			{
				outerLoop:
				for(int j = i+1; j < reserved; ++j)
				{
					innerLoop:
					//if(data[j] == null) continue;
					switch(data[i].compare(data[j])) {
					case FlatLevelCombination.LESS: 
						data[i--] = data[--reserved];
						break outerLoop;
					case FlatLevelCombination.GREATER: 
						data[j--] = data[--reserved];
						break innerLoop;
					}
				}
			}
			return Arrays.copyOf(data, reserved);
		}
		
		@Override
		public DirectoryNode getParent() {
			return parent;
		}
	}
	
	Node getRoot()
	{
		return root;
	}
	
	protected Node root;
	public final int dimensions;
	public final LevelManager manager;
	public LSDAbstractTree(final LevelManager manager, final FlatLevelCombination firstElement) throws PreferenceException
	{
		this.manager = manager;
		this.dimensions = manager.getBasePreferenceCount();
		final BucketNode bn = new BucketNode(getSplitAxis(0));
		bn.add(firstElement);
		root = bn;
	}
	
	
	public int getLevel(final FlatLevelCombination element, int i)
	{
		//return element.getLevel(i);
		return (int) manager.getBasePref(i).level(manager.getAttributeSelector(i).evaluate(element, null, manager.getBasePref(i).getDomainType()), null);
	}
	public int getOverallLevel(final FlatLevelCombination element)
	{
		//return element.getOverallLevel();
		
		int level = 0;
		for(int i = 0; i < dimensions; ++i)
			level += (int) manager.getBasePref(i).level(manager.getAttributeSelector(i).evaluate(element, null, manager.getBasePref(i).getDomainType()), null);
		return level;
		
	}
	
	public abstract void add(final FlatLevelCombination element) throws PreferenceException;
	
	
	int getSplitAxis(final int depth)
	{
		return depth % dimensions;
	}
	private final class AxisProjectionComparator implements Comparator<FlatLevelCombination> {
		final int axis;
		AxisProjectionComparator(int axis) { this.axis = axis; }
		
		 @Override
		 public int compare(final FlatLevelCombination a, final FlatLevelCombination b) { 
			 return Integer.compare(getLevel(a,axis), getLevel(b,axis));
		 }
	};
}
