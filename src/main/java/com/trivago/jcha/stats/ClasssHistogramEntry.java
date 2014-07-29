package com.trivago.jcha.stats;


public class ClasssHistogramEntry implements Cloneable
{
	public final String className;
	public int instances;
	public long bytes;
	
	public ClasssHistogramEntry(String className, int instances, int bytes)
	{
		this.className = className;
		this.instances = instances;
		this.bytes = bytes;
	}

	public ClasssHistogramEntry(String className, String instances, String bytes)
	{
		this.className = className;
		this.instances = Integer.parseInt(instances);
		this.bytes = Integer.parseInt(bytes);
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(className);
		builder.append(": instances=");
		builder.append(instances);
		builder.append(", bytes=");
		builder.append(bytes);
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Creates a "deep" copy.
	 */
	@Override
	public ClasssHistogramEntry clone()
	{
		try
		{
			return (ClasssHistogramEntry)super.clone();
		}
		catch (CloneNotSupportedException exc)
		{
			throw new RuntimeException(exc); // impossible
		}
	}
	
}
