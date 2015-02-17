package tools;

import individual.*;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by aelie on 18/09/14.
 */
public class Tools {

    public static final int FORWARD_ORDER = 1;
    public static final int SHUFFLE_ORDER = 0;
    public static final int BACKWARD_ORDER = -1;
    public static final int RANDOM_EXTINCTION = 0;
    public static final int SERVICE_EXTINCTION = 1;

    public static Set<Service> extractServiceList(Set<Service> services, double serviceListRatio) {
        int size = (int) ((services.size() * serviceListRatio) + (Simulator.getInstance().getRandom().nextDouble() * ((1 - 2 * serviceListRatio) * services.size())));
        Set<Service> result = new HashSet<>();
        Set<Service> servicePool = new HashSet<>(services);
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

    public static double diversity(Set<? extends Actor> actors) {
        SpeciesAndPopulation<Actor> sap = new SpeciesAndPopulation<>(new ArrayList<>(actors));
        return sap.calculateShannon();
    }

    public static double richness(Set<? extends Actor> actors) {
        SpeciesAndPopulation<Actor> sap = new SpeciesAndPopulation<>(new ArrayList<>(actors));
        return (double) sap.getNumSpecies() / (double) actors.size();
    }

    public static double evenness(Set<? extends Actor> actors) {
        return diversity(actors) / Math.log10(richness(actors) * actors.size());
    }

    public static double jaccard(Actor actor1, Actor actor2) {
        Set<Service> union = new LinkedHashSet<>(actor1.getServices());
        union.addAll(actor2.getServices());
        return getMatchingServices(actor1.getServices(), actor2.getServices()).size() / union.size();
    }

    public static double disparity(Set<? extends Actor> actors) {
        double sum = 0;
        for (Actor actor : actors) {
            for (Actor otherActor : actors) {
                if (actor != otherActor) {
                    sum += jaccard(actor, otherActor);
                }
            }
        }
        return sum / (double) (actors.size() * (actors.size() - 1));
    }

    public static List<Double> robustnessRandom(Map<Server, Set<Application>> connections, int direction) {
        List<Double> extinctionSequence = new ArrayList<>();
        double robustnessMax = getAliveServers(connections).size() * getAliveApplications(connections).size();
        if (robustnessMax > 0) {
            Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<>(connections);
            Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
            Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
            SummaryStatistics robustness = new SummaryStatistics();
            for (Server server : orderSet(connections.keySet(), direction)) {
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

    public static List<Double> robustnessService(Map<Server, Set<Application>> connections) {
        List<Double> extinctionSequence = new ArrayList<>();
        double robustnessMax = getAliveServers(connections).size() * getAliveApplications(connections).size();
        if (robustnessMax > 0) {
            Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<>(connections);
            Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
            Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
            SummaryStatistics robustness = new SummaryStatistics();
            while (!applicationsCopy.isEmpty()) {
                Service infected = shuffleSet(Simulator.getInstance().getServicePool()).iterator().next();
                for (Server server : shuffleSet(connections.keySet())) {
                    if (server.getServices().contains(infected)) {
                        robustness.addValue(applicationsCopy.size());
                        extinctionSequence.add((double) applicationsCopy.size());
                        applicationsCopy.removeAll(killServer(connectionsCopy, serversCopy, server));
                    }
                }
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

    public static Map<Integer, List<Double>> robustnessParallel(Map<Server, Set<Application>> connections, int runs, int direction, int type) {
        Map<Integer, List<Double>> result = new HashMap<>();
        ExecutorService es = Executors.newCachedThreadPool();
        List<Callable<List<Double>>> callables = new ArrayList<>();
        switch (type) {
            case RANDOM_EXTINCTION:
                for (int i = 0; i < runs; i++) {
                    callables.add(() -> robustnessRandom(connections, direction));
                }
                break;
            case SERVICE_EXTINCTION:
                for (int i = 0; i < runs; i++) {
                    callables.add(() -> robustnessService(connections));
                }
                break;
            default:
                for (int i = 0; i < runs; i++) {
                    callables.add(() -> robustnessRandom(connections, direction));
                }
                break;
        }
        try {
            int index = 0;
            for (Future<List<Double>> future : es.invokeAll(callables)) {
                result.put(index, future.get());
                index++;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        es.shutdown();
        return result;
    }

    public static Map<Integer, List<Double>> robustnessSerial(Map<Server, Set<Application>> connections, int runs) {
        Map<Integer, List<Double>> result = new HashMap<>();
        double robustnessMax = getAliveServers(connections).size() * getAliveApplications(connections).size();
        for (int i = 0; i < runs; i++) {
            if (robustnessMax > 0) {
                List<Double> extinctionSequence = new ArrayList<>();
                Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<>(connections);
                Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
                Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
                SummaryStatistics robustness = new SummaryStatistics();
                for (Server server : shuffleSet(connections.keySet())) {
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
        Map<Integer, List<Double>> result = new HashMap<>();
        double robustnessMax = getAliveServers(connections).size() * Simulator.getInstance().getApplicationPoolSize();
        for (int i = 0; i < runs; i++) {
            if (robustnessMax > 0) {
                List<Double> extinctionSequence = new ArrayList<>();
                Map<Server, Set<Application>> connectionsCopy = new LinkedHashMap<>(connections);
                Set<Server> serversCopy = shuffleSet(getAliveServers(connections));
                Set<Application> applicationsCopy = new LinkedHashSet<>(getAliveApplications(connections));
                Set<Service> deadlyServices = new LinkedHashSet<>(new ArrayList<>(shuffleSet(Simulator.getInstance().getServicePool())).subList(0, deadlyServicesAmount));
                double robustness = 0;
                for (Server server : shuffleSet(connections.keySet())) {
                    Iterator<Service> serviceIter = deadlyServices.iterator();
                    boolean infected = false;
                    while (serviceIter.hasNext() && !infected) {
                        if (server.getServices().contains(serviceIter.next())) {
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
        return new LinkedHashSet<>(setAsList);
    }

    public static <T extends Actor> Set<T> orderSet(Set<T> set, int direction) {
        if (direction != SHUFFLE_ORDER) {
            List<T> list = new ArrayList<>(set);
            Collections.sort(list, (o1, o2) -> direction * (o1.getServices().size() - o2.getServices().size()));
            return new LinkedHashSet<>(list);
        } else {
            return shuffleSet(set);
        }
    }

    public static Set<Server> getProvidingServers(Application application, Map<Server, Set<Application>> connections) {
        return connections.keySet().stream()
                .filter(server -> connections.get(server).contains(application))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        Set<Service> unsatisfiedServices = new LinkedHashSet<>(application.getServices());
        connections.keySet().stream()
                .filter(server -> connections.get(server).contains(application))
                .forEach(server -> unsatisfiedServices.removeAll(getMatchingServices(server.getServices(), application.getServices())));
        return unsatisfiedServices;
    }

    public static boolean isApplicationSatisfied(Application application, Map<Server, Set<Application>> connections) {
        Set<Service> services = new LinkedHashSet<>(application.getServices());
        connections.keySet().stream()
                .filter(server -> connections.get(server).contains(application))
                .forEach(server -> services.removeAll(server.getServices()));
        return services.isEmpty();
    }

    public static Set<Application> killServers(Map<Server, Set<Application>> connections, Set<Server> initialServers, Set<Server> serversToBeKilled) {
        Set<Application> disconnectedApplications = new LinkedHashSet<>();
        for (Server server : serversToBeKilled) {
            Set<Application> linkedApplications = connections.get(server);
            initialServers.remove(server);
            connections.remove(server);
            if (linkedApplications != null) {
                for (Application application : linkedApplications) {
                    if (!isApplicationSatisfied(application, connections)) {
                        disconnectedApplications.add(application);
                    }
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
        return new ArrayList<>(set).get((int) (Simulator.getInstance().getRandom().nextDouble() * set.size()));
    }
}
