package tools;

import individual.Application;
import individual.Server;
import individual.Service;
import individual.Simulator;

import java.util.*;

/**
 * Created by aelie on 18/09/14.
 */
public class Tools {

    public static Set<Service> extractServiceList(Set<Service> services, double serviceListRatio) {
        int size = (int)((services.size() * serviceListRatio) + (Simulator.getInstance().getMtf().nextDouble() * ((1 - 2 * serviceListRatio) * services.size())));
        Set<Service> result = new HashSet<Service>();
        Set<Service> servicePool = new HashSet<Service>(services);
        while(result.size() < size) {
            Service[] template = new Service[servicePool.size()];
            int randomIndex = (int)(Simulator.getInstance().getMtf().nextDouble() * servicePool.size());
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

    public static double robustness(Map<Server, Map<Application, Set<Service>>> connections) {
        double robustnessMax = getAliveServers(connections).size() * getAliveApplications(connections).size();
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
}
