package individual;

import eu.diversify.ffbpg.BPGraph;
import eu.diversify.ffbpg.Facade;
import eu.diversify.ffbpg.Platform;
import eu.diversify.ffbpg.random.IntegerGenerator;
import eu.diversify.ffbpg.random.IntegerSetGenerator;
import individual.gui.Display;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.ui.RefineryUtilities;
import tools.Tools;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by aelie on 03/09/14.
 */
public class Simulator {
    static String currentPath;
    static int maxTime = 500;
    static boolean useManager = false;
    private static Simulator INSTANCE;
    static double serverReproductionModifier = 1;
    static double serverReproductionProbability = 0.31;
    static double serverDeathProbability = 0.3;
    static double applicationReproductionProbability = 0.31;
    static double applicationDeathProbability = 0.3;
    static double mutationProbability = 0.1;
    static int applicationPoolSize = 300;
    static int serverPoolSize = 100;
    public static int serverMaxConnections = 18;
    static int servicePoolSize = 50;
    static double serviceListRatio = 0.2;
    static double generatorLambda = 0.25;
    static double generatorUniform = 0.005;
    static int generatorPoisson = 6;
    static int neighborhoodSize = 10;
    Set<Server> serverPool;
    Set<Application> applicationPool;
    Set<Service> servicePool;
    int serverCounter = 0;
    int applicationCounter = 0;
    int serviceCounter = 0;
    int currentTime = 0;
    boolean onPause = false;
    Random random;
    Map<Server, Set<Application>> connections;
    Set<Application> disconnectedApplications;
    static int robustnessRuns = 50;
    static int simulationRuns = 50;
    static Map<Integer, Map<Integer, List<Double>>> robustnessRandomShuffleHistory;
    static Map<Integer, Map<Integer, List<Double>>> robustnessRandomForwardHistory;
    static Map<Integer, Map<Integer, List<Double>>> robustnessRandomBackwardHistory;
    static Map<Integer, Map<Integer, List<Double>>> robustnessServiceShuffleHistory;
    Map<Integer, Map<String, Double>> costHistory;
    Map<Integer, Set<Server>> serverHistory;
    Map<Integer, Set<Application>> applicationHistory;
    SimulationManager manager;
    DisplayGraph dg;
    boolean silentMode = false;

    static boolean evolveServersNeutral = false;
    static boolean evolveServersEcology = false;
    static boolean evolveAndMutateServersEcology = false;
    static boolean evolveApplications = false;
    static boolean evolveServersWorstToBest = false;
    static int applicationMinimumSize = 1;
    static int poisson = -1;
    static int negexp = -1;
    static String display = null;
    static boolean log = false;

    static boolean testRobustness = false;
    static boolean testSimulations = false;

    static String resultFolder;

    private Simulator() {
    }

