package individual;

import ec.util.MersenneTwisterFast;
import eu.diversify.ffbpg.BPGraph;
import eu.diversify.ffbpg.Facade;
import eu.diversify.ffbpg.Platform;
import eu.diversify.ffbpg.random.IntegerGenerator;
import eu.diversify.ffbpg.random.IntegerSetGenerator;
import org.jfree.ui.RefineryUtilities;
import tools.Tools;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Created by aelie on 03/09/14.
 */
public class Simulator {
    //commencer serveurs homogenes
    //#app<#server * #maxconnec
    double reproductionModificator = 0.7;
    double deathModificator = 0.7;
    double mutationProbability = 0.0;
    int applicationPoolSize = 100; //VARIABLE
    int serverPoolSize = 30; //VARIABLE
    int serverMaxConnections = 20;
    int servicePoolSize = 100; //VARIABLE
    double serviceListRatio = 0.2; //for every node : sLR*#maxService < #service < 1 - sLR*#maxService
    Set<Server> serverPool;
    Set<Application> applicationPool;
    Set<Service> servicePool;
    int serverCounter = 0;
    int applicationCounter = 0;
    int serviceCounter = 0;

    int currentTime = 0;
    int maxTime = 10;
    boolean onPause = false;
    MersenneTwisterFast mtf;

    Map<Server, Set<Application>> connections;
    Set<Application> disconnectedApplications;

    int robustnessRuns = 30;
    List<Map<Integer, List<Double>>> robustnessResults;
    double initialDiversity;
    double finalDiversity;

    private static Simulator INSTANCE;
    SimulationManager manager;
    static boolean useManager= false;

    DisplayGraph dg;
    boolean silentMode = false;

    private Simulator() { }

