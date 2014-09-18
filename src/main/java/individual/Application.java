package individual;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by aelie on 03/09/14.
 */
public class Application implements Serializable {
    String name;
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

    public String toString() {
        return name;
    }
}
