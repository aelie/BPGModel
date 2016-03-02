package individual.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.RectangleEdge;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * Created by aelie on 17/04/15.
 */
public class Display extends JFrame {

    JPanel testP = new JPanel();

    JScrollPane jSP_tabbed_panel;
    JPanel jP_main;
    JTabbedPane jTP_display;
    JPanel jP_matrixchart;
    JPanel jP_charts;
    JPanelMatrix jPS_matrix;
    ChartPanel CP_robustness;
    ChartPanel CP_weight;
    ChartPanel CP_potential_connections;
    ChartPanel CP_cumulative_services;
    ChartPanel CP_cumulative_links;
    JPanel jP_display;
    JPanel jP_display_servers;
    JPanel jP_display_applications;
    JPanel jP_slider;
    JSlider jS_slider;
    JButton jB_begin;
    JButton jB_previous;
    JButton jB_next;
    JButton jB_end;
    JButton jB_play;
    JButton jB_export;
    JTextField jTF_step;
    JCheckBox jCB_dead;
    JPanel jP_info;
    JLabel jL_servers_alive_value;
    JLabel jL_applications_alive_value;
    JLabel jL_potential_connections_value;
    JPanel jP_top;
    JLabel jL_actorName_value;
    JLabel jL_actorType_value;
    JLabel jL_actorAge_value;
    JLabel jL_actorGeneration_value;
    JLabel jL_actorConnections_value;
    JLabel jL_actorServices_value;
    JLabel jL_actorConnected_value;
    JPanel jP_right;
    JList<String> jL_services;
    MouseListener mL_actorComponent;
    public static int screen_width = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
    public static int screen_height = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
    static int bigComponentSize = screen_width / 35;
    static int smallComponentSize = screen_width / 70;
    static int servicesPanelWidth = 120;
    public static int componentBaseSize = bigComponentSize;

    int currentStep;
    boolean isPlaying = false;
    boolean displayDead = false;
    Timer timer;

    BpgHistory bpgHistory;

    Map<Integer, List<ActorComponent>> serverSituationHistory;
    Map<Integer, List<ActorComponent>> applicationSituationHistory;

    Map<String, Integer> referenceServersByPosition;
    Map<String, Integer> referenceApplicationsByPosition;
    Map<Integer, Double[][]> matrixByStep;
    Map<Integer, Map<Double, Integer>> linksByWeightByStep;

    List<ActorComponent> connectedTo;

    DefaultTableXYDataset datasetRobustness;
    DefaultTableXYDataset datasetWeight;
    DefaultTableXYDataset datasetIncompatibles;
    DefaultTableXYDataset datasetCumulativeServices;
    DefaultTableXYDataset datasetCumulativeLinks;
    Map<Double, XYSeries> weightSeries;

    boolean componentLocked = false;
    ActorComponent currentSelectedComponent;

    static boolean log = true;

    public Display(String inputFile) {
        bpgHistory = new BpgHistory(inputFile);
        buildSituations();
        buildMatrixByStep(bpgHistory.getStepNumber() - 1);
        buildWeightByStep();
        bpgHistory.buildRobustnessByStep();
        bpgHistory.buildCumulativeServicesByStep();
        bpgHistory.buildIncompatiblesByStep();
        bpgHistory.buildCumulativeLinksByStep();
        init();
        if(log) {
            exportCSV();
        }
    }

