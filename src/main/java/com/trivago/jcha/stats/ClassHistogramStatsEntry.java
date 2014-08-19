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

import java.util.concurrent.atomic.AtomicInteger;


public class ClassHistogramStatsEntry
{
	static final AtomicInteger indexGenerator = new AtomicInteger();

	final String className;
	final int index;
	long byteDiff;
	int instanceDiff;
	float byteChangePercent;
	float instanceChangePercent;

	public ClassHistogramStatsEntry(String className)
	{
		this.className = className;
		index = indexGenerator.incrementAndGet();
	}

	/**
	 * @return the byteDiff
	 */
	public long getByteDiff()
	{
		return byteDiff;
	}

	/**
	 * @param byteDiff the byteDiff to set
	 */
	public void setByteDiff(long byteDiff)
	{
		this.byteDiff = byteDiff;
	}

	/**
	 * @return the instanceDiff
	 */
	public int getInstanceDiff()
	{
		return instanceDiff;
	}

	/**
	 * @param instanceDiff the instanceDiff to set
	 */
	public void setInstanceDiff(int instanceDiff)
	{
		this.instanceDiff = instanceDiff;
	}

	/**
	 * @return the byteChangePercent
	 */
	public float getByteChangePercent()
	{
		return byteChangePercent;
	}

	/**
	 * @param byteChangePercent the byteChangePercent to set
	 */
	public void setByteChangePercent(float byteChangePercent)
	{
		this.byteChangePercent = byteChangePercent;
	}

	/**
	 * @return the instanceChangePercent
	 */
	public float getInstanceChangePercent()
	{
		return instanceChangePercent;
	}

	/**
	 * @param instanceChangePercent the instanceChangePercent to set
	 */
	public void setInstanceChangePercent(float instanceChangePercent)
	{
		this.instanceChangePercent = instanceChangePercent;
	}

	@Override
	public String toString()
	{
		return String.format("Instances: %+9d [%+4.0f%%] | Bytes %+9d [%+4.0f%%] %s", instanceDiff, instanceChangePercent-100, byteDiff, byteChangePercent-100, className);
	}

	public int getIndex()
	{
		return index;
	}
}
