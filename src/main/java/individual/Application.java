package individual;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by aelie on 03/09/14.
 */
public class Application implements Actor, Serializable {
    String name;
    Application father = null;
    Application mother = null;
    Set<Service> services;
    //int age;

    public Application(String name, Set<Service> services) {
        this.name = name;
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

    public String toString() {
        return name;
    }
}
