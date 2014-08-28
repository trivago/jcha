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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class ClassHistogramStats
{
	// key is className
	public final Map<String, ClassHistogramStatsEntry> stats = new HashMap<>();

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (Entry<String, ClassHistogramStatsEntry> entry : stats.entrySet())
		{
			sb.append("[").append(entry.getKey());
			sb.append(entry.getValue());
			sb.append("]").append(System.lineSeparator());
		}
		return sb.toString();
	}

	/**
	 * Returns a copy of this object, with the filters given in the arguments applied.
	 * 
	 * @param showEqual
	 * @param minInstanceDiff
	 * @param minByteDiff
	 * @return
	 */
	public ClassHistogramStats ignoreHarmless(boolean showEqual, int minInstanceDiff, int minByteDiff)
	{
		if (minInstanceDiff < 0)
			throw new IllegalArgumentException("minInstanceDiff must be >=0"); 
		if (minByteDiff < 0)
			throw new IllegalArgumentException("minByteDiff must be >=0"); 

		ClassHistogramStats newStats = new ClassHistogramStats();
		for (Entry<String, ClassHistogramStatsEntry> entry : stats.entrySet())
		{
			String key = entry.getKey();
			ClassHistogramStatsEntry chse = entry.getValue();
			if (chse.byteDiff == 0 && chse.instanceDiff == 0)
			{
				if (!showEqual)
				{
					continue;
				}
			}
			
			int instanceDiffAbsolute = Math.abs(chse.instanceDiff);
			if (instanceDiffAbsolute < minInstanceDiff)
			{
				continue;
			}

			long byteDiffAbsolute = Math.abs(chse.byteDiff);
			if (byteDiffAbsolute < minByteDiff)
			{
				continue;
			}

			
			newStats.stats.put(key, chse);
		}
		return newStats;
	}	
}
