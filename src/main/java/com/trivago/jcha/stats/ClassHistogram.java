package com.trivago.jcha.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * ClassHistograam. Currently this is not much more than a wrapper around a Map.
 * @author cesken
 *
 */
public class ClassHistogram
{
	private static final boolean DEBUGPARSER = false;

	// key is class name (as String)
	private Map<String, ClasssHistogramEntry> id2entry = new HashMap<>();
	private String description;

	/**
	 * Creates an empty histogram.
	 */
	public ClassHistogram ()
	{
		
	}

	/**
	 * Parses a class histogram file (wirteen by jcmd), and constructs a class histogram instance from it.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public ClassHistogram (String file, boolean ignoreKnownDuplicates) throws IOException
	{
		ClassHistogram ch = this; // migrated from method to constructor
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
				
				// Ignore duplicated class histogram entries.See:
				// http://stackoverflow.com/questions/24746998/why-are-the-java-class-histogram-entries-from-jcmd-not-unique
				boolean knownDuplicate = "[[I".equals(className);
				knownDuplicate = knownDuplicate | "GregorSamsa".equals(className); // might need fully-qualified class name here
				
				if (ch.containsKey(className))
				{
					if (!knownDuplicate)
					{
						System.err.println("Warning: Duplicated entry:  old: " + ch.get(className) + " | new: " + che);
					}
				}
				if (knownDuplicate && ignoreKnownDuplicates)
				{
					// skip
				}
				else
				{
					ch.put(className, che);
				}
			}

//			return ch;
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

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (ClasssHistogramEntry entry : id2entry.values())
		{
			sb.append(entry).append(System.lineSeparator());
		}
		return sb.toString();
	}

	public void put(String className, ClasssHistogramEntry che)
	{
		id2entry.put(className, che);
	}

	public boolean containsKey(String className)
	{
		return id2entry.containsKey(className);
	}

	public ClasssHistogramEntry get(String className)
	{
		return id2entry.get(className);
	}

	public Set<String> keySet()
	{
		return id2entry.keySet();
	}

	public Collection<ClasssHistogramEntry> values()
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

}
