package individual;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by aelie on 05/09/14.
 */
public class Display{
    JFrame serverFrame;
    JFrame applicationFrame;
    JFrame robustnessFrame;

    public JFrame getServerFrame() {
        return serverFrame;
    }

    public JFrame getApplicationFrame() {
        return applicationFrame;
    }

    public JFrame getRobustnessFrame() {
        return robustnessFrame;
    }

    public Display(int time) {
        //server
        CategoryDataset serverDataset = createServerDataset(Simulator.getInstance().serverPool);
        JFreeChart serverChart = createBarChart(serverDataset, "Servers");
        final ChartPanel serverChartPanel = new ChartPanel(serverChart);
        serverChartPanel.setPreferredSize(new Dimension(500, 270));
        serverFrame = new JFrame("Server " + time);
        serverFrame.add(serverChartPanel);
        //application
        CategoryDataset applicationDataset = createApplicationDataset(Simulator.getInstance().applicationPool);
        JFreeChart applicationChart = createBarChart(applicationDataset, "Applications");
        final ChartPanel applicationChartPanel = new ChartPanel(applicationChart);
        applicationChartPanel.setPreferredSize(new Dimension(500, 270));
        applicationFrame = new JFrame("Application " + time);
        applicationFrame.setContentPane(applicationChartPanel);
        //robustness
        XYSeriesCollection finalRobustnessDataset = createFinalRobustnessDataset(Simulator.getInstance().getRobustnessResults().get(1));
        JFreeChart robustnessChart = createXYChart(finalRobustnessDataset, "FinalRobustness");
        final ChartPanel robustnessChartPanel = new ChartPanel(robustnessChart);
        robustnessChartPanel.setPreferredSize(new Dimension(1500, 800));
        robustnessFrame = new JFrame("FinalRobustness " + time);
        robustnessFrame.setContentPane(robustnessChartPanel);
    }

    public CategoryDataset createServerDataset(Set<Server> servers) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int index = 0;
        for(Server server : servers) {
            dataset.addValue((Number)(server.getAvailableServices().size()), "Server servicePool", index);
            dataset.addValue((Number)(server.getCurrentConnectionNumber()), "Server connexions", index);
            index++;
        }

        return dataset;
    }

    public CategoryDataset createApplicationDataset(Set<Application> applications) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int index = 0;
        for(Application application : applications) {
            dataset.addValue((Number)(application.getRequiredServices().size()), "Application servicePool", index);
            index++;
        }

        return dataset;
    }

    public XYSeriesCollection createFinalRobustnessDataset(Map<Integer, List<Double>> robustnessHistory) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        double[] mean = new double[robustnessHistory.get(0).size()];
        double meanRobustness = 0;
        for(Integer run : robustnessHistory.keySet()) {
            final XYSeries serie = new XYSeries(run);
            meanRobustness += robustnessHistory.get(run).remove(0);
            int index = 0;
            for(Double value : robustnessHistory.get(run)) {
                serie.add(index, value);
                mean[index] = mean[index] + value;
                index++;
            }
            dataset.addSeries(serie);
        }
        final XYSeries serie = new XYSeries("Mean = " + meanRobustness / robustnessHistory.size());
        int index = 0;
        for(Double meanValue : mean) {
            serie.add(index, meanValue / robustnessHistory.size());
            index++;
        }
        dataset.addSeries(serie);

        return dataset;
    }

    private JFreeChart createBarChart(final CategoryDataset dataset, String title) {
        final JFreeChart chart = ChartFactory.createBarChart(
                title,         // chart title
                "Category",               // domain axis label
                "Value",                  // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );
        // set the background color for the chart...
        chart.setBackgroundPaint(Color.white);

        // get a reference to the plot for further customisation...
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        // set the range axis to display integers only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // disable bar outlines...
        final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);

        // set up gradient paints for series...
        /*final GradientPaint gp0 = new GradientPaint(
                0.0f, 0.0f, Color.blue,
                0.0f, 0.0f, Color.lightGray
        );
        final GradientPaint gp1 = new GradientPaint(
                0.0f, 0.0f, Color.green,
                0.0f, 0.0f, Color.lightGray
        );
        final GradientPaint gp2 = new GradientPaint(
                0.0f, 0.0f, Color.red,
                0.0f, 0.0f, Color.lightGray
        );
        renderer.setSeriesPaint(0, gp0);
        renderer.setSeriesPaint(1, gp1);
        renderer.setSeriesPaint(2, gp2);*/

        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
        // OPTIONAL CUSTOMISATION COMPLETED.

        return chart;
    }

    private JFreeChart createXYChart(final XYSeriesCollection dataset, String title) {
        final JFreeChart chart = ChartFactory.createXYLineChart(
                title,         // chart title
                "Category",               // domain axis label
                "Value",                  // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );
        // set the background color for the chart...
        chart.setBackgroundPaint(Color.white);

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        // set the range axis to display integers only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        final XYItemRenderer renderer = plot.getRenderer();
        return chart;
    }
}
