package individual.gui;

import individual.Application;
import individual.Server;
import individual.Service;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.RectangleEdge;
import tools.Tools;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.metal.MetalSliderUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by aelie on 17/04/15.
 */
public class Display extends JFrame {

    String testFile;

    JPanel testP = new JPanel();

    JPanel jP_main;
    JTabbedPane jTP_display;
    JPanel jP_matrixchart;
    JPanelMatrix jPS_matrix;
    ChartPanel CP_robustness;
    ChartPanel CP_weight;
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

    public static int stepNumber;
    public static int maxSimultaneousServers;
    public static int maxServerGeneration;
    public static int maxServerSize;
    public static int maxServerConnections;
    public static Set<String> serverNames;
    public static int maxSimultaneousApplications;
    public static int maxApplicationGeneration;
    public static int maxApplicationSize;
    public static int maxApplicationConnections;
    public static Set<String> applicationNames;
    public static Set<String> serviceNames;

    Map<Integer, Map<FakeServer, List<FakeApplication>>> graphHistory;
    Map<Integer, List<FakeServer>> serverHistory;
    Map<Integer, List<FakeApplication>> applicationHistory;

    Map<Integer, List<ActorComponent>> serverSituationHistory;
    Map<Integer, List<ActorComponent>> applicationSituationHistory;

    Map<String, Integer> referenceServersByPosition;
    Map<String, Integer> referenceApplicationsByPosition;
    Map<Integer, Double[][]> matrixByStep;

    List<ActorComponent> connectedTo;

    Map<Integer, Double> robustnessByStep;
    DefaultTableXYDataset datasetRobustness;
    Map<Integer, Map<Double, Integer>> linksByWeightByStep;
    DefaultTableXYDataset datasetWeight;
    Map<Double, XYSeries> weightSeries;

    boolean componentLocked = false;
    ActorComponent currentSelectedComponent;

    public Display(String inputFile) {
        testFile = System.getProperty("user.dir") + File.separator + (inputFile == null ? "connections.log" : inputFile);
        if (!new File(testFile).exists()) {
            System.err.println("File " + testFile + " not found! Exiting...");
            System.exit(-1);
        }
        if (new File(inputFile).isDirectory()) {
            preParseInputFileFF(testFile);
        } else {
            preParseInputFileAE(testFile);
        }
        buildMatrixByStep(stepNumber - 1);
        buildWeightByStep();
        buildRobustnessByStep();
        init();
    }

