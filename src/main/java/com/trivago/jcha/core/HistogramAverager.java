package com.trivago.jcha.core;

import java.util.List;

import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClassHistogramEntry;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;

public class HistogramAverager
{
	/**
	 * Builds ClassHistogramStats by comparing the first half of the given histograms with the second half.
	 * <p>
	 * Implementation: From each half an average Histogram is built. These two histograms are then 
	 * 
	 * @param histograms
	 * @param param
	 * @return
	 */
	public static ClassHistogramStats compareFirstWithSecondHalf(List<ClassHistogram> histograms, Parameters param)
	{
		ClassHistogramStats statsFiltered;
		final ClassHistogram ch1;
		final ClassHistogram ch2;
		int histCount = histograms.size();
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
		statsFiltered = stats.ignoreHarmless(param.showIdentical() , 0,0);
		return statsFiltered;
	}

	/**
	 * Builds averages on the part of the histograms from startIndex to endIndex
	 * @param histograms
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private static ClassHistogram buildAverageHistogram(List<ClassHistogram> histograms, int startIndex, int endIndex)
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

	/**
	 * Compares two ClassHistogram objects, and returns the result as ClassHistogramStats.
	 * Only classes appearing in both histograms are taken into account - that means classes
	 * that got loaded or unloaded between the two histograms are ignored.
	 * 
	 * @param ch1
	 * @param ch2
	 * @return
	 */
	private static ClassHistogramStats calculateDiff(ClassHistogram ch1, ClassHistogram ch2)
	{
		ClassHistogramStats stats = new ClassHistogramStats();
		for (ClassHistogramEntry che1 : ch1.values())
		{
			String key = che1.className;
			ClassHistogramEntry che2 = ch2.get(key);
			if (che2 == null)
			{
				// This case can definitely happen, e.g. it is normal if a class gets unloaded.
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

	private static void errorcollector(String string)
	{
//		System.err.println(string);		
	}

}
