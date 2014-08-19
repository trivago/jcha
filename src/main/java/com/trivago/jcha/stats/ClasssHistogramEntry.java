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
		builder.append("[ ");
		builder.append(className);
		builder.append(": instances=");
		builder.append(instances);
		builder.append(", bytes=");
		builder.append(bytes);
		builder.append(" ]");
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
