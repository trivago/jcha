package com.trivago.jcha.core;

import java.util.Set;
import java.util.TreeSet;

import com.trivago.jcha.comparator.BaseComparator;
import com.trivago.jcha.comparator.ComparatorFactory;
import com.trivago.jcha.correlation.BaseCorrelator;
import com.trivago.jcha.correlation.CorrelationFactory;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;

public class JchaUtil
{
	/**
	 * 
	 * @param statsFiltered
	 * @return
	 */
	public static BaseCorrelator correlate(ClassHistogramStats statsFiltered, Parameters param)
	{
		BaseCorrelator correlator;
		BaseComparator comparator = ComparatorFactory.get(param.getSortStyle());
		correlator = CorrelationFactory.get(param.getSortStyle());
		correlator.setMaxGroupingPercentage(param.getMaxGroupingPercentage());
		
		// Correlators currently need to be fed with sorted data ==> sort the data
		Set<ClassHistogramStatsEntry> statsSorted = new TreeSet<>(comparator);
		for (ClassHistogramStatsEntry entry : statsFiltered.stats.values())
		{
			statsSorted.add(entry);
		}
		for (ClassHistogramStatsEntry entry : statsSorted)
		{
			correlator.addValueSorted(entry);
		}
		return correlator;
	}
	
}
