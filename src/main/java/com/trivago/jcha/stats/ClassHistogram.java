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

package com.trivago.jcha.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ClassHistogram. Currently this is not much more than a wrapper around a Map.
 * It implements a natural ordering based on the {@link #snapshotTimeMillis} timestamp. See {@link #compareTo(ClassHistogram)}
 * for details.
 * 
 * @author cesken
 *
 */
public class ClassHistogram implements Comparable<ClassHistogram>
{
	private static final boolean DEBUGPARSER = false;
	private static AtomicInteger IndexCounter = new AtomicInteger();
	
	// key is class name (as String)
	private Map<String, ClassHistogramEntry> id2entry = new HashMap<>();
	private String description;
	private final int index;
	private Long snapshotTimeMillisFrom = null;
	private Long snapshotTimeMillisTo = null;

	/**
	 * Creates an empty histogram.
	 */
	public ClassHistogram ()
	{
		index = IndexCounter.incrementAndGet();
	}

	/**
	 * Parses a class histogram file (written by jcmd) from a file, and constructs a class histogram instance from it.
	 * 
	 * @param file
	 * @param classFilter 
	 * @throws IOException
	 */
	public ClassHistogram (String file, boolean ignoreKnownDuplicates, Set<String> classFilter) throws IOException
	{
		index = IndexCounter.incrementAndGet();
		setSnapshotTimeMillis(getTimestampFromFile(file));
		FileInputStream fis = new FileInputStream(new File(file));
		init(fis, ignoreKnownDuplicates, classFilter);
	}
	
	/**
	 * Parses a class histogram file (written by jcmd) from an InputStream, and constructs a class histogram instance from it.
	 * 
	 * @param file
	 * @param classFilter 
	 * @throws IOException
	 */
	public ClassHistogram (InputStream is, boolean ignoreKnownDuplicates, Set<String> classFilter) throws IOException
	{
		index = IndexCounter.incrementAndGet();
		init(is, ignoreKnownDuplicates, classFilter);
	}
	
	public void init(InputStream is, boolean ignoreKnownDuplicates, Set<String> classFilter) throws IOException
	{
		ClassHistogram ch = this; // migrated from method to constructor
		try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));)
		{
			boolean useClassFilter = !classFilter.isEmpty();
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
				
				if (useClassFilter && !classFilter.contains(className))
				{
					continue; // class not of interest to user
				}

				
				ClassHistogramEntry che = new ClassHistogramEntry(className, row[1], row[2]);
				
				// Ignore duplicated class histogram entries.See:
				// http://stackoverflow.com/questions/24746998/why-are-the-java-class-histogram-entries-from-jcmd-not-unique
				boolean knownDuplicate = "[[I".equals(className);
				knownDuplicate = knownDuplicate | "GregorSamsa".equals(className); // might need fully-qualified class name here
				
				// possibly warn
				if (ch.containsKey(className))
				{
					if (!knownDuplicate)
					{
						System.err.println("Warning: Duplicated entry:  old: " + ch.get(className) + " | new: " + che);
					}
				}
				
				// Add or skip
				if (knownDuplicate && ignoreKnownDuplicates)
				{
					// skip
				}
				else
				{
					ch.put(className, che);
				}
			}
		}
	}

	
	
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * Sets both from and to
	 * @param snapshotTimeMillis
	 */
	public void setSnapshotTimeMillis(Long snapshotTimeMillis)
	{
		this.snapshotTimeMillisFrom = snapshotTimeMillis;
		this.snapshotTimeMillisTo = snapshotTimeMillis;
	}

	
	public Long getSnapshotTimeMillisFrom()
	{
		return snapshotTimeMillisFrom;
	}

	public void setSnapshotTimeMillisFrom(Long snapshotTimeMillis)
	{
		this.snapshotTimeMillisFrom = snapshotTimeMillis;
	}

	public Long getSnapshotTimeMillisTo()
	{
		return snapshotTimeMillisTo;
	}

	public void setSnapshotTimeMillisTo(Long snapshotTimeMillis)
	{
		this.snapshotTimeMillisTo = snapshotTimeMillis;
	}

	/**
	 * Returns an unique number for this ClassHistogram entry. Instances created latere have a higher
	 * number (a.k.a. auto-increment).
	 *  
	 * @return
	 */
	public int getIndex()
	{
		return index;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (ClassHistogramEntry entry : id2entry.values())
		{
			sb.append(entry).append(System.lineSeparator());
		}
		return sb.toString();
	}

	public void put(String className, ClassHistogramEntry che)
	{
		id2entry.put(className, che);
	}

	public boolean containsKey(String className)
	{
		return id2entry.containsKey(className);
	}

	public ClassHistogramEntry get(String className)
	{
		return id2entry.get(className);
	}

	public Set<String> keySet()
	{
		return id2entry.keySet();
	}

	public Collection<ClassHistogramEntry> values()
	{
		return id2entry.values();
	}

	
	
	private void debugparser(String string)
	{
		if (DEBUGPARSER)
		{
			System.err.println(string);
		}
	}

	/**
	 * Returns a timestamp from the given file. If possible, the creation timestamp will be returned.
	 *  
	 * @param fileName
	 * @return The timestamp in millis since Epoch, or null if timestamp cnnot be determined 
	 */
	private Long getTimestampFromFile(String fileName)
	{
		File f = new File(fileName);
		 long lastModified = f.lastModified();
		 // 0 is either file not found, or cannot read, or ...
		 
		 return (lastModified == 0) ? null : lastModified;
		 // TODO Move to Files.readAttributes(), as it is more flexible (creation timestamp), and more exact in error handling
	}

	@Override
	public boolean equals(Object obj)
	{
		return (this == obj);
	}

	/**
	 * Order by {@link #snapshotTimeMillisFrom}. Fallback: If at least one of the compared timestamps is null,
	 * the comparison is done using {@link #index}.
	 */
	@Override
	public int compareTo(ClassHistogram o)
	{
		boolean timestampsOK =o.getSnapshotTimeMillisFrom() != null && getSnapshotTimeMillisFrom() != null;
		if (timestampsOK)
		{
			int timestampDiff = Long.signum(getSnapshotTimeMillisFrom() - o.getSnapshotTimeMillisFrom());
			if (timestampDiff != 0)
				return timestampDiff;
		}
		
		return o.getIndex() - getIndex();
	}

}
