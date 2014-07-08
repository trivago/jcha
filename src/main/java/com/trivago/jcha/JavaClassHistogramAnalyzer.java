package com.trivago.jcha;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.trivago.jcha.comparator.AbsoluteInstancesComparator;
import com.trivago.jcha.comparator.AbsoluteSizeComparator;
import com.trivago.jcha.comparator.RelativeInstancesComparator;
import com.trivago.jcha.comparator.RelativeSizeComparator;

public class JavaClassHistogramAnalyzer
{
	enum SortStyle {AbsSize, RelSize, AbsCount, RelCount}


	private static final boolean DEBUGPARSER = false;
	private String file1 = null;
	private String file2 = null;
	private SortStyle sortStyle = SortStyle.AbsCount;
	private int limit = Integer.MAX_VALUE;
	private boolean showIdentical = false;
	
	public static void main(String[] args) throws IOException
	{
		JavaClassHistogramAnalyzer javaClassHistogramAnalyzer = new JavaClassHistogramAnalyzer(args);
		javaClassHistogramAnalyzer.work();
	}

	public JavaClassHistogramAnalyzer(String[] args)
	{
		parseArgs(args);
	
	}
	
	public void parseArgs(String[] args)
	{
		for (int i=0; i<args.length; i++)
		{
			String arg = args[i];
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
			else
			{
				if (file1==null)
				{
					file1 = arg;
				}
				else
				{
					if (file2==null)
						file2 = arg;
					else
						usage(1);
				}
			}
		}
		if (file2==null)
		{
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
		System.out.println("Usage: jcha [-s SortStyle] [-n limit] [-i] file1 files");
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
		ClassHistogram ch1 = loadHistogram(file1);
		ClassHistogram ch2 = loadHistogram(file2);
		
		ClassHistogramStats stats = calculateDiff(ch1, ch2);
		
		ClassHistogramStats stats2 = stats.ignoreHarmless(showIdentical , 0,0);
//		ClassHistogramStats stats2 = stats.ignoreHarmless(showEqual, 100,0);
		
		Comparator comparator = null;
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
				int byteDiff = che2.bytes - che1.bytes;
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
