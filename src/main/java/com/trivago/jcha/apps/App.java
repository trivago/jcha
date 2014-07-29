package com.trivago.jcha.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.trivago.jcha.stats.ClassHistogram;
import com.trivago.jcha.stats.ClasssHistogramEntry;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;

public class App extends Application
{
	com.trivago.jcha.core.Parameters param = new com.trivago.jcha.core.Parameters();
	static String[] args;
	
	@Override
	public void start(Stage stage)
	{
		param.setLimit(20);
		param.parseArgs(App.args, 0); // -<- This is likely not the endorsed way. Can I pick up the args from launch()?
		List<ClassHistogram> histograms = loadHistograms();
		int count = histograms.size();
		
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

//			chart.set
	
			
//			ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(new PieChart.Data("Grapefruit",
//					13), new PieChart.Data("Oranges", 25), new PieChart.Data("Plums", 10), new PieChart.Data("Pears", 22),
//					new PieChart.Data("Apples", 30));
//			final PieChart chart = new PieChart(pieChartData);

	        String limitText = "";
	        if (param.getLimit() != 0)
	        {
	        	limitText = " (class limit " + param.getLimit() + ")"; 
	        }
	        
	        String orderText = " orderBy ignored (impl. pending)";
	        
			if (count == 1)
				chart.setTitle("Displaying histogram" + limitText + "\n" + orderText);
			else
				chart.setTitle("Displaying " + count + " histograms" + limitText + orderText);

			Map<String, XYChart.Series> chartMap = new HashMap<>();
			
			int x = 1;
			for (ClassHistogram histogram : histograms )
			{
				int limit = param.getLimit();
	
				for (ClasssHistogramEntry entry : histogram.values())
				{
					XYChart.Series series = resolveSeries(chartMap, entry.className, chart);
					
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

	private Series resolveSeries(Map<String, Series> chartMap, String className, LineChart chart)
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
				histograms.add(new ClassHistogram(files.get(i)));
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