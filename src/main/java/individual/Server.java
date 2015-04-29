package individual;

import tools.Tools;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by aelie on 03/09/14.
 */
public class Server implements Actor, Serializable {
    String name;
    int generation = 0;
    Set<Service> services;
    int maxConnectionNumber;
    int currentConnectionNumber = 0; //nombre applicationPool connectées
    //int neededResource;
    int age = 0;

    public Server(String name, int generation, Set<Service> services, int maxConnectionNumber) {
        this.name = name;
        this.generation = generation;
        this.services = services;
        this.maxConnectionNumber = maxConnectionNumber;
    }

    /*public void mutate() {
        Set<Service> mutatedServices = new LinkedHashSet<>();
        for (Service service : Simulator.getInstance().getServicePool()) {
            if (Simulator.getInstance().getRandom().nextDouble() < service.getMutationProbability()) {
                if (!services.contains(service)) {
                    mutatedServices.add(service);
                }
            } else {
                if (services.contains(service)) {
                    mutatedServices.add(service);
                }
            }
        }
        services = mutatedServices;
    }

    public void mutateMostUseless(Map<Server, Set<Application>> connections) {
        if(services != null) {
            Set<Service> mutatedServices = new LinkedHashSet<>();
            List<Service> sortedServices = Tools.getServicesOrderedByUsage(this, connections);
            for (Service service : Simulator.getInstance().getServicePool()) {
                if (!services.contains(service)) {
                    if (Simulator.getInstance().getRandom().nextDouble() < service.getMutationProbability()) {
                        mutatedServices.add(service);
                    } else {
                        if (services.contains(service)) {
                            mutatedServices.add(service);
                        }
                    }
                } else {
                    double mutationProbability = sortedServices.indexOf(service) / (double) sortedServices.size();
                    if (Simulator.getInstance().getRandom().nextDouble() < mutationProbability) {
                        mutatedServices.add(new ArrayList<>(Simulator.getInstance().getServicePool()).get(
                                Simulator.getInstance().getRandom().nextInt(Simulator.getInstance().getServicePool().size())));
                    } else {
                        mutatedServices.add(service);
                    }
                }
            }
            services = mutatedServices;
        }
    }*/

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getGeneration() {
        return generation;
    }

    @Override
    public void setGeneration(int generation) {
        this.generation = generation;
    }

    @Override
    public void newGeneration() {
        generation++;
    }

    @Override
    public void older() {
        age++;
    }

    public Set<Service> getServices() {
        return services;
    }

    public void setServices(Set<Service> services) {
        this.services = services;
    }

    public int evaluateNeededResource() {
        return services.size() + maxConnectionNumber + currentConnectionNumber;
    }

    public int getMaxConnectionNumber() {
        return maxConnectionNumber;
    }

    public void setMaxConnectionNumber(int maxConnectionNumber) {
        this.maxConnectionNumber = maxConnectionNumber;
    }

    public int getCurrentConnectionNumber() {
        return currentConnectionNumber;
    }

    public void setCurrentConnectionNumber(int currentConnectionNumber) {
        this.currentConnectionNumber = currentConnectionNumber;
    }

    public boolean canConnect() {
        return currentConnectionNumber < maxConnectionNumber;
    }

    public boolean canDisconnect() {
        return currentConnectionNumber > 0;
    }

    public void addConnection() {
        if (canConnect()) {
            currentConnectionNumber++;
        } else {
            System.err.println("Server " + name + " can't add connection");
        }
    }

    public void removeConnection() {
        if (canDisconnect()) {
            currentConnectionNumber--;
        } else {
            System.err.println("Server " + name + " can't remove connection");
        }
    }

    public void disconnect() {
        currentConnectionNumber = 0;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toVerboseString(Map<Server, Set<Application>> connections) {
        return name
                + "/" + generation
                + "/" + maxConnectionNumber
                + "/" + Tools.getSatisfiedApplications(this, connections).size()
                + "/" + age
                + "/" + services.stream()
                .map(Service::getName)
                .collect(Collectors.joining("/"));
    }
}