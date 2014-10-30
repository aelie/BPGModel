package individual;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Created by aelie on 05/09/14.
 */
public class Charts {

    Map<String, JFreeChart> chartByTitle;
    JFrame serverFrame;
    JFrame applicationFrame;
    JFrame robustnessFrame;
    JFrame costFrame;

    public Charts() {
    }

    public void init(int time) {
        chartByTitle = new HashMap<>();
        String title = "(" + Simulator.getInstance().getParametersAsString() + ")";
        //server
        CategoryDataset serverDataset = createServerDataset(Simulator.getInstance().serverPool);
        JFreeChart serverChart = createBarChart(serverDataset, "Server" + time);
        final ChartPanel serverChartPanel = new ChartPanel(serverChart);
        serverChartPanel.setPreferredSize(new Dimension(500, 270));
        serverFrame = new JFrame(title);
        serverFrame.add(serverChartPanel);
        //application
        CategoryDataset applicationDataset = createApplicationDataset(Simulator.getInstance().applicationPool);
        JFreeChart applicationChart = createBarChart(applicationDataset, "Application" + time);
        final ChartPanel applicationChartPanel = new ChartPanel(applicationChart);
        applicationChartPanel.setPreferredSize(new Dimension(500, 270));
        applicationFrame = new JFrame(title);
        applicationFrame.setContentPane(applicationChartPanel);
        //robustnessMultiRun
        XYSeriesCollection finalRobustnessDataset = createFinalRobustnessDataset(Simulator.getInstance().getRobustnessHistory().get(Simulator.getInstance().getRobustnessHistory().size() - 1));
        JFreeChart finalRobustnessChart = createXYChartColoredSpecials(finalRobustnessDataset, "Robustness" + time);
        chartByTitle.put("Robustness" + time, finalRobustnessChart);
        final ChartPanel finalRobustnessChartPanel = new ChartPanel(finalRobustnessChart);
        finalRobustnessChartPanel.setPreferredSize(new Dimension(1500, 800));
        robustnessFrame = new JFrame(title);
        robustnessFrame.getContentPane().add(finalRobustnessChartPanel);
        //other metrics
        XYSeriesCollection costDataset = createMetricsHistoryDataset(Simulator.getInstance().getCostHistory(),
                Simulator.getInstance().getRobustnessHistory());
        costFrame = new JFrame(title);
        costFrame.setLayout(new GridLayout(2, 5));
        for (Object object : costDataset.getSeries()) {
            XYSeries series = (XYSeries) object;
            JFreeChart costChart = createXYChart(new XYSeriesCollection(series), (String) series.getKey());
            chartByTitle.put((String) series.getKey(), costChart);
            final ChartPanel costChartPanel = new ChartPanel(costChart);
            costChartPanel.setPreferredSize(new Dimension(1500, 800));
            costFrame.getContentPane().add(costChartPanel);
        }
        //server size distribution
        CategoryDataset serverSizeDataset = createServerSizeDataset(Simulator.getInstance().getServerHistory());
        JFreeChart serverSizeChart = createBarChart(serverSizeDataset, "ServerSize");
        chartByTitle.put("ServerSize", serverSizeChart);
        final ChartPanel serverSizeChartPanel = new ChartPanel(serverSizeChart);
        serverSizeChartPanel.setPreferredSize(new Dimension(1500, 800));
        costFrame.getContentPane().add(serverSizeChartPanel);
    }

