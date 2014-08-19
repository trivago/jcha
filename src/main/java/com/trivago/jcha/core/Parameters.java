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

package com.trivago.jcha.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Parameters
{
	
	private List<String> files = new ArrayList<>();
	private SortStyle sortStyle = SortStyle.AbsCount;
	private int limit = Integer.MAX_VALUE;
	private boolean showIdentical = false;
	private boolean ignoreKnownDuplicates = true; // currently always true
	
	private Set<String> classFilter = new HashSet<>();

	enum ParameterMode { Automatic, Histogram, ClassName }
	
	/**
	 * Sets the fields from the given (command line) arguments. The arguments also contain the
	 * name(s) of the histogram file(s). If the arguments are invalid, this method will terminate the application
	 * via {@link System#exit()} and not return. Giving less than requiredHistograms files is considered as 
	 * invalid arguments. 
	 *  
	 * 
	 * @param args
	 * @param requiredHistograms
	 */
	public void parseArgs(String[] args, int requiredHistograms)
	{
		ParameterMode parameterMode = ParameterMode.Automatic;
		
		for (int i=0; i<args.length; i++)
		{
			String arg = args[i];
			boolean autoParsed = false;
			if (parameterMode == ParameterMode.Automatic)
			{
				autoParsed = true;
	//			System.out.println("parse " + arg);
				switch (arg)
				{
					case "-s":
						// -s = sort style
						ensureOneMoreArg(i, args.length);
						sortStyle = SortStyle.valueOf(args[++i]);
						break;
					case "-n":
						// -n = number of results (limit)
						ensureOneMoreArg(i, args.length);
						limit   = Integer.parseInt(args[++i]);
						break;
					case "-i":
						// -i = show identical
						showIdentical  = true;
						break;
					case "-H":
					case "--histograms":
						parameterMode = ParameterMode.Histogram;
						break;
					case "-C":
					case "--classes":
						parameterMode = ParameterMode.ClassName;
						break;
					case "-h":
					case "--help":
						// -h = help
						usage(0);
						break;
						
					default:
						autoParsed = false;
							
				}
			}
			if (!autoParsed)
			{
				if (parameterMode == ParameterMode.ClassName)
					classFilter.add(arg);
				else
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
			// Already at last argument => no more => argument error
			usage(1);
		}
	}

	void usage()
	{
		usage(null);
	}

	private void usage(Integer exitCode)
	{
		System.out.println("Usage: jcha [-s SortStyle] [-n limit] [-i] file1 file2 [file3 ...] [-C class1 ...]");
		System.out.println("  -i             Show also identical/unchanged classes");
		System.out.println("  -n limit       Limit number of shown classes");
		System.out.println("  -s SortStyle   {" + Arrays.toString(SortStyle.values()) + "} Default=" + sortStyle);
		System.out.println("  -H | --histograms  All following parameters are treated as histogram file names (*)" );
		System.out.println("  -C | --classes     All following parameters are treated as fully qualified class names (whitelist) (*)" );
		System.out.println("(*) Arguments -C and -H are exclusive - using both is not possible" );
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
	
	public boolean isClassAcceptable(String className)
	{
		if (classFilter.isEmpty())
		{
			// If user has given no classes, then do not filter out any
			return true;
		}
		
		return classFilter.contains(className);
	}

	public Set<String> classFilter()
	{
		return classFilter;
	}

}
