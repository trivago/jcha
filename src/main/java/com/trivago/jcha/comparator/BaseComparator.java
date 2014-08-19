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

package com.trivago.jcha.comparator;

import java.util.Comparator;

import com.trivago.jcha.stats.ClassHistogramStatsEntry;



/**
 * Sort by byte count descending.
 * 
 * @author cesken
 *
 */
public abstract class BaseComparator implements Comparator<ClassHistogramStatsEntry>
{
	public int compareBase(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2, int childValue)
	{
		if (childValue != 0)
			return childValue;
		else
			return o2.getIndex() - o1.getIndex();
	}

	public int compareBase(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2, long childValue)
	{
		if (childValue != 0)
		{
			if (childValue > Integer.MAX_VALUE)
				return Integer.MAX_VALUE;
			else if (childValue < Integer.MIN_VALUE)
				return Integer.MIN_VALUE;
			else
				return (int)childValue;
		}
		else
			return o2.getIndex() - o1.getIndex();
	}
}
