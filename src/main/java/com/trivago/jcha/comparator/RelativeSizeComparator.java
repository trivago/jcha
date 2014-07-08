package com.trivago.jcha.comparator;

import com.trivago.jcha.ClassHistogramStatsEntry;


/**
 * Sort by byte count descending.
 * 
 * @author cesken
 *
 */
public class RelativeSizeComparator extends BaseComparator
{

	@Override
	public int compare(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2)
	{
		float diff = o2.getByteChangePercent() - o1.getByteChangePercent();
		return compareBase(o1, o2, (int)diff);
	}

}