    public static Simulator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Simulator();
        }
        return INSTANCE;
    }

    public void setVariables(int applicationPoolSize, int serverPoolSize, int servicePoolSize,
                             double mutationProbability, int serverMaxConnexion,
                             int maxTime, int robustnessRuns) {
        this.applicationPoolSize = applicationPoolSize;
        this.serverPoolSize = serverPoolSize;
        this.servicePoolSize = servicePoolSize;
        this.mutationProbability = mutationProbability;
        this.serverMaxConnections = serverMaxConnexion;
        this.maxTime = maxTime;
        this.robustnessRuns = robustnessRuns;
    }

    public void warmup(int seed, boolean useFFBPG, boolean silentMode) {
        currentTime = 0;
        serverCounter = 0;
        applicationCounter = 0;
        serviceCounter = 0;
        mtf = new MersenneTwisterFast(seed);
        this.silentMode = silentMode;
        if(useManager) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    manager = new SimulationManager(maxTime);
                    manager.display();
                }
            });
        }
        //dg = new DisplayGraph();
        init(useFFBPG);
        //forceInit();
        //exportGraph();
        displayGraph(-2);
        if(!useFFBPG) {
            initialLink();
        }
        displayGraph(-1);
        initialDiversity = Tools.diversity(connections);
        robustnessResults = new ArrayList<Map<Integer, List<Double>>>();
        robustnessResults.add(Tools.robustness(connections, applicationPoolSize, robustnessRuns));
    }

    public void init(boolean fromFFBPG) {
        servicePool = new LinkedHashSet<Service>();
        serverPool = new LinkedHashSet<Server>();
        applicationPool = new LinkedHashSet<Application>();
        connections = new LinkedHashMap<Server, Set<Application>>();
        if(fromFFBPG) {
            IntegerGenerator sizes_generator = Facade.getPoissonIntegerGenerator(6);
            IntegerSetGenerator srv_generator = Facade.getNegExpIntegerSetGenerator(0.25, 0.005);
            BPGraph ffbpg = Facade.createRandomBPGraph(applicationPoolSize, serverPoolSize, servicePoolSize,
                    sizes_generator, srv_generator, serverPoolSize, serverMaxConnections);
            for(Integer serviceId : ffbpg.getAllUsedServices().toArray()) {
                servicePool.add(new Service("s" + serviceId, serviceId, mutationProbability));
            }
            Map<eu.diversify.ffbpg.Application, Application> applicationMap = new LinkedHashMap<>();
            for(eu.diversify.ffbpg.Application ffbpgApplication : ffbpg.getApplications()) {
                Application application = new Application(ffbpgApplication.getName(), null);
                Set<Service> services = new LinkedHashSet<Service>();
                for(Integer serviceId : ffbpgApplication.getRequiredServices().toArray()) {
                    for(Service service : servicePool) {
                        if(service.getId() == serviceId) {
                            services.add(service);
                        }
                    }
                }
                application.setRequiredServices(services);
                applicationPool.add(application);
                applicationMap.put(ffbpgApplication, application);
            }
            for(Platform platform : ffbpg.getPlatforms()) {
                Server server = new Server(platform.getName(), null, serverMaxConnections);
                Set<Service> services = new LinkedHashSet<Service>();
                for(Integer serviceId : platform.getProvidedServices().toArray()) {
                    for(Service service : servicePool) {
                        if(service.getId() == serviceId) {
                            services.add(service);
                        }
                    }
                }
                server.setAvailableServices(services);
                serverPool.add(server);
                for(eu.diversify.ffbpg.Application ffbpgApplication : ffbpg.getLinkedApplicationsForPlatform(platform)) {
                    if(!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<Application>());
                    }
                    connections.get(server).add(applicationMap.get(ffbpgApplication));
                    server.addConnexion();
                }
            }
        } else {
            for (serviceCounter = 0; serviceCounter < servicePoolSize; serviceCounter++) {
                servicePool.add(new Service("s" + serviceCounter, serviceCounter, mutationProbability));
            }
            for (serverCounter = 0; serverCounter < serverPoolSize; serverCounter++) {
                serverPool.add(new Server("S" + serverCounter, Tools.extractServiceList(servicePool, serviceListRatio), serverMaxConnections));
            }
            for (applicationCounter = 0; applicationCounter < applicationPoolSize; applicationCounter++) {
                applicationPool.add(new Application("A" + applicationCounter, Tools.extractServiceList(servicePool, serviceListRatio)));
            }
        }
    }

    public void start(boolean displayCharts) {
        runUntil(maxTime);
        displayGraph(currentTime);
        //System.out.println("D :: " + diversityHistory);
        if(!silentMode) {
            System.out.println("R" + currentTime + " :: " + getRobustnessResults().get(1));
        }
        if(displayCharts) {
            Charts charts = new Charts();
            charts.init(currentTime);
            charts.getRobustnessFrame().pack();
            RefineryUtilities.centerFrameOnScreen(charts.getRobustnessFrame());
            charts.getRobustnessFrame().setVisible(true);
        }
    }

    public void runUntil(int desiredTime) {
        int targetTime = Math.min(desiredTime, maxTime);
        while (currentTime < targetTime && !onPause) {
            if (manager != null) {
                manager.updateSliderPosition(currentTime);
            }
            evolve();
            //evolveConstantPopulation();
            //System.out.println(Tools.getAliveApplications(connections).size() + "/" + Tools.getAliveServers(connections).size());
            currentTime++;
        }
        finalDiversity = Tools.diversity(connections);
        robustnessResults.add(Tools.robustness(connections, applicationPoolSize, robustnessRuns));
    }

    public void runSteps(int desiredSteps) {
        runUntil(currentTime + desiredSteps);
    }

    public List<Double> getDiversityResults() {
        return Arrays.asList(initialDiversity, finalDiversity);
    }

    public List<Map<Integer, List<Double>>> getRobustnessResults() {
        return robustnessResults;
    }

    public void displayGraph(int time) {
        if(!silentMode) {
        /*final Display display = new Display(time);
        display.getServerFrame().pack();
        RefineryUtilities.centerFrameOnScreen(display.getServerFrame());
        display.getServerFrame().setVisible(true);
        display.getApplicationFrame().pack();
        RefineryUtilities.centerFrameOnScreen(display.getApplicationFrame());
        display.getApplicationFrame().setVisible(true);
        display.getRobustnessFrame().pack();
        RefineryUtilities.centerFrameOnScreen(display.getRobustnessFrame());
        display.getRobustnessFrame().setVisible(true);*/
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

    public void evolve() {
        //System.out.println("EVOLVE");
        Set<Server> serversToBeCloned = new LinkedHashSet<Server>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for (Server server : Tools.getAliveServers(connections)) {
            double reproductionProbability = (double)server.getCurrentConnectionNumber() / (double)server.getMaxConnectionNumber() * reproductionModificator;
            if (mtf.nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
            }
            double deathProbability = 1 - reproductionProbability * deathModificator;
            if (mtf.nextDouble() < deathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getAvailableServices(), server.getMaxConnectionNumber());
            clone.mutate();
            //serverPool.add(clone);
            connections.put(clone, new LinkedHashSet<Application>());
        }
        /*disconnectedApplications = new HashMap<Application, Set<Service>>();
        for(Server server : serversToBeRemoved) {
            for(Application app : connections.get(server).keySet()) {
                if (!disconnectedApplications.containsKey(app)) {
                    disconnectedApplications.put(app, new LinkedHashSet<Service>());
                }
                disconnectedApplications.get(app).addAll(connections.get(server).get(app));
            }
            serverPool.remove(server);
        }*/
        disconnectedApplications = Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved);
        //System.out.println("ES++" + serversToBeCloned);
        //System.out.println("ES--" + serversToBeRemoved);
        //System.out.println("before RL");
        //displayGraph(currentTime);
        relink();
        //System.out.println("after RL");
        //displayGraph(currentTime);
    }

    public void evolveConstantPopulation() {
        List<Server> serversToBeCloned = new ArrayList<Server>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        List<Server> orderedServers = new ArrayList<>(Tools.getAliveServers(connections));
        Collections.sort(orderedServers, new Comparator<Server>() {
            @Override
            public int compare(Server o1, Server o2) {
                return o1.getCurrentConnectionNumber() / o1.getMaxConnectionNumber() -
                        o2.getCurrentConnectionNumber() / o2.getMaxConnectionNumber();
            }
        });
        for (Server server : orderedServers) {
            double reproductionProbability = server.getCurrentConnectionNumber() / server.getMaxConnectionNumber() * 0.7;
            if (mtf.nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
                serversToBeRemoved.add(orderedServers.get(orderedServers.size() - serversToBeCloned.size()));
            }
        }
        int complement = serverPoolSize - orderedServers.size();
        serversToBeCloned.addAll(orderedServers.subList(0, complement));
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getAvailableServices(), server.getMaxConnectionNumber());
            clone.mutate();
            connections.put(clone, new LinkedHashSet<Application>());
        }
        disconnectedApplications = Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved);
        //System.out.println("before RL");
        //displayGraph(currentTime);
        relink();
        //System.out.println("after RL");
        //displayGraph(currentTime);
    }

    public void initialLink() {
        if(!silentMode) {
            System.out.println("ILINK");
        }
        Set<Application> applicationsToBeRemoved = new LinkedHashSet<>();
        List<Application> sapp = new ArrayList<>(applicationPool);
        Collections.shuffle(sapp);
        applicationPool = new LinkedHashSet<>(sapp);
        for (Application app : applicationPool) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(app.getRequiredServices());
            Iterator<Server> iterServer = serverPool.iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<>());
                    }
                    connections.get(server).add(app);
                    server.addConnexion();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
    }

    public void relink() {
        if(!silentMode) {
            System.out.println("RELINK");
        }
        List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        for (Application app : disconnectedApplications) {
            Set<Service> unsatisfiedServices = Tools.getUnsatisfiedServices(app, connections);
            Iterator<Server> iterServer = Tools.getAliveServers(connections).iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new LinkedHashSet<Application>());
                    }
                    connections.get(server).add(app);
                    server.addConnexion();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for (Server server : Tools.getAliveServers(connections)) {
            if (connections.get(server).isEmpty()) {
                serversToBeRemoved.add(server);
            }
        }
        //System.out.println("SK:" + serversToBeRemoved.size());
        Tools.killServers(connections, Tools.getAliveServers(connections), serversToBeRemoved);
        /*for (Server server : serversToBeRemoved) {
            connections.remove(server);
        }*/
        //System.out.println("AK " + applicationsToBeRemoved);
        /*applicationPool.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for (Server server : serverPool) {
            if (!connections.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }*/
        //System.out.println("SK " + serversToBeRemoved);
        //serverPool.removeAll(serversToBeRemoved);
        //System.out.println(connections);
    }

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        simulator.warmup(1, true, false);
        if(!useManager) {
            simulator.start(true);
        }
    }

    public String getParametersAsString() {
        return "#A=" + Tools.getAliveApplications(connections).size() + "/" + applicationPoolSize +
                " | #S=" + Tools.getAliveServers(connections).size() + "/" + serverPoolSize +
                " | #s=" + servicePoolSize +
                " | %mut=" + mutationProbability;
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

    public MersenneTwisterFast getMtf() {
        return mtf;
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
        FileInputStream fis = null;
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
        servicePool = new LinkedHashSet<Service>();
        for (serviceCounter = 0; serviceCounter < 5; serviceCounter++) {
            servicePool.add(new Service("s" + serviceCounter, serviceCounter, 0.2));
        }
        serverPool = new LinkedHashSet<Server>();
        Service[] tmp = new Service[5];
        Set<Service> ss1 = new LinkedHashSet<Service>();
        ss1.add(servicePool.toArray(tmp)[0]);
        ss1.add(servicePool.toArray(tmp)[1]);
        ss1.add(servicePool.toArray(tmp)[2]);
        ss1.add(servicePool.toArray(tmp)[3]);
        ss1.add(servicePool.toArray(tmp)[4]);
        serverPool.add(new Server("S1001", ss1, 2));
        Set<Service> ss2 = new LinkedHashSet<Service>();
        ss2.add(servicePool.toArray(tmp)[0]);
        ss2.add(servicePool.toArray(tmp)[1]);
        ss2.add(servicePool.toArray(tmp)[2]);
        ss2.add(servicePool.toArray(tmp)[3]);
        serverPool.add(new Server("S1002", ss2, 2));
        Set<Service> ss3 = new LinkedHashSet<Service>();
        ss3.add(servicePool.toArray(tmp)[0]);
        ss3.add(servicePool.toArray(tmp)[1]);
        ss3.add(servicePool.toArray(tmp)[4]);
        serverPool.add(new Server("S1003", ss3, 2));
        applicationPool = new LinkedHashSet<Application>();
        Set<Service> sa1 = new LinkedHashSet<Service>();
        sa1.add(servicePool.toArray(tmp)[0]);
        sa1.add(servicePool.toArray(tmp)[1]);
        sa1.add(servicePool.toArray(tmp)[2]);
        sa1.add(servicePool.toArray(tmp)[3]);
        sa1.add(servicePool.toArray(tmp)[4]);
        applicationPool.add(new Application("A1", sa1));
        Set<Service> sa2 = new LinkedHashSet<Service>();
        sa2.add(servicePool.toArray(tmp)[2]);
        sa2.add(servicePool.toArray(tmp)[3]);
        sa2.add(servicePool.toArray(tmp)[4]);
        applicationPool.add(new Application("A2", sa2));
        Set<Service> sa3 = new LinkedHashSet<Service>();
        sa3.add(servicePool.toArray(tmp)[1]);
        sa3.add(servicePool.toArray(tmp)[3]);
        sa3.add(servicePool.toArray(tmp)[4]);
        applicationPool.add(new Application("A3", sa3));
        Set<Service> sa4 = new LinkedHashSet<Service>();
        sa4.add(servicePool.toArray(tmp)[1]);
        sa4.add(servicePool.toArray(tmp)[2]);
        applicationPool.add(new Application("A4", sa4));
        Set<Service> sa5 = new LinkedHashSet<Service>();
        sa5.add(servicePool.toArray(tmp)[0]);
        sa5.add(servicePool.toArray(tmp)[1]);
        applicationPool.add(new Application("A5", sa5));
        Set<Service> sa6 = new LinkedHashSet<Service>();
        sa6.add(servicePool.toArray(tmp)[0]);
        sa6.add(servicePool.toArray(tmp)[1]);
        sa6.add(servicePool.toArray(tmp)[2]);
        applicationPool.add(new Application("A6", sa6));
        connections = new LinkedHashMap<Server, Set<Application>>();
        initialLink();
    }
}