    public static Simulator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Simulator();
        }
        return INSTANCE;
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-path") && i < args.length - 1) {
                currentPath = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("-minsize") && i < args.length - 1) {
                applicationMinimumSize = Integer.parseInt(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-runs") && i < args.length - 1) {
                simulationRuns = Integer.parseInt(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-SN")) {
                evolveServersNeutral = true;
            }
            if (args[i].equalsIgnoreCase("-SE")) {
                evolveServersEcology = true;
            }
            if (args[i].equalsIgnoreCase("-MSE")) {
                evolveAndMutateServersEcology = true;
            }
            if (args[i].equalsIgnoreCase("-A")) {
                evolveApplications = true;
            }
            if (args[i].equalsIgnoreCase("-SWTB")) {
                evolveServersWorstToBest = true;
            }
            if (args[i].equalsIgnoreCase("-BL")) {
                evolveServersNeutral = false;
                evolveServersEcology = true;
                evolveServersWorstToBest = false;
                evolveApplications = true;
            }
            if (args[i].equalsIgnoreCase("-SS")) {
                evolveServersNeutral = false;
                evolveServersEcology = true;
                evolveServersWorstToBest = true;
                evolveApplications = true;
            }
            if (args[i].equalsIgnoreCase("-negexp") && i < args.length - 1) {
                negexp = Integer.parseInt(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-poisson") && i < args.length - 1) {
                poisson = Integer.parseInt(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-display") && i < args.length - 1) {
                display = args[i + 1];
            }
            if (args[i].equalsIgnoreCase("-log")) {
                log = true;
            }
        }
        if (display != null) {
            new Display(display).display();
            return;
        }
        if (!evolveServersNeutral && !evolveServersEcology && !evolveAndMutateServersEcology && !evolveApplications && !evolveServersWorstToBest) {
            System.err.println("Please choose a strategy between -SN, -SE, MSE, -A and -SWTB (Smart&Simple is -SE -SWTB -A)");
            return;
        } else {
            System.out.println("Using evolution strategies:");
            if (evolveServersNeutral) System.out.println("- Servers Neutral");
            if (evolveServersEcology) System.out.println("- Servers Ecology");
            if (evolveAndMutateServersEcology) System.out.println("- Mutated Servers Ecology");
            if (evolveServersWorstToBest) System.out.println("- Servers Worst To Best");
            if (evolveApplications) System.out.println("- Applications");
        }
        if (negexp < 0 && poisson < 0) {
            System.err.println("Please choose a distribution and parameter between -negexp param and -poisson param");
            return;
        }
        System.out.println("Using distribution:");
        if (negexp >= 0) System.out.println("- NegExp (lambda=" + negexp + ")");
        if (poisson >= 0) System.out.println("- Poisson (lambda=" + poisson + ")");
        System.out.println("Simulating " + simulationRuns + " runs");
        System.out.println("Path: " + currentPath);
        System.out.println("MinSize: " + applicationMinimumSize);
        Simulator simulator = Simulator.getInstance();
        if (testSimulations) {
            simulator.testSimulationRuns(1000);
        } else {
            if (testRobustness) {
                simulator.warmup(1, true, false);
                simulator.testRobustnessRuns(1000);
            } else {
                // MAIN SIMULATION STARTS HERE
                if (!useManager) {
                    if (simulationRuns > 1) {
                        List<DescriptiveStatistics> meanRobustnessList = new ArrayList<>();
                        readProperties();
                        for (int i = 0; i < maxTime; i++) {
                            meanRobustnessList.add(new DescriptiveStatistics());
                        }
                        for (int simulationRun = 0; simulationRun < simulationRuns; simulationRun++) {
                            simulator.warmup(1, true, false);
                            simulator.start(false);
                            for (int step = 0; step < maxTime; step++) {
                                for (int robustnessRun = 0; robustnessRun < robustnessRuns; robustnessRun++) {
                                    meanRobustnessList.get(step).addValue(robustnessRandomShuffleHistory.get(step).get(robustnessRun).get(0));
                                }
                            }
                        }
                        writeCompiledRobustnessToFile(meanRobustnessList, currentPath + System.getProperty("file.separator")
                                + "compiledRobustness"
                                + (evolveServersNeutral ? "-SN" : "")
                                + (evolveServersEcology ? "-SE" : "")
                                + (evolveAndMutateServersEcology ? "-MSE" : "")
                                + (evolveServersWorstToBest ? "-SWTB" : "")
                                + (evolveApplications ? "-A" : "")
                                + (poisson >= 0 ? "-poisson-" + poisson : "-negexp-" + negexp)
                                + "-" + applicationMinimumSize
                                + ".csv");
                    } else {
                        simulator.warmup(1, true, false);
                        simulator.start(true);
                    }
                }
            }
        }
    }

    public static void readProperties() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("config.properties"));
            maxTime = Integer.parseInt(prop.getProperty("maxTime", "500"));
            serverReproductionModifier = Integer.parseInt(prop.getProperty("serverReproductionModifier", "1"));
            serverReproductionProbability = Double.parseDouble(prop.getProperty("serverReproductionProbability", "0.31"));
            serverDeathProbability = Double.parseDouble(prop.getProperty("serverDeathProbability", "0.3"));
            applicationReproductionProbability = Double.parseDouble(prop.getProperty("applicationReproductionProbability", "0.31"));
            applicationDeathProbability = Double.parseDouble(prop.getProperty("applicationDeathProbability", "0.3"));
            mutationProbability = Double.parseDouble(prop.getProperty("mutationProbability", "0.2"));
            applicationPoolSize = Integer.parseInt(prop.getProperty("applicationPoolSize", "300"));
            serverPoolSize = Integer.parseInt(prop.getProperty("serverPoolSize", "100"));
            serverMaxConnections = Integer.parseInt(prop.getProperty("serverMaxConnections", "18"));
            servicePoolSize = Integer.parseInt(prop.getProperty("servicePoolSize", "50"));
            serviceListRatio = Double.parseDouble(prop.getProperty("serviceListRatio", "0.2"));
            generatorLambda = Double.parseDouble(prop.getProperty("generatorLambda", "0.25"));
            generatorUniform = Double.parseDouble(prop.getProperty("generatorUniform", "0.005"));
            generatorPoisson = Integer.parseInt(prop.getProperty("generatorPoisson", "6"));
            robustnessRuns = Integer.parseInt(prop.getProperty("robustnessRuns", "50"));
        } catch (IOException ex) {
            System.err.println("config.properties file not found, using default values");
        }
    }

    public void setVariables(int applicationPoolSize, int serverPoolSize, int servicePoolSize,
                             double mutationProbability, int serverMaxConnexion,
                             int maxTime, int robustnessRuns) {
        this.applicationPoolSize = applicationPoolSize;
        this.serverPoolSize = serverPoolSize;
        this.servicePoolSize = servicePoolSize;
        this.mutationProbability = mutationProbability;
        this.serverMaxConnections = serverMaxConnexion;
        Simulator.maxTime = maxTime;
        this.robustnessRuns = robustnessRuns;
    }

    public void warmup(int seed, boolean useFFBPG, boolean silentMode) {
        if (currentPath == null) {
            try {
                currentPath = ClassLoader.getSystemResource("").getPath();
            } catch (NullPointerException npe) {
                currentPath = System.getProperty("user.dir");
            }
        }
        readProperties();
        currentTime = 0;
        serverCounter = 0;
        applicationCounter = 0;
        serviceCounter = 0;
        random = new Random(seed);
        this.silentMode = silentMode;
        if (useManager) {
            EventQueue.invokeLater(() -> {
                manager = new SimulationManager(maxTime);
                manager.display();
            });
        }
        //dg = new DisplayGraph();
        //gd = new GraphicalDisplay();
        //gd.display();
        init(useFFBPG);
        //forceInit();
        //exportGraph();
        //displayGraph(-2);
        if (!useFFBPG) {
            initialLink();
        }
        //displayGraph(-1);
        robustnessRandomShuffleHistory = new HashMap<>();
        robustnessRandomShuffleHistory.put(0, Tools.robustnessParallel(connections, robustnessRuns, Tools.SHUFFLE_ORDER, Tools.RANDOM_EXTINCTION));
        robustnessRandomForwardHistory = new HashMap<>();
        robustnessRandomForwardHistory.put(0, Tools.robustnessParallel(connections, robustnessRuns, Tools.FORWARD_ORDER, Tools.RANDOM_EXTINCTION));
        robustnessRandomBackwardHistory = new HashMap<>();
        robustnessRandomBackwardHistory.put(0, Tools.robustnessParallel(connections, robustnessRuns, Tools.BACKWARD_ORDER, Tools.RANDOM_EXTINCTION));
        robustnessServiceShuffleHistory = new HashMap<>();
        robustnessServiceShuffleHistory.put(0, Tools.robustnessParallel(connections, robustnessRuns, Tools.SHUFFLE_ORDER, Tools.SERVICE_EXTINCTION));
        costHistory = new HashMap<>();
        serverHistory = new HashMap<>();
        applicationHistory = new HashMap<>();
        disconnectedApplications = new LinkedHashSet<>();
    }

    public void init(boolean fromFFBPG) {
        servicePool = new LinkedHashSet<>();
        serverPool = new LinkedHashSet<>();
        applicationPool = new LinkedHashSet<>();
        connections = new LinkedHashMap<>();
        if (fromFFBPG) {
            // RECUPERATION DU MODELE DEPUIS L'API DE FRANCK
            //IntegerGenerator sizes_generator = Facade.getNegExpIntegerGenerator(15);
            IntegerGenerator apps_sizes_generator = null;
            IntegerGenerator plats_sizes_generator = null;
            if (poisson >= 0) {
                apps_sizes_generator = Facade.getPoissonIntegerGenerator(poisson);
                plats_sizes_generator = Facade.getPoissonIntegerGenerator(poisson);
            } else if (negexp >= 0) {
                apps_sizes_generator = Facade.getNegExpIntegerGenerator(negexp);
                plats_sizes_generator = Facade.getNegExpIntegerGenerator(negexp);
            }
            IntegerSetGenerator srv_generator = Facade.getNegExpIntegerSetGenerator(generatorLambda, generatorUniform);
            /*BPGraph ffbpg = Facade.createRandomBPGraph(applicationPoolSize, serverPoolSize, servicePoolSize,
                    sizes_generator, srv_generator, serverPoolSize, serverMaxConnections, applicationMinimumSize);*/
            BPGraph ffbpg = Facade.createRandomBPGraph(applicationPoolSize, serverPoolSize, servicePoolSize,
                    apps_sizes_generator, plats_sizes_generator, srv_generator, serverPoolSize, serverMaxConnections, applicationMinimumSize, applicationMinimumSize);
            for (Integer serviceId : ffbpg.getAllUsedServices().toArray()) {
                servicePool.add(new Service("s" + serviceId, serviceId, mutationProbability));
            }
            Map<eu.diversify.ffbpg.Application, Application> applicationMap = new LinkedHashMap<>();
            for (eu.diversify.ffbpg.Application ffbpgApplication : ffbpg.getApplications()) {
                Application application = new Application(ffbpgApplication.getName(), 0, null);
                Set<Service> services = new LinkedHashSet<>();
                for (Integer serviceId : ffbpgApplication.getRequiredServices().toArray()) {
                    services.addAll(servicePool.stream()
                            .filter(service -> service.getId() == serviceId)
                            .collect(Collectors.toList()));
                }
                application.setServices(services);
                applicationPool.add(application);
                applicationMap.put(ffbpgApplication, application);
            }
            for (Platform platform : ffbpg.getPlatforms()) {
                Server server = new Server(platform.getName(), 0, null, serverMaxConnections);
                Set<Service> services = new LinkedHashSet<>();
                for (Integer serviceId : platform.getProvidedServices().toArray()) {
                    services.addAll(servicePool.stream()
                            .filter(service -> service.getId() == serviceId)
                            .collect(Collectors.toList()));
                }
                server.setServices(services);
                serverPool.add(server);
                for (eu.diversify.ffbpg.Application ffbpgApplication : ffbpg.getLinkedApplicationsForPlatform(platform)) {
                    if (!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<>());
                    }
                    connections.get(server).add(applicationMap.get(ffbpgApplication));
                    server.addConnection();
                }
            }
        } else {
            for (serviceCounter = 0; serviceCounter < servicePoolSize; serviceCounter++) {
                servicePool.add(new Service("s" + serviceCounter, serviceCounter, mutationProbability));
            }
            for (serverCounter = 0; serverCounter < serverPoolSize; serverCounter++) {
                serverPool.add(new Server("S" + serverCounter, 0, Tools.extractServiceList(servicePool, serviceListRatio), serverMaxConnections));
            }
            for (applicationCounter = 0; applicationCounter < applicationPoolSize; applicationCounter++) {
                applicationPool.add(new Application("A" + applicationCounter, 0, Tools.extractServiceList(servicePool, serviceListRatio)));
            }
        }
        System.out.println("Applications: " + applicationPool.size() + "/" + applicationPoolSize + " | Servers: " + serverPool.size() + "/" + serverPoolSize);
    }

    public void start(boolean displayCharts) {
        runUntil(maxTime);
        //displayGraph(currentTime);
        if (!silentMode) {
            System.out.println("R" + currentTime + " :: " + getRobustnessRandomShuffleHistory().get(getRobustnessRandomShuffleHistory().size() - 1));
        }
        System.out.println("Writing charts to file.");
        resultFolder = currentPath + System.getProperty("file.separator") + "results" + System.getProperty("file.separator") + System.currentTimeMillis();
        new File(resultFolder).mkdirs();
        if (displayCharts) {
            Charts charts = new Charts();
            charts.init(currentTime);
            charts.getRobustnessFrame().pack();
            RefineryUtilities.centerFrameOnScreen(charts.getRobustnessFrame());
            charts.getRobustnessFrame().setVisible(true);
            charts.getCostFrame().pack();
            RefineryUtilities.centerFrameOnScreen(charts.getCostFrame());
            charts.getCostFrame().setVisible(true);
            charts.extractCharts(resultFolder);
        }
        System.out.println("Writing robustnesses to file.");
        writeRobustnessToFile(robustnessRandomShuffleHistory, resultFolder + System.getProperty("file.separator") + "random_shuffle.csv");
        /*writeRobustnessToFile(robustnessRandomForwardHistory, resultFolder + System.getProperty("file.separator") + "random_forward.csv");
        writeRobustnessToFile(robustnessRandomBackwardHistory, resultFolder + System.getProperty("file.separator") + "random_backward.csv");
        writeRobustnessToFile(robustnessServiceShuffleHistory, resultFolder + System.getProperty("file.separator") + "service_shuffle.csv");*/
        System.out.println("All done!");
    }

    public void runUntil(int desiredTime) {
        PrintWriter pw_C = null;
        if (log) {
            try {
                pw_C = new PrintWriter("connections.log", "UTF-8");
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        int targetTime = Math.min(desiredTime, maxTime);
        while (currentTime < targetTime && !onPause) {
            if (manager != null) {
                manager.updateSliderPosition(currentTime);
            }
            if (evolveServersNeutral) {
                evolveServersNeutral();
            }
            if (evolveServersEcology) {
                evolveServersEcological();
            }
            if (evolveAndMutateServersEcology) {
                evolveAndMutateServersEcological();
            }
            if (evolveApplications) {
                evolveApplications();
            }
            if (evolveServersWorstToBest) {
                evolveServersWorstToBestService();
            }
            //relink();
            relinkWithNeighborhood();
            older();
            //gd.setGraph(connections);
            currentTime++;
            Map<String, Double> cost = new HashMap<>();
            cost.put("TotalOfferedServices", (double) connections.keySet()
                    .stream()
                    .mapToInt(server -> server.getServices().size())
                    .sum());
            double totalLinks = (double) connections.keySet()
                    .stream()
                    .mapToInt(Server::getCurrentConnectionNumber)
                    .sum();
            cost.put("TotalLinks", totalLinks);
            cost.put("TotalServers", (double) Tools.getAliveServers(connections).size());
            cost.put("TotalApplications", (double) Tools.getAliveApplications(connections).size());
            cost.put("MeanServerConnectionNumber", connections.keySet()
                    .stream()
                    .mapToInt(Server::getCurrentConnectionNumber)
                    .summaryStatistics()
                    .getAverage());
            cost.put("MeanApplicationConnectionNumber", Tools.getAliveApplications(connections)
                    .stream()
                    .mapToInt(application -> Tools.getProvidingServers(application, connections).size())
                    .summaryStatistics()
                    .getAverage());
            cost.put("MeanApplicationSize", Tools.getAliveApplications(connections)
                    .stream()
                    .mapToInt(application -> application.getServices().size())
                    .summaryStatistics()
                    .getAverage());
            //cost.put("Diversity", Tools.diversity(Tools.getAliveServers(connections)));
            //cost.put("Richness", Tools.richness(Tools.getAliveServers(connections)));
            cost.put("Evenness", Tools.evenness(Tools.getAliveServers(connections)));
            //cost.put("Disparity", Tools.disparity(Tools.getAliveServers(connections)));
            cost.put("Connectivity", totalLinks / (
                    (double) Tools.getAliveServers(connections).size() *
                            (double) Tools.getAliveApplications(connections).size()));
            //weighed links
            double linkWeight = 0;
            for (Server server : connections.keySet()) {
                for (Application application : connections.get(server)) {
                    linkWeight += Tools.getMatchingServices(server.getServices(), application.getServices()).size();
                }
            }
            cost.put("WeighedLinks", linkWeight / totalLinks);
            costHistory.put(currentTime, cost);
            robustnessRandomShuffleHistory.put(currentTime, Tools.robustnessParallel(connections, robustnessRuns, Tools.SHUFFLE_ORDER, Tools.RANDOM_EXTINCTION));
            robustnessRandomForwardHistory.put(currentTime, Tools.robustnessParallel(connections, 1, Tools.FORWARD_ORDER, Tools.RANDOM_EXTINCTION));
            robustnessRandomBackwardHistory.put(currentTime, Tools.robustnessParallel(connections, 1, Tools.BACKWARD_ORDER, Tools.RANDOM_EXTINCTION));
            robustnessServiceShuffleHistory.put(currentTime, Tools.robustnessParallel(connections, robustnessRuns, Tools.SHUFFLE_ORDER, Tools.SERVICE_EXTINCTION));
            serverHistory.put(currentTime, Tools.getAliveServers(connections));
            applicationHistory.put(currentTime, Tools.getAliveApplications(connections));

            if (pw_C != null) {
                String logOutput = "";
                for (Server server : connections.keySet()) {
                    logOutput += server.toVerboseString(connections) + "=";
                    for (Application application : connections.get(server)) {
                        logOutput += application.toVerboseString(connections) + ";";
                    }
                    logOutput += "|";
                }
                SummaryStatistics meanRobustness = new SummaryStatistics();
                for (List<Double> extinctionSequence : robustnessRandomShuffleHistory.get(currentTime).values()) {
                    meanRobustness.addValue(extinctionSequence.get(0));
                }
                logOutput += meanRobustness.getMean() + "|";
                meanRobustness = new SummaryStatistics();
                for (List<Double> extinctionSequence : robustnessRandomForwardHistory.get(currentTime).values()) {
                    meanRobustness.addValue(extinctionSequence.get(0));
                }
                logOutput += meanRobustness.getMean() + "|";
                meanRobustness = new SummaryStatistics();
                for (List<Double> extinctionSequence : robustnessRandomBackwardHistory.get(currentTime).values()) {
                    meanRobustness.addValue(extinctionSequence.get(0));
                }
                logOutput += meanRobustness.getMean() + "|";
                meanRobustness = new SummaryStatistics();
                for (List<Double> extinctionSequence : robustnessServiceShuffleHistory.get(currentTime).values()) {
                    meanRobustness.addValue(extinctionSequence.get(0));
                }
                logOutput += meanRobustness.getMean();
                pw_C.println(logOutput);
                pw_C.flush();
            }
        }
        if (pw_C != null) {
            pw_C.close();
        }
    }

    public void runSteps(int desiredSteps) {
        runUntil(currentTime + desiredSteps);
    }

    public Map<Integer, Map<Integer, List<Double>>> getRobustnessRandomShuffleHistory() {
        return robustnessRandomShuffleHistory;
    }

    public Map<Integer, Map<Integer, List<Double>>> getRobustnessRandomForwardHistory() {
        return robustnessRandomForwardHistory;
    }

    public Map<Integer, Map<Integer, List<Double>>> getRobustnessRandomBackwardHistory() {
        return robustnessRandomBackwardHistory;
    }

    public Map<Integer, Map<Integer, List<Double>>> getRobustnessServiceShuffleHistory() {
        return robustnessServiceShuffleHistory;
    }

    public Map<Integer, Map<String, Double>> getCostHistory() {
        return costHistory;
    }

    public Map<Integer, Set<Server>> getServerHistory() {
        return serverHistory;
    }

    public void displayGraph(int time) {
        if (!silentMode) {
            System.out.println("T=" + time);
            System.out.println("A:" + applicationPool + System.getProperty("line.separator")
                    + "S:" + serverPool + System.getProperty("line.separator")
                    + "s:" + servicePool);
            System.out.println(connections);
            if (dg != null) {
                dg.displayGraph(connections);
            }
        }
    }

    public void older() {
        for (Server server : Tools.getAliveServers(connections)) {
            server.older();
        }
        for (Application application : Tools.getAliveApplications(connections)) {
            application.older();
        }
    }

    public void evolveServersNeutral() {
        Set<Server> serversToBeCloned = new LinkedHashSet<>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<>();
        for (Server server : Tools.getAliveServers(connections)) {
            if (getRandom().nextDouble() < serverReproductionProbability) {
                serversToBeCloned.add(server);
            }
            if (getRandom().nextDouble() < serverDeathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("SC" + serverCounter, server.getGeneration() + 1, Tools.getMutatedServices(server), server.getMaxConnectionNumber());
            connections.put(clone, new LinkedHashSet<>());
        }
        disconnectedApplications.addAll(Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved));
    }

    public void evolveServersEcological() {
        Set<Server> serversToBeCloned = new LinkedHashSet<>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<>();
        for (Server server : Tools.getAliveServers(connections)) {
            double reproductionProbability = (double) server.getCurrentConnectionNumber() / (double) server.getMaxConnectionNumber() * serverReproductionModifier;
            if (getRandom().nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
            }
            double deathProbability = 1 - reproductionProbability;
            if (getRandom().nextDouble() < deathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("SC" + serverCounter, server.getGeneration() + 1, Tools.getMutatedServices(server), server.getMaxConnectionNumber());
            connections.put(clone, new LinkedHashSet<>());
        }
        disconnectedApplications.addAll(Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved));
    }

    public void evolveAndMutateServersEcological() {
        Set<Server> serversToBeCloned = new LinkedHashSet<>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<>();
        for (Server server : Tools.getAliveServers(connections)) {
            double reproductionProbability = (double) server.getCurrentConnectionNumber() / (double) server.getMaxConnectionNumber() * serverReproductionModifier;
            if (getRandom().nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
            }
            double deathProbability = 1 - reproductionProbability;
            if (getRandom().nextDouble() < deathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("SC" + serverCounter, server.getGeneration() + 1, Tools.getMostUselessServicesMutated(server, connections), server.getMaxConnectionNumber());
            connections.put(clone, new LinkedHashSet<>());
        }
        disconnectedApplications.addAll(Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved));
    }

    public void evolveApplications() {
        List<Application> applications = new ArrayList<>(Tools.getAliveApplications(connections));
        if (getRandom().nextDouble() < applicationReproductionProbability && applications.size() > 0) {
            Application father = applications.get((int) (getRandom().nextDouble() * applications.size()));
            Application mother = applications.get((int) (getRandom().nextDouble() * applications.size()));
            Set<Service> babyServices = new LinkedHashSet<>(father.getServices());
            babyServices.addAll(mother.getServices());
            List<Service> babyServicesList = new ArrayList<>(babyServices);
            Collections.shuffle(babyServicesList, getRandom());
            babyServices = new LinkedHashSet<>(babyServicesList.subList(
                    0,
                    Math.min(babyServicesList.size(), babyServicesList.size() / 2 + (getRandom().nextDouble() > 0.5 ? 2 : 0))));
            Application baby = new Application("AB" + applicationCounter++, Math.max(father.getGeneration(), mother.getGeneration()) + 1, babyServices);
            baby.setParents(father, mother);
            applicationPool.add(baby);
            disconnectedApplications.add(baby);
            System.out.println("[" + currentTime + "]++ " + baby + "(" + baby.getServices().size() + ")");
        }
        if (getRandom().nextDouble() < applicationDeathProbability && applications.size() > 0) {
            Application stoneColdDead = applications.get((int) (getRandom().nextDouble() * applications.size()));
            connections.keySet().stream()
                    .filter(server -> connections.get(server).remove(stoneColdDead))
                    .forEach(Server::removeConnection);
            applicationPool.remove(stoneColdDead);
            disconnectedApplications.remove(stoneColdDead);
            System.out.println("[" + currentTime + "]-- " + stoneColdDead);
        }
    }

    public void evolveServersWorstToBestService() {
        for (Server server : connections.keySet()) {
            //find least wanted available service
            Map<Service, Integer> satisfiedServicesAmount = new HashMap<>();
            for (Application application : connections.get(server)) {
                for (Service service : Tools.getMatchingServices(server.getServices(), application.getServices())) {
                    if (!satisfiedServicesAmount.containsKey(service)) {
                        satisfiedServicesAmount.put(service, 0);
                    }
                    satisfiedServicesAmount.put(service, satisfiedServicesAmount.get(service) + 1);
                }
            }
            List<Service> orderedSatisfiedServices = Arrays.asList(satisfiedServicesAmount.keySet()
                    .stream()
                    .sorted((o1, o2) -> satisfiedServicesAmount.get(o1) - satisfiedServicesAmount.get(o2))
                    .toArray(Service[]::new));
            //find most needed required service
            Map<Service, Integer> requiredNotAvailableAmount = new HashMap<>();
            for (Application application : connections.get(server)) {
                Set<Service> applicationServices = new LinkedHashSet<>(application.getServices());
                applicationServices.removeAll(Tools.getMatchingServices(server.getServices(), application.getServices()));
                for (Service service : applicationServices) {
                    if (!requiredNotAvailableAmount.containsKey(service)) {
                        requiredNotAvailableAmount.put(service, 0);
                    }
                    requiredNotAvailableAmount.put(service, requiredNotAvailableAmount.get(service) + 1);
                }
            }
            List<Service> orderedRequiredNotAvailable = Arrays.asList(requiredNotAvailableAmount.keySet()
                    .stream()
                    .sorted((o1, o2) -> requiredNotAvailableAmount.get(o2) - requiredNotAvailableAmount.get(o1))
                    .toArray(Service[]::new));
            //exchange least wanted with most needed
            if (orderedSatisfiedServices.size() > 0) {
                Set<Service> toMutate = new LinkedHashSet<>(orderedSatisfiedServices.subList(0, 1));
                for (Service service : toMutate) {
                    boolean serviceHarmless = true;
                    server.getServices().remove(service);
                    Set<Application> toDisconnect = connections.get(server).stream()
                            .filter(application -> Tools.getMatchingServices(application.getServices(),
                                    server.getServices()).size() == 0)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    for (Application application : toDisconnect) {
                        connections.get(server).remove(application);
                        if (Tools.isApplicationSatisfied(application, connections)) {
                            server.removeConnection();
                            serviceHarmless = true;
                        } else {
                            connections.get(server).add(application);
                            serviceHarmless = false;
                        }
                    }
                    if (serviceHarmless && orderedRequiredNotAvailable.size() > 0) {
                        server.getServices().add(orderedRequiredNotAvailable.get(0));
                    } else {
                        server.getServices().add(service);
                    }
                }
            }
        }
    }

    public void evolveConstantPopulation() {
        List<Server> serversToBeCloned = new ArrayList<>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<>();
        List<Server> orderedServers = new ArrayList<>(Tools.getAliveServers(connections));
        Collections.sort(orderedServers, (o1, o2) ->
                o1.getCurrentConnectionNumber() / o1.getMaxConnectionNumber() -
                        o2.getCurrentConnectionNumber() / o2.getMaxConnectionNumber());
        for (Server server : orderedServers) {
            double reproductionProbability = server.getCurrentConnectionNumber() / server.getMaxConnectionNumber() * serverReproductionModifier;
            if (getRandom().nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
                serversToBeRemoved.add(orderedServers.get(orderedServers.size() - serversToBeCloned.size()));
            }
        }
        int complement = serverPoolSize - orderedServers.size();
        serversToBeCloned.addAll(orderedServers.subList(0, complement));
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getGeneration() + 1, Tools.getMostUselessServicesMutated(server, connections), server.getMaxConnectionNumber());
            //clone.mutate();
            connections.put(clone, new LinkedHashSet<>());
        }
        disconnectedApplications = Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved);
    }

    public void initialLink() {
        if (!silentMode) {
            System.out.println("ILINK");
        }
        Set<Application> applicationsToBeRemoved = new LinkedHashSet<>();
        List<Application> sapp = new ArrayList<>(applicationPool);
        Collections.shuffle(sapp);
        applicationPool = new LinkedHashSet<>(sapp);
        for (Application app : applicationPool) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<>(app.getServices());
            Iterator<Server> iterServer = serverPool.iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<>());
                    }
                    connections.get(server).add(app);
                    server.addConnection();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
    }

    public void relink() {
        if (!silentMode) {
            System.out.println("[" + currentTime + "]RELINK(" + disconnectedApplications.size() + ")");
        }
        List<Application> applicationsToBeRemoved = new ArrayList<>();
        List<Application> applicationsRelinked = new ArrayList<>();
        for (Application application : disconnectedApplications) {
            Set<Service> unsatisfiedServices = Tools.getUnsatisfiedServices(application, connections);
            //Iterator<Server> iterServer = Tools.getAliveServers(connections).iterator();
            List<Server> serverList = new ArrayList<>(Tools.getAliveServers(connections));
            Collections.shuffle(serverList, getRandom());
            Iterator<Server> iterServer = new LinkedHashSet<>(serverList).iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<>());
                    }
                    connections.get(server).add(application);
                    server.addConnection();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(application);
            } else {
                applicationsRelinked.add(application);
            }
        }
        disconnectedApplications.addAll(applicationsToBeRemoved);
        disconnectedApplications.removeAll(applicationsRelinked);
        Set<Server> serversToBeRemoved = Tools.getAliveServers(connections).stream()
                .filter(server -> connections.get(server).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        disconnectedApplications.addAll(Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved));
    }

    public void relinkWithNeighborhood() {
        if (!silentMode) {
            System.out.println("[" + currentTime + "]RELINK-NEIGHBOR(" + disconnectedApplications.size() + ")");
        }
        List<Application> applicationsToBeRemoved = new ArrayList<>();
        List<Application> applicationsRelinked = new ArrayList<>();
        Set<Service> unsatisfiedServices;
        List<Server> serverList;
        Iterator<Server> iterServer;
        Server server;
        Set<Service> matchingServices;
        for (Application application : disconnectedApplications) {
            /*if (application.getNeighborhood() == null) {
                application.setNeighborhood(new LinkedHashSet<>());
            }
            if (application.getNeighborhood().size() < neighborhoodSize) {
                Set<Server> neighborhood = new LinkedHashSet<>();
                neighborhood.addAll(Tools.getProvidingServers(application, connections));
                if (neighborhood.size() < neighborhoodSize) {
                    while (neighborhood.size() < neighborhoodSize) {
                        neighborhood.add(Tools.getRandomElement(Tools.getAliveServers(connections)));
                    }
                }
                application.setNeighborhood(neighborhood);
            } else if (application.getNeighborhood().size() > neighborhoodSize) {
                System.err.println("NEIGHBORHOOD ERROR : " + application.getNeighborhood().size() + ">" + neighborhoodSize);
            }*/
            Set<Server> neighborhood = new LinkedHashSet<>();
            neighborhood.addAll(Tools.getProvidingServers(application, connections));
            for (int i = 0; i < neighborhoodSize; i++) {
                neighborhood.add(Tools.getRandomElement(Tools.getAliveServers(connections)));
            }
            application.setNeighborhood(neighborhood);
            unsatisfiedServices = Tools.getUnsatisfiedServices(application, connections);
            serverList = new ArrayList<>(application.getNeighborhood());
            Collections.shuffle(serverList, getRandom());
            iterServer = new LinkedHashSet<>(serverList).iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                server = iterServer.next();
                matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<>());
                    }
                    connections.get(server).add(application);
                    server.addConnection();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(application);
            } else {
                applicationsRelinked.add(application);
            }
        }
        disconnectedApplications.addAll(applicationsToBeRemoved);
        disconnectedApplications.removeAll(applicationsRelinked);
        Set<Server> serversToBeRemoved = Tools.getAliveServers(connections).stream()
                .filter(aliveServer -> connections.get(aliveServer).isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        disconnectedApplications.addAll(Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved));
    }

    public String getParametersAsString() {
        return "#A=" + Tools.getAliveApplications(connections).size() + "/" + applicationPoolSize +
                " | #S=" + Tools.getAliveServers(connections).size() + "/" + serverPoolSize + "[" + serverMaxConnections + "]" +
                " | #s=" + servicePoolSize +
                " | maxTime=" + maxTime +
                " | appRepro=" + applicationReproductionProbability +
                " | appDeath=" + applicationDeathProbability +
                " | robRuns=" + robustnessRuns +
                " | %mut=" + mutationProbability +
                " | Poisson/lambda/uniform=" + generatorPoisson + "/" + generatorLambda + "/" + generatorUniform;
    }

    public Map<String, Double> outputResults(Map<Integer, Map<Integer, List<Double>>> robustnessHistory) {
        Map<String, Double> result = new LinkedHashMap<>();
        //I100,I10,I30,F100,F10,F30
        List<SummaryStatistics> robustnesses = new ArrayList<>(Arrays.asList(new SummaryStatistics(), new SummaryStatistics(), new SummaryStatistics(),
                new SummaryStatistics(), new SummaryStatistics(), new SummaryStatistics()));
        //initial
        for (Integer index : robustnessHistory.get(0).keySet()) {
            List<Double> extinctionSequence = robustnessHistory.get(0).get(index);
            int index10 = (int) ((extinctionSequence.size() - 1) * 1.0 / 10.0);
            int index30 = (int) ((extinctionSequence.size() - 1) * 3.0 / 10.0);
            robustnesses.get(0).addValue(extinctionSequence.get(0));
            robustnesses.get(1).addValue(extinctionSequence.get(index10) / extinctionSequence.get(1));
            robustnesses.get(2).addValue(extinctionSequence.get(index30) / extinctionSequence.get(1));
        }
        //final
        for (Integer index : robustnessHistory.get(robustnessHistory.size() - 1).keySet()) {
            List<Double> extinctionSequence = robustnessHistory.get(robustnessHistory.size() - 1).get(index);
            int index10 = (int) ((extinctionSequence.size() - 1) * 1.0 / 10.0);
            int index30 = (int) ((extinctionSequence.size() - 1) * 3.0 / 10.0);
            robustnesses.get(3).addValue(extinctionSequence.get(0));
            robustnesses.get(4).addValue(extinctionSequence.get(index10) / extinctionSequence.get(1));
            robustnesses.get(5).addValue(extinctionSequence.get(index30) / extinctionSequence.get(1));
        }
        result.put("I10", robustnesses.get(1).getMean());
        result.put("F10", robustnesses.get(4).getMean());
        result.put("D10", robustnesses.get(4).getMean() - robustnesses.get(1).getMean());
        result.put("I30", robustnesses.get(2).getMean());
        result.put("F30", robustnesses.get(5).getMean());
        result.put("D30", robustnesses.get(5).getMean() - robustnesses.get(2).getMean());
        result.put("I100", robustnesses.get(0).getMean());
        result.put("F100", robustnesses.get(3).getMean());
        result.put("D100", robustnesses.get(3).getMean() - robustnesses.get(0).getMean());
        return result;
    }

    public void writeRobustnessToFile(Map<Integer, Map<Integer, List<Double>>> robustnessHistory, String fileName) {
        try {
            PrintWriter pw_R = new PrintWriter(fileName, "UTF-8");
            pw_R.println("Step,MeanValue");
            for (Integer step : robustnessHistory.keySet()) {
                SummaryStatistics meanRobustness = new SummaryStatistics();
                for (List<Double> extinctionSequence : robustnessHistory.get(step).values()) {
                    meanRobustness.addValue(extinctionSequence.get(0));
                }
                pw_R.println(step + "," + meanRobustness.getMean());
                pw_R.flush();
            }
            pw_R.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void writeCompiledRobustnessToFile(List<DescriptiveStatistics> meanRobustnessList, String fileName) {
        try {
            PrintWriter pw_R = new PrintWriter(fileName, "UTF-8");
            pw_R.println("Step,Mean,Min,Max,FirstQuartile,ThirdQuartile");
            for (int i = 0; i < meanRobustnessList.size(); i++) {
                pw_R.println(i
                        + "," + meanRobustnessList.get(i).getMean()
                        + "," + meanRobustnessList.get(i).getMin()
                        + "," + meanRobustnessList.get(i).getMax()
                        + "," + meanRobustnessList.get(i).getPercentile(25)
                        + "," + meanRobustnessList.get(i).getPercentile(75));
                pw_R.flush();
            }
            pw_R.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void testRobustnessRuns(int maxRobustnessRuns) {
        try {
            PrintWriter pw_R = new PrintWriter("robustnessRunsTest.csv", "UTF-8");
            pw_R.println("RobustnessRuns,Value,Value10%");
            for (int i = 1; i < maxRobustnessRuns + 1; i++) {
                System.out.println(i);
                SummaryStatistics robustness = new SummaryStatistics();
                SummaryStatistics robustness10 = new SummaryStatistics();
                for (List<Double> extinction : Tools.robustnessParallel(connections, i, Tools.SHUFFLE_ORDER, Tools.RANDOM_EXTINCTION).values()) {
                    robustness.addValue(extinction.get(0));
                    int partialIndex = (int) ((extinction.size() - 1) * 10 / 100.0);
                    double value = 0;
                    for (int j = 1; j < partialIndex; j++) {
                        value += extinction.get(j);
                    }
                    robustness10.addValue(value / (partialIndex * extinction.get(1)));
                }
                pw_R.println(i + "," + robustness.getMean() + "," + robustness10.getMean());
                pw_R.flush();
            }
            pw_R.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void testSimulationRuns(int maxSimulationRuns) {
        try {
            PrintWriter pw_R = new PrintWriter("simulationRunsTest.csv", "UTF-8");
            pw_R.println("SimulationRuns,Value");
            int counter = 0;
            for (int i = 0; i < maxSimulationRuns; i++) {
                System.out.println("Simulation run #" + i);
                warmup(i, true, true);
                start(false);
                SummaryStatistics initialRobustness = new SummaryStatistics();
                for (List<Double> initialExtinction : robustnessRandomShuffleHistory.get(0).values()) {
                    initialRobustness.addValue(initialExtinction.get(0));
                }
                SummaryStatistics meanRobustness = new SummaryStatistics();
                for (int j = robustnessRandomShuffleHistory.size() - 1; j > robustnessRandomShuffleHistory.size() - 50; j--) {
                    Map<Integer, List<Double>> robustnesses = robustnessRandomShuffleHistory.get(j);
                    SummaryStatistics robustness = new SummaryStatistics();
                    for (List<Double> extinction : robustnesses.values()) {
                        robustness.addValue(extinction.get(0));
                    }
                    meanRobustness.addValue(robustness.getMean() - initialRobustness.getMean());
                }
                pw_R.println(counter++ + "," + meanRobustness.getMean());
                pw_R.flush();
            }
            pw_R.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public Set<Service> getServicePool() {
        return servicePool;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public void setOnPause(boolean onPause) {
        this.onPause = onPause;
    }

    public Random getRandom() {
        return random == null ? new Random() : random;
    }

    public double getMutationProbability() {
        return mutationProbability;
    }

    public int getApplicationPoolSize() {
        return applicationPoolSize;
    }

    public int getServerPoolSize() {
        return serverPoolSize;
    }

    public int getServerMaxConnections() {
        return serverMaxConnections;
    }

    public int getServicePoolSize() {
        return servicePoolSize;
    }

    public double getServiceListRatio() {
        return serviceListRatio;
    }

    public void exportGraph() {
        try {
            FileOutputStream fos = new FileOutputStream("/home/aelie/git/diversify/BPGModel/graphs/graph.txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(servicePool);
            oos.writeObject(serverPool);
            oos.writeObject(applicationPool);
            //oos.writeObject(connections);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importGraph() {
        FileInputStream fis;
        try {
            fis = new FileInputStream("/home/aelie/git/diversify/BPGModel/graphs/graph.txt");
            ObjectInputStream ois = new ObjectInputStream(fis);
            try {
                servicePool = (Set<Service>) ois.readObject();
                serverPool = (Set<Server>) ois.readObject();
                applicationPool = (Set<Application>) ois.readObject();
                //connections = (Map<Server, Map<Application, List<Service>>>)ois.readObject();
                ois.close();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forceInit() {
        applicationPoolSize = 6;
        serverPoolSize = 3;
        servicePoolSize = 5;
        servicePool = new LinkedHashSet<>();
        for (serviceCounter = 0; serviceCounter < 5; serviceCounter++) {
            servicePool.add(new Service("s" + serviceCounter, serviceCounter, 0.2));
        }
        serverPool = new LinkedHashSet<>();
        Service[] tmp = new Service[5];
        Set<Service> ss1 = new LinkedHashSet<>();
        ss1.add(servicePool.toArray(tmp)[0]);
        ss1.add(servicePool.toArray(tmp)[1]);
        ss1.add(servicePool.toArray(tmp)[2]);
        ss1.add(servicePool.toArray(tmp)[3]);
        ss1.add(servicePool.toArray(tmp)[4]);
        serverPool.add(new Server("S1001", 0, ss1, 2));
        Set<Service> ss2 = new LinkedHashSet<>();
        ss2.add(servicePool.toArray(tmp)[0]);
        ss2.add(servicePool.toArray(tmp)[1]);
        ss2.add(servicePool.toArray(tmp)[2]);
        ss2.add(servicePool.toArray(tmp)[3]);
        serverPool.add(new Server("S1002", 0, ss2, 2));
        Set<Service> ss3 = new LinkedHashSet<>();
        ss3.add(servicePool.toArray(tmp)[0]);
        ss3.add(servicePool.toArray(tmp)[1]);
        ss3.add(servicePool.toArray(tmp)[4]);
        serverPool.add(new Server("S1003", 0, ss3, 2));
        applicationPool = new LinkedHashSet<>();
        Set<Service> sa1 = new LinkedHashSet<>();
        sa1.add(servicePool.toArray(tmp)[0]);
        sa1.add(servicePool.toArray(tmp)[1]);
        sa1.add(servicePool.toArray(tmp)[2]);
        sa1.add(servicePool.toArray(tmp)[3]);
        sa1.add(servicePool.toArray(tmp)[4]);
        applicationPool.add(new Application("A1", 0, sa1));
        Set<Service> sa2 = new LinkedHashSet<>();
        sa2.add(servicePool.toArray(tmp)[2]);
        sa2.add(servicePool.toArray(tmp)[3]);
        sa2.add(servicePool.toArray(tmp)[4]);
        applicationPool.add(new Application("A2", 0, sa2));
        Set<Service> sa3 = new LinkedHashSet<>();
        sa3.add(servicePool.toArray(tmp)[1]);
        sa3.add(servicePool.toArray(tmp)[3]);
        sa3.add(servicePool.toArray(tmp)[4]);
        applicationPool.add(new Application("A3", 0, sa3));
        Set<Service> sa4 = new LinkedHashSet<>();
        sa4.add(servicePool.toArray(tmp)[1]);
        sa4.add(servicePool.toArray(tmp)[2]);
        applicationPool.add(new Application("A4", 0, sa4));
        Set<Service> sa5 = new LinkedHashSet<>();
        sa5.add(servicePool.toArray(tmp)[0]);
        sa5.add(servicePool.toArray(tmp)[1]);
        applicationPool.add(new Application("A5", 0, sa5));
        Set<Service> sa6 = new LinkedHashSet<>();
        sa6.add(servicePool.toArray(tmp)[0]);
        sa6.add(servicePool.toArray(tmp)[1]);
        sa6.add(servicePool.toArray(tmp)[2]);
        applicationPool.add(new Application("A6", 0, sa6));
        connections = new LinkedHashMap<>();
        initialLink();
    }
}
