package tools;

import individual.Application;
import individual.Server;
import individual.Service;
import individual.Simulator;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.*;

/**
 * Created by aelie on 18/09/14.
 */
public class Tools {

    public static Set<Service> extractServiceList(Set<Service> services, double serviceListRatio) {
        int size = (int) ((services.size() * serviceListRatio) + (Simulator.getInstance().getRandom().nextDouble() * ((1 - 2 * serviceListRatio) * services.size())));
        Set<Service> result = new HashSet<Service>();
        Set<Service> servicePool = new HashSet<Service>(services);
        while (result.size() < size) {
            Service[] template = new Service[servicePool.size()];
            int randomIndex = (int) (Simulator.getInstance().getRandom().nextDouble() * servicePool.size());
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

    public static double diversity(Map<Server, Set<Application>> connections) {
        SpeciesAndPopulation<Server> sap = new SpeciesAndPopulation<>(new ArrayList<>(getAliveServers(connections)));
        return sap.calculateShannon() / Math.log10(getAliveServers(connections).size());
    }

    public static List<Double> robustness(Map<Server, Set<Application>> connections) {
        List<Double> extinctionSequence = new ArrayList<>();
        double robustnessMax = getAliveServers(connections).size() * getAliveApplications(connections).size();
        if (robustnessMax > 0) {
            Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<>(connections);
            Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
            Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
            SummaryStatistics robustness = new SummaryStatistics();
            Iterator<Server> serverIter = shuffleSet(connections.keySet()).iterator();
            while (serverIter.hasNext()) {
                Server server = serverIter.next();
                robustness.addValue(applicationsCopy.size());
                extinctionSequence.add((double) applicationsCopy.size());
                applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, server));
            }
            //first index: robustnessMultiRun
            extinctionSequence.add(0, robustness.getSum() / robustnessMax);
            //last index: 0.0
            extinctionSequence.add(0.0);
        } else {
            extinctionSequence = new ArrayList<>(Arrays.asList(0.0, 0.0));
        }
        return extinctionSequence;
    }