    public void init() {
        testP.setBackground(Color.red);
        setTitle(stepNumber + " steps");
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
        jP_matrixchart = new JPanel();
        jP_matrixchart.setLayout(new BorderLayout());
        jP_matrixchart.add(jPS_matrix, BorderLayout.NORTH);
        jP_matrixchart.add(CP_robustness, BorderLayout.WEST);
        jP_matrixchart.add(CP_weight, BorderLayout.CENTER);
        jP_main.add(buildTopPanel(), BorderLayout.NORTH);
        jP_main.add(buildServicesPanel(), BorderLayout.EAST);
        //jSP_matrix = new JScrollPane(jPS_matrix);
        jTP_display.addTab("Dual", jP_display);
        jTP_display.addTab("Matrix", /*jPS_matrix*/jP_matrixchart);
        jTP_display.setSelectedIndex(1);
        jP_main.add(jTP_display, BorderLayout.CENTER);
        getContentPane().add(jP_main);
        mL_actorComponent = new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
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

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Object source = e.getSource();
                if (source instanceof ActorComponent) {
                    if (!componentLocked) {
                        displayLinks((ActorComponent) source);
                        displayActor((ActorComponent) source);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Object source = e.getSource();
                if (source instanceof ActorComponent) {
                    if (!componentLocked) {
                        displayLinks(null);
                        displayActor(null);
                    }
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
            datasetRobustness.getSeries(0).addOrUpdate(new Integer(i), robustnessByStep.get(i));
        }
        for (int i = step; i < stepNumber; i++) {
            datasetRobustness.getSeries(0).addOrUpdate(new Integer(i), new Integer(-1));
        }
        datasetRobustness.getSeries(0).setNotify(true);
    }

    public void updateWeightDataset(int step) {
        if (datasetWeight == null) {
            datasetWeight = new DefaultTableXYDataset();
            weightSeries = new HashMap<>();
            for (Double weight = 0d; weight < maxApplicationSize; weight++) {
                weightSeries.put(weight, new XYSeries("Weight " + weight, false, false));
                datasetWeight.addSeries(weightSeries.get(weight));
            }
            weightSeries.put(-1d, new XYSeries("Weight sum", false, false));
            datasetWeight.addSeries(weightSeries.get(-1d));
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
            for (int i = step; i < stepNumber; i++) {
                weightSeries.get(weight).addOrUpdate(new Integer(i), new Integer(-1));
            }
            weightSeries.get(weight).setNotify(true);
        }
    }

    public JFreeChart buildRobustnessChart() {
        if (datasetRobustness == null) {
            updateRobustnessDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Robustness", "Steps", "Robustness", datasetRobustness, false, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, stepNumber);
        ((XYPlot) (chart.getPlot())).getRangeAxis().setRange(0, 1);
        return chart;
    }

    public JFreeChart buildWeightChart() {
        if (datasetWeight == null) {
            updateWeightDataset(0);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Weight", "Steps", "Weight", datasetWeight, true, false, false);
        ((XYPlot) (chart.getPlot())).getDomainAxis().setRange(0, stepNumber);
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        return chart;
    }

    public Map<Integer, Map<Server, Set<Application>>> buildConnectionsByStep() {
        Map<Integer, Map<Server, Set<Application>>> result = new HashMap<>();
        for (Integer step : graphHistory.keySet()) {
            result.put(step, new LinkedHashMap<>());
            for (FakeServer fakeServer : graphHistory.get(step).keySet()) {
                Server tmp = new Server(fakeServer.name,
                        fakeServer.generation,
                        new LinkedHashSet<>(fakeServer.services.stream()
                                .map(name -> new Service(name, 0, 0))
                                .collect(Collectors.toList())), fakeServer.maxConnections);
                result.get(step).put(tmp, new LinkedHashSet<>());
                for (FakeApplication fakeApplication : graphHistory.get(step).get(fakeServer)) {
                    result.get(step).get(tmp).add(
                            new Application(fakeApplication.name,
                                    fakeApplication.generation,
                                    new LinkedHashSet<>(fakeApplication.services.stream()
                                            .map(name -> new Service(name, 0, 0))
                                            .collect(Collectors.toList()))));
                }
            }
        }
        return result;
    }

    public void buildRobustnessByStep() {
        System.out.println("Building robustness data...");
        robustnessByStep = new HashMap<>();
        Map<Integer, Map<Server, Set<Application>>> connectionsByStep = buildConnectionsByStep();
        for (Integer step : connectionsByStep.keySet()) {
            robustnessByStep.put(step, Tools.robustnessRandom(connectionsByStep.get(step), Tools.SHUFFLE_ORDER).get(0));
        }
    }

    public void buildWeightByStep() {
        System.out.println("Building weight data...");
        linksByWeightByStep = new HashMap<>();
        for (int step = 0; step < stepNumber; step++) {
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
        List<String> orderedServiceNames = new ArrayList<>(serviceNames);
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
        return jP_info;
    }

    public JPanel buildSliderPanel() {
        jP_slider = new JPanel();
        jP_slider.setLayout(new FlowLayout(FlowLayout.LEADING));
        jS_slider = new JSlider(JSlider.HORIZONTAL, 0, stepNumber - 1, 1);
        jS_slider.setPreferredSize(new Dimension((int) (screen_width / 4.0), 20));
        jS_slider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            setGraph(source.getValue());
        });
        jS_slider.setUI(new MetalSliderUI() {
            protected void scrollDueToClickInTrack(int direction) {
                int value = slider.getValue();
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    value = this.valueForXPosition(slider.getMousePosition().x);
                } else if (slider.getOrientation() == JSlider.VERTICAL) {
                    value = this.valueForYPosition(slider.getMousePosition().y);
                }
                slider.setValue(value);
            }
        });
        jB_begin = new JButton("<<");
        jB_begin.addActionListener(e -> setGraph(0));
        jB_previous = new JButton("<");
        jB_previous.addActionListener(e -> setGraph(currentStep > 0 ? --currentStep : 0));
        jTF_step = new JTextField("0");
        jTF_step.setPreferredSize(new Dimension(40, 20));
        jTF_step.addActionListener(e -> setGraph(Integer.parseInt(jTF_step.getText())));
        jB_next = new JButton(">");
        jB_next.addActionListener(e -> setGraph(currentStep < stepNumber - 1 ? ++currentStep : stepNumber - 1));
        jB_end = new JButton(">>");
        jB_end.addActionListener(e -> setGraph(stepNumber - 1));
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
        ActionListener taskPerformer = evt -> setGraph(currentStep < stepNumber - 1 ? ++currentStep : stepNumber - 1);
        timer = new Timer(stepTime, taskPerformer);
        timer.start();
    }

    public void display() {
        setSize(new Dimension(screen_width, screen_height));
        setGraph(0);
        setVisible(true);
    }

    public void exportMatrix(String outputFile) {
        try {
            PrintWriter pw = new PrintWriter(outputFile);
            pw.println("\"Step\",\"Server\",\"Application\",\"Links\"");
            for (int step = 0; step < stepNumber; step++) {
                for (int i = 0; i < applicationHistory.get(step).size(); i++) {
                    for (int j = 0; j < serverHistory.get(step).size(); j++) {
                        int value = 0;
                        for (String serverService : serverHistory.get(step).get(j).services) {
                            if (applicationHistory.get(step).get(i).services.contains(serverService)) {
                                value++;
                            }
                        }
                        pw.println("\"" + step +
                                "\",\"" + serverHistory.get(step).get(j).name +
                                "\",\"" + applicationHistory.get(step).get(i).name +
                                "\",\"" + value + "\"");
                    }
                }
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * parser for log files from AE's BPG simulation
     *
     * @param inputFile the log file
     */
    public void preParseInputFileAE(String inputFile) {
        System.out.println("Parsing AE log file...");
        // gathering lots of data
        serverNames = new LinkedHashSet<>();
        applicationNames = new LinkedHashSet<>();
        serviceNames = new LinkedHashSet<>();
        // connected applications, by servers, by simulation step
        graphHistory = new HashMap<>(); // Map<Integer, Map<FakeServer, List<FakeApplication>>>
        // servers, by simulation step
        serverHistory = new HashMap<>(); // Map<Integer, List<FakeServer>>
        // applications, by simulation step
        applicationHistory = new HashMap<>(); // Map<Integer, List<FakeApplication>>
        int stepCounter = 0;
        try {
            BufferedReader br = Files.newBufferedReader(new File(inputFile).toPath());
            String line;
            while ((line = br.readLine()) != null) {
                // init the maps
                graphHistory.put(stepCounter, new HashMap<>());
                serverHistory.put(stepCounter, new ArrayList<>());
                applicationHistory.put(stepCounter, new ArrayList<>());
                List<FakeServer> currentServers = new ArrayList<>();
                // splitting the servers
                String[] serversRaw = line.split("\\|");
                // grabbing robustness from last position
                double robustnessRandomShuffle = Double.parseDouble(serversRaw[serversRaw.length - 4]);
                double robustnessRandomForward = Double.parseDouble(serversRaw[serversRaw.length - 3]);
                double robustnessRandomBackward = Double.parseDouble(serversRaw[serversRaw.length - 2]);
                double robustnessServiceShuffle = Double.parseDouble(serversRaw[serversRaw.length - 1]);
                // stat
                maxSimultaneousServers = Math.max(serversRaw.length - 4, maxSimultaneousServers);
                // for all server full String
                for (String serverRaw : Arrays.asList(serversRaw).subList(0, serversRaw.length - 4)) {
                    // grabbing the server part
                    String server = serverRaw.split("=")[0];
                    // grabbing the application part
                    String applications = serverRaw.split("=")[1];
                    // server name
                    serverNames.add(server.split("/")[0]);
                    // building the server object
                    FakeServer fakeServer = new FakeServer(server.split("/")[0], Integer.parseInt(server.split("/")[1]),
                            Integer.parseInt(server.split("/")[2]), Integer.parseInt(server.split("/")[3]), Integer.parseInt(server.split("/")[4]),
                            Arrays.asList(server.split("/")).subList(5, server.split("/").length));
                    // stats and tools
                    serverHistory.get(stepCounter).add(fakeServer);
                    currentServers.add(fakeServer);
                    graphHistory.get(stepCounter).put(fakeServer, new ArrayList<>());
                    maxServerGeneration = Math.max(Integer.parseInt(server.split("/")[1]), maxServerGeneration);
                    maxServerConnections = Math.max(Integer.parseInt(server.split("/")[2]), maxServerConnections);
                    maxServerSize = Math.max(Arrays.asList(server.split("/")).size(), maxServerSize);
                    serviceNames.addAll(Arrays.asList(server.split("/")).subList(5, server.split("/").length));
                    maxSimultaneousApplications = Math.max(applications.split(";").length, maxSimultaneousApplications);
                    // applications
                    for (String application : applications.split(";")) {
                        // building the application object
                        FakeApplication fakeApplication = new FakeApplication(application.split("/")[0], Integer.parseInt(application.split("/")[1]),
                                Integer.parseInt(application.split("/")[2]), Integer.parseInt(application.split("/")[4]),
                                Arrays.asList(application.split("/")).subList(5, application.split("/").length),
                                Arrays.asList(application.split("/")[3].split("_")).stream()
                                        .map(neighborName -> (FakeServer) findActor(neighborName, currentServers))
                                        .collect(Collectors.toList()));
                        // stats and tools
                        applicationHistory.get(stepCounter).add(fakeApplication);
                        graphHistory.get(stepCounter).get(fakeServer).add(fakeApplication);
                        applicationNames.add(application.split("/")[0]);
                        maxApplicationGeneration = Math.max(Integer.parseInt(application.split("/")[1]), maxApplicationGeneration);
                        maxApplicationConnections = Math.max(Integer.parseInt(application.split("/")[2]), maxApplicationConnections);
                        maxApplicationSize = Math.max(Arrays.asList(application.split("/")).size(), maxApplicationSize);
                        serviceNames.addAll(Arrays.asList(application.split("/")).subList(5, application.split("/").length));
                    }
                }
                stepCounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Names: " + serverNames.size() + "/" + applicationNames.size() + "/" + serviceNames.size());
        System.out.println("MaxSimult: " + maxSimultaneousServers + "/" + maxSimultaneousApplications);
        System.out.println("MaxGen: " + maxServerGeneration + "/" + maxApplicationGeneration);
        System.out.println("MaxSize: " + maxServerSize + "/" + maxApplicationSize);
        stepNumber = stepCounter;
        buildSituations();
    }

    /**
     * parser for files from FF's BPG simulation
     *
     * @param inputFolder
     */
    public void preParseInputFileFF(String inputFolder) {
        System.out.println("Parsing FF log file...");
        serverNames = new LinkedHashSet<>();
        applicationNames = new LinkedHashSet<>();
        serviceNames = new LinkedHashSet<>();
        graphHistory = new HashMap<>();
        serverHistory = new HashMap<>();
        applicationHistory = new HashMap<>();
        int fileStep;
        try {
            // for all files in the input folder
            for (File inputFile : Arrays.asList(new File(inputFolder).listFiles()).stream()
                    .filter(file -> !file.isDirectory())
                    .collect(Collectors.toList())) {
                // grabbing the step from the file name
                fileStep = Integer.parseInt(inputFile.getName().split("_")[2]);
                graphHistory.put(fileStep, new HashMap<>());
                serverHistory.put(fileStep, new ArrayList<>());
                applicationHistory.put(fileStep, new ArrayList<>());
                List<FakeServer> currentServers = new ArrayList<>();
                BufferedReader br = Files.newBufferedReader(inputFile.toPath());
                String line;
                // skipping lines until "PLATFORMS"
                while ((line = br.readLine()) != null && !line.equalsIgnoreCase("PLATFORMS")) {

                }
                //reading platforms
                while (!(line = br.readLine()).equalsIgnoreCase("APPLICATIONS")) {
                    if (line.length() > 0) {
                        String server = line;
                        String services = br.readLine();
                        serverNames.add(server.split(";")[0].trim());
                        FakeServer fakeServer = new FakeServer(server.split(";")[0].trim(), 0,
                                Integer.parseInt(server.split(";")[1]), Integer.parseInt(server.split(";")[2]), fileStep,
                                Arrays.asList(services.substring(1, services.length() - 1).split(",")).stream()
                                        .map(String::trim)
                                        .collect(Collectors.toList()));
                        serverHistory.get(fileStep).add(fakeServer);
                        currentServers.add(fakeServer);
                        graphHistory.get(fileStep).put(fakeServer, new ArrayList<>());
                        maxServerGeneration = Math.max(0, maxServerGeneration);
                        maxServerConnections = Math.max(Integer.parseInt(server.split(";")[1]), maxServerConnections);
                        maxServerSize = Math.max(fakeServer.services.size(), maxServerSize);
                        serviceNames.addAll(fakeServer.services);
                        maxSimultaneousApplications = Math.max(0, maxSimultaneousApplications);
                    }
                }
                //reading applications
                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        String application = line;
                        String services = br.readLine();
                        String servers = br.readLine();
                        String neighbors = br.readLine();
                        FakeApplication fakeApplication = new FakeApplication(application.split(";")[0].trim(), 0,
                                servers.substring(1, servers.length() - 2).split("\\s").length, fileStep,
                                Arrays.asList(services.substring(1, services.length() - 1).split(",")).stream()
                                        .map(String::trim)
                                        .collect(Collectors.toList()),
                                Arrays.asList(neighbors.substring(1, neighbors.length() - 2).split("\\s")).stream()
                                        .map(neighborName -> (FakeServer) findActor(neighborName, currentServers))
                                        .collect(Collectors.toList()));
                        applicationHistory.get(fileStep).add(fakeApplication);
                        for (String server : servers.substring(1, servers.length() - 2).split("\\s")) {
                            graphHistory.get(fileStep)
                                    .get(findActor(server, new ArrayList<>(graphHistory.get(fileStep).keySet())))
                                    .add(fakeApplication);
                        }
                        applicationNames.add(fakeApplication.name);
                        maxApplicationGeneration = Math.max(fakeApplication.generation, maxApplicationGeneration);
                        maxApplicationConnections = Math.max(fakeApplication.connections, maxApplicationConnections);
                        maxApplicationSize = Math.max(fakeApplication.services.size(), maxApplicationSize);
                        serviceNames.addAll(fakeApplication.services);
                    }
                }
                stepNumber = Math.max(fileStep + 1, stepNumber);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Names: " + serverNames.size() + "/" + applicationNames.size() + "/" + serviceNames.size());
        System.out.println("MaxSimult: " + maxSimultaneousServers + "/" + maxSimultaneousApplications);
        System.out.println("MaxGen: " + maxServerGeneration + "/" + maxApplicationGeneration);
        System.out.println("MaxSize: " + maxServerSize + "/" + maxApplicationSize);
        buildSituations();
    }

    public FakeActor findActor(String name, List actors) {
        for (Object fakeActor : actors) {
            if (((FakeActor) fakeActor).name.equalsIgnoreCase(name)) {
                return (FakeActor) fakeActor;
            }
        }
        return null;
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
        for (int step = 0; step < stepNumber; step++) {
            //serverSituationHistory.put(step, new ArrayList<>());
            serverSituationStep = new ArrayList<>();
            stepStartResearch = 0;
            for (String serverName : serverNames) {
                if ((livingServer = (FakeServer) findInStep(serverName, serverHistory, step)) != null) {
                    state = ActorComponent.ALIVE;
                    serverSituationStep.add(new ActorComponent(livingServer, state, ActorComponent.CIRCLE));
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
            for (String applicationName : applicationNames) {
                if ((livingApplication = (FakeApplication) findInStep(applicationName, applicationHistory, step)) != null) {
                    state = ActorComponent.ALIVE;
                    applicationSituationStep.add(new ActorComponent(livingApplication, state, ActorComponent.SQUARE));
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
        for (String service : serviceNames) {
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
        jL_servers_alive_value.setText(aliveCounter + "/" + serverNames.size() + "[" + serverNames.size() / stepNumber + "]");
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
        jL_applications_alive_value.setText(aliveCounter + "/" + applicationNames.size() + "[" + applicationNames.size() / stepNumber + "]");

        jPS_matrix.setMatrixSize(applicationHistory.get(step).size(), serverHistory.get(step).size());
        jPS_matrix.setMatrix(matrixByStep.get(step));
        updateRobustnessDataset(step);
        updateWeightDataset(step);
        if (componentLocked) {
            displayComponent(currentSelectedComponent);
        }
    }

    public Double[][] getMatrix(int step, int referenceStep) {
        if (referenceServersByPosition == null || referenceApplicationsByPosition == null) {
            referenceServersByPosition = new HashMap<>();
            referenceApplicationsByPosition = new HashMap<>();
            List<FakeServer> referenceOrderedServers = new ArrayList<>(serverHistory.get(referenceStep));
            List<FakeApplication> referenceOrderedApplications = new ArrayList<>(applicationHistory.get(referenceStep));
            Map<FakeServer, Integer> referenceServersWeight = new HashMap<>();
            Map<FakeApplication, Integer> referenceApplicationsWeight = new HashMap<>();
            for (FakeServer server : serverHistory.get(referenceStep)) {
                referenceServersWeight.put(server, applicationHistory.get(referenceStep).stream()
                        .filter(application -> graphHistory.get(referenceStep).get(server).contains(application))
                        .mapToInt(application -> application.getCommonServices(server).size())
                        .sum());
            }
            for (FakeApplication application : applicationHistory.get(referenceStep)) {
                referenceApplicationsWeight.put(application, serverHistory.get(referenceStep).stream()
                        .filter(server -> graphHistory.get(referenceStep).get(server).contains(application))
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
        Double[][] result = new Double[applicationHistory.get(step).size()][serverHistory.get(step).size()];
        /*int max = 0;
        for (FakeServer server : serverHistory.get(step)) {
            int value = applicationHistory.get(step).stream()
                    .mapToInt(application -> application.getCommonServices(server).size())
                    .max().getAsInt();
            max = value > max ? value : max;
        }*/
        Map<FakeServer, Integer> serversWeight = new HashMap<>();
        Map<FakeApplication, Integer> applicationsWeight = new HashMap<>();
        for (FakeServer server : serverHistory.get(step)) {
            serversWeight.put(server, applicationHistory.get(step).stream()
                    .filter(application -> graphHistory.get(step).get(server).contains(application))
                    .mapToInt(application -> application.getCommonServices(server).size())
                    .sum());
        }
        List<FakeServer> orderedServers = new ArrayList<>(serversWeight.keySet());
        for (FakeApplication application : applicationHistory.get(step)) {
            applicationsWeight.put(application, serverHistory.get(step).stream()
                    .filter(server -> graphHistory.get(step).get(server).contains(application))
                    .mapToInt(server -> server.getCommonServices(application).size())
                    .sum());
        }
        List<FakeApplication> orderedApplications = new ArrayList<>(applicationsWeight.keySet());
        for (int i = 0; i < orderedApplications.size(); i++) {
            for (int j = 0; j < orderedServers.size(); j++) {
                if (graphHistory.get(step).get(orderedServers.get(j)).contains(orderedApplications.get(i))) {
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
        for (int step = 0; step < stepNumber; step++) {
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
                    for (FakeApplication fakeApplication : graphHistory.get(currentStep).get(actorComponent.getFakeActor())) {
                        if (fakeApplication.name.equals(displayedApplication.getFakeActor().name)) {
                            connectedTo.add(displayedApplication);
                        }
                    }
                }
            } else {
                for (ActorComponent displayedServer : serverSituationHistory.get(currentStep)) {
                    for (FakeApplication fakeApplication : graphHistory.get(currentStep).get(displayedServer.getFakeActor())) {
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
}

class BoldListCellRenderer extends DefaultListCellRenderer {

    Map<String, Double> boldItems = new HashMap<>();
    Map<String, Double> itemsDemand = new HashMap<>();

    public void setBoldItems(Map<String, Double> boldItems) {
        this.boldItems = boldItems;
    }

    public void setItemsDemand(Map<String, Double> itemsDemand) {
        this.itemsDemand = itemsDemand;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component item = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
            if (itemsDemand.keySet().contains(value)) {
                String demand = Double.toString(itemsDemand.get(value));
                ((BoldListCellRenderer) item).setText(((BoldListCellRenderer) item).getText()
                        + " (" + demand.substring(0, Math.min(5, demand.length())) + ")");
            }
            if (boldItems.keySet().contains(value)) {
                item.setBackground(new Color((int) (boldItems.get(value) * 255), 0, 0));
                item.setForeground(Color.white);
                String demand = Double.toString(boldItems.get(value));
                ((BoldListCellRenderer) item).setText(((BoldListCellRenderer) item).getText()
                        + ": " + demand.substring(0, Math.min(5, demand.length())));
            }
        }
        return item;
    }
}

class FakeActor {
    public String name;
    public int generation;
    public int connections;
    public int age;
    public List<String> services;

    public FakeActor(String name, int generation, int connections, int age, List<String> services) {
        this.name = name;
        this.generation = generation;
        this.connections = connections;
        this.age = age;
        this.services = services;
    }

    public List<String> getCommonServices(FakeActor external) {
        return services.stream().filter(s -> external.services.contains(s)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return name;
    }
}

class FakeServer extends FakeActor {
    public int maxConnections;

    public FakeServer(String name, int generation, int maxConnections, int connections, int age, List<String> services) {
        super(name, generation, connections, age, services);
        this.maxConnections = maxConnections;
    }
}

class FakeApplication extends FakeActor {
    List<FakeServer> neighborhood;

    public FakeApplication(String name, int generation, int connections, int age, List<String> services, List<FakeServer> neighborhood) {
        super(name, generation, connections, age, services);
        this.neighborhood = neighborhood;
    }
}

class JPanelMatrix extends JPanel {
    private static final int PREF_W = 2;
    private static final int PREF_H = PREF_W;
    int size = 6;
    int padding = 0;
    int totalPaddingX = 10;
    int totalPaddingY = 10;
    private Double[][] matrix;
    int matrixWidth;
    int matrixHeight;

    public void setMatrixSize(int width, int height) {
        matrixWidth = width;
        matrixHeight = height;
        matrix = new Double[matrixWidth][matrixHeight];
    }

    public void setCell(int x, int y, double intensity) {
        matrix[x][y] = intensity;
    }

    public void setMatrix(Double[][] matrix) {
        this.matrix = matrix;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(totalPaddingX * 2 + matrixWidth * (size + padding), totalPaddingY * 2 + matrixHeight * (size + padding));//this.getParent().getSize();//new Dimension(PREF_W, PREF_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        double max = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                max = matrix[i][j] > max ? matrix[i][j] : max;
            }
        }
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                double value = matrix[i][j] / max;
                if (value >= 0) {
                    int rgbValue = (int) (value == 0 ? 0 : value * 200 + 55);
                    g2.setColor(new Color(rgbValue, rgbValue, rgbValue));
                } else {
                    int rgbValue = (int) (-value < 0.00001 ? 0 : -value * 128 + 55);
                    g2.setColor(new Color(rgbValue, 0, 0));
                }
                g2.fillRect(totalPaddingX + i * (size + padding), totalPaddingY + j * (size + padding), size, size);
                g2.setColor(Color.black);
                g2.draw(new Rectangle(totalPaddingX + i * (size + padding), totalPaddingY + j * (size + padding), size, size));
            }
        }
    }
}