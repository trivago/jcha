package com.trivago.jcha.comparator;

import java.util.Comparator;

import com.trivago.jcha.ClassHistogramStatsEntry;


/**
 * Sort by byte count descending.
 * 
 * @author cesken
 *
 */
public class AbsoluteSizeComparator extends BaseComparator
{

	@Override
	public int compare(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2)
	{
		int diff = o2.getByteDiff() - o1.getByteDiff(); 
		return compareBase(o1, o2, diff);
	}

}
