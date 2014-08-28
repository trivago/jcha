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

package com.trivago.jcha.correlation;

import com.trivago.jcha.stats.ClassHistogramStatsEntry;

public interface Correlator<T extends Number>
{
	/**
	 * Adds the value from the given ClassHistogramStatsEntry. The actual value is
	 * determined by a subclass implementation. Currently the Correlator implementations
	 * require that entries are passed in sorted according to {@link #value(ClassHistogramStatsEntry)}.
	 * @param chs
	 */
	void addValueSorted(ClassHistogramStatsEntry chse);
	
	/**
	 * Returns the implementation specific statistics value
	 * @param chse
	 * @return
	 */
	T value(ClassHistogramStatsEntry chse);
}
