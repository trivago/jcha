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

}