    public static Map<Integer, List<Double>> robustnessMultiRun(Map<Server, Set<Application>> connections, int runs) {
        Map<Integer, List<Double>> result = new HashMap<>();
        double robustnessMax = getAliveServers(connections).size() * getAliveApplications(connections).size();
        for (int i = 0; i < runs; i++) {
            if (robustnessMax > 0) {
                List<Double> extinctionSequence = new ArrayList<>();
                Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<>(connections);
                Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
                Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
                SummaryStatistics robustness = new SummaryStatistics();
                Iterator<Server> serverIter = shuffleSet(connections.keySet()).iterator();
                while (serverIter.hasNext()) {
                    Server server = serverIter.next();
                    robustness.addValue(applicationsCopy.size());
                    extinctionSequence.add((double) applicationsCopy.size());
                    applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, server));
                }
                //first index: robustnessMultiRun
                extinctionSequence.add(0, robustness.getSum() / robustnessMax);
                //last index: 0.0
                extinctionSequence.add(0.0);
                result.put(i, extinctionSequence);
            } else {
                result.put(i, new ArrayList<>(Arrays.asList(0.0, 0.0)));
            }
        }
        return result;
    }

    public static Map<Integer, List<Double>> serviceAttackES(Map<Server, Set<Application>> connections, int runs, int deadlyServicesAmount) {
        Map<Integer, List<Double>> result = new HashMap<Integer, List<Double>>();
        double robustnessMax = getAliveServers(connections).size() * Simulator.getInstance().getApplicationPoolSize();
        for (int i = 0; i < runs; i++) {
            if (robustnessMax > 0) {
                List<Double> extinctionSequence = new ArrayList<Double>();
                Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<Server, Set<Application>>(connections);
                Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
                Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
                Set<Service> deadlyServices = new LinkedHashSet<>(new ArrayList<>(shuffleSet(Simulator.getInstance().getServicePool())).subList(0, deadlyServicesAmount));
                double robustness = 0;
                Iterator<Server> serverIter = shuffleSet(connections.keySet()).iterator();
                while (serverIter.hasNext()) {
                    Server server = serverIter.next();
                    Iterator<Service> serviceIter = deadlyServices.iterator();
                    boolean infected = false;
                    while(serviceIter.hasNext() && !infected) {
                        if(server.getAvailableServices().contains(serviceIter.next())) {
                            robustness += applicationsCopy.size();
                            extinctionSequence.add((double) applicationsCopy.size());
                            applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, server));
                            infected = true;
                        }
                    }
                }
                //first index: robustnessMultiRun
                extinctionSequence.add(0, robustness / robustnessMax);
                //last index: 0.0
                extinctionSequence.add(0.0);
                result.put(i, extinctionSequence);
            } else {
                result.put(i, new ArrayList<>(Arrays.asList(0.0, 0.0)));
            }
        }
        return result;
    }

    public static Set<Server> getAliveServers(Map<Server, Set<Application>> connexions) {
        return new LinkedHashSet<>(connexions.keySet());
    }

    public static Set<Application> getAliveApplications(Map<Server, Set<Application>> connections) {
        Set<Application> result = new LinkedHashSet<>();
        for (Server server : connections.keySet()) {
            result.addAll(connections.get(server));
        }
        return result;
    }

    public static <T> Set<T> shuffleSet(Set<T> set) {
        List<T> setAsList = new ArrayList<>(set);
        Collections.shuffle(setAsList, Simulator.getInstance().getRandom());
        return new LinkedHashSet<T>(setAsList);
    }

    public static Set<Server> getProvidingServers(Application application, Map<Server, Set<Application>> connections) {
        Set<Server> providingServers = new LinkedHashSet<>();
        for (Server server : connections.keySet()) {
            if (connections.get(server).contains(application)) {
                providingServers.add(server);
            }
        }
        return providingServers;
    }

    public static Set<Server> getSecondDegreeServers(Server server, Map<Server, Set<Application>> connections) {
        Set<Server> secondDegreeServers = new LinkedHashSet<>();
        for (Application application : connections.get(server)) {
            secondDegreeServers.addAll(getProvidingServers(application, connections));
        }
        return secondDegreeServers;
    }

    public static Set<Application> getSatisfiedApplications(Server server, Map<Server, Set<Application>> connections) {
        return connections.get(server);
    }

    public static Set<Service> getUnsatisfiedServices(Application application, Map<Server, Set<Application>> connections) {
        Set<Service> unsatisfiedServices = new LinkedHashSet<>(application.getRequiredServices());
        for (Server server : connections.keySet()) {
            if (connections.get(server).contains(application)) {
                unsatisfiedServices.removeAll(getMatchingServices(server.getAvailableServices(), application.getRequiredServices()));
            }
        }
        return unsatisfiedServices;
    }

    public static boolean isApplicationSatisfied(Application application, Map<Server, Set<Application>> connections) {
        Set<Service> services = new LinkedHashSet<>(application.getRequiredServices());
        for (Server server : connections.keySet()) {
            if (connections.get(server).contains(application)) {
                services.removeAll(server.getAvailableServices());
            }
        }
        return services.isEmpty();
    }

    public static Set<Application> killServers(Map<Server, Set<Application>> connections, Set<Server> initialServers, Set<Server> serversToBeKilled) {
        Set<Application> disconnectedApplications = new LinkedHashSet<>();
        for (Server server : serversToBeKilled) {
            Set<Application> linkedApplications = connections.get(server);
            initialServers.remove(server);
            connections.remove(server);
            for (Application application : linkedApplications) {
                if (!isApplicationSatisfied(application, connections)) {
                    disconnectedApplications.add(application);
                }
            }
        }
        return disconnectedApplications;
    }

    public static Set<Application> killServer(Map<Server, Set<Application>> connections, Set<Server> initialServers, Server serverToBeKilled) {
        Set<Server> singleServerSet = Collections.singleton(serverToBeKilled);
        return killServers(connections, initialServers, singleServerSet);
    }

    public static <E> E getRandomElement(Set<E> set) {
        return new ArrayList<>(set).get((int)(Simulator.getInstance().getRandom().nextDouble() * set.size()));
    }
}
