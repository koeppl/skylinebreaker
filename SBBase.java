package skylinebreaker;

import flatlc.levels.FlatLevelCombination;

public abstract class SBBase {


	public static final class NearestNeighborOfZero
	{
		public FlatLevelCombination neighbor = null;
		public int norm = Integer.MAX_VALUE;
		public int threadCount = 0;
	}
	
	 public static FlatLevelCombination[] combineLocalSkyline(final FlatLevelCombination[] a, final FlatLevelCombination[] b) 
		{
			int areserved = a.length;
			int breserved = b.length;
			
			for(int i = 0; i < a.length && i < areserved; ++i)
			{
				if(b.length == 0) break;
				aLoop:
				for(int j = 0; j < b.length && j < breserved; ++j)
				{
					bLoop:
					switch(a[i].compare(b[j])) {
					case FlatLevelCombination.LESS: 
						a[i--] = a[--areserved];
						break aLoop;
					case FlatLevelCombination.GREATER: 
						b[j--] = b[--breserved];
						break bLoop;
					}
				}
			}
			final FlatLevelCombination[] c = new FlatLevelCombination[areserved + breserved];
			for(int i = 0; i < areserved; ++i) c[i] = a[i];
			for(int i = 0; i < breserved; ++i) c[i+areserved] = b[i];
			return c;
		}
}
