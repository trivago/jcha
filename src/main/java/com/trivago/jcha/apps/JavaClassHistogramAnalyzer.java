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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trivago.jcha.core.HistogramAverager;
import com.trivago.jcha.core.JchaUtil;
import com.trivago.jcha.core.Parameters;
import com.trivago.jcha.correlation.BaseCorrelator;
import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;

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
		param.parseArgs(args, "jcha", 2);
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
		ClassHistogramStats statsFiltered = HistogramAverager.compareFirstWithSecondHalf(histograms, param);
		
		// -3- Feed the Correlator -------------------------------------------
		BaseCorrelator correlator = JchaUtil.correlate(statsFiltered, param);

		
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

	private void warn(String message)
	{
		logger.warn(message);
	}


}
