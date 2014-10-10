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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.trivago.jcha.stats.ClassHistogramStatsEntry;

public abstract class BaseCorrelator implements Correlator<Number>
{
	Number lastValue = null;
	private Map<String, ArrayList<ClassHistogramStatsEntry>> groups = new LinkedHashMap<>();
	ArrayList<ClassHistogramStatsEntry> currentGroup = null;
	
	int similarValueCount = 0;
	double maxGroupingPercentage = 0;
	
	@Override
	public void addValueSorted(ClassHistogramStatsEntry chse)
	{
		Number newValue = value(chse);
		double percentageDiffFromGroupStart = percentageDiff(lastValue, newValue);
		
		if (lastValue == null)
		{
			// Case 1) First loop iteration: Always starts a possible new group			
			addGroup(chse, newValue);

			lastValue = newValue;
			similarValueCount = 1;
		}
		else if (percentageDiffFromGroupStart < getMaxGroupingPercentage())
		{
			// Case 2) Difference small: Add to current group
			similarValueCount ++;
			currentGroup.add(chse);
		}
		else
		{
			// Case 2) Difference big: Start new group
			addGroup(chse, newValue);
			
			// reset stats
			lastValue = newValue;
			similarValueCount = 1; // current entry is start of possible new group
		}

	}


	private void addGroup(ClassHistogramStatsEntry chse, Number newValue)
	{
		String groupName = newValue.toString();
		currentGroup = new ArrayList<>();
		currentGroup.add(chse);
		getGroups().put(groupName, currentGroup);
	}
	
	/**
	 * Returns the relative change in percent between lastValue and newValue.
	 * It treats the first call (lastValue == null). or if the
	 * relative change cannot be mathematically computed, 
	 * 
	 * @param lastValue
	 * @param newValue
	 * @return
	 */
	private double percentageDiff(Number lastValue, Number newValue)
	{
		if (lastValue == null)
			return 0;
		
		if (newValue.equals(0))
		{
			// Special case, to avoid division by zero
			if (lastValue.equals(0))
				return 0;
			else
				return 100; // changed from something to 0 => 100% change
		}

//		double d = (lastValue* 100.0D /newValue );
//		double d2 = d -100;
//		System.out.println("l=" + lastValue + ", n="+ newValue + " => " + d2);
		
		// convert Number to double, so we can do things like abs() or "<"
		double lastD = Math.abs(lastValue.doubleValue());
		double newD  = Math.abs(newValue.doubleValue());
		lastValue = lastD;
		newValue = newD;
		
		if (lastD > newD)
			return (lastD * 100.0D / newD )-100;
		else
			return (newD  * 100.0D / lastD)-100;
	}


	public double getMaxGroupingPercentage()
	{
		return maxGroupingPercentage;
	}


	/**
	 * Sets the percentage for building groups. Values will be treated in the
	 * same group if they do not differ more than the given percentage. The default,
	 * if this method is not given, is 0% (no difference).
	 * 
	 * @param maxGroupingPercentage
	 */
	public void setMaxGroupingPercentage(double maxGroupingPercentage)
	{
		this.maxGroupingPercentage = maxGroupingPercentage;
	}


	/**
	 * Returns a Map with the groups
	 * @return
	 */
	public Map<String, ArrayList<ClassHistogramStatsEntry>> getGroups()
	{
		return groups;
	}

}
