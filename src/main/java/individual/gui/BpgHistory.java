package individual.gui;

import individual.Application;
import individual.Server;
import individual.Service;
import tools.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by aelie on 05/01/16.
 */
public class BpgHistory {
    public static final int FF = 0;
    public static final int AE = 1;

    public static int dataSource;
    String testFile;

    private int stepNumber;
    private Set<String> serverNames;
    private Set<String> applicationNames;
    private Set<String> serviceNames;
    public int maxSimultaneousServers;
    int maxServerGeneration;
    int maxServerSize;
    public int maxServerConnections;
    public int maxSimultaneousApplications;
    int maxApplicationGeneration;
    int maxApplicationSize;
    public int maxApplicationConnections;

    private Map<Integer, Map<FakeServer, List<FakeApplication>>> graphHistory;
    private Map<Integer, List<FakeServer>> serverHistory;
    private Map<Integer, List<FakeApplication>> applicationHistory;

    Map<Integer, Double> robustnessByStep;
    Map<Integer, Integer> IncompatiblesByStep;
    Map<Integer, Integer> cumulativeServicesByStep;
    Map<Integer, Integer> cumulativeLinksByStep;

    public BpgHistory(String inputFile) {
        testFile = System.getProperty("user.dir") + File.separator + (inputFile == null ? "connections.log" : inputFile);
        if (!new File(testFile).exists()) {
            System.err.println("File " + testFile + " not found! Exiting...");
            System.exit(-1);
        }
        if (new File(inputFile).isDirectory()) {
            dataSource = FF;
            preParseInputFileFF(testFile);
        } else {
            dataSource = AE;
            preParseInputFileAE(testFile);
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
        robustnessByStep = new HashMap<>();
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
                robustnessByStep.put(fileStep, (double) (Integer.parseInt(br.readLine().split("=")[1].trim())) / 1000d);
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
                                neighbors.equalsIgnoreCase("") ? new ArrayList<>() : Arrays.asList(neighbors.substring(1, neighbors.length() - 2).split("\\s")).stream()
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
                serviceNames.remove("");
                stepNumber = Math.max(fileStep + 1, stepNumber);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Names: " + serverNames.size() + "/" + applicationNames.size() + "/" + serviceNames.size());
        System.out.println("MaxSimult: " + maxSimultaneousServers + "/" + maxSimultaneousApplications);
        System.out.println("MaxGen: " + maxServerGeneration + "/" + maxApplicationGeneration);
        System.out.println("MaxSize: " + maxServerSize + "/" + maxApplicationSize);
    }

    public FakeActor findActor(String name, List actors) {
        for (Object fakeActor : actors) {
            if (((FakeActor) fakeActor).name.equalsIgnoreCase(name)) {
                return (FakeActor) fakeActor;
            }
        }
        return null;
    }

    public void buildRobustnessByStep() {
        if (dataSource == AE) {
            System.out.println("Building robustness data...");
            robustnessByStep = new HashMap<>();
            Map<Integer, Map<Server, Set<Application>>> connectionsByStep = buildConnectionsByStep();
            for (Integer step : connectionsByStep.keySet()) {
                robustnessByStep.put(step, Tools.robustnessRandom(connectionsByStep.get(step), Tools.SHUFFLE_ORDER).get(0));
            }
        } else {
            // done during parsing
        }
    }

    public void buildIncompatiblesByStep() {
        System.out.println("Building potential connections data...");
        IncompatiblesByStep = new HashMap<>();
        for (Integer step : graphHistory.keySet()) {
            IncompatiblesByStep.put(step, getIncompatibles(step));
        }
    }

    public void buildCumulativeServicesByStep() {
        System.out.println("Building cumulative services data...");
        cumulativeServicesByStep = new HashMap<>();
        for (Integer step : graphHistory.keySet()) {
            cumulativeServicesByStep.put(step, getCumulativeServices(step));
        }
    }

    public void buildCumulativeLinksByStep() {
        System.out.println("Building cumulative links data...");
        cumulativeLinksByStep = new HashMap<>();
        for (Integer step : graphHistory.keySet()) {
            cumulativeLinksByStep.put(step, getCumulativeLinks(step));
        }
    }

    public Map<Integer, Map<Server, Set<Application>>> buildConnectionsByStep() {
        Map<Integer, Map<Server, Set<Application>>> result = new HashMap<>();
        for (Integer step : graphHistory.keySet()) {
            Map<String, Application> applicationNamePool = new HashMap<>();
            result.put(step, new LinkedHashMap<>());
            for (FakeServer fakeServer : graphHistory.get(step).keySet()) {
                Server tmpServ = new Server(fakeServer.name,
                        fakeServer.generation,
                        new LinkedHashSet<>(fakeServer.services.stream()
                                .map(name -> new Service(name, 0, 0))
                                .collect(Collectors.toList())), fakeServer.maxConnections);
                result.get(step).put(tmpServ, new LinkedHashSet<>());
                for (FakeApplication fakeApplication : graphHistory.get(step).get(fakeServer)) {
                    if (!applicationNamePool.keySet().contains(fakeApplication.name)) {
                        Application tmpApp = new Application(fakeApplication.name,
                                fakeApplication.generation,
                                new LinkedHashSet<>(fakeApplication.services.stream()
                                        .map(name -> new Service(name, 0, 0))
                                        .collect(Collectors.toList())));
                        result.get(step).get(tmpServ).add(tmpApp);
                        applicationNamePool.put(fakeApplication.name, tmpApp);
                    } else {
                        result.get(step).get(tmpServ).add(applicationNamePool.get(fakeApplication.name));
                    }
                }
            }
        }
        return result;
    }

    public int getCumulativeServices(int step) {
        return serverHistory.get(step).stream()
                .mapToInt(FakeActor::getSize)
                .sum();
    }

    public int getCumulativeLinks(int step) {
        return serverHistory.get(step).stream()
                .mapToInt(server -> applicationHistory.get(step).stream()
                        .mapToInt(app -> server.getCommonServices(app).size())
                        .sum())
                .sum();
    }

    public int getIncompatibles(int step) {
        return serverHistory.get(step).stream()
                .mapToInt(server -> applicationHistory.get(step).stream()
                        .mapToInt(app -> server.getCommonServices(app).size() == 0 ? 1 : 0)
                        .sum())
                .sum();
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public Set<String> getApplicationNames() {
        return applicationNames;
    }

    public Set<String> getServerNames() {
        return serverNames;
    }

    public Set<String> getServiceNames() {
        return serviceNames;
    }

    public Map<Integer, Map<FakeServer, List<FakeApplication>>> getGraphHistory() {
        return graphHistory;
    }

    public Map<Integer, List<FakeServer>> getServerHistory() {
        return serverHistory;
    }

    public Map<Integer, List<FakeApplication>> getApplicationHistory() {
        return applicationHistory;
    }

    public int getMaxServerGeneration() {
        return maxServerGeneration;
    }

    public int getMaxServerSize() {
        return maxServerSize;
    }

    public int getMaxApplicationGeneration() {
        return maxApplicationGeneration;
    }

    public int getMaxApplicationSize() {
        return maxApplicationSize;
    }

    public Map<Integer, Double> getRobustnessByStep() {
        return robustnessByStep;
    }

    public Map<Integer, Integer> getCumulativeServicesByStep() {
        return cumulativeServicesByStep;
    }

    public Map<Integer, Integer> getCumulativeLinksByStep() {
        return cumulativeLinksByStep;
    }

    public Map<Integer, Integer> getIncompatiblesByStep() {
        return IncompatiblesByStep;
    }
}
