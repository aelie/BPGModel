package individual;

import java.util.Set;

/**
 * Created by aelie on 29/10/14.
 */
public interface Actor {

    public String getName();

    public void setName(String name);

    public Set<Service> getServices();

    public void setServices(Set<Service> services);
}
