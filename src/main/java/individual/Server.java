package individual;
import java.io.Serializable;
import java.util.*;

/**
 * Created by aelie on 03/09/14.
 */
public class Server implements Actor, Serializable {
    String name;
    Set<Service> services;
    int maxConnectionNumber;
    int currentConnectionNumber = 0; //nombre applicationPool connectées
    //int neededResource;
    //int age;

    public Server(String name, Set<Service> services, int maxConnectionNumber) {
        this.name = name;
        this.services = services;
        this.maxConnectionNumber = maxConnectionNumber;
    }

    public Set<Application> getConnectedApplications(Map<Server, Map<Application, Set<Service>>> connections) {
        return connections.get(this).keySet();
    }

    public void mutate() {
        Set<Service> mutatedServices = new LinkedHashSet<Service>();
        for(Service service : Simulator.getInstance().getServicePool()) {
            if(Simulator.getInstance().getRandom().nextDouble() < service.getMutationProbability()) {
                if(!services.contains(service)) {
                    mutatedServices.add(service);
                }
            } else {
                if(services.contains(service)) {
                    mutatedServices.add(service);
                }
            }
        }
        services = mutatedServices;
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
        if(canConnect()) {
            currentConnectionNumber++;
        } else {
            System.err.println("Server " + name + " can't add connection");
        }
    }

    public void removeConnection() {
        if(canDisconnect()) {
            currentConnectionNumber--;
        } else {
            System.err.println("Server " + name + " can't remove connection");
        }
    }

    public void disconnect() {
        currentConnectionNumber = 0;
    }

    public String toString() {
        return name;
    }
}
