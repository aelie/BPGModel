package individual;

import ec.util.MersenneTwisterFast;
import tools.Tools;

import java.io.*;
import java.util.*;

/**
 * Created by aelie on 03/09/14.
 */
public class Simulator {
    //commencer serveurs homogenes
    //#app<#server * #maxconnec
    final double mutationProbability = 0.2;
    final int applicationPoolSize = 500; //VARIABLE
    final int serverPoolSize = 20; //VARIABLE
    final int serversMaxConnectionNumber = 10;
    final int servicePoolSize = 50; //VARIABLE
    final double serviceListRatio = 0.2; //for every node : sLR*#maxService < #service < 1 - sLR*#maxService
    Set<Server> serverPool;
    Set<Application> applicationPool;
    Set<Service> servicePool;
    int serverCounter = 0;
    int applicationCounter = 0;
    int serviceCounter = 0;

    int currentTime = 0;
    int maxTime = 100;
    MersenneTwisterFast mtf;

    Map<Server, Map<Application, Set<Service>>> connections;
    Map<Application, Set<Service>> disconnectedApplications;

    List<Double> robustnessHistory;

    private static Simulator INSTANCE;

    private Simulator() {
    }

    public void warmup() {
        mtf = new MersenneTwisterFast(1);
        init();
        //forceInit();
        //importGraph();
        exportGraph();
        displayGraph(-2);
        //initialLink();
        //displayGraph();

        //displayGraph();
        //importGraph();
        //displayGraph();
        initialLink();
        displayGraph(-1);
    }

    public void start() {
        for (currentTime = 0; currentTime < maxTime; currentTime++) {
            evolveNiche();
            /*if(currentTime%100 == 0) {
                displayGraph(currentTime);
            }*/
            diversity();
            System.out.println(Tools.getAliveApplications(connections).size()+"/"+Tools.getAliveServers(connections).size());
            robustnessHistory.add(Tools.robustness(connections));
        }
        displayGraph(currentTime);
        System.out.println(robustnessHistory);
    }

    ;

    public static Simulator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Simulator();
        }
        return INSTANCE;
    }

    public void init() {
        servicePool = new LinkedHashSet<Service>();
        for (serviceCounter = 0; serviceCounter < servicePoolSize; serviceCounter++) {
            servicePool.add(new Service("s" + serviceCounter, mutationProbability));
        }
        serverPool = new LinkedHashSet<Server>();
        /*for(serverCounter = 0; serverCounter < serverPoolSize; serverCounter++) {
            serverPool.add(new Server("S" + serverCounter, extractServiceList(), serversMaxConnectionNumber));
        }*/
        for (serverCounter = 0; serverCounter < serverPoolSize; serverCounter++) {
            serverPool.add(new Server("S" + serverCounter, servicePool, serversMaxConnectionNumber));
        }
        applicationPool = new LinkedHashSet<Application>();
        for (applicationCounter = 0; applicationCounter < applicationPoolSize; applicationCounter++) {
            applicationPool.add(new Application("A" + applicationCounter, Tools.extractServiceList(servicePool, serviceListRatio)));
        }
        robustnessHistory = new ArrayList<Double>();
    }

    public void forceInit() {
        servicePool = new LinkedHashSet<Service>();
        for (serviceCounter = 0; serviceCounter < 5; serviceCounter++) {
            servicePool.add(new Service("s" + serviceCounter, 0.2));
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
        robustnessHistory = new ArrayList<Double>();
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
        //System.out.println(connections);
    }

    public void evolveNeutral() {
        //reproduction aléatoire
        //mortalité aléatoire
    }

    public void evolveNiche() {
        System.out.println("EVOLVE");
        Set<Server> serversToBeCloned = new LinkedHashSet<Server>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for (Server server : Tools.getAliveServers(connections)) {
            double reproductionProbability = server.getCurrentConnectionNumber() / server.getMaxConnectionNumber() * 0.9;
            if (mtf.nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
            }
            double deathProbability = 1 - reproductionProbability * 0.9;
            if (mtf.nextDouble() < deathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for (Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getAvailableServices(), server.getMaxConnectionNumber());
            clone.mutate();
            //serverPool.add(clone);
            connections.put(clone, new LinkedHashMap<Application, Set<Service>>());
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
        System.out.println("ES++" + serversToBeCloned);
        System.out.println("ES--" + serversToBeRemoved);
        System.out.println("before RL");
        displayGraph(currentTime);
        relink();
        System.out.println("after RL");
        displayGraph(currentTime);
        //IS1 + RS2
        //IS2(RS1) + RS1
    }

    public void initialLink() {
        System.out.println("ILINK");
        connections = new LinkedHashMap<Server, Map<Application, Set<Service>>>();
        List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        for (Application app : applicationPool) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(app.getRequiredServices());
            //int serverIndex = 0;
            Iterator<Server> iterServer = serverPool.iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()/*serverIndex < serverPool.size()*/) {
                Server server = iterServer.next();//serverPool.get(serverIndex);
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new HashMap<Application, Set<Service>>());
                    }
                    connections.get(server).put(app, matchingServices);
                    server.addConnection();
                }
                //serverIndex++;
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
        /*System.out.println("AK " + applicationsToBeRemoved);
        applicationPool.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for (Server server : serverPool) {
            if (!connections.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }
        System.out.println("SK " + serversToBeRemoved);
        serverPool.removeAll(serversToBeRemoved);*/
        //System.out.println(connections);
    }

    public void relink() {
        //1 connexion = 1 utilisateurs pour n servicePool
        //déterministe comme ecologie
        //RS1 : serveur connecté est rejeté en fin de liste
        //RS2 : 1er arrivé 1er servi
        //relink à chaque evolution MAIS chaque application garde ses liens si possible
        System.out.println("RELINK");
        //List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        for (Application app : disconnectedApplications.keySet()) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(disconnectedApplications.get(app));
            Iterator<Server> iterServer = Tools.getAliveServers(connections).iterator();
            while (!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = Tools.getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if (matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if (!connections.containsKey(server)) {
                        connections.put(server, new HashMap<Application, Set<Service>>());
                    }
                    connections.get(server).put(app, matchingServices);
                    server.addConnection();
                }
            }
            /*if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }*/
        }
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for(Server server : Tools.getAliveServers(connections)) {
            if(connections.get(server).isEmpty()) {
                serversToBeRemoved.add(server);
            }
        }
        System.out.println("SK:"+serversToBeRemoved.size());
        for(Server server : serversToBeRemoved) {
            connections.remove(server);
        }
        /*System.out.println("AK " + applicationsToBeRemoved);
        applicationPool.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for (Server server : serverPool) {
            if (!connections.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }
        System.out.println("SK " + serversToBeRemoved);
        serverPool.removeAll(serversToBeRemoved);*/
        //System.out.println(connections);
    }

    public double diversity() {
        return 0;
    }

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        simulator.warmup();
        simulator.start();
    }

    public Set<Server> getServerPool() {
        return serverPool;
    }

    public void setServerPool(Set<Server> serverPool) {
        this.serverPool = serverPool;
    }

    public Set<Application> getApplicationPool() {
        return applicationPool;
    }

    public void setApplicationPool(Set<Application> applicationPool) {
        this.applicationPool = applicationPool;
    }

    public Set<Service> getServicePool() {
        return servicePool;
    }

    public void setServicePool(Set<Service> servicePool) {
        this.servicePool = servicePool;
    }

    public MersenneTwisterFast getMtf() {
        return mtf;
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
}
