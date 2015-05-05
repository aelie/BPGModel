package individual;

import tools.Tools;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by aelie on 03/09/14.
 */
public class Application implements Actor, Serializable {
    String name;
    int generation = 0;
    Application father = null;
    Application mother = null;
    Set<Service> services;
    int age = 0;
    Set<Server> neighborhood;

    public Application(String name, int generation, Set<Service> services) {
        this.name = name;
        this.generation = generation;
        this.services = services;
    }

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

    public void setParents(Application father, Application mother) {
        this.father = father;
        this.mother = mother;
    }

    public Set<Server> getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(Set<Server> neighborhood) {
        this.neighborhood = neighborhood;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toVerboseString(Map<Server, Set<Application>> connections) {
        return name
                + "/" + generation
                + "/" + Tools.getProvidingServers(this, connections).size()
                + "/" + age
                + "/" + services.stream()
                .map(Service::getName)
                .collect(Collectors.joining("/"));
    }
}
