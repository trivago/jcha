package com.trivago.jcha.comparator;

import com.trivago.jcha.ClassHistogramStatsEntry;


/**
 * Sort by byte count descending.
 * 
 * @author cesken
 *
 */
public class RelativeInstancesComparator extends BaseComparator
{

	@Override
	public int compare(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2)
	{
		float diff = o2.getInstanceChangePercent() - o1.getInstanceChangePercent();
		return compareBase(o1, o2, (int)diff);
	}

}
