package individual;

import java.util.Set;

/**
 * Created by aelie on 29/10/14.
 */
public interface Actor {

    String name = "Actor";
    int generation = 0;
    int age = 0;

    String getName();

    void setName(String name);

    int getGeneration();

    void setGeneration(int generation);

    void newGeneration();

    void older();

    Set<Service> getServices();

    void setServices(Set<Service> services);
}
