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

    public boolean isConnected(Map<Server, Map<Application, Set<Service>>> connections) {
        return getConnectedServices(connections).containsAll(requiredServices)
                && requiredServices.containsAll(getConnectedServices(connections));
    }

    public Set<Service> getConnectedServices(Map<Server, Map<Application, Set<Service>>> connections) {
        Set<Service> connectedServices = new LinkedHashSet<Service>();
        for(Server server : connections.keySet()) {
            connectedServices.addAll(connections.get(server).get(this));
        }
        return connectedServices;
    }

    public Set<Service> getDisconnectedServices(Map<Server, Map<Application, Set<Service>>> connections) {
        Set<Service> disconnectedServices = new LinkedHashSet<Service>(requiredServices);
        for(Server server : connections.keySet()) {
            disconnectedServices.removeAll(connections.get(server).get(this));
        }
        return disconnectedServices;
    }

    public String toString() {
        return name;
    }
}
