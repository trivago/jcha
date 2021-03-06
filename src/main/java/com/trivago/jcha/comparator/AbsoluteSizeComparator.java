package com.trivago.jcha.comparator;

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

import com.trivago.jcha.stats.ClassHistogramStatsEntry;



/**
 * Sort by byte count descending.
 * 
 * @author cesken
 *
 */
public class AbsoluteSizeComparator extends BaseComparator
{

	@Override
	public int compare(ClassHistogramStatsEntry o1, ClassHistogramStatsEntry o2)
	{
		long diff = o2.getByteDiff() - o1.getByteDiff(); 
		return compareBase(o1, o2, diff);
	}

}
