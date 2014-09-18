package individual;

import ec.util.MersenneTwisterFast;
import org.jfree.ui.RefineryUtilities;

import java.io.*;
import java.util.*;

/**
 * Created by aelie on 03/09/14.
 */
public class Simulator {
    //commencer serveurs homogenes
    //#app<#server * #maxconnec
    final double mutationProbability = 0.0;
    final int applicationsNumber = 500; //VARIABLE
    final int serversNumber = 20; //VARIABLE
    final int serversMaxConnectionNumber = 10;
    final int servicesNumber = 50; //VARIABLE
    final double serviceListRatio = 0.2; //for every node : sLR*#maxService < #service < 1 - sLR*#maxService
    Set<Server> servers;
    Set<Application> applications;
    Set<Service> services;
    int serverCounter = 0;
    int applicationCounter = 0;
    int serviceCounter = 0;

    int currentTime = 0;
    int maxTime = 1;
    MersenneTwisterFast mtf;

    Map<Server, Map<Application, Set<Service>>> connections;
    Map<Application, Set<Service>> disconnectedApplications;

    List<Double> robustnessHistory;

    private static Simulator INSTANCE;

    private Simulator() {
    }

    public void warmup() {
        mtf = new MersenneTwisterFast(1);
        //init();
        forceInit();
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
        for(currentTime = 0; currentTime < maxTime; currentTime++) {
            evolveNiche();
            /*if(currentTime%100 == 0) {
                displayGraph(currentTime);
            }*/
            diversity();
            robustnessHistory.add(robustnessNew(connections));
        }
        displayGraph(currentTime);
        System.out.println(robustnessHistory);
    };

