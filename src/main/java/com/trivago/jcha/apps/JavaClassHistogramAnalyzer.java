package com.trivago.jcha.apps;

/*********************************************************************************
 * Copyright 2014-present trivago GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **********************************************************************************/

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trivago.jcha.comparator.AbsoluteInstancesComparator;
import com.trivago.jcha.comparator.AbsoluteSizeComparator;
import com.trivago.jcha.comparator.BaseComparator;
import com.trivago.jcha.comparator.ComparatorFactory;
import com.trivago.jcha.comparator.RelativeInstancesComparator;
import com.trivago.jcha.comparator.RelativeSizeComparator;
import com.trivago.jcha.core.Parameters;
import com.trivago.jcha.core.SortStyle;
import com.trivago.jcha.correlation.CorrelationFactory;
import com.trivago.jcha.correlation.Correlator;
import com.trivago.jcha.correlation.BaseCorrelator;
import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;
import com.trivago.jcha.stats.ClassHistogramEntry;

public class JavaClassHistogramAnalyzer
{
    static final Logger logger = LogManager.getLogger(JavaClassHistogramAnalyzer.class);
    private Parameters param = new Parameters();
	
	public static void main(String[] args) throws IOException
	{
		JavaClassHistogramAnalyzer jcha = new JavaClassHistogramAnalyzer(args);
		jcha.work();
	}

	public JavaClassHistogramAnalyzer(String[] args)
	{
		param.parseArgs(args, 2);
	}
	

	/**
	 * Read all histograms, and compare the first half with the last half.
	 * 
	 * @throws IOException
	 */
	private void work() throws IOException
	{
		// -1- Read histograms -------------------------------------------------
		List<ClassHistogram> histograms = readHistograms();

		// -2- Calculate average of first and second half -------------------------------------------------
		int histCount = histograms.size();

		final ClassHistogram ch1;
		final ClassHistogram ch2;
		if (histCount < 2)
		{
			System.err.println("Error: Less than 2 histogram files could be parsed. Please check file types and permissions. Cannot continue.");
			ch1 = ch2 = null; // << make javac happy (local vars are final)
			System.exit(1);
		}
		else if (histCount == 2)
		{
			// Trivial case
			ch1 = histograms.get(0);
			ch2 = histograms.get(histograms.size()-1);
		}
		else
		{
			// Less trivial case: Run average on first half and second half
			int midOfArraySkip = histCount %2 == 0 ? 1 : 0; // even number of histograms => skip "mid" in first half 
			int midOfArray = Math.round(histCount/2.0f) - 1; // 3 => 1  ; 4 => 2 ; 5 => 2
			ch1 = buildAverageHistogram(histograms, 0 , midOfArray-midOfArraySkip);
			ch2 = buildAverageHistogram(histograms, midOfArray, histCount-1);
		}
		
		ClassHistogramStats stats = calculateDiff(ch1, ch2);
		ClassHistogramStats statsFiltered = stats.ignoreHarmless(param.showIdentical() , 0,0);
		
		
		// -3- Feed the Correlator -------------------------------------------
		BaseComparator comparator = ComparatorFactory.get(param.getSortStyle());
		BaseCorrelator correlator = CorrelationFactory.get(param.getSortStyle());
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

		
		// -4- Print histograms ----------------------------------------------
		int pos = 0;
		System.out.println(correlator.getGroups().size() + " groups were detected, for simalarity " + param.getMaxGroupingPercentage() + "%");

outer:	for (Entry<String, ArrayList<ClassHistogramStatsEntry>> groupEntry : correlator.getGroups().entrySet())
		{
			String key = groupEntry.getKey();
			ArrayList<ClassHistogramStatsEntry> group = groupEntry.getValue();
			System.out.println("--- Group start --------------------------------------------------------");
			for (ClassHistogramStatsEntry entry : group)
			{
				System.out.println(entry);
				if (pos++ == param.getLimit())
				{
					break outer;
				}
			}
		}
	}

