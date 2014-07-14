package com.trivago.jcha.comparator;

import java.util.Comparator;

import com.trivago.jcha.ClassHistogramStatsEntry;


/**
 * Sort by byte count descending.
 * 
 * @author cesken
 *
 */
public abstract class BaseComparator implements Comparator<ClassHistogramStatsEntry>
{
	public int compareBase(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2, int childValue)
	{
		if (childValue != 0)
			return childValue;
		else
			return o2.getIndex() - o1.getIndex();
	}

	public int compareBase(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2, long childValue)
	{
		if (childValue != 0)
		{
			if (childValue > Integer.MAX_VALUE)
				return Integer.MAX_VALUE;
			else if (childValue < Integer.MIN_VALUE)
				return Integer.MIN_VALUE;
			else
				return (int)childValue;
		}
		else
			return o2.getIndex() - o1.getIndex();
	}
}
