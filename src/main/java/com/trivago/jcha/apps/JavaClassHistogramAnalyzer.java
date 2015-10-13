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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trivago.jcha.core.CaptureStyle;
import com.trivago.jcha.core.HistogramAverager;
import com.trivago.jcha.core.JchaUtil;
import com.trivago.jcha.core.Parameters;
import com.trivago.jcha.correlation.BaseCorrelator;
import com.trivago.jcha.remote.DiagnosticMBean;
import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClassHistogramEntry;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;

public class JavaClassHistogramAnalyzer
{
    static final Logger logger = LogManager.getLogger(JavaClassHistogramAnalyzer.class);
    private Parameters param = new Parameters();
	
	public static void main(String[] args) throws IOException
	{
		JavaClassHistogramAnalyzer jcha = new JavaClassHistogramAnalyzer(args);
		if (jcha.param.getJmxAddress() == null)
			jcha.work();
		else
			jcha.showLive();
		
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
		SortedSet<ClassHistogram> histograms = readHistograms();

		// -2- Calculate average of first and second half -------------------------------------------------
		ClassHistogramStats statsFiltered = HistogramAverager.compareFirstWithSecondHalf(histograms, param);
		
		// -3- Feed the Correlator -------------------------------------------
		BaseCorrelator correlator = JchaUtil.correlate(statsFiltered, param);

		
		// -4- Print histograms ----------------------------------------------
		int pos = 0;
		// -4a- Header
		Long fromMillis = statsFiltered.getSnapshotTimeMillisFrom();
		Long toMillis = statsFiltered.getSnapshotTimeMillisTo();
		if (fromMillis !=null && toMillis != null)
		{
			System.out.println("Time-range: " + new Date(fromMillis) + " - " + new Date(toMillis));
		}
		else
		{
			System.out.println("Time-range: " + fromMillis + " - " + toMillis);			
		}
		System.out.println(correlator.getGroups().size() + " groups were detected, for simalarity " + param.getMaxGroupingPercentage() + "%");

		// -4b- Rows
outer:	for (Entry<String, ArrayList<ClassHistogramStatsEntry>> groupEntry : correlator.getGroups().entrySet())
		{
//			String key = groupEntry.getKey();
			ArrayList<ClassHistogramStatsEntry> group = groupEntry.getValue();
			boolean showGroupHeaders = group.size() > 1; 
			if (showGroupHeaders)
			{
				System.out.println("--- Group start --------------------------------------------------------");
			}
			for (ClassHistogramStatsEntry entry : group)
			{
				if (showGroupHeaders)
					System.out.print("> "); // group marker
				else
					System.out.print("  "); // no group
				
				System.out.println(entry);
				
				if (pos++ == param.getLimit())
				{
					break outer;
				}
			}
			if (showGroupHeaders)
			{
				System.out.println("--- Group end ----------------------------------------------------------");
			}
		}
	}

	private void showLive()
	{
		DiagnosticMBean mbean = new DiagnosticMBean(param.getJmxAddress());
		mbean.connect();

		// Run until the user quits via CTRL-c on the shell
		int MAX_ERRORS = 10;
		int errorCount = 0;
		while (true)
		{
			CaptureStyle captureStyle = null;
			try
			{
				String histo = mbean.readHistogram();
				InputStream is = new ByteArrayInputStream(histo.getBytes(StandardCharsets.UTF_8));
				captureStyle = param.getCaptureStyle();
				switch (captureStyle)
				{
					case Cooked:
						ClassHistogram ch = new ClassHistogram(is, param.ignoreKnownDuplicates(), param.classFilter());
						ch.setSnapshotTimeMillis(System.currentTimeMillis());
						int line = 0;
						for (ClassHistogramEntry entry : ch.values())
						{
							//
							System.out.println(entry);
							if (line++ == param.getLimit())
							{
								break;
							}
						}
						break;

					case Raw:
						// Note: Raw does not support param.getLimit()), as it makes only much sense when it operates on sorted (cooked) data.
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
						String outfileName = param.getJmxAddress() + "." + sdf.format(new Date()) + ".jcha";
						File outfile = new File(outfileName);
						OutputStream os = new FileOutputStream(outfile);
					    byte[] buffer = new byte[1024];
					    int bytesRead;
					    while ((bytesRead = is.read(buffer)) != -1)
					    {
					        os.write(buffer, 0, bytesRead);
					    }
					    os.close();
					    is.close();
					    break;
					    
					default:
						throw new IllegalArgumentException("Unsupported CaptureStyle=" + captureStyle);

				} // switch
			}
			catch (Exception e)
			{
				System.err.println("Reading histogram failed. Output format= " + captureStyle + ": " + e);
				errorCount++;
			}
			if (errorCount == MAX_ERRORS)
			{
				System.err.println("Abort reading histograms, due to many errors: errorCount=" + errorCount);
				break;
			}
			else
			{
				try
				{
					Thread.sleep(1000 * param.getUpdateIntervalSecs());
				}
				catch (InterruptedException e)
				{
					// ignore
				}
			}
		}

	}

	private SortedSet<ClassHistogram> readHistograms()
	{
		List<String> files = param.getFiles();
		int histCountFiles = files.size();
		SortedSet<ClassHistogram> histograms = new TreeSet<>();
		for (int i=0; i< histCountFiles; i++)
		{
			try
			{
				String fileName = files.get(i);
				ClassHistogram ch = new ClassHistogram(fileName, param.ignoreKnownDuplicates(), param.classFilter());
				histograms.add(ch);
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
		logger.error(message); // temporarily move to error status
	}


}
