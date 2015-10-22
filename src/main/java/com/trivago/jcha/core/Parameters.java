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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parameters for the jcha applications. The values all have a sensible default value.
 * The defaults can be overriden by the user by command line parameters, see {@link #parseArgs(String[], int)}.
 * Also the application sometimes overrides the default by automatically determining a dynamic value from
 * other parameters or the read histograms, for example see {@link #overrideClassFilter(Set)}.
 *  
 * @author cesken
 *
 */
public class Parameters
{
    static final Logger logger = LogManager.getLogger(Parameters.class);

	private List<String> files = new ArrayList<>();
	private SortStyle sortStyle = SortStyle.AbsCount;
	private DataPointType dataPointType = DataPointType.FirstDerivation; 
	private int limit = Integer.MAX_VALUE;
	private boolean showIdentical = false;
	private boolean ignoreKnownDuplicates = true; // currently always true
	
	private Set<String> classFilter = new HashSet<>();
	public final static double MaxGroupingPercentageDefault = 0.5;
	private double maxGroupingPercentage = MaxGroupingPercentageDefault;

	private static final int UpdateIntervalDefault = 300;
	public int updateIntervalSecs = UpdateIntervalDefault;

	private String jmxAddress = null;

	private CaptureStyle captureStyle = CaptureStyle.Raw;

	private boolean quietMode = false;

	enum ParameterMode { Automatic, Histogram, ClassName }
	
	/**
	 * Sets the fields from the given (command line) arguments. The arguments also contain the
	 * name(s) of the histogram file(s). If the arguments are invalid, this method will terminate the application
	 * via {@link System#exit()} and not return. Giving less than requiredHistograms files is considered as 
	 * invalid arguments. 
	 *  
	 * 
	 * @param args
	 * @param executableName The name how the program is normally called (used for --help only)
	 * @param requiredHistograms
	 */
	public void parseArgs(String[] args, String executableName, int requiredHistograms)
	{
		ParameterMode parameterMode = ParameterMode.Automatic;
		
		for (int i=0; i<args.length; i++)
		{
			String arg = args[i];
			boolean autoParsed = false;
			if (parameterMode == ParameterMode.Automatic)
			{
				autoParsed = true;
				try
				{
					switch (arg)
					{
						case "-s":
							// -s = sort style
							sortStyle = SortStyle.valueOf(nextArg(++i, args));
							break;
						case "-n":
							// -n = number of results (limit)
							limit   = Integer.parseInt(nextArg(++i, args));
							break;
						case "-i":
							// -i = show identical
							showIdentical  = true;
							break;
						case "-g":
							// -g = grouping percentage
							maxGroupingPercentage = Double.parseDouble(nextArg(++i, args));
							break;
						case "--jmx":
							// --jmx host:port = Show live view from MBean via JMX (Java 8)
							jmxAddress  = nextArg(++i, args);
							// If we capture live via JMX, we do not need any histograms upfront
							requiredHistograms = 0;
							break;
						case "-F":
							// -F = format
							captureStyle = CaptureStyle.valueOf(nextArg(++i, args));
							break;							
						case "-U":
							// -U = update interval for live view
							updateIntervalSecs = Integer.parseInt(nextArg(++i, args));
							break;
						case "-H":
						case "--histograms":
							parameterMode = ParameterMode.Histogram;
							break;
						case "-A":
							dataPointType = DataPointType.Absolute;
							break;
						case "-C":
						case "--classes":
							parameterMode = ParameterMode.ClassName;
							break;
						case "-q":
							// -q = quiet mode
							quietMode = true;
							break;
						case "-h":
						case "--help":
							// -h = help
							usage(0, executableName);
							break;
						case "-v":
						case "--version":
							// -v = version
							String ver;
							if (executableName.equals("jcha-gui"))
								ver = Version.JCHAGUI.versionString();
							else
								ver = Version.JCHA.versionString();
							System.out.println(executableName + " " + ver);
							System.exit(0);
							
						default:
							autoParsed = false;
								
					}
				}
				catch (NumberFormatException nfe)
				{
					System.err.println("Error: Argument for " + arg + " must be numeric");
					System.exit(1);
				}
				catch (IllegalArgumentException iae)
				{
					System.err.println("Error while parsing parameter " + arg + ": " + iae.getMessage());
					System.exit(1);
				}
				catch (Exception exc)
				{
					System.err.println("Error: Parsing for argument " + arg + " failed: " + exc);
					System.exit(1);					
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
			usage(1, executableName);
		}
	}

	/**
	 * Returns the next argument from the command line, or throws IllegalArgumentException
	 * if there is no more argument left.
	 * @param i
	 * @param args
	 * @return
	 */
	private String nextArg(int i, String[] args)
	{
		int length = args.length;
		if (i == length)
		{
			// Already at last argument => no more => argument error
			throw new IllegalArgumentException("Argument missing");
		}
		return args[i];
	}

	private void usage(Integer exitCode, String executableName)
	{
		System.out.println(executableName + " " + Version.JCHA.versionString());
		System.out.println("Usage: " + executableName + " [-s SortStyle] [-n limit] [-i] file1 file2 [file3 ...] [-C class1 ...]");
		System.out.println("  -i                 Show also identical/unchanged classes");
		System.out.println("  -n limit           Limit number of shown classes");
		System.out.println("  -s SortStyle   {" + Arrays.toString(SortStyle.values()) + "} Default=" + sortStyle);
		System.out.println("  -A                 Use raw/absoulte values (only supported in jcha-gui)");
		System.out.println("  -g percent         Mark classes differing less than given percentage (according to SortStyle) as a group (default: " + MaxGroupingPercentageDefault +"). 0=no grouping." );
		System.out.println("  --jmx host:port    Show live view from MBean via JMX (Java 8 servers)");
		System.out.println("  -F format          Output format for capture { " + Arrays.toString(CaptureStyle.values()) + "} Default=" + getCaptureStyle() );
		System.out.println("  -U interval        Update interval in seconds for live view (default: " + UpdateIntervalDefault + "s)");
		System.out.println("  -q                 Enable quiet mode. Supresses output to STDOUT except for the histogram output");
		System.out.println("  -v | -- version    Show version information");
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

	public DataPointType getDataPointType()
	{
		return dataPointType;
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

	public double getMaxGroupingPercentage()
	{
		return maxGroupingPercentage;
	}

	/**
	 * @return the updateIntervalSecs
	 */
	public int getUpdateIntervalSecs()
	{
		return updateIntervalSecs;
	}

	/**
	 * Allows to override the classFilter. Usable when the classFilter is automatically calculated.
	 * @param classFilter
	 */
	public void overrideClassFilter(Set<String> classFilter)
	{
		this.classFilter = classFilter;
	}

	/**
	 * Returns the address of a JMX enabled MBean server.
	 * The format is host:port.
	 *  
	 * @return
	 */
	public String getJmxAddress()
	{
		return jmxAddress;
	}

	public CaptureStyle getCaptureStyle()
	{
		return captureStyle;
	}

	public boolean quietMode()
	{
		return quietMode;
	}

}