    public void init() {
        testP.setBackground(Color.red);
        setTitle(bpgHistory.getStepNumber() + " steps");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jP_main = new JPanel();
        jP_main.setLayout(new BorderLayout());
        jTP_display = new JTabbedPane();
        jP_display = new JPanel();
        jP_display.setLayout(new BorderLayout());
        jP_display_servers = new JPanel();
        jP_display_servers.setLayout(new WrapLayout(FlowLayout.CENTER));
        ((WrapLayout) jP_display_servers.getLayout()).setHgap(1);
        ((WrapLayout) jP_display_servers.getLayout()).setVgap(1);
        jP_display_servers.setPreferredSize(new Dimension((int) ((screen_width - servicesPanelWidth) / 3.0), screen_height - 150));
        jP_display_servers.setBorder(BorderFactory.createEtchedBorder());
        jP_display_applications = new JPanel();
        jP_display_applications.setLayout(new WrapLayout(FlowLayout.CENTER));
        ((WrapLayout) jP_display_applications.getLayout()).setHgap(1);
        ((WrapLayout) jP_display_applications.getLayout()).setVgap(1);
        jP_display_applications.setPreferredSize(new Dimension((int) ((screen_width - servicesPanelWidth) * 2 / 3.0), screen_height - 150));
        jP_display_applications.setBorder(BorderFactory.createEtchedBorder());
        jP_display.add(jP_display_servers, BorderLayout.WEST);
        jP_display.add(jP_display_applications, BorderLayout.CENTER);
        jPS_matrix = new JPanelMatrix();
        CP_robustness = new ChartPanel(buildRobustnessChart());
        CP_weight = new ChartPanel(buildWeightChart());
        CP_potential_connections = new ChartPanel(buildIncompatiblesChart());
        CP_cumulative_services = new ChartPanel(buildCumulativeServicesChart());
        CP_cumulative_links = new ChartPanel(buildCumulativeLinksChart());
        jP_charts = new JPanel();
        jP_charts.setLayout(new GridLayout(2, 3));
        jP_charts.add(CP_robustness);
        jP_charts.add(CP_weight);
        jP_charts.add(CP_potential_connections);
        jP_charts.add(CP_cumulative_services);
        jP_charts.add(CP_cumulative_links);
        jP_matrixchart = new JPanel();
        jP_matrixchart.setLayout(new BorderLayout());
        jP_matrixchart.add(jPS_matrix, BorderLayout.NORTH);
        jP_matrixchart.add(jP_charts, BorderLayout.CENTER);
        jP_main.add(buildTopPanel(), BorderLayout.NORTH);
        jP_main.add(buildServicesPanel(), BorderLayout.EAST);
        jSP_tabbed_panel = new JScrollPane(jTP_display);
        jTP_display.addTab("Dual", jP_display);
        jTP_display.addTab("Matrix", jP_matrixchart);
        jTP_display.setSelectedIndex(1);
        jP_main.add(jSP_tabbed_panel, BorderLayout.CENTER);
        getContentPane().add(jP_main);
        mL_actorComponent = new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (jTP_display.getSelectedIndex() == 0) {
                    Object source = e.getSource();
                    if (source instanceof ActorComponent) {
                        if (!componentLocked) {
                            componentLocked = true;
                            currentSelectedComponent = (ActorComponent) source;
                        } else {
                            componentLocked = false;
                            currentSelectedComponent = null;
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (jTP_display.getSelectedIndex() == 0) {
                    Object source = e.getSource();
                    if (source instanceof ActorComponent) {
                        if (!componentLocked) {
                            displayLinks((ActorComponent) source);
                            displayActor((ActorComponent) source);
                        }
                    }
                } else if (jTP_display.getSelectedIndex() == 1) {
                    Object source = e.getSource();
                    if (source instanceof JPanelMatrix) {

                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (jTP_display.getSelectedIndex() == 0) {
                    Object source = e.getSource();
                    if (source instanceof ActorComponent) {
                        if (!componentLocked) {
                            displayLinks(null);
                            displayActor(null);
                        }
                    }
                } else if (jTP_display.getSelectedIndex() == 1) {

                }
            }
        };
    }

    public void displayComponent(ActorComponent actor) {
        if (componentLocked) {
            displayLinks(actor);
            displayActor(actor);
        }
    }

    public void updateRobustnessDataset(int step) {
        if (datasetRobustness == null) {
            datasetRobustness = new DefaultTableXYDataset();
            datasetRobustness.addSeries(new XYSeries("Robustness", false, false));
        }
        datasetRobustness.getSeries(0).setNotify(false);
        for (int i = 0; i < step; i++) {
            datasetRobustness.getSeries(0).addOrUpdate(new Integer(i), bpgHistory.getRobustnessByStep().get(i));
        }
        for (int i = step; i < bpgHistory.getStepNumber(); i++) {
            datasetRobustness.getSeries(0).addOrUpdate(new Integer(i), new Integer(-1));
        }
        datasetRobustness.getSeries(0).setNotify(true);
    }

    public void updateWeightDataset(int step) {
        if (datasetWeight == null) {
            datasetWeight = new DefaultTableXYDataset();
            weightSeries = new HashMap<>();
            for (Double weight = 0d; weight < bpgHistory.maxApplicationSize; weight++) {
                weightSeries.put(weight, new XYSeries("Weight " + weight, false, false));
                datasetWeight.addSeries(weightSeries.get(weight));
            }
            //weightSeries.put(-1d, new XYSeries("Weight sum", false, false));
            //datasetWeight.addSeries(weightSeries.get(-1d));
        }
        for (Double weight : weightSeries.keySet()) {
            weightSeries.get(weight).setNotify(false);
            for (int i = 0; i < step; i++) {
                double value;
                if (weight == -1d) {
                    value = linksByWeightByStep.get(i).values().stream().mapToDouble(w -> w).sum();
                } else {
                    if (linksByWeightByStep.get(i).containsKey(weight)) {
                        value = linksByWeightByStep.get(i).get(weight);
                    } else {
                        value = 0;
                    }
                }
                weightSeries.get(weight).addOrUpdate(new Integer(i), new Double(value));
            }
            for (int i = step; i < bpgHistory.getStepNumber(); i++) {
                weightSeries.get(weight).addOrUpdate(new Integer(i), new Integer(-1));
            }
            weightSeries.get(weight).setNotify(true);
        }
    }

    public void updateIncompatiblesDataset(int step) {
        if (datasetIncompatibles == null) {
            datasetIncompatibles = new DefaultTableXYDataset();
            datasetIncompatibles.addSeries(new XYSeries("Incompatibles", false, false));
        }
        datasetIncompatibles.getSeries(0).setNotify(false);
        for (int i = 0; i < step; i++) {
            datasetIncompatibles.getSeries(0).addOrUpdate(new Integer(i), new Integer(bpgHistory.getIncompatiblesByStep().get(i)));
        }
        for (int i = step; i < bpgHistory.getStepNumber(); i++) {
            datasetIncompatibles.getSeries(0).addOrUpdate(new Integer(i), new Integer(-1));
        }
        datasetIncompatibles.getSeries(0).setNotify(true);
    }

    public void updateCumulativeServicesDataset(int step) {
        if (datasetCumulativeServices == null) {
            datasetCumulativeServices = new DefaultTableXYDataset();
            datasetCumulativeServices.addSeries(new XYSeries("CumulativeServices", false, false));
        }
        datasetCumulativeServices.getSeries(0).setNotify(false);
        for (int i = 0; i < step; i++) {
            datasetCumulativeServices.getSeries(0).addOrUpdate(new Integer(i), new Integer(bpgHistory.getCumulativeServicesByStep().get(i)));
        }
        for (int i = step; i < bpgHistory.getStepNumber(); i++) {
            datasetCumulativeServices.getSeries(0).addOrUpdate(new Integer(i), new Integer(-1));
        }
        datasetCumulativeServices.getSeries(0).setNotify(true);
    }

    public void updateCumulativeLinksDataset(int step) {
        if (datasetCumulativeLinks == null) {
            datasetCumulativeLinks = new DefaultTableXYDataset();
            datasetCumulativeLinks.addSeries(new XYSeries("CumulativeLinks", false, false));
        }
        datasetCumulativeLinks.getSeries(0).setNotify(false);
        for (int i = 0; i < step; i++) {
            datasetCumulativeLinks.getSeries(0).addOrUpdate(new Integer(i), new Integer(bpgHistory.getCumulativeLinksByStep().get(i)));
        }
        for (int i = step; i < bpgHistory.getStepNumber(); i++) {
            datasetCumulativeLinks.getSeries(0).addOrUpdate(new Integer(i), new Integer(-1));
        }
        datasetCumulativeLinks.getSeries(0).setNotify(true);
    }

    public JFreeChart buildRobustnessChart() {
        if (datasetRobustness == null) {
            updateRobustnessDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Robustness", "Steps", "Robustness", datasetRobustness, false, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, bpgHistory.getStepNumber());
        double robMin = Collections.min(bpgHistory.robustnessByStep.values());
        double robMax = Collections.max(bpgHistory.robustnessByStep.values());
        ((XYPlot) (chart.getPlot())).getRangeAxis().setRange(Math.max(robMin - (robMax - robMin) / 10, 0), Math.min(robMax + (robMax - robMin) / 10, 1));
        return chart;
    }

    public JFreeChart buildWeightChart() {
        if (datasetWeight == null) {
            updateWeightDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Weight", "Steps", "Weight", datasetWeight, true, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, bpgHistory.getStepNumber());
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        return chart;
    }

    public JFreeChart buildIncompatiblesChart() {
        if (datasetIncompatibles == null) {
            updateIncompatiblesDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Incompatibles", "Steps", "Incompatibles", datasetIncompatibles, true, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, bpgHistory.getStepNumber());
        return chart;
    }

    public JFreeChart buildCumulativeServicesChart() {
        if (datasetCumulativeServices == null) {
            updateCumulativeServicesDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("CumulativeServices", "Steps", "CumulativeServices", datasetCumulativeServices, true, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, bpgHistory.getStepNumber());
        return chart;
    }

    public JFreeChart buildCumulativeLinksChart() {
        if (datasetCumulativeLinks == null) {
            updateCumulativeLinksDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("CumulativeLinks", "Steps", "CumulativeLinks", datasetCumulativeLinks, true, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, bpgHistory.getStepNumber());
        return chart;
    }

    public void buildWeightByStep() {
        System.out.println("Building weight data...");
        linksByWeightByStep = new HashMap<>();
        for (int step = 0; step < bpgHistory.getStepNumber(); step++) {
            linksByWeightByStep.put(step, new HashMap<>());
            for (int i = 0; i < matrixByStep.get(step).length; i++) {
                for (int j = 0; j < matrixByStep.get(step)[0].length; j++) {
                    if (matrixByStep.get(step)[i][j] > 0) {
                        if (!linksByWeightByStep.get(step).containsKey(matrixByStep.get(step)[i][j])) {
                            linksByWeightByStep.get(step).put(matrixByStep.get(step)[i][j], 0);
                        }
                        linksByWeightByStep.get(step).put(matrixByStep.get(step)[i][j], linksByWeightByStep.get(step).get(matrixByStep.get(step)[i][j]) + 1);
                    }
                }
            }
        }
    }

    public JPanel buildTopPanel() {
        jP_top = new JPanel();
        jP_top.setLayout(new FlowLayout(FlowLayout.LEADING));
        JPanel jP_info_slider = new JPanel();
        jP_info_slider.setLayout(new BorderLayout());
        jP_info_slider.add(buildSliderPanel(), BorderLayout.NORTH);
        jP_info_slider.add(buildAliveInfoPanel(), BorderLayout.CENTER);
        jP_top.add(jP_info_slider);
        jP_top.add(buildActorInfoPanel());
        return jP_top;
    }

    public JPanel buildServicesPanel() {
        jP_right = new JPanel();
        jP_right.setPreferredSize(new Dimension(servicesPanelWidth, 0));
        jL_services = new JList<>(new DefaultListModel<>());
        jL_services.setCellRenderer(new BoldListCellRenderer());
        jL_services.setLayoutOrientation(JList.VERTICAL);
        JScrollPane jSP_services = new JScrollPane(jL_services);
        List<String> orderedServiceNames = new ArrayList<>(bpgHistory.getServiceNames());
        Collections.sort(orderedServiceNames, (o1, o2) -> Integer.parseInt(o1.replaceAll("[^0-9]+", "")) - Integer.parseInt(o2.replaceAll("[^0-9]+", "")));
        orderedServiceNames.forEach(((DefaultListModel<String>) jL_services.getModel())::addElement);
        jP_right.setLayout(new BorderLayout());
        jP_right.add(new JLabel("Services"), BorderLayout.NORTH);
        jP_right.add(new JLabel("name (demand): connection"), BorderLayout.SOUTH);
        jP_right.add(jSP_services, BorderLayout.CENTER);
        return jP_right;
    }

    public JPanel buildActorInfoPanel() {
        JPanel jP_actorInfo = new JPanel();
        jP_actorInfo.setLayout(new BorderLayout());
        jP_actorInfo.setPreferredSize(new Dimension(800, 80));
        JPanel jP_info_grid = new JPanel();
        jP_info_grid.setLayout(new GridLayout(0, 4));
        ((GridLayout) jP_info_grid.getLayout()).setHgap(5);
        JPanel jP_info_connected = new JPanel();
        jP_info_connected.setLayout(new FlowLayout(FlowLayout.LEADING));
        JLabel jL_actorType = new JLabel("Type:");
        jL_actorType.setHorizontalAlignment(JLabel.TRAILING);
        JLabel jL_actorName = new JLabel("Name:");
        jL_actorName.setHorizontalAlignment(JLabel.TRAILING);
        JLabel jL_actorAge = new JLabel("Age:");
        jL_actorAge.setHorizontalAlignment(JLabel.TRAILING);
        JLabel jL_actorGeneration = new JLabel("Generation:");
        jL_actorGeneration.setHorizontalAlignment(JLabel.TRAILING);
        JLabel jL_actorConnections = new JLabel("Connections:");
        jL_actorConnections.setHorizontalAlignment(JLabel.TRAILING);
        JLabel jL_actorServices = new JLabel("Services:");
        jL_actorServices.setHorizontalAlignment(JLabel.TRAILING);
        JLabel jL_actorConnected = new JLabel("Connected:");
        jL_actorConnected.setHorizontalAlignment(JLabel.TRAILING);
        jL_actorType_value = new JLabel();
        jL_actorType_value.setHorizontalAlignment(JLabel.LEADING);
        jL_actorName_value = new JLabel();
        jL_actorName_value.setHorizontalAlignment(JLabel.LEADING);
        jL_actorAge_value = new JLabel();
        jL_actorAge_value.setHorizontalAlignment(JLabel.LEADING);
        jL_actorGeneration_value = new JLabel();
        jL_actorGeneration_value.setHorizontalAlignment(JLabel.LEADING);
        jL_actorConnections_value = new JLabel();
        jL_actorConnections_value.setHorizontalAlignment(JLabel.LEADING);
        jL_actorServices_value = new JLabel();
        jL_actorServices_value.setHorizontalAlignment(JLabel.LEADING);
        jL_actorConnected_value = new JLabel();
        jL_actorConnected_value.setHorizontalAlignment(JLabel.LEADING);
        jP_info_grid.add(jL_actorType);
        jP_info_grid.add(jL_actorType_value);
        jP_info_grid.add(jL_actorName);
        jP_info_grid.add(jL_actorName_value);
        jP_info_grid.add(jL_actorAge);
        jP_info_grid.add(jL_actorAge_value);
        jP_info_grid.add(jL_actorGeneration);
        jP_info_grid.add(jL_actorGeneration_value);
        jP_info_grid.add(jL_actorConnections);
        jP_info_grid.add(jL_actorConnections_value);
        jP_info_grid.add(jL_actorServices);
        jP_info_grid.add(jL_actorServices_value);
        jP_info_connected.add(jL_actorConnected);
        jP_info_connected.add(jL_actorConnected_value);
        jP_actorInfo.add(jP_info_grid, BorderLayout.CENTER);
        jP_actorInfo.add(jP_info_connected, BorderLayout.SOUTH);
        return jP_actorInfo;
    }

    public JPanel buildAliveInfoPanel() {
        jP_info = new JPanel();
        jP_info.setLayout(new FlowLayout(FlowLayout.LEADING));
        JLabel jL_servers_alive_label = new JLabel("Servers alive:");
        jL_servers_alive_label.setHorizontalAlignment(JLabel.TRAILING);
        jL_servers_alive_value = new JLabel();
        jL_servers_alive_value.setHorizontalAlignment(JLabel.LEADING);
        jL_servers_alive_value.setPreferredSize(new Dimension(150, 20));
        jP_info.add(jL_servers_alive_label);
        jP_info.add(jL_servers_alive_value);
        JLabel jL_applications_alive_label = new JLabel("Applications alive:");
        jL_applications_alive_label.setHorizontalAlignment(JLabel.TRAILING);
        jL_applications_alive_value = new JLabel();
        jL_applications_alive_value.setHorizontalAlignment(JLabel.LEADING);
        jL_applications_alive_value.setPreferredSize(new Dimension(150, 20));
        jP_info.add(jL_applications_alive_label);
        jP_info.add(jL_applications_alive_value);

        JLabel jL_potential_connections_label = new JLabel("Potential connections:");
        jL_potential_connections_label.setHorizontalAlignment(JLabel.TRAILING);
        jL_potential_connections_value = new JLabel();
        jL_potential_connections_value.setHorizontalAlignment(JLabel.LEADING);
        jL_potential_connections_value.setPreferredSize(new Dimension(150, 20));
        jP_info.add(jL_potential_connections_label);
        jP_info.add(jL_potential_connections_value);
        return jP_info;
    }

    public JPanel buildSliderPanel() {
        jP_slider = new JPanel();
        jP_slider.setLayout(new FlowLayout(FlowLayout.LEADING));
        jS_slider = new JSlider(JSlider.HORIZONTAL, 0, bpgHistory.getStepNumber() - 1, 1);
        jS_slider.setPreferredSize(new Dimension((int) (screen_width / 4.0), 20));
        jS_slider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            setGraph(source.getValue());
        });
        /*jS_slider.setUI(new MetalSliderUI() {
            protected void scrollDueToClickInTrack(int direction) {
                int value = slider.getValue();
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    value = this.valueForXPosition(slider.getMousePosition().x);
                } else if (slider.getOrientation() == JSlider.VERTICAL) {
                    value = this.valueForYPosition(slider.getMousePosition().y);
                }
                slider.setValue(value);
            }
        });*/
        jB_begin = new JButton("<<");
        jB_begin.addActionListener(e -> setGraph(0));
        jB_previous = new JButton("<");
        jB_previous.addActionListener(e -> setGraph(currentStep > 0 ? --currentStep : 0));
        jTF_step = new JTextField("0");
        jTF_step.setPreferredSize(new Dimension(40, 20));
        jTF_step.addActionListener(e -> setGraph(Integer.parseInt(jTF_step.getText())));
        jB_next = new JButton(">");
        jB_next.addActionListener(e -> setGraph(currentStep < bpgHistory.getStepNumber() - 1 ? ++currentStep : bpgHistory.getStepNumber() - 1));
        jB_end = new JButton(">>");
        jB_end.addActionListener(e -> setGraph(bpgHistory.getStepNumber() - 1));
        jB_play = new JButton("|>");
        jB_play.addActionListener(e -> {
            if (!isPlaying) {
                jB_play.setText("||");
                isPlaying = true;
                playEvolution(100);
            } else {
                jB_play.setText("|>");
                timer.stop();
                isPlaying = false;
            }
        });
        jB_export = new JButton("Export Matrix");
        jB_export.addActionListener(e -> exportMatrix("matrix.csv"));
        jCB_dead = new JCheckBox();
        jCB_dead.setText("Display dead actors");
        jCB_dead.addActionListener(e -> {
            displayDead = jCB_dead.isSelected();
            if (displayDead) {
                componentBaseSize = smallComponentSize;
                jP_display_servers.setPreferredSize(new Dimension((int) (screen_width * 3 / 4.0) - servicesPanelWidth, screen_height - 150));
                jP_display_applications.setPreferredSize(new Dimension((int) (screen_width / 4.0) - servicesPanelWidth, screen_height - 150));
            } else {
                componentBaseSize = bigComponentSize;
                jP_display_servers.setPreferredSize(new Dimension((int) (screen_width / 4.0) - servicesPanelWidth, screen_height - 150));
                jP_display_applications.setPreferredSize(new Dimension((int) (screen_width * 3 / 4.0) - servicesPanelWidth, screen_height - 150));
            }
            setGraph(currentStep);
        });
        jP_slider.add(jS_slider);
        jP_slider.add(jB_begin);
        jP_slider.add(jB_previous);
        jP_slider.add(jTF_step);
        jP_slider.add(jB_next);
        jP_slider.add(jB_end);
        jP_slider.add(jB_play);
        jP_slider.add(jB_export);
        //jP_slider.add(jCB_dead);
        return jP_slider;
    }

    public void playEvolution(int stepTime) {
        ActionListener taskPerformer = evt -> setGraph(currentStep < bpgHistory.getStepNumber() - 1 ? ++currentStep : bpgHistory.getStepNumber() - 1);
        timer = new Timer(stepTime, taskPerformer);
        timer.start();
    }

    public void display() {
        setSize(new Dimension(screen_width, screen_height));
        setGraph(bpgHistory.getStepNumber() - 1);
        setVisible(true);
    }

    public void exportCSV() {
        try {
            PrintWriter pw = new PrintWriter("export.csv");
            pw.println("\"Step\",\"Server\",\"Application\",\"Links\"");
            int step = 0;
            for (int i = 0; i < bpgHistory.getApplicationHistory().get(step).size(); i++) {
                for (int j = 0; j < bpgHistory.getServerHistory().get(step).size(); j++) {
                    int value = 0;
                    for (String serverService : bpgHistory.getServerHistory().get(step).get(j).services) {
                        if (bpgHistory.getApplicationHistory().get(step).get(i).services.contains(serverService)) {
                            value++;
                        }
                    }
                    pw.println("\"" + step +
                            "\",\"" + bpgHistory.getServerHistory().get(step).get(j).name +
                            "\",\"" + bpgHistory.getApplicationHistory().get(step).get(i).name +
                            "\",\"" + value + "\"");
                }
            }
            step = bpgHistory.getStepNumber() - 1;
            for (int i = 0; i < bpgHistory.getApplicationHistory().get(step).size(); i++) {
                for (int j = 0; j < bpgHistory.getServerHistory().get(step).size(); j++) {
                    int value = 0;
                    for (String serverService : bpgHistory.getServerHistory().get(step).get(j).services) {
                        if (bpgHistory.getApplicationHistory().get(step).get(i).services.contains(serverService)) {
                            value++;
                        }
                    }
                    pw.println("\"" + step +
                            "\",\"" + bpgHistory.getServerHistory().get(step).get(j).name +
                            "\",\"" + bpgHistory.getApplicationHistory().get(step).get(i).name +
                            "\",\"" + value + "\"");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void exportMatrix(String outputFile) {
        try {
            PrintWriter pw = new PrintWriter(outputFile);
            pw.println("\"Step\",\"Server\",\"Application\",\"Links\"");
            for (int step = 0; step < bpgHistory.getStepNumber(); step++) {
                for (int i = 0; i < bpgHistory.getApplicationHistory().get(step).size(); i++) {
                    for (int j = 0; j < bpgHistory.getServerHistory().get(step).size(); j++) {
                        int value = 0;
                        for (String serverService : bpgHistory.getServerHistory().get(step).get(j).services) {
                            if (bpgHistory.getApplicationHistory().get(step).get(i).services.contains(serverService)) {
                                value++;
                            }
                        }
                        pw.println("\"" + step +
                                "\",\"" + bpgHistory.getServerHistory().get(step).get(j).name +
                                "\",\"" + bpgHistory.getApplicationHistory().get(step).get(i).name +
                                "\",\"" + value + "\"");
                    }
                }
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void buildSituations() {
        System.out.println("Building graph database...");
        serverSituationHistory = new HashMap<>();
        applicationSituationHistory = new HashMap<>();
        FakeServer livingServer;
        FakeApplication livingApplication;
        int stepStartResearch;
        int state = ActorComponent.ALIVE;
        int counter;
        List<ActorComponent> serverSituationStep;
        List<ActorComponent> applicationSituationStep;
        for (int step = 0; step < bpgHistory.getStepNumber(); step++) {
            //serverSituationHistory.put(step, new ArrayList<>());
            serverSituationStep = new ArrayList<>();
            stepStartResearch = 0;
            for (String serverName : bpgHistory.getServerNames()) {
                if ((livingServer = (FakeServer) findInStep(serverName, bpgHistory.getServerHistory(), step)) != null) {
                    state = ActorComponent.ALIVE;
                    serverSituationStep.add(new ActorComponent(livingServer, state, ActorComponent.CIRCLE, bpgHistory));
                } /*else {
                    counter = stepStartResearch;
                    while (livingServer == null && counter < stepNumber) {
                        livingServer = (FakeServer) findInStep(serverName, serverHistory, counter);
                        counter++;
                    }
                    stepStartResearch = counter - 1;
                    if (counter > step) {
                        state = ActorComponent.LIMBO;
                    } else {
                        state = ActorComponent.DEAD;
                    }
                }*/
                //serverSituationHistory.get(step).add(new ActorComponent(livingServer, state));
                //serverSituationStep.add(new ActorComponent(livingServer, state));
            }
            serverSituationHistory.put(step, serverSituationStep);
            //applicationSituationHistory.put(step, new ArrayList<>());
            applicationSituationStep = new ArrayList<>();
            stepStartResearch = 0;
            for (String applicationName : bpgHistory.getApplicationNames()) {
                if ((livingApplication = (FakeApplication) findInStep(applicationName, bpgHistory.getApplicationHistory(), step)) != null) {
                    state = ActorComponent.ALIVE;
                    applicationSituationStep.add(new ActorComponent(livingApplication, state, ActorComponent.SQUARE, bpgHistory));
                } /*else {
                    counter = stepStartResearch;
                    while (livingApplication == null && counter < stepNumber) {
                        livingApplication = (FakeApplication) findInStep(applicationName, applicationHistory, counter);
                        counter++;
                    }
                    stepStartResearch = counter - 1;
                    if (counter > step) {
                        state = ActorComponent.LIMBO;
                    } else {
                        state = ActorComponent.DEAD;
                    }
                }*/
                //applicationSituationHistory.get(step).add(new ActorComponent(livingApplication, state));
                //applicationSituationStep.add(new ActorComponent(livingApplication, state));
            }
            applicationSituationHistory.put(step, applicationSituationStep);
            if (step % 50 == 0) {
                System.out.print("*");
            }
        }
        System.out.println();
    }

    public FakeActor findInStep(String actorName, Map actorHistory, int step) {
        for (Object livingActor : (List) actorHistory.get(step)) {
            if (((FakeActor) livingActor).name.equals(actorName)) {
                return (FakeActor) livingActor;
            }
        }
        return null;
    }

    public List<Set<ActorComponent>> createConnectionHistory(ActorComponent actor) {
        //TODO
        return null;
    }

    public void setGraph(int step) {
        currentStep = step;
        jTF_step.setText(Integer.toString(currentStep));
        jS_slider.setValue(currentStep);
        Map<String, Double> servicesDemand = new HashMap<>();
        for (String service : bpgHistory.getServiceNames()) {
            servicesDemand.put(service, 0.0);
            applicationSituationHistory.get(step).stream().filter(actorComponent -> actorComponent.getServices().contains(service)).forEach(actorComponent ->
                    servicesDemand.put(service, servicesDemand.get(service) + 1)
            );
            servicesDemand.put(service, servicesDemand.get(service) / (double) applicationSituationHistory.get(step).size());
        }
        ((BoldListCellRenderer) jL_services.getCellRenderer()).setItemsDemand(servicesDemand);
        jP_display_servers.removeAll();
        jP_display_applications.removeAll();
        jP_main.updateUI();
        int aliveCounter = 0;
        List<ActorComponent> sortedServers = serverSituationHistory.get(step);
        Collections.sort(sortedServers, (o1, o2) -> o2.services.size() - o1.services.size());
        for (ActorComponent actorComponent : sortedServers) {
            if (actorComponent.getMouseListeners().length == 0) {
                actorComponent.addMouseListener(mL_actorComponent);
            }
            if (displayDead) {
                jP_display_servers.add(actorComponent);
            } else {
                if (actorComponent.getState() == ActorComponent.ALIVE) {
                    jP_display_servers.add(actorComponent);
                }
            }
            aliveCounter += actorComponent.getState() == ActorComponent.ALIVE ? 1 : 0;
        }
        jL_servers_alive_value.setText(aliveCounter + "/" + bpgHistory.getServerNames().size() + "[" + bpgHistory.getServerNames().size() / bpgHistory.getStepNumber() + "]");
        aliveCounter = 0;
        List<ActorComponent> sortedApplications = applicationSituationHistory.get(step);
        Collections.sort(sortedApplications, (o1, o2) -> o2.services.size() - o1.services.size());
        for (ActorComponent actorComponent : sortedApplications) {
            if (actorComponent.getMouseListeners().length == 0) {
                actorComponent.addMouseListener(mL_actorComponent);
            }
            if (displayDead) {
                jP_display_applications.add(actorComponent);
            } else {
                if (actorComponent.getState() == ActorComponent.ALIVE) {
                    jP_display_applications.add(actorComponent);
                }
            }
            aliveCounter += actorComponent.getState() == ActorComponent.ALIVE ? 1 : 0;
        }
        jL_applications_alive_value.setText(aliveCounter + "/" + bpgHistory.getApplicationNames().size() + "[" + bpgHistory.getApplicationNames().size() / bpgHistory.getStepNumber() + "]");

        jPS_matrix.setMatrixSize(bpgHistory.getApplicationHistory().get(step).size(), bpgHistory.getServerHistory().get(step).size());
        jPS_matrix.setMatrix(matrixByStep.get(step));

        updateRobustnessDataset(step);
        updateWeightDataset(step);
        updateIncompatiblesDataset(step);
        updateCumulativeServicesDataset(step);
        updateCumulativeLinksDataset(step);
        if (componentLocked) {
            displayComponent(currentSelectedComponent);
        }
    }

    public Double[][] getMatrix(int step, int referenceStep) {
        if (referenceServersByPosition == null || referenceApplicationsByPosition == null) {
            referenceServersByPosition = new HashMap<>();
            referenceApplicationsByPosition = new HashMap<>();
            List<FakeServer> referenceOrderedServers = new ArrayList<>(bpgHistory.getServerHistory().get(referenceStep));
            List<FakeApplication> referenceOrderedApplications = new ArrayList<>(bpgHistory.getApplicationHistory().get(referenceStep));
            Map<FakeServer, Integer> referenceServersWeight = new HashMap<>();
            Map<FakeApplication, Integer> referenceApplicationsWeight = new HashMap<>();
            for (FakeServer server : bpgHistory.getServerHistory().get(referenceStep)) {
                referenceServersWeight.put(server, bpgHistory.getApplicationHistory().get(referenceStep).stream()
                        .filter(application -> bpgHistory.getGraphHistory().get(referenceStep).get(server).contains(application))
                        .mapToInt(application -> application.getCommonServices(server).size())
                        .sum());
            }
            for (FakeApplication application : bpgHistory.getApplicationHistory().get(referenceStep)) {
                referenceApplicationsWeight.put(application, bpgHistory.getServerHistory().get(referenceStep).stream()
                        .filter(server -> bpgHistory.getGraphHistory().get(referenceStep).get(server).contains(application))
                        .mapToInt(server -> server.getCommonServices(application).size())
                        .sum());
            }
            Collections.sort(referenceOrderedServers, (s1, s2) -> referenceServersWeight.get(s2) - referenceServersWeight.get(s1));
            Collections.sort(referenceOrderedApplications, (a1, a2) -> referenceApplicationsWeight.get(a2) - referenceApplicationsWeight.get(a1));
            for (int i = 0; i < referenceOrderedServers.size(); i++) {
                referenceServersByPosition.put(referenceOrderedServers.get(i).name, i);
            }
            for (int i = 0; i < referenceOrderedApplications.size(); i++) {
                referenceApplicationsByPosition.put(referenceOrderedApplications.get(i).name, i);
            }
        }
        Double[][] result = new Double[bpgHistory.getApplicationHistory().get(step).size()][bpgHistory.getServerHistory().get(step).size()];
        /*int max = 0;
        for (FakeServer server : serverHistory.get(step)) {
            int value = applicationHistory.get(step).stream()
                    .mapToInt(application -> application.getCommonServices(server).size())
                    .max().getAsInt();
            max = value > max ? value : max;
        }*/
        Map<FakeServer, Integer> serversWeight = new HashMap<>();
        Map<FakeApplication, Integer> applicationsWeight = new HashMap<>();
        for (FakeServer server : bpgHistory.getServerHistory().get(step)) {
            serversWeight.put(server, bpgHistory.getApplicationHistory().get(step).stream()
                    .filter(application -> bpgHistory.getGraphHistory().get(step).get(server).contains(application))
                    .mapToInt(application -> application.getCommonServices(server).size())
                    .sum());
        }
        List<FakeServer> orderedServers = new ArrayList<>(serversWeight.keySet());
        for (FakeApplication application : bpgHistory.getApplicationHistory().get(step)) {
            applicationsWeight.put(application, bpgHistory.getServerHistory().get(step).stream()
                    .filter(server -> bpgHistory.getGraphHistory().get(step).get(server).contains(application))
                    .mapToInt(server -> server.getCommonServices(application).size())
                    .sum());
        }
        List<FakeApplication> orderedApplications = new ArrayList<>(applicationsWeight.keySet());
        for (int i = 0; i < orderedApplications.size(); i++) {
            for (int j = 0; j < orderedServers.size(); j++) {
                if (bpgHistory.getGraphHistory().get(step).get(orderedServers.get(j)).contains(orderedApplications.get(i))) {
                    result
                            [referenceApplicationsByPosition.get(orderedApplications.get(i).name)]
                            [referenceServersByPosition.get(orderedServers.get(j).name)] =
                            (double) (orderedApplications.get(i).getCommonServices(orderedServers.get(j)).size());// / (double) max;
                } else {
                    result
                            [referenceApplicationsByPosition.get(orderedApplications.get(i).name)]
                            [referenceServersByPosition.get(orderedServers.get(j).name)] =
                            -(double) (orderedApplications.get(i).getCommonServices(orderedServers.get(j)).size());// / (double) max;
                }
            }
        }
        return result;
    }

    public void buildMatrixByStep(int referenceStep) {
        System.out.println("Building link matrix...");
        matrixByStep = new HashMap<>();
        for (int step = 0; step < bpgHistory.getStepNumber(); step++) {
            matrixByStep.put(step, getMatrix(step, referenceStep));
        }
    }

    public void displayActor(ActorComponent actorComponent) {
        if (actorComponent != null) {
            jL_actorType_value.setText(actorComponent.getType() == ActorComponent.SERVER ? "Server" : "Application");
            jL_actorName_value.setText(actorComponent.getName());
            jL_actorAge_value.setText(Integer.toString(actorComponent.getAge()));
            jL_actorGeneration_value.setText(Integer.toString(actorComponent.getGeneration()));
            jL_actorConnections_value.setText(Integer.toString(actorComponent.getFakeActor().connections));
            jL_actorServices_value.setText(Integer.toString(actorComponent.getActorSize()));
            if (connectedTo.size() > 0) {
                jL_actorConnected_value.setText(connectedTo.get(0).getName());
                for (int i = 1; i < connectedTo.size(); i++) {
                    jL_actorConnected_value.setText(jL_actorConnected_value.getText() + " / " + connectedTo.get(i).getName());
                }
            }
            Map<String, Double> satisfactionMap = new HashMap<>();
            for (String service : actorComponent.getServices()) {
                satisfactionMap.put(service, 0.0);
                connectedTo.stream().filter(connected -> connected.getServices().contains(service)).forEach(connected ->
                        satisfactionMap.put(service, satisfactionMap.get(service) + 1)
                );
                satisfactionMap.put(service, satisfactionMap.get(service) / (double) connectedTo.size());
            }
            ((BoldListCellRenderer) jL_services.getCellRenderer()).setBoldItems(satisfactionMap);
            jL_services.updateUI();
        } else {
            jL_actorType_value.setText("");
            jL_actorName_value.setText("");
            jL_actorAge_value.setText("");
            jL_actorGeneration_value.setText("");
            jL_actorConnections_value.setText("");
            jL_actorServices_value.setText("");
            jL_actorConnected_value.setText("");
            ((BoldListCellRenderer) jL_services.getCellRenderer()).setBoldItems(new HashMap<>());
            jL_services.updateUI();
        }
    }

    public void displayLinks(ActorComponent actorComponent) {
        if (actorComponent != null) {
            connectedTo = new ArrayList<>();
            if (actorComponent.getType() == ActorComponent.SERVER) {
                for (ActorComponent displayedApplication : applicationSituationHistory.get(currentStep)) {
                    for (FakeApplication fakeApplication : bpgHistory.getGraphHistory().get(currentStep).get(actorComponent.getFakeActor())) {
                        if (fakeApplication.name.equals(displayedApplication.getFakeActor().name)) {
                            connectedTo.add(displayedApplication);
                        }
                    }
                }
            } else {
                for (ActorComponent displayedServer : serverSituationHistory.get(currentStep)) {
                    for (FakeApplication fakeApplication : bpgHistory.getGraphHistory().get(currentStep).get(displayedServer.getFakeActor())) {
                        if (fakeApplication.name.equals(actorComponent.getFakeActor().name)) {
                            connectedTo.add(displayedServer);
                        }
                    }
                }
            }
            actorComponent.setHighlighted(true);
            for (ActorComponent linkedComponent : connectedTo) {
                linkedComponent.setHighlighted(true);
            }
            //neighborhood
            if (actorComponent.getType() == ActorComponent.APPLICATION) {
                for (FakeServer fakeServer : ((FakeApplication) actorComponent.getFakeActor()).neighborhood) {
                    for (ActorComponent displayedServer : serverSituationHistory.get(currentStep)) {
                        if (fakeServer != null) {
                            if (fakeServer.name.equals(displayedServer.getFakeActor().name)) {
                                displayedServer.setNeighbor(true);
                            }
                        }
                    }
                }
            }
        } else {
            for (ActorComponent displayedApplication : applicationSituationHistory.get(currentStep)) {
                displayedApplication.setHighlighted(false);
            }
            for (ActorComponent displayedServer : serverSituationHistory.get(currentStep)) {
                displayedServer.setHighlighted(false);
                displayedServer.setNeighbor(false);
            }
        }
        jP_display.updateUI();
    }

    public BpgHistory getBpgHistory() {
        return bpgHistory;
    }
}

