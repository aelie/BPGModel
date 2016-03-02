package individual.gui;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by aelie on 14/12/15.
 */
class FakeActor {
    public String name;
    public int generation;
    public int connections;
    public int age;
    public List<String> services;

    public FakeActor(String name, int generation, int connections, int age, List<String> services) {
        this.name = name;
        this.generation = generation;
        this.connections = connections;
        this.age = age;
        this.services = services;
    }

    public List<String> getCommonServices(FakeActor external) {
        return services.stream().filter(s -> external.services.contains(s)).collect(Collectors.toList());
    }

    public int getSize() {
        return services.size();
    }

    @Override
    public String toString() {
        return name;
    }
}
