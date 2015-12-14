package individual.gui;

import java.util.List;

/**
 * Created by aelie on 14/12/15.
 */
class FakeApplication extends FakeActor {
    List<FakeServer> neighborhood;

    public FakeApplication(String name, int generation, int connections, int age, List<String> services, List<FakeServer> neighborhood) {
        super(name, generation, connections, age, services);
        this.neighborhood = neighborhood;
    }
}
