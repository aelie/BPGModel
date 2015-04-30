package individual.gui;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.metal.MetalSliderUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * Created by aelie on 17/04/15.
 */
public class Display extends JFrame {

    String testFile;

    JScrollPane jSP_display;
    JPanel jP_main;
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

    List<ActorComponent> connectedTo;

    public Display() {
        testFile = System.getProperty("user.dir") + File.separator + "connections.log";
        if (!new File(testFile).exists()) {
            System.err.println("File " + testFile + " not found! Exiting...");
            System.exit(-1);
        }
        preParseInputFile(testFile);
        init();
    }

    public void init() {
        setTitle(stepNumber + " steps");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jP_main = new JPanel();
        jP_main.setLayout(new BorderLayout());
        jP_display = new JPanel();
        jP_display.setLayout(new BorderLayout());
        jP_display_servers = new JPanel();
        jP_display_servers.setLayout(new WrapLayout(FlowLayout.CENTER));
        ((WrapLayout) jP_display_servers.getLayout()).setHgap(1);
        ((WrapLayout) jP_display_servers.getLayout()).setVgap(1);
        jP_display_servers.setPreferredSize(new Dimension((int) ((screen_width - servicesPanelWidth) / 4.0), screen_height - 150));
        jP_display_servers.setBorder(BorderFactory.createEtchedBorder());
        jP_display_applications = new JPanel();
        jP_display_applications.setLayout(new WrapLayout(FlowLayout.CENTER));
        ((WrapLayout) jP_display_applications.getLayout()).setHgap(1);
        ((WrapLayout) jP_display_applications.getLayout()).setVgap(1);
        jP_display_applications.setPreferredSize(new Dimension((int) ((screen_width - servicesPanelWidth) * 3 / 4.0), screen_height - 150));
        jP_display_applications.setBorder(BorderFactory.createEtchedBorder());
        jP_display.add(jP_display_servers, BorderLayout.WEST);
        jP_display.add(jP_display_applications, BorderLayout.CENTER);
        jP_main.add(buildTopPanel(), BorderLayout.NORTH);
        jP_main.add(buildServicesPanel(), BorderLayout.EAST);
        //jSP_display = new JScrollPane(jP_display);
        jP_main.add(jP_display, BorderLayout.CENTER);
        getContentPane().add(jP_main);
        mL_actorComponent = new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Object source = e.getSource();
                if (source instanceof ActorComponent) {
                    if (!((ActorComponent) source).isHighlighted()) {
                        displayLinks((ActorComponent) source);
                    } else {
                        displayLinks(null);
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
                    displayLinks((ActorComponent) source);
                    displayActor((ActorComponent) source);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Object source = e.getSource();
                if (source instanceof ActorComponent) {
                    displayLinks(null);
                    displayActor(null);
                }
            }
        };
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
        for (String service : orderedServiceNames) {
            ((DefaultListModel<String>) jL_services.getModel()).addElement(service);
        }
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
                playEvolution();
            } else {
                jB_play.setText("|>");
                timer.stop();
                isPlaying = false;
            }
        });
        jCB_dead = new JCheckBox();
        jCB_dead.setText("Display dead actors");
        jCB_dead.addActionListener(e -> {
            displayDead = jCB_dead.isSelected();
            if (displayDead) {
                componentBaseSize = smallComponentSize;
                jP_display_servers.setPreferredSize(new Dimension((int) (screen_width * 3 / 4.0) - 120, screen_height - 150));
                jP_display_applications.setPreferredSize(new Dimension((int) (screen_width / 4.0) - 120, screen_height - 150));
            } else {
                componentBaseSize = bigComponentSize;
                jP_display_servers.setPreferredSize(new Dimension((int) (screen_width / 4.0) - 120, screen_height - 150));
                jP_display_applications.setPreferredSize(new Dimension((int) (screen_width * 3 / 4.0) - 120, screen_height - 150));
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
        //jP_slider.add(jCB_dead);
        return jP_slider;
    }

    public void playEvolution() {
        ActionListener taskPerformer = evt -> setGraph(currentStep < stepNumber ? ++currentStep : stepNumber);
        timer = new Timer(1000, taskPerformer);
        timer.start();
    }

    public void display() {
        setSize(new Dimension(screen_width, screen_height));
        setGraph(0);
        setVisible(true);
    }

    public void preParseInputFile(String inputFile) {
        System.out.println("Parsing log file...");
        serverNames = new LinkedHashSet<>();
        applicationNames = new LinkedHashSet<>();
        serviceNames = new LinkedHashSet<>();
        graphHistory = new HashMap<>();
        serverHistory = new HashMap<>();
        applicationHistory = new HashMap<>();
        int stepCounter = 0;
        try {
            BufferedReader br = Files.newBufferedReader(new File(inputFile).toPath());
            String line;
            while ((line = br.readLine()) != null) {
                graphHistory.put(stepCounter, new HashMap<>());
                serverHistory.put(stepCounter, new ArrayList<>());
                applicationHistory.put(stepCounter, new ArrayList<>());
                String[] keys = line.split("\\|");
                maxSimultaneousServers = Math.max(keys.length, maxSimultaneousServers);
                for (String key : keys) {
                    String server = key.split("=")[0];
                    String applications = key.split("=")[1];
                    serverNames.add(server.split("/")[0]);
                    FakeServer fakeServer = new FakeServer(server.split("/")[0], Integer.parseInt(server.split("/")[1]),
                            Integer.parseInt(server.split("/")[2]), Integer.parseInt(server.split("/")[3]), Integer.parseInt(server.split("/")[4]),
                            Arrays.asList(server.split("/")).subList(5, server.split("/").length));
                    serverHistory.get(stepCounter).add(fakeServer);
                    graphHistory.get(stepCounter).put(fakeServer, new ArrayList<>());
                    maxServerGeneration = Math.max(Integer.parseInt(server.split("/")[1]), maxServerGeneration);
                    maxServerConnections = Math.max(Integer.parseInt(server.split("/")[2]), maxServerConnections);
                    maxServerSize = Math.max(Arrays.asList(server.split("/")).size(), maxServerSize);
                    serviceNames.addAll(Arrays.asList(server.split("/")).subList(5, server.split("/").length));
                    maxSimultaneousApplications = Math.max(applications.split(";").length, maxSimultaneousApplications);
                    for (String application : applications.split(";")) {
                        FakeApplication fakeApplication = new FakeApplication(application.split("/")[0], Integer.parseInt(application.split("/")[1]),
                                Integer.parseInt(application.split("/")[2]), Integer.parseInt(application.split("/")[3]),
                                Arrays.asList(application.split("/")).subList(4, application.split("/").length));
                        applicationHistory.get(stepCounter).add(fakeApplication);
                        graphHistory.get(stepCounter).get(fakeServer).add(fakeApplication);
                        applicationNames.add(application.split("/")[0]);
                        maxApplicationGeneration = Math.max(Integer.parseInt(application.split("/")[1]), maxApplicationGeneration);
                        maxApplicationConnections = Math.max(Integer.parseInt(application.split("/")[2]), maxApplicationConnections);
                        maxApplicationSize = Math.max(Arrays.asList(application.split("/")).size(), maxApplicationSize);
                        serviceNames.addAll(Arrays.asList(application.split("/")).subList(4, application.split("/").length));
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
                    serverSituationStep.add(new ActorComponent(livingServer, state));
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
                    applicationSituationStep.add(new ActorComponent(livingApplication, state));
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

    public void setGraph(int step) {
        currentStep = step;
        jTF_step.setText(Integer.toString(currentStep));
        jS_slider.setValue(currentStep);
        Map<String, Double> servicesDemand = new HashMap<>();
        for (String service : serviceNames) {
            servicesDemand.put(service, 0.0);
            for (ActorComponent actorComponent : applicationSituationHistory.get(step)) {
                if (actorComponent.getServices().contains(service)) {
                    servicesDemand.put(service, servicesDemand.get(service) + 1);
                }
            }
            servicesDemand.put(service, servicesDemand.get(service) / (double)applicationSituationHistory.get(step).size());
        }
        ((BoldListCellRenderer) jL_services.getCellRenderer()).setItemsDemand(servicesDemand);
        jP_display_servers.removeAll();
        jP_display_applications.removeAll();
        jP_main.updateUI();
        int aliveCounter = 0;
        for (ActorComponent actorComponent : serverSituationHistory.get(step)) {
            actorComponent.addMouseListener(mL_actorComponent);
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
        for (ActorComponent actorComponent : applicationSituationHistory.get(step)) {
            actorComponent.addMouseListener(mL_actorComponent);
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
                for (ActorComponent connected : connectedTo) {
                    if (connected.getServices().contains(service)) {
                        satisfactionMap.put(service, satisfactionMap.get(service) + 1);
                    }
                }
                satisfactionMap.put(service, satisfactionMap.get(service) / (double)connectedTo.size());
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
        } else {
            for (ActorComponent displayedApplication : applicationSituationHistory.get(currentStep)) {
                displayedApplication.setHighlighted(false);
            }
            for (ActorComponent displayedServer : serverSituationHistory.get(currentStep)) {
                displayedServer.setHighlighted(false);
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
                item.setBackground(new Color((int)(boldItems.get(value) * 255), 0, 0));
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
}

class FakeServer extends FakeActor {
    int maxConnections;

    public FakeServer(String name, int generation, int maxConnections, int connections, int age, List<String> services) {
        super(name, generation, connections, age, services);
        this.maxConnections = maxConnections;
    }
}

class FakeApplication extends FakeActor {
    public FakeApplication(String name, int generation, int connections, int age, List<String> services) {
        super(name, generation, connections, age, services);
    }
}