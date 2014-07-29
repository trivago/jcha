package com.trivago.jcha.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Parameters
{
	
	private List<String> files = new ArrayList<>();
	private SortStyle sortStyle = SortStyle.AbsCount;
	private int limit = Integer.MAX_VALUE;
	private boolean showIdentical = false;
	private boolean ignoreKnownDuplicates = true; // currently always true

	public void parseArgs(String[] args, int requiredHistograms)
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
				limit   = Integer.parseInt(args[++i]);
			}
			else if (arg.equals("-i"))
			{
				// -i = show identical
				showIdentical  = true;
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
		if (files.size() < requiredHistograms)
		{
			System.err.println("Less than " + requiredHistograms + " files specified");
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

	public List<String> getFiles()
	{
		return  files;
	}

	/**
	 * @return the sortStyle
	 */
	public SortStyle getSortStyle()
	{
		return sortStyle;
	}

	/**
	 * @return the limit
	 */
	public int getLimit()
	{
		return limit;
	}

	/**
	 * @return the showIdentical
	 */
	public boolean showIdentical()
	{
		return showIdentical;
	}

	public void setLimit(int limit)
	{
		this.limit = limit;
	}

	public boolean ignoreKnownDuplicates()
	{
		return ignoreKnownDuplicates ;
	}

}