    public CategoryDataset createServerDataset(Set<Server> servers) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int index = 0;
        for (Server server : servers) {
            dataset.addValue((Number) (server.getServices().size()), "Server servicePool", index);
            dataset.addValue((Number) (server.getCurrentConnectionNumber()), "Server connections", index);
            index++;
        }
        return dataset;
    }

    public CategoryDataset createServerSizeDataset(Map<Integer, Set<Server>> servers) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<Server> orderedInitialServers = Arrays.asList(servers.get(1)
                .stream()
                .sorted((s1, s2) -> s2.getServices().size() - s1.getServices().size())
                .toArray(Server[]::new));
        List<Server> orderedFinalServers = Arrays.asList(servers.get(servers.size())
                .stream()
                .sorted((s1, s2) -> s2.getServices().size() - s1.getServices().size())
                .toArray(Server[]::new));
        for (Server server : orderedInitialServers) {
            dataset.addValue((Number) (server.getServices().size()), "Initial", server.name);
        }
        for (Server server : orderedFinalServers) {
            dataset.addValue((Number) (server.getServices().size()), "Final", server.name);
        }
        return dataset;
    }

    public CategoryDataset createApplicationDataset(Set<Application> applications) {
        final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int index = 0;
        for (Application application : applications) {
            dataset.addValue((Number) (application.getServices().size()), "Application servicePool", index);
            index++;
        }
        return dataset;
    }

    public XYSeriesCollection createFinalRobustnessDataset(Map<Integer, List<Double>> robustnessHistory) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        List<SummaryStatistics> sumValues = new LinkedList<>();
        SummaryStatistics meanRobustness = new SummaryStatistics();
        for (Integer run : robustnessHistory.keySet()) {
            final XYSeries serie = new XYSeries(run);
            meanRobustness.addValue(robustnessHistory.get(run).get(0));
            int index = 0;
            for (Double value : robustnessHistory.get(run)) {
                if (index > 0) {
                    serie.add(index - 1, value);
                    if (sumValues.size() < index) {
                        sumValues.add(new SummaryStatistics());
                    }
                    sumValues.get(index - 1).addValue(value);
                }
                index++;
            }
            dataset.addSeries(serie);
        }
        final XYSeries serie = new XYSeries("Mean = " + meanRobustness.getMean());
        int index = 0;
        for (SummaryStatistics sumValue : sumValues) {
            serie.add(index, sumValue.getMean());
            index++;
        }
        dataset.addSeries(serie);
        final XYSeries serieMax = new XYSeries("Max = " + meanRobustness.getMax());
        for (Integer run : robustnessHistory.keySet()) {
            if (robustnessHistory.get(run).get(0) == meanRobustness.getMax()) {
                index = 0;
                for (Double value : robustnessHistory.get(run)) {
                    if (index > 0) {
                        serieMax.add(index - 1, value);
                    }
                    index++;
                }
            }
        }
        dataset.addSeries(serieMax);

        return dataset;
    }

    public XYSeriesCollection createMetricsHistoryDataset(Map<Integer, Map<String, Double>> costHistory,
                                                          Map<Integer, Map<Integer, List<Double>>> robustnessHistory) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        if (costHistory == null) {
            throw new NullPointerException("Argument costHistory is null");
        }
        Map<String, XYSeries> series = new LinkedHashMap<>();
        //costs
        for (Integer step : costHistory.keySet()) {
            for (String label : costHistory.get(step).keySet()) {
                if (!series.containsKey(label)) {
                    series.put(label, new XYSeries(label));
                }
                series.get(label).add(step, costHistory.get(step).get(label));
            }
        }
        //robustnessMultiRun
        series.put("Robustness", new XYSeries("Robustness"));
        //series.put("Robustness10", new XYSeries("Robustness10"));
        //series.put("Robustness30", new XYSeries("Robustness30"));
        series.put("RobustnessValue10", new XYSeries("RobustnessValue10"));
        series.put("RobustnessValue30", new XYSeries("RobustnessValue30"));
        for (Integer step : robustnessHistory.keySet()) {
            SummaryStatistics meanRobustness = new SummaryStatistics();
            double mean10 = 0;
            double mean30 = 0;
            double value10 = 0;
            double value30 = 0;
            for (List<Double> extinctionSequence : robustnessHistory.get(step).values()) {
                meanRobustness.addValue(extinctionSequence.get(0));
                int index10 = (int) ((extinctionSequence.size() - 1) * 1.0 / 10.0);
                int index30 = (int) ((extinctionSequence.size() - 1) * 3.0 / 10.0);
                /*double partialMean10 = 0;
                double partialMean30 = 0;
                for(int index = 1; index < index30 + 1; index ++) {
                    if(index < index10 + 1) {
                        partialMean10 += extinctionSequence.get(index);
                    }
                    partialMean30 += extinctionSequence.get(index);
                }
                mean10 += partialMean10 / (extinctionSequence.get(1) * index10);
                mean30 += partialMean30 / (extinctionSequence.get(1) * index30);*/
                value10 = extinctionSequence.get(index10) / extinctionSequence.get(1);
                value30 = extinctionSequence.get(index30) / extinctionSequence.get(1);
            }
            //meanRobustness /= (robustnessHistory.get(step).values().size() - 1);
            mean10 /= (robustnessHistory.get(step).values().size() - 1);
            mean30 /= (robustnessHistory.get(step).values().size() - 1);
            series.get("Robustness").add((double) step, meanRobustness.getMean());
            //series.get("Robustness10").add((double)step, mean10);
            //series.get("Robustness30").add((double)step, mean30);
            series.get("RobustnessValue10").add((double) step, value10);
            series.get("RobustnessValue30").add((double) step, value30);
        }
        for (XYSeries serie : series.values()) {
            dataset.addSeries(serie);
        }

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
        //plot.setDomainGridlinePaint(Color.white);
        //plot.setRangeGridlinePaint(Color.white);

        // set the range axis to display integers only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // disable bar outlines...
        //final BarRenderer renderer = (BarRenderer) plot.getRenderer();
        //renderer.setDrawBarOutline(false);

        //final CategoryAxis domainAxis = plot.getDomainAxis();
        //domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
        // OPTIONAL CUSTOMISATION COMPLETED.

        return chart;
    }

    private JFreeChart createXYChartColoredSpecials(final XYSeriesCollection dataset, String title) {
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
        //chart.removeLegend();

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        // set the range axis to display integers only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        for (int i = 0; i < plot.getSeriesCount() - 2; i++) {
            plot.getRenderer().setSeriesPaint(i, Color.lightGray);
        }
        plot.getRenderer().setSeriesPaint(plot.getSeriesCount() - 2, Color.red);//mean
        plot.getRenderer().setSeriesPaint(plot.getSeriesCount() - 1, Color.green);//max
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
        //chart.removeLegend();

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        // set the range axis to display integers only...
        if (title.startsWith("Robustness")
                || title.equalsIgnoreCase("Evenness")
                || title.equalsIgnoreCase("Richness")) {
            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setAutoRange(true);//setRange(0, 1);
            rangeAxis.setAutoRangeIncludesZero(false);
        }
        //rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        return chart;
    }

    public void extractCharts(String folder) {
        String fullFolder = folder + System.getProperty("file.separator") + System.currentTimeMillis();
        File file = new File(fullFolder);
        JFreeChart clone = null;
        if (file.mkdirs()) {
            for (String title : chartByTitle.keySet()) {
                try {
                    file = new File(fullFolder + System.getProperty("file.separator") + title + ".png");
                    clone = (JFreeChart) (chartByTitle.get(title).clone());
                    ChartUtilities.saveChartAsPNG(file, clone, 1500, 800);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Unable to create folder");
        }
        try {
            PrintWriter pw_R = new PrintWriter(fullFolder + System.getProperty("file.separator") + "parameters.txt", "UTF-8");
            pw_R.println(Simulator.getInstance().getParametersAsString());
            pw_R.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public JFrame getServerFrame() {
        return serverFrame;
    }

    public JFrame getApplicationFrame() {
        return applicationFrame;
    }

    public JFrame getRobustnessFrame() {
        return robustnessFrame;
    }

    public JFrame getCostFrame() {
        return costFrame;
    }
}
