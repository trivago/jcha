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
import java.util.Set;
import java.util.TreeSet;

import com.trivago.jcha.comparator.AbsoluteInstancesComparator;
import com.trivago.jcha.comparator.AbsoluteSizeComparator;
import com.trivago.jcha.comparator.BaseComparator;
import com.trivago.jcha.comparator.RelativeInstancesComparator;
import com.trivago.jcha.comparator.RelativeSizeComparator;
import com.trivago.jcha.core.Parameters;
import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;
import com.trivago.jcha.stats.ClasssHistogramEntry;

public class JavaClassHistogramAnalyzer
{
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
	

	private void work() throws IOException
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

		// Any invalid
		int histCount = histograms.size();

		final ClassHistogram ch1;
		final ClassHistogram ch2;
		if (histCount == 2)
		{
			ch1 = histograms.get(0);
			ch2 = histograms.get(histograms.size()-1);
		}
		else
		{
			int midOfArray = Math.round(histCount/2.0f) - 1; // 3 => 1  ; 4 => 2 ; 5 => 2
			ch1 = buildAverageHistogram(histograms, 0 , midOfArray);
			ch2 = buildAverageHistogram(histograms, midOfArray , histCount-1);
		}
		
		ClassHistogramStats stats = calculateDiff(ch1, ch2);
		
		ClassHistogramStats stats2 = stats.ignoreHarmless(param.showIdentical() , 0,0);
//		ClassHistogramStats stats2 = stats.ignoreHarmless(showEqual, 100,0);
		
		BaseComparator comparator = null;
		switch (param.getSortStyle())
		{
			case AbsCount: comparator = new AbsoluteInstancesComparator(); break;
			case RelCount: comparator = new RelativeInstancesComparator(); break;
			case AbsSize: comparator = new AbsoluteSizeComparator(); break;
			case RelSize: comparator = new RelativeSizeComparator(); break;
		}
		Set<ClassHistogramStatsEntry> statsSorted = new TreeSet<>(comparator);
		for (ClassHistogramStatsEntry entry : stats2.stats.values())
		{
			statsSorted.add(entry);
		}
		
		int pos = 0;
		for (ClassHistogramStatsEntry entry : statsSorted)
		{
			System.out.println(entry);
			pos++;
			if (pos == param.getLimit())
			{
				break;
			}
		}
	}

	private ClassHistogram buildAverageHistogram(List<ClassHistogram> histograms, int startIndex, int endIndex)
	{
		int count = endIndex - startIndex + 1;
		ClassHistogram combined = new ClassHistogram();
		combined.setDescription("Averaged class histogram, built from " + count + " histograms.");
		for (int i=startIndex; i<= endIndex; i++)
		{
			ClassHistogram ch = histograms.get(i);
			for (ClasssHistogramEntry che : ch.values())
			{
				String key = che.className;
				ClasssHistogramEntry cheCombined = combined.get(key);
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
		
		for (ClasssHistogramEntry che : combined.values())
		{
			che.instances = Math.round(che.instances / (float)count); // Rounding up: 1/2 => 1
			che.bytes = Math.round(che.bytes / (float)count);
		}

		return combined;
	}

	private void warn(String message)
	{
		System.err.println(message);
		// TODO Migrate to logger
	}

	private ClassHistogramStats calculateDiff(ClassHistogram ch1, ClassHistogram ch2)
	{
		ClassHistogramStats stats = new ClassHistogramStats();
		for (ClasssHistogramEntry che1 : ch1.values())
		{
			String key = che1.className;
			ClasssHistogramEntry che2 = ch2.get(key);
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
