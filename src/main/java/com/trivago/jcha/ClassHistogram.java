package com.trivago.jcha;

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
	// key is class name (as String)
	private Map<String, ClasssHistogramEntry> id2entry = new HashMap<>();

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
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

	
}
