package individual.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by aelie on 22/04/15.
 */
public class ActorComponent extends JComponent implements Comparable<ActorComponent> {

    FakeActor fakeActor;

    public static final int EMPTY = 0;
    public static final int APPLICATION = 1;
    public static final int SERVER = 2;

    public static final int LIMBO = 0;
    public static final int ALIVE = 1;
    public static final int DEAD = 2;

    public static final int CIRCLE = 0;
    public static final int SQUARE = 1;

    int actorConnections = 0;
    int maxConnections = 18;
    int type;
    int generation = 0;
    int age = 0;
    String name;
    int shape = CIRCLE;
    java.util.List<String> services;
    int state;
    boolean highlighted = false;

    public ActorComponent(FakeActor fakeActor, int state) {
        super();
        this.fakeActor = fakeActor;
        this.name = fakeActor.name;
        this.type = fakeActor instanceof FakeServer ? SERVER : APPLICATION;
        this.generation = fakeActor.generation;
        this.actorConnections = fakeActor.connections;
        this.maxConnections = fakeActor instanceof FakeServer ? ((FakeServer) fakeActor).maxConnections : 0;
        this.age = fakeActor.age;
        this.services = fakeActor.services;
        this.state = state;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(Display.componentBaseSize, Display.componentBaseSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Display.componentBaseSize, Display.componentBaseSize);
    }

    public void setActorConnections(int actorConnections) {
        this.actorConnections = actorConnections;
    }

    public int getActorSize() {
        return services.size();
    }

    public int getGeneration() {
        return generation;
    }

    public String getName() {
        return name;
    }

    public java.util.List<String> getServices() {
        return services;
    }

    public int getState() {
        return state;
    }

    public int getType() {
        return type;
    }

    public int getAge() {
        return age;
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public FakeActor getFakeActor() {
        return fakeActor;
    }

    @Override
    public void paintComponent(Graphics g) {
        int offset = 5;
        int componentSize = 0;
        super.paintComponent(g);
        int colorComponent = (int) (age / (double) Display.stepNumber * 255);
        Color backgroundAge = new Color(255 - colorComponent, 255 - colorComponent, 255 - colorComponent);
        if (state == LIMBO) {
            backgroundAge = Color.magenta;
        } else if (state == DEAD) {
            backgroundAge = Color.red;
        }
        g.setColor(backgroundAge);
        g.fillRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
        if (highlighted) {
            g.setColor(Color.red);
            ((Graphics2D)g).setStroke(new BasicStroke(10));
            g.drawRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
        }
        if (type == ActorComponent.SERVER) {
            g.setColor(new Color(0, 0, (int) (255 - generation / (double) Display.maxServerGeneration * 255)));
            componentSize = (int) (services.size() / (double) Display.maxServerSize * (Display.componentBaseSize - offset * 2));
        } else if (type == ActorComponent.APPLICATION) {
            g.setColor(new Color(0, (int) (255 - generation / (double) Display.maxApplicationGeneration * 255), 0));
            componentSize = (int) (services.size() / (double) Display.maxApplicationSize * (Display.componentBaseSize - offset * 2));
        }
        if (shape == ActorComponent.CIRCLE) {
            int arc = (int) (actorConnections / (double) maxConnections * 360) % 360;
            if (type != ActorComponent.EMPTY) {
                g.fillOval((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
            if (type == ActorComponent.SERVER) {
                g.setColor(backgroundAge);
                g.fillArc((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize, 90 - arc / 2, arc);
            }
        } else {
            int height = (int) ((1 - actorConnections / (double) maxConnections) * componentSize);
            if (type != ActorComponent.EMPTY) {
                g.fillRect((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
            if (type == ActorComponent.SERVER) {
                g.setColor(backgroundAge);
                g.fillRect((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, height);
                g.setColor(new Color(0, 0, (int) (255 - generation / (double) Display.maxServerGeneration * 255)));
                g.drawRect((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
        }
    }

    @Override
    public int compareTo(ActorComponent o) {
        return -(getActorSize() - o.getActorSize());
    }

    @Override
    public String toString() {
        return "name=" + name
                + ",type=" + type
                + ",generation=" + generation
                + ",size=" + services.size()
                + ",connections=" + actorConnections
                + ",state=" + state;
    }
}
