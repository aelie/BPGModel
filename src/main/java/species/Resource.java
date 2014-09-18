package species;
/**
 * Created by aelie on 03/09/14.
 */
public class Resource {
    int availableResource;

    public int evaluateAvailableResource() {
        return Simulator.getSingleton().getTotalApplicationUsers();
    }
}
