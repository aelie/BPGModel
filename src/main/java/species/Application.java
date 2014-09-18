package species;

import java.util.List;

/**
 * Created by aelie on 03/09/14.
 */
public class Application {
    List<Service> requiredServices;
    int userNumber; //specie size

    public List<Service> getRequiredServices() {
        return requiredServices;
    }

    public void setRequiredServices(List<Service> requiredServices) {
        this.requiredServices = requiredServices;
    }

    public int getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(int userNumber) {
        this.userNumber = userNumber;
    }
}
