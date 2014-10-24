package individual;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by aelie on 03/09/14.
 */
public class Application implements Serializable {
    String name;
    Application father = null;
    Application mother = null;
    Set<Service> requiredServices;
    //int age;

    public Application(String name, Set<Service> requiredServices) {
        this.name = name;
        this.requiredServices = requiredServices;
    }

    public Set<Service> getRequiredServices() {
        return requiredServices;
    }

    public void setRequiredServices(Set<Service> requiredServices) {
        this.requiredServices = requiredServices;
    }

    public void setParents(Application father, Application mother) {
        this.father = father;
        this.mother = mother;
    }

    public String toString() {
        return name;
    }
}
