package com.trivago.jcha.apps;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;

import com.trivago.jcha.core.HistogramAverager;
import com.trivago.jcha.core.JchaUtil;
import com.trivago.jcha.correlation.BaseCorrelator;
import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClassHistogramEntry;
import com.trivago.jcha.stats.ClassHistogramStats;
import com.trivago.jcha.stats.ClassHistogramStatsEntry;

public class App extends Application
{
	com.trivago.jcha.core.Parameters param = new com.trivago.jcha.core.Parameters();
	static String[] args;
	
	@Override
	public void start(Stage stage)
	{
		if (param.getLimit() <= 0 || param.getLimit() == Integer.MAX_VALUE)
		{
			param.setLimit(9);
		}
		param.parseArgs(App.args, "jcha-gui", 1); // -<- This is likely not the endorsed way. Can I pick up the args from launch()?
		List<ClassHistogram> histograms = loadHistograms();
		int count = histograms.size();
		
		param.overrideClassFilter(calculateClassFilter(histograms));
		
		Scene scene = new Scene(new Group());
		stage.setWidth(800);
		stage.setHeight(600);
		stage.setTitle("Java Class Histogram Analyzer");

		if (count == 0)
		{
			stage.setTitle("No Histograms given on command line");
		}
		else
		{
			
	        final NumberAxis xAxis = new NumberAxis();
	        final NumberAxis yAxis = new NumberAxis();
	        xAxis.setLabel("Time");
	        yAxis.setLabel("Object count"); // or size
	        //creating the chart
	        final LineChart<Number,Number> chart = 
	                new LineChart<Number,Number>(xAxis,yAxis);

	        String limitText = "";
	        if (!param.classFilter().isEmpty())
	        {
	        	limitText = " (filtering for " + param.classFilter().size() + " classes)";
	        }
	        else if (param.getLimit() != 0)
	        {
	        	limitText = " (class limit " + param.getLimit() + ")"; 
	        }
	        
	        String orderText = ""; //" orderBy ignored (impl. pending)";
	        
			if (count == 1)
				chart.setTitle("Analyzed 1 histogram" + orderText);
			else
				chart.setTitle("Analyzed " + count + " histograms" + limitText + orderText);

			Map<String, XYChart.Series> chartMap = new HashMap<>();
			
			int x = 1;
			for (ClassHistogram histogram : histograms )
			{
				int limit = param.getLimit();
	
				for (ClassHistogramEntry entry : histogram.values())
				{
					if (!param.isClassAcceptable(entry.className))
					{
						continue;
					}
					XYChart.Series series = resolveSeries(chartMap, entry.className);
					
			        //populating the series with data
			        series.getData().add(new XYChart.Data(x, entry.instances));
	
			        if (limit-- == 0)
			        {
			        	break; // enough
			        }
				}
				
				x++; // Advance in x-Axis
			}
			
			ObservableList<Series<Number, Number>> data = chart.getData();
			for (Series series : chartMap.values())
			{
				data.add(series);
			}
			
			((Group) scene.getRoot()).getChildren().add(chart);
		}

		stage.setScene(scene);
		stage.show();
	}

	/**
	 * Returns a class filter that is most appropriate for the given histograms and the Parameters
	 * field from this object.
	 * If the user has explicitly specified a filter, no calculation is done - the user filter is returned as-is.
	 * 
	 * @param histograms
	 * @param classFilter
	 * @param limit
	 * @return
	 */
	private Set<String> calculateClassFilter(List<ClassHistogram> histograms)
	{
		if (!param.classFilter().isEmpty())
			return param.classFilter();
		
		Set<String> classFilter = new HashSet<>();
		ClassHistogramStats statsFiltered = HistogramAverager.compareFirstWithSecondHalf(histograms, param);
		BaseCorrelator correlator = JchaUtil.correlate(statsFiltered, param);
		
		int pos = 0;
outer:	for (Entry<String, ArrayList<ClassHistogramStatsEntry>> groupEntry : correlator.getGroups().entrySet())
		{
			String key = groupEntry.getKey();
			ArrayList<ClassHistogramStatsEntry> group = groupEntry.getValue();
			for (ClassHistogramStatsEntry entry : group)
			{
				classFilter.add(entry.id());
				if (++pos == param.getLimit())
				{
					break outer;
				}
			}
		}
	
		return classFilter;
	}

	/**
	 * Returns the Series for the given className. It is taken from the given Map.
	 * 
	 * @param chartMap
	 * @param className
	 * @return
	 */
	private Series resolveSeries(Map<String, Series> chartMap, String className)
	{
		Series series2 = chartMap.get(className);
		if (series2 != null)
		{
			return series2; // Already exists. Fine. Use it
		}

		series2 = new XYChart.Series();
        series2.setName(className);
        chartMap.put(className, series2);

		return series2;
	}

	List<ClassHistogram> loadHistograms()
	{
		List<String> files = param.getFiles();
		int histCountFiles = files.size();
		List<ClassHistogram> histograms = new ArrayList<>(histCountFiles);
		for (int i=0; i< histCountFiles; i++)
		{
			try
			{
				histograms.add(new ClassHistogram(files.get(i), param.ignoreKnownDuplicates(), param.classFilter()));
			}
			catch (Exception exc)
			{
				warn("Ignoring histogram: " + exc.getMessage());
			}
		}

		return histograms;
	}
	
	private void warn(String string)
	{
		System.err.println("WARN " + string);
		// TODO Move to logger
		
	}

	public static void main(String[] args)
	{
		App.args = args;    
		launch(args);
	}
}