package species;
import java.util.List;

/**
 * Created by aelie on 03/09/14.
 */
public class Simulator {
    List<Server> servers;
    List<Application> applications;
    List<Service> services;

    private static Simulator singleton;

    private Simulator() {

    }

    public static Simulator getSingleton() {
        if(singleton == null) {
            return new Simulator();
        } else {
            return singleton;
        }
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public int getTotalApplicationUsers() {
        final int sum = 0;
        //applications.stream().forEach((Application a) -> sum += a.getUserNumber());
        return sum;
    }
}
