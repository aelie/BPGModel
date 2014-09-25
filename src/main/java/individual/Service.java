package individual;

import java.io.Serializable;

/**
 * Created by aelie on 03/09/14.
 */
public class Service implements Serializable {
    String name;
    int id;
    double mutationProbability; //0<p<0.5 arbitraire //VARIABLE

    public Service(String name, int id, double mutationProbability) {
        this.name = name;
        this.id = id;
        this.mutationProbability = mutationProbability;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMutationProbability() {
        return mutationProbability;
    }

    public void setMutationProbability(double mutationProbability) {
        this.mutationProbability = mutationProbability;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return name;
    }
}
