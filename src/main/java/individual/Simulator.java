package individual;

import ec.util.MersenneTwisterFast;
import eu.diversify.ffbpg.BPGraph;
import eu.diversify.ffbpg.Facade;
import eu.diversify.ffbpg.Platform;
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
    double mutationProbability = 0.2;
    int applicationPoolSize = 10; //VARIABLE
    int serverPoolSize = 5; //VARIABLE
    int serverMaxConnexion = 3;
    int servicePoolSize = 10; //VARIABLE
    double serviceListRatio = 0.2; //for every node : sLR*#maxService < #service < 1 - sLR*#maxService
    Set<Server> serverPool;
    Set<Application> applicationPool;
    Set<Service> servicePool;
    int serverCounter = 0;
    int applicationCounter = 0;
    int serviceCounter = 0;

    int currentTime = 0;
    int maxTime = 5;
    boolean onPause = false;
    MersenneTwisterFast mtf;

    Map<Server, Map<Application, Set<Service>>> connexions;
    Map<Application, Set<Service>> disconnectedApplications;

    int robustnessRuns = 30;
    Map<Integer, List<Double>> initialRobustness;
    Map<Integer, List<Double>> finalRobustness;
    double initialDiversity;
    double finalDiversity;

    private static Simulator INSTANCE;
    SimulationManager manager;
    static boolean useManager= false;

    DisplayGraph dg;

    private Simulator() { }

    public void setVariables(int applicationPoolSize, int serverPoolSize, int servicePoolSize,
                             double mutationProbability, int serverMaxConnexion, double serviceListRatio,
                             int maxTime, int robustnessRuns) {
        this.applicationPoolSize = applicationPoolSize;
        this.serverPoolSize = serverPoolSize;
        this.servicePoolSize = servicePoolSize;
        this.mutationProbability = mutationProbability;
        this.serverMaxConnexion = serverMaxConnexion;
        this.serviceListRatio = serviceListRatio;
        this.maxTime = maxTime;
        this.robustnessRuns = robustnessRuns;
    }

    public void warmup(int seed, boolean useFFBPG) {
        currentTime = 0;
        serverCounter = 0;
        applicationCounter = 0;
        serviceCounter = 0;
        mtf = new MersenneTwisterFast(seed);
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
        //exportGraph();
        displayGraph(-2);
        initialLink();
        displayGraph(-1);
        initialDiversity = Tools.diversity(connexions);
        initialRobustness = Tools.robustness(connexions, robustnessRuns);
    }

    public void start() {
        runUntil(maxTime);
        displayGraph(currentTime);
        //System.out.println("D :: " + diversityHistory);
        //System.out.println("R :: " + robustnessHistory);
    }

    public List<Double> getDiversityResults() {
        return Arrays.asList(initialDiversity, finalDiversity);
    }

    public List<Map<Integer, List<Double>>> getRobustnessResults() {
        return Arrays.asList(initialRobustness, finalRobustness);
    }

    public void runUntil(int desiredTime) {
        int targetTime = Math.min(desiredTime, maxTime);
        while (currentTime < targetTime && !onPause) {
            if (manager != null) {
                manager.updateSliderPosition(currentTime);
            }
            evolve();
            //evolveConstantPopulation();
            //System.out.println(Tools.getAliveApplications(connexions).size() + "/" + Tools.getAliveServers(connexions).size());
            currentTime++;
        }
        finalDiversity = Tools.diversity(connexions);
        finalRobustness = Tools.robustness(connexions, robustnessRuns);
    }

    public void runSteps(int desiredSteps) {
        runUntil(currentTime + desiredSteps);
    }

    public static Simulator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Simulator();
        }
        return INSTANCE;
    }

    public void init(boolean fromFFBPG) {
        servicePool = new LinkedHashSet<Service>();
        serverPool = new LinkedHashSet<Server>();
        applicationPool = new LinkedHashSet<Application>();
        if(fromFFBPG) {
            BPGraph ffbpg = Facade.createBPgraph(applicationPoolSize + serverPoolSize, servicePoolSize,
                    Facade.getPoissonIntegerGenerator(6), Facade.getNegExpIntegerSetGenerator(0.25, 0.005));
            for(Integer serviceId : ffbpg.getAllUsedServices().toArray()) {
                servicePool.add(new Service("s" + serviceId, serviceId, mutationProbability));
            }
            int i = 0;
            for(Platform platform : ffbpg.getPlatforms().subList(0, serverPoolSize)) {
                Server server = new Server("S" + i, null, serverMaxConnexion);
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
                i++;
            }
            i = 0;
            for(eu.diversify.ffbpg.Application ffbpgApplication : ffbpg.getApplications().subList(0, applicationPoolSize)) {
                Application application = new Application("A" + i, null);
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
                i++;
            }
        } else {
            for (serviceCounter = 0; serviceCounter < servicePoolSize; serviceCounter++) {
                servicePool.add(new Service("s" + serviceCounter, serviceCounter, mutationProbability));
            }
            for (serverCounter = 0; serverCounter < serverPoolSize; serverCounter++) {
                serverPool.add(new Server("S" + serverCounter, Tools.extractServiceList(servicePool, serviceListRatio), serverMaxConnexion));
            }
        /*for (serverCounter = 0; serverCounter < serverPoolSize; serverCounter++) {
            serverPool.add(new Server("S" + serverCounter, servicePool, serverMaxConnexion));
        }*/
            for (applicationCounter = 0; applicationCounter < applicationPoolSize; applicationCounter++) {
                applicationPool.add(new Application("A" + applicationCounter, Tools.extractServiceList(servicePool, serviceListRatio)));
            }
        }
    }

    public void displayGraph(int time) {
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
        /*System.out.println("A:" + applicationPool + System.getProperty("line.separator")
                + "S:" + serverPool + System.getProperty("line.separator")
                + "s:" + servicePool);*/
        System.out.println(connexions);
        //dg.displayGraph(connexions);
    }

    public void evolve() {
        //System.out.println("EVOLVE");
        Set<Server> serversToBeCloned = new LinkedHashSet<Server>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for (Server server : Tools.getAliveServers(connexions)) {
            double reproductionProbability = server.getCurrentConnectionNumber() / server.getMaxConnectionNumber() * 0.7;
            if (mtf.nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
            }
            double deathProbability = 1 - reproductionProbability * 0.7;
            if (mtf.nextDouble() < deathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getAvailableServices(), server.getMaxConnectionNumber());
            clone.mutate();
            //serverPool.add(clone);
            connexions.put(clone, new LinkedHashMap<Application, Set<Service>>());
        }
        /*disconnectedApplications = new HashMap<Application, Set<Service>>();
        for(Server server : serversToBeRemoved) {
            for(Application app : connexions.get(server).keySet()) {
                if (!disconnectedApplications.containsKey(app)) {
                    disconnectedApplications.put(app, new LinkedHashSet<Service>());
                }
                disconnectedApplications.get(app).addAll(connexions.get(server).get(app));
            }
            serverPool.remove(server);
        }*/
        disconnectedApplications = Tools.killServers(connexions, Tools.getAliveServers(connexions), serversToBeRemoved);
        //System.out.println("ES++" + serversToBeCloned);
        //System.out.println("ES--" + serversToBeRemoved);
        System.out.println("before RL");
        displayGraph(currentTime);
        relink();
        System.out.println("after RL");
        displayGraph(currentTime);
    }

    public void evolveConstantPopulation() {
        List<Server> serversToBeCloned = new ArrayList<Server>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        List<Server> orderedServers = new ArrayList<>(Tools.getAliveServers(connexions));
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
        System.out.println(complement+"/"+serversToBeCloned.size()+"/"+serversToBeRemoved.size());
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getAvailableServices(), server.getMaxConnectionNumber());
            clone.mutate();
            connexions.put(clone, new LinkedHashMap<Application, Set<Service>>());
        }
        System.out.println("CKS1 "+connexions.keySet().size());
        disconnectedApplications = Tools.killServers(connexions, Tools.getAliveServers(connexions), serversToBeRemoved);
        System.out.println("CKS2 "+connexions.keySet().size());
        //System.out.println("before RL");
        //displayGraph(currentTime);
        relink();
        //System.out.println("after RL");
        //displayGraph(currentTime);
        System.out.println("CKS3 "+connexions.keySet().size());
    }

    public void initialLink() {
        //System.out.println("ILINK");
        connexions = new LinkedHashMap<Server, Map<Application, Set<Service>>>();
        List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        List<Application> sapp = new ArrayList<>(applicationPool);
        Collections.shuffle(sapp);
        applicationPool = new LinkedHashSet<>(sapp);
        for (Application app : applicationPool) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(app.getRequiredServices());
            //int serverIndex = 0;
            Iterator<Server> iterServer = serverPool.iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()/*serverIndex < serverPool.size()*/) {
                Server server = iterServer.next();//serverPool.get(serverIndex);
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connexions.containsKey(server)) {
                        connexions.put(server, new HashMap<Application, Set<Service>>());
                    }
                    connexions.get(server).put(app, matchingServices);
                    server.addConnexion();
                }
                //serverIndex++;
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
        System.out.println("AK " + applicationsToBeRemoved);
        /*applicationPool.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for (Server server : serverPool) {
            if (!connexions.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }
        System.out.println("SK " + serversToBeRemoved);
        serverPool.removeAll(serversToBeRemoved);*/
        //System.out.println(connexions);
    }

    public void relink() {
        //System.out.println("RELINK");
        List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        for (Application app : disconnectedApplications.keySet()) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(disconnectedApplications.get(app));
            Iterator<Server> iterServer = Tools.getAliveServers(connexions).iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connexions.containsKey(server)) {
                        connexions.put(server, new HashMap<Application, Set<Service>>());
                    }
                    connexions.get(server).put(app, matchingServices);
                    server.addConnexion();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for (Server server : Tools.getAliveServers(connexions)) {
            if (connexions.get(server).isEmpty()) {
                serversToBeRemoved.add(server);
            }
        }
        //System.out.println("SK:" + serversToBeRemoved.size());
        Tools.killServers(connexions, Tools.getAliveServers(connexions), serversToBeRemoved);
        /*for (Server server : serversToBeRemoved) {
            connexions.remove(server);
        }*/
        System.out.println("AK " + applicationsToBeRemoved);
        /*applicationPool.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for (Server server : serverPool) {
            if (!connexions.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }*/
        System.out.println("SK " + serversToBeRemoved);
        //serverPool.removeAll(serversToBeRemoved);
        //System.out.println(connexions);
    }

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        simulator.warmup(1, false);
        if(!useManager) {
            simulator.start();
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

    public int getServerMaxConnexion() {
        return serverMaxConnexion;
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
            //oos.writeObject(connexions);
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
                //connexions = (Map<Server, Map<Application, List<Service>>>)ois.readObject();
                ois.close();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forceInit() {
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
    }
}
