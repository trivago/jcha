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

import com.trivago.jcha.core.SortStyle;

public class CorrelationFactory
{

	/**
	 * Returns a Correlator for the given SortStyle.
	 *
	 * @param sortStyle
	 * @return
	 */
	public static BaseCorrelator get(SortStyle sortStyle)
	{
		if (sortStyle == null)
			throw new IllegalArgumentException("sortStyle must not be null");
		
		switch (sortStyle)
		{
			case AbsCount:
				return new CorrelatorAbsInstances();
			case AbsSize:
				return new CorrelatorAbsSize();
			case RelCount:
				return new CorrelatorRelInstances();
			case RelSize:
				return new CorrelatorRelSize();
			default:
				throw new IllegalArgumentException("Unsupported sortStyle" + sortStyle);
		}
	}

}
