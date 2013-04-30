package skylinebreaker;


import java.util.HashMap;

import preference.csv.optimize.LevelCombination;
import preference.csv.optimize.LevelManager;
import preference.exception.PreferenceException;
import flatlc.levels.FlatLevelCombination;

public class LSDPruningTree extends LSDAbstractTree {

	public final static class LSDPruningTreeFactory implements LSDAbstractTreeFactory
	{
		@Override
		public LSDAbstractTree get(final LevelManager manager, final FlatLevelCombination firstElement) throws PreferenceException {
			return new LSDPruningTree(manager, firstElement);
		}	
	}
	
	
	public LSDPruningTree(final LevelManager manager, final FlatLevelCombination firstElement) throws PreferenceException
	{
		super(manager, firstElement);
		pruningLevel = manager.getLevelInstance(firstElement, null).getPruningLevel();
		computeOverallLevel(firstElement); // needed to index this element with the levels mapping
	}
	
	private double pruningLevel;
	@Override
	public void add(final FlatLevelCombination element) throws PreferenceException
	{
		final LevelCombination element_level = manager.getLevelInstance(element, null);
		final double element_overalllevel = computeOverallLevel(element);
		if(pruningLevel < element_overalllevel) return;
		final double element_pruningLevel = element_level.getPruningLevel();
		if(element_pruningLevel < pruningLevel) pruningLevel = element_pruningLevel;
		
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
	
	public int computeOverallLevel(final FlatLevelCombination element)
	{
		Integer[] l = new Integer[dimensions];
		int level = 0;
		for(int i = 0; i < dimensions; ++i)
		{
			l[i] = (int) manager.getBasePref(i).level(manager.getAttributeSelector(i).evaluate(element, null, manager.getBasePref(i).getDomainType()), null);
			level += l[i];
		}
		levels.put(element.getIdentifier(), l);
		return level;
	}
	
	
	final private HashMap<Integer,Integer[]> levels = new HashMap<Integer,Integer[]>();
	/*
	@Override
	public int getLevel(final FlatLevelCombination element, int i)
	{
		return levels.get(element.getIdentifier())[i];
	}
	
	@Override
	public int getOverallLevel(final FlatLevelCombination element)
	{
		int level = 0;
		for(int i = 0; i < dimensions; ++i)
			level += getLevel(element,i);
		return level;
		
	}
	*/
	
}
