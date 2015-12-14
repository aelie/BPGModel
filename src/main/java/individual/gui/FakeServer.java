package individual.gui;

import java.util.List;

/**
 * Created by aelie on 14/12/15.
 */
class FakeServer extends FakeActor {
    public int maxConnections;

    public FakeServer(String name, int generation, int maxConnections, int connections, int age, List<String> services) {
        super(name, generation, connections, age, services);
        this.maxConnections = maxConnections;
    }
}
