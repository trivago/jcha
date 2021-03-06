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

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.TimeStringConverter;

import com.trivago.jcha.core.DataPointType;
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
			// Limit 9 is useful, as more typically obscures the graphs.
			// Also more than 9 currently make the legend disappear. Possibly have to add (re)sizing code to fix that. 
			param.setLimit(9);
		}
		param.parseArgs(App.args, "jcha-gui", 1); // -<- This is likely not the endorsed way. Can I pick up the args from launch()?
		SortedSet<ClassHistogram> histograms = loadHistograms();
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
	        final LineChart<Number, Number> chart = createChart(histograms, count, param.getDataPointType());
			
			((Group) scene.getRoot()).getChildren().add(chart);
		}

		stage.setScene(scene);
		stage.show();
	}



	class TimestampToStringConverter extends StringConverter<Number>
	{
		final TimeStringConverter tsc;

		/**
		 * Creates a converter, that uses the default timezone to convert the timestamp into a String.
		 */
		TimestampToStringConverter()
		{
			this(TimeZone.getDefault());
		}

		/**
		 * Creates a converter, that uses the given timezone to convert the timestamp into a String.
		 */
		TimestampToStringConverter(TimeZone tz)
		{
			DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
			df.setTimeZone(tz);
			tsc = new TimeStringConverter(df);
		}
		
		@Override
		public String toString(Number t)
		{
			return tsc.toString(new Date(t.longValue()));
		}

		@Override
		public Number fromString(String string)
		{
			return 1;
		}

	}

	private LineChart<Number, Number> createChart(Collection<ClassHistogram> histograms, int count, DataPointType dataPointType)
	{
		boolean useDateAxis = false;
		boolean useSizeYAxis = true;

		final NumberAxis xAxis = new NumberAxis();
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Time");
		if (useDateAxis)
		{
			xAxis.setTickLabelFormatter(new TimestampToStringConverter());
		}

		if (useSizeYAxis)
		{
			yAxis.setLabel("Object size"); // or size
		}
		else
		{
			yAxis.setLabel("Object count"); // or size			
		}
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

		Map<String, XYChart.Series<Number,Number>> chartMap = new HashMap<>();
		Map<String, Long> lastInstances = new HashMap<>();
		
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
				XYChart.Series<Number,Number> series = resolveSeries(chartMap, entry.className);
				
				final long chartValue ;
				long value = useSizeYAxis ? entry.bytes : entry.instances;
				
				if (dataPointType == DataPointType.FirstDerivation)
				{
					Long lastInstanceCount = lastInstances.get(entry.className);
					if (lastInstanceCount == null)
						chartValue = 0;
					else
						chartValue = value - lastInstanceCount; 
				}
				else
				{
					chartValue = value;
				}
				
				long xValue;
				if (useDateAxis)
				{
					xValue = histogram.getSnapshotTimeMillisTo();
//					if (xValue < 1344813629000L)
//						continue;
				}
				else
				{
					xValue = x;
				}
				
				lastInstances.put(entry.className, value);
				
		        //populating the series with data
		        series.getData().add(new XYChart.Data<Number,Number>(xValue, chartValue));

		        if (limit-- == 0)
		        {
		        	break; // enough
		        }
			}
			
			x++; // Advance in x-Axis
		}
		
		ObservableList<Series<Number, Number>> data = chart.getData();
		for (Series<Number,Number> series : chartMap.values())
		{
			data.add(series);
		}
//		data.addListener(arg0);
		return chart;
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
	private Set<String> calculateClassFilter(SortedSet<ClassHistogram> histograms)
	{
		if (!param.classFilter().isEmpty())
			return param.classFilter();
		
		Set<String> classFilter = new HashSet<>();
		ClassHistogramStats statsFiltered = HistogramAverager.compareFirstWithSecondHalf(histograms, param);
		BaseCorrelator correlator = JchaUtil.correlate(statsFiltered, param);
		
		int pos = 0;
outer:	for (Entry<String, ArrayList<ClassHistogramStatsEntry>> groupEntry : correlator.getGroups().entrySet())
		{
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
	private Series<Number,Number> resolveSeries(Map<String, Series<Number,Number>> chartMap, String className)
	{
		Series<Number,Number> series2 = chartMap.get(className);
		if (series2 != null)
		{
			return series2; // Already exists. Fine. Use it
		}

		series2 = new XYChart.Series<Number,Number>();
        series2.setName(className);
        chartMap.put(className, series2);

		return series2;
	}

	SortedSet<ClassHistogram> loadHistograms()
	{
		List<String> files = param.getFiles();
		int histCountFiles = files.size();
		SortedSet<ClassHistogram> histograms = new TreeSet<>();
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