    public static Simulator getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Simulator();
        }
        return INSTANCE;
    }

    public void exportGraph() {
        try {
            FileOutputStream fos = new FileOutputStream("/home/aelie/git/diversify/BPGModel/graphs/graph.txt");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(services);
            oos.writeObject(servers);
            oos.writeObject(applications);
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
                services = (Set<Service>)ois.readObject();
                servers = (Set<Server>)ois.readObject();
                applications = (Set<Application>)ois.readObject();
                //connections = (Map<Server, Map<Application, List<Service>>>)ois.readObject();
                ois.close();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        services = new LinkedHashSet<Service>();
        for(serviceCounter = 0; serviceCounter < servicesNumber; serviceCounter++) {
            services.add(new Service("s" + serviceCounter, mutationProbability));
        }
        servers = new LinkedHashSet<Server>();
        /*for(serverCounter = 0; serverCounter < serversNumber; serverCounter++) {
            servers.add(new Server("S" + serverCounter, extractServiceList(), serversMaxConnectionNumber));
        }*/
        for(serverCounter = 0; serverCounter < serversNumber; serverCounter++) {
            servers.add(new Server("S" + serverCounter, services, serversMaxConnectionNumber));
        }
        applications = new LinkedHashSet<Application>();
        for(applicationCounter = 0; applicationCounter < applicationsNumber; applicationCounter++) {
            applications.add(new Application("A" + applicationCounter, extractServiceList()));
        }
        robustnessHistory = new ArrayList<Double>();
    }

    public void forceInit() {
        services = new LinkedHashSet<Service>();
        for(serviceCounter = 0; serviceCounter < 5; serviceCounter++) {
            services.add(new Service("s" + serviceCounter, 0.2));
        }
        servers = new LinkedHashSet<Server>();
        Service[] tmp = new Service[5];
        Set<Service> ss1 = new LinkedHashSet<Service>();
        ss1.add(services.toArray(tmp)[0]);
        ss1.add(services.toArray(tmp)[1]);
        ss1.add(services.toArray(tmp)[2]);
        ss1.add(services.toArray(tmp)[3]);
        ss1.add(services.toArray(tmp)[4]);
        servers.add(new Server("S1001", ss1, 2));
        Set<Service> ss2 = new LinkedHashSet<Service>();
        ss2.add(services.toArray(tmp)[0]);
        ss2.add(services.toArray(tmp)[1]);
        ss2.add(services.toArray(tmp)[2]);
        ss2.add(services.toArray(tmp)[3]);
        servers.add(new Server("S1002", ss2, 2));
        Set<Service> ss3 = new LinkedHashSet<Service>();
        ss3.add(services.toArray(tmp)[0]);
        ss3.add(services.toArray(tmp)[1]);
        ss3.add(services.toArray(tmp)[4]);
        servers.add(new Server("S1003", ss3, 2));
        applications = new LinkedHashSet<Application>();
        Set<Service> sa1 = new LinkedHashSet<Service>();
        sa1.add(services.toArray(tmp)[0]);
        sa1.add(services.toArray(tmp)[1]);
        sa1.add(services.toArray(tmp)[2]);
        sa1.add(services.toArray(tmp)[3]);
        sa1.add(services.toArray(tmp)[4]);
        applications.add(new Application("A1", sa1));
        Set<Service> sa2 = new LinkedHashSet<Service>();
        sa2.add(services.toArray(tmp)[2]);
        sa2.add(services.toArray(tmp)[3]);
        sa2.add(services.toArray(tmp)[4]);
        applications.add(new Application("A2", sa2));
        Set<Service> sa3 = new LinkedHashSet<Service>();
        sa3.add(services.toArray(tmp)[1]);
        sa3.add(services.toArray(tmp)[3]);
        sa3.add(services.toArray(tmp)[4]);
        applications.add(new Application("A3", sa3));
        Set<Service> sa4 = new LinkedHashSet<Service>();
        sa4.add(services.toArray(tmp)[1]);
        sa4.add(services.toArray(tmp)[2]);
        applications.add(new Application("A4", sa4));
        Set<Service> sa5 = new LinkedHashSet<Service>();
        sa5.add(services.toArray(tmp)[0]);
        sa5.add(services.toArray(tmp)[1]);
        applications.add(new Application("A5", sa5));
        Set<Service> sa6 = new LinkedHashSet<Service>();
        sa6.add(services.toArray(tmp)[0]);
        sa6.add(services.toArray(tmp)[1]);
        sa6.add(services.toArray(tmp)[2]);
        applications.add(new Application("A6", sa6));
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
        System.out.println("T="+time);
        /*System.out.println("A:" + applications + System.getProperty("line.separator")
                + "S:" + servers + System.getProperty("line.separator")
                + "s:" + services);*/
        System.out.println(connections);
    }

    public Set<Service> extractServiceList() {
        int size = (int)((servicesNumber * serviceListRatio) + (mtf.nextDouble() * ((1 - 2 * serviceListRatio) * servicesNumber)));
        Set<Service> result = new HashSet<Service>();
        Set<Service> servicePool = new HashSet<Service>(services);
        while(result.size() < size) {
            Service[] template = new Service[servicePool.size()];
            int randomIndex = (int)(mtf.nextDouble() * servicePool.size());
            Service selectedService = servicePool.toArray(template)[randomIndex];
            result.add(selectedService);
            servicePool.remove(selectedService);
        }
        return result;
    }

    public void evolveNeutral() {
        //reproduction aléatoire
        //mortalité aléatoire
    }

    public void evolveNiche() {
        //reproduction selon succes (== #connexion)
        //P(reproduction) = #connexion/#slots
        //P(mort) = (#slots - #connexion)/#slots
        //reproduction = clone + mutation
        //mutation = probabilité de création/disparition pour tous les services possibles
        //#connexion = nombre applications connectés

        //mortalitè selon inverse succès (== #connexion)
        System.out.println("EVOLVE");
        Set<Server> serversToBeCloned = new LinkedHashSet<Server>();
        Set<Server> serversToBeRemoved = new LinkedHashSet<Server>();
        for(Server server : servers) {
            double reproductionProbability = server.getCurrentConnectionNumber() / server.getMaxConnectionNumber() * 0.9;
            if(mtf.nextDouble() < reproductionProbability) {
                serversToBeCloned.add(server);
            }
            double deathProbability = 1 - reproductionProbability * 0.9;
            if(mtf.nextDouble() < deathProbability) {
                serversToBeRemoved.add(server);
            }
        }
        for(Server server : serversToBeCloned) {
            serverCounter++;
            Server clone = new Server("S" + serverCounter, server.getAvailableServices(), server.getMaxConnectionNumber());
            clone.mutate();
            servers.add(clone);
        }
        /*disconnectedApplications = new HashMap<Application, Set<Service>>();
        for(Server server : serversToBeRemoved) {
            for(Application app : connections.get(server).keySet()) {
                if (!disconnectedApplications.containsKey(app)) {
                    disconnectedApplications.put(app, new LinkedHashSet<Service>());
                }
                disconnectedApplications.get(app).addAll(connections.get(server).get(app));
            }
            servers.remove(server);
        }*/
        disconnectedApplications = killServers(connections, servers, serversToBeRemoved);
        System.out.println("ES++"+serversToBeCloned);
        System.out.println("ES--"+serversToBeRemoved);
        System.out.println("before RL");
        displayGraph(currentTime);
        relink();
        System.out.println("after RL");
        displayGraph(currentTime);
        //IS1 + RS2
        //IS2(RS1) + RS1
    }

    public static Map<Application, Set<Service>> killServers(Map<Server, Map<Application, Set<Service>>> graphConnections, Set<Server> initialServers, Set<Server> serversToBeKilled) {
        Map<Application, Set<Service>> disconnectedApplications = new HashMap<Application, Set<Service>>();
        for(Server server : serversToBeKilled) {
            for(Application app : graphConnections.get(server).keySet()) {
                if (!disconnectedApplications.containsKey(app)) {
                    disconnectedApplications.put(app, new LinkedHashSet<Service>());
                }
                disconnectedApplications.get(app).addAll(graphConnections.get(server).get(app));
            }
            initialServers.remove(server);
            graphConnections.remove(server);
        }
        return disconnectedApplications;
    }

    public static Map<Application, Set<Service>> killServer(Map<Server, Map<Application, Set<Service>>> graphConnections, Set<Server> initialServers, Server serverToBeKilled) {
        Set<Server> singleServerSet = new LinkedHashSet<Server>();
        singleServerSet.add(serverToBeKilled);
        return killServers(graphConnections, initialServers, singleServerSet);
    }

    public void initialLink() {
        //IS1 : manual
        //IS2 : relink
        System.out.println("ILINK");
        connections = new LinkedHashMap<Server, Map<Application, Set<Service>>>();
        List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        for(Application app : applications) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(app.getRequiredServices());
            //int serverIndex = 0;
            Iterator<Server> iterServer = servers.iterator();
            while(!unsatisfiedServices.isEmpty() && iterServer.hasNext()/*serverIndex < servers.size()*/) {
                Server server = iterServer.next();//servers.get(serverIndex);
                Set<Service> matchingServices = getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if(matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if(!connections.containsKey(server)) {
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
            System.out.println("AK " + applicationsToBeRemoved);
            applications.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for(Server server : servers) {
            if(!connections.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }
        System.out.println("SK " + serversToBeRemoved);
        servers.removeAll(serversToBeRemoved);
        //System.out.println(connections);
    }

    public void relink() {
        //1 connexion = 1 utilisateurs pour n services
        //déterministe comme ecologie
        //RS1 : serveur connecté est rejeté en fin de liste
        //RS2 : 1er arrivé 1er servi
        //relink à chaque evolution MAIS chaque application garde ses liens si possible
        System.out.println("RELINK");
        List<Application> applicationsToBeRemoved = new ArrayList<Application>();
        for(Application app : disconnectedApplications.keySet()) {
            Set<Service> unsatisfiedServices = new LinkedHashSet<Service>(disconnectedApplications.get(app));
            Iterator<Server> iterServer = servers.iterator();
            while(!unsatisfiedServices.isEmpty() && iterServer.hasNext()) {
                Server server = iterServer.next();
                Set<Service> matchingServices = getMatchingServices(unsatisfiedServices, server.getAvailableServices());
                if(matchingServices.size() > 0 && server.canConnect()) {
                    unsatisfiedServices.removeAll(matchingServices);
                    if(!connections.containsKey(server)) {
                        connections.put(server, new HashMap<Application, Set<Service>>());
                    }
                    connections.get(server).put(app, matchingServices);
                    server.addConnection();
                }
            }
            if (!unsatisfiedServices.isEmpty()) {
                applicationsToBeRemoved.add(app);
            }
        }
            System.out.println("AK " + applicationsToBeRemoved);
            applications.removeAll(applicationsToBeRemoved);
        List<Server> serversToBeRemoved = new ArrayList<Server>();
        for(Server server : servers) {
            if(!connections.containsKey(server)) {
                serversToBeRemoved.add(server);
            }
        }
        System.out.println("SK " + serversToBeRemoved);
        servers.removeAll(serversToBeRemoved);
        //System.out.println(connections);
    }

    public static Set<Service> getMatchingServices(Set<Service> services1, Set<Service> services2) {
        Set<Service> result = new LinkedHashSet<Service>();
        for(Service service1 : services1) {
            for(Service service2 : services2) {
                if(service1 == service2) {
                    result.add(service1);
                }
            }
        }
        return result;
    }

    public double diversity() {
        return 0;
    }

    public double robustness(Map<Server, Map<Application, Set<Service>>> connections) {
        double robustnessMax = servers.size() * applications.size();
        //double robustnessMax = serversNumber * applicationsNumber;
        Map<Server, Map<Application, Set<Service>>> connectionsCopy = new LinkedHashMap<Server, Map<Application, Set<Service>>>(connections);
        Set<Server> serversCopy = new LinkedHashSet<Server>(servers);
        Set<Application> applicationsCopy = new LinkedHashSet<Application>(applications);
        double robustness = 0;
        for(int i = 0; i < connections.keySet().size(); i++) {
            Iterator<Server> serverIter = connections.keySet().iterator();
            while(serversCopy.size() > 0) {
                Server s = serverIter.next();
                applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, s).keySet());
                robustness += applicationsCopy.size();
            }
        }
        return robustness / robustnessMax;
    }

    public static double robustnessNew(Map<Server, Map<Application, Set<Service>>> connections) {
        double robustnessMax = getAliveServers(connections).size()
                * getAliveApplications(connections).size();
        Map<Server, Map<Application, Set<Service>>> connectionsCopy = new LinkedHashMap<Server, Map<Application, Set<Service>>>(connections);
        Set<Server> serversCopy = new LinkedHashSet<Server>(getAliveServers(connections));
        Set<Application> applicationsCopy = new LinkedHashSet<Application>(getAliveApplications(connections));
        double robustness = 0;
        for(int i = 0; i < connections.keySet().size(); i++) {
            Iterator<Server> serverIter = connections.keySet().iterator();
            while(serversCopy.size() > 0) {
                Server s = serverIter.next();
                applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, s).keySet());
                robustness += applicationsCopy.size();
            }
        }
        return robustness / robustnessMax;
    }

    public static Set<Server> getAliveServers(Map<Server, Map<Application, Set<Service>>> connections) {
        return connections.keySet();
    }

    public static Set<Application> getAliveApplications(Map<Server, Map<Application, Set<Service>>> connections) {
        Set<Application> result = new LinkedHashSet<Application>();
        for(Server server : connections.keySet()) {
            result.addAll(connections.get(server).keySet());
        }
        return result;
    }

    public static void main(String[] args) {
        Simulator simulator = Simulator.getInstance();
        simulator.warmup();
        simulator.start();
    }

    public Set<Server> getServers() {
        return servers;
    }

    public void setServers(Set<Server> servers) {
        this.servers = servers;
    }

    public Set<Application> getApplications() {
        return applications;
    }

    public void setApplications(Set<Application> applications) {
        this.applications = applications;
    }

    public Set<Service> getServices() {
        return services;
    }

    public void setServices(Set<Service> services) {
        this.services = services;
    }

    public MersenneTwisterFast getMtf() {
        return mtf;
    }
}
