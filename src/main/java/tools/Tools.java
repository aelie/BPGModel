package tools;

import individual.Application;
import individual.Server;
import individual.Service;
import individual.Simulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by aelie on 18/09/14.
 */
public class Tools {

    public static Set<Service> extractServiceList(Set<Service> services, double serviceListRatio) {
        int size = (int) ((services.size() * serviceListRatio) + (Simulator.getInstance().getMtf().nextDouble() * ((1 - 2 * serviceListRatio) * services.size())));
        Set<Service> result = new HashSet<Service>();
        Set<Service> servicePool = new HashSet<Service>(services);
        while (result.size() < size) {
            Service[] template = new Service[servicePool.size()];
            int randomIndex = (int) (Simulator.getInstance().getMtf().nextDouble() * servicePool.size());
            Service selectedService = servicePool.toArray(template)[randomIndex];
            result.add(selectedService);
            servicePool.remove(selectedService);
        }
        return result;
    }

    public static Set<Service> getMatchingServices(Set<Service> services1, Set<Service> services2) {
        Set<Service> result = new LinkedHashSet<Service>();
        for (Service service1 : services1) {
            for (Service service2 : services2) {
                if (service1 == service2) {
                    result.add(service1);
                }
            }
        }
        return result;
    }

    public static double diversity(Map<Server, Map<Application, Set<Service>>> connections) {
        SpeciesAndPopulation<Server> sap = new SpeciesAndPopulation<Server>(new ArrayList<Server>(getAliveServers(connections)));
        return sap.calculateShannon() / Math.log10(getAliveServers(connections).size());
    }

    public static Map<Integer, List<Double>> robustness(Map<Server, Map<Application, Set<Service>>> connexions, int applicationPoolSize, int runs) {
        Map<Integer, List<Double>> result = new HashMap<Integer, List<Double>>();
        //double robustnessMax = getAliveServers(connexions).size() * getAliveApplications(connexions).size();
        double robustnessMax = getAliveServers(connexions).size() * applicationPoolSize;//Simulator.getInstance().getApplicationPoolSize();
        for (int i = 0; i < runs; i++) {
            if (robustnessMax > 0) {
                List<Double> extinctionSequence = new ArrayList<Double>();
                Map<Server, Map<Application, Set<Service>>> connectionsCopy = new LinkedHashMap<Server, Map<Application, Set<Service>>>(connexions);
                //Set<Server> serversCopy = new LinkedHashSet<Server>(getAliveServers(connexions));
                Set<Server> serversCopy = shuffleSet(getAliveServers(connexions));
                //Set<Application> applicationsCopy = new LinkedHashSet<Application>(getAliveApplications(connexions));
                Set<Application> applicationsCopy = shuffleSet(getAliveApplications(connexions));
                double robustness = 0;
                Set<Server> connexionServers = shuffleSet(connexions.keySet());
                for (int j = 0; j < /*connexions.keySet()*/connexionServers.size(); j++) {
                    Iterator<Server> serverIter = /*connexions.keySet()*/connexionServers.iterator();
                    while (serversCopy.size() > 0) {
                        Server s = serverIter.next();
                        robustness += applicationsCopy.size();
                        extinctionSequence.add((double) applicationsCopy.size());
                        applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, s).keySet());
                    }
                }
                extinctionSequence.add(0, robustness / robustnessMax);
                result.put(i, extinctionSequence);
            } else {
                result.put(i, new ArrayList<>(Arrays.asList(0.0, 0.0)));
            }
        }
        return result;
    }// save history, run 30 times, 10%R, 25%R, 100%R

    public static Set<Server> getAliveServers(Map<Server, Map<Application, Set<Service>>> connections) {
        return connections.keySet();
    }

    public static Set<Application> getAliveApplications(Map<Server, Map<Application, Set<Service>>> connections) {
        Set<Application> result = new LinkedHashSet<Application>();
        for (Server server : connections.keySet()) {
            result.addAll(connections.get(server).keySet());
        }
        return result;
    }

    public static <T> Set<T> shuffleSet(Set<T> set) {
        List<T> setAsList = new ArrayList<>(set);
        Collections.shuffle(setAsList);
        return new LinkedHashSet<T>(setAsList);
    }

    public static Set<Server> getProvidingServers(Application application, Map<Server, Map<Application, Set<Service>>> connexions) {
        Set<Server> providingServers = new LinkedHashSet<>();
        for(Server server : connexions.keySet()) {
            if(connexions.get(server).containsKey(application)) {
                providingServers.add(server);
            }
        }
        return providingServers;
    }

    public static Set<Application> getSatisfiedApplications(Server server, Map<Server, Map<Application, Set<Service>>> connexions) {
        return connexions.get(server).keySet();
    }

    public static Map<Application, Set<Service>> killServers(Map<Server, Map<Application, Set<Service>>> graphConnections, Set<Server> initialServers, Set<Server> serversToBeKilled) {
        Map<Application, Set<Service>> disconnectedApplications = new HashMap<Application, Set<Service>>();
        for (Server server : serversToBeKilled) {
            for (Application app : graphConnections.get(server).keySet()) {
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
        Set<Server> singleServerSet = Collections.singleton(serverToBeKilled);
        return killServers(graphConnections, initialServers, singleServerSet);
    }
}