	private List<ClassHistogram> readHistograms()
	{
		List<String> files = param.getFiles();
		int histCountFiles = files.size();
		List<ClassHistogram> histograms = new ArrayList<>(histCountFiles);
		for (int i=0; i< histCountFiles; i++)
		{
			try
			{
				histograms.add(new ClassHistogram(files.get(i), param.ignoreKnownDuplicates(), param.classFilter()));
			}
			catch (Exception exc)
			{
				warn("Ignoring histogram: " + exc.getMessage());
			}
		}
		return histograms;
	}

	/**
	 * Returns the relative change in percent between lastValue and newValue.
	 * It treats the first call (lastValue == null). or if the
	 * relative change cannot be mathematically computed, 
	 * 
	 * @param lastValue
	 * @param newValue
	 * @return
	 */
	private double percentageDiff(Integer lastValue, int newValue)
	{
		if (lastValue == null)
			return 0;
		
		if (newValue == 0)
		{
			// Special case, to avoid division by zero
			if (lastValue == 0)
				return 0;
			else
				return 100; // changed from something to 0 => 100% change
		}

//		double d = (lastValue* 100.0D /newValue );
//		double d2 = d -100;
//		System.out.println("l=" + lastValue + ", n="+ newValue + " => " + d2);
		
		lastValue = Math.abs(lastValue);
		newValue  = Math.abs(newValue);
		
		if (lastValue > newValue)
			return (lastValue* 100.0D /newValue )-100;
		else
			return (newValue * 100.0D /lastValue)-100;
	}

	private ClassHistogram buildAverageHistogram(List<ClassHistogram> histograms, int startIndex, int endIndex)
	{
		int count = endIndex - startIndex + 1;
		ClassHistogram combined = new ClassHistogram();
		combined.setDescription("Averaged class histogram, built from " + count + " histograms.");
		for (int i=startIndex; i<= endIndex; i++)
		{
			ClassHistogram ch = histograms.get(i);
			for (ClassHistogramEntry che : ch.values())
			{
				String key = che.className;
				ClassHistogramEntry cheCombined = combined.get(key);
				if (cheCombined == null)
				{
					cheCombined = che.clone();
					combined.put(key, cheCombined);
				}
				else
				{
					cheCombined.instances += che.instances;
					cheCombined.bytes += che.bytes;
				}
			}
		}
		
		for (ClassHistogramEntry che : combined.values())
		{
			che.instances = Math.round(che.instances / (float)count); // Rounding up: 1/2 => 1
			che.bytes = Math.round(che.bytes / (float)count);
		}

		return combined;
	}

	private void warn(String message)
	{
		logger.warn(message);
	}

	private ClassHistogramStats calculateDiff(ClassHistogram ch1, ClassHistogram ch2)
	{
		ClassHistogramStats stats = new ClassHistogramStats();
		for (ClassHistogramEntry che1 : ch1.values())
		{
			String key = che1.className;
			ClassHistogramEntry che2 = ch2.get(key);
			if (che2 == null)
			{
				errorcollector("Class disappeared in second file: " + key);
			}
			else
			{
				ClassHistogramStatsEntry chse = new ClassHistogramStatsEntry(key);
				long byteDiff = che2.bytes - che1.bytes;
				float byteChangePercent = 100f * che2.bytes / (float)che1.bytes;
				chse.setByteDiff(byteDiff);
				chse.setByteChangePercent(byteChangePercent);

				int instanceDiff = che2.instances - che1.instances;
				float instanceChangePercent = 100f * che2.instances / (float)che1.instances;
				chse.setInstanceDiff(instanceDiff);
				chse.setInstanceChangePercent(instanceChangePercent);
			
				stats.stats.put(key, chse);
			}
		}
			
		return stats;
	}

	private void errorcollector(String string)
	{
//		System.err.println(string);		
	}

}
