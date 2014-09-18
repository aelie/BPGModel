package species;
import java.util.List;

/**
 * Created by aelie on 03/09/14.
 */
public class Server {
    List<Service> availableServices;
    int userNumber; //specie size
    int maxConnectionNumber;
    int currentConnectionNumber;
    int neededRessource;

    public int evaluateNeededRessource() {
        return availableServices.size() + maxConnectionNumber + currentConnectionNumber;
    }
}
