package individual;
import java.io.Serializable;
import java.util.*;

/**
 * Created by aelie on 03/09/14.
 */
public class Server implements Serializable {
    String name;
    Set<Service> availableServices;
    int maxConnectionNumber;
    int currentConnectionNumber = 0; //nombre applications connectées
    //int neededResource;
    //int age;

    public Server(String name, Set<Service> availableServices, int maxConnectionNumber) {
        this.name = name;
        this.availableServices = availableServices;
        this.maxConnectionNumber = maxConnectionNumber;
    }

    public void mutate() {
        Set<Service> mutatedServices = new LinkedHashSet<Service>();
        for(Service service : Simulator.getInstance().getServices()) {
            if(Simulator.getInstance().getMtf().nextDouble() < service.getMutationProbability()) {
                if(!availableServices.contains(service)) {
                    mutatedServices.add(service);
                }
            } else {
                if(availableServices.contains(service)) {
                    mutatedServices.add(service);
                }
            }
        }
        availableServices = mutatedServices;
    }

    public Set<Service> getAvailableServices() {
        return availableServices;
    }

    public void setAvailableServices(Set<Service> availableServices) {
        this.availableServices = availableServices;
    }

    public int evaluateNeededResource() {
        return availableServices.size() + maxConnectionNumber + currentConnectionNumber;
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
        currentConnectionNumber++;
    }

    public void disconnect() {
        currentConnectionNumber = 0;
    }

    public void removeConnection() {
        currentConnectionNumber--;
    }

    public String toString() {
        return name;
    }
}
