package com.trivago.jcha;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.trivago.jcha.comparator.AbsoluteInstancesComparator;
import com.trivago.jcha.comparator.AbsoluteSizeComparator;
import com.trivago.jcha.comparator.BaseComparator;
import com.trivago.jcha.comparator.RelativeInstancesComparator;
import com.trivago.jcha.comparator.RelativeSizeComparator;

public class JavaClassHistogramAnalyzer
{
	enum SortStyle {AbsSize, RelSize, AbsCount, RelCount}


	private static final boolean DEBUGPARSER = false;
	private List<String> files = new ArrayList<>();
//	private String file2 = null;
	private SortStyle sortStyle = SortStyle.AbsCount;
	private int limit = Integer.MAX_VALUE;
	private boolean showIdentical = false;
	
	public static void main(String[] args) throws IOException
	{
		System.out.println("main " + args.length);
		JavaClassHistogramAnalyzer javaClassHistogramAnalyzer = new JavaClassHistogramAnalyzer(args);
		javaClassHistogramAnalyzer.work();
	}

	public JavaClassHistogramAnalyzer(String[] args)
	{
		System.out.println("construct " + args.length);
		parseArgs(args);
	
	}
	
	public void parseArgs(String[] args)
	{
		for (int i=0; i<args.length; i++)
		{
			String arg = args[i];
//			System.out.println("parse " + arg);
			if ( arg.equals("-s"))
			{
				// -s = sort style
				ensureOneMoreArg(i, args.length);
				sortStyle = SortStyle.valueOf(args[++i]);
			}
			else if (arg.equals("-n"))
			{
				// -n = number of results (limit)
				ensureOneMoreArg(i, args.length);
				limit  = Integer.parseInt(args[++i]);
			}
			else if (arg.equals("-i"))
			{
				// -i = show identical
				showIdentical = true;
			}
			else if (arg.equals("-h") || arg.equals("--help"))
			{
				// -h = help
				usage(0);
			}
			else
			{
//				System.out.println("arg=" +arg);
				files.add(arg);
			}
		}
		if (files.size() < 2)
		{
			System.out.println("Foo");
			usage(1);
		}
	}

	private void ensureOneMoreArg(int i, int length)
	{
		if (i == length-1)
		{
			// Alread at last argument => no more => argument error
			usage(1);
		}
	}

	void usage()
	{
		usage(null);
	}

	private void usage(Integer exitCode)
	{
		System.out.println("Usage: jcha [-s SortStyle] [-n limit] [-i] file1 file2 [file3 ...]");
		System.out.println("  -i             Show also identical/unchanged classes");
		System.out.println("  -n limit       Limit number of shown classes");
		System.out.println("  -s SortStyle   {" + Arrays.toString(SortStyle.values()) + "} Default=" + sortStyle);
		if (exitCode != null)
		{
			System.exit(exitCode);
		}

	}

	private void work() throws IOException
	{
		int histCountFiles = files.size();
		List<ClassHistogram> histograms = new ArrayList<>(histCountFiles);
		for (int i=0; i< histCountFiles; i++)
		{
			try
			{
				histograms.add(loadHistogram(files.get(i)));
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
		
		ClassHistogramStats stats2 = stats.ignoreHarmless(showIdentical , 0,0);
//		ClassHistogramStats stats2 = stats.ignoreHarmless(showEqual, 100,0);
		
		BaseComparator comparator = null;
		switch (sortStyle)
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
			if (pos == limit)
			{
				break;
			}
		}
		
		
//		System.out.println("CH1: " + ch1);
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

	private ClassHistogram loadHistogram(String file) throws IOException
	{

		ClassHistogram ch = new ClassHistogram();
		try (FileInputStream fis = new FileInputStream(new File(file));
				BufferedReader reader = new BufferedReader(new InputStreamReader(fis));)
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] row = line.trim().split(" +");
				if (row.length != 4)
				{
					debugparser("Ignoring: " + line);
					continue;
				}
				String className = row[3];
				ClasssHistogramEntry che = new ClasssHistogramEntry(className, row[1], row[2]);
				if (ch.containsKey(className))
				{
					System.err.println("Warning: Duplicated entry:");
					System.err.println("Old: " + ch.get(className));
					System.err.println("New: " + che);
					
				}
				ch.put(className, che);
			}

			return ch;
		}
	}

	private void debugparser(String string)
	{
		if (DEBUGPARSER)
		{
			System.err.println(string);
		}
	}
}
