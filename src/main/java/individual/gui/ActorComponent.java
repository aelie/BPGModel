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
    boolean neighbor = false;

    public ActorComponent(FakeActor fakeActor, int state, int shape) {
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
        this.shape = shape;
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

    public boolean isNeighbor() {
        return neighbor;
    }

    public void setNeighbor(boolean neighbor) {
        this.neighbor = neighbor;
    }

    public FakeActor getFakeActor() {
        return fakeActor;
    }

    @Override
    public void paintComponent(Graphics g) {
        int offset = 5;
        int componentSize = 0;
        super.paintComponent(g);
        int ageColorComponent = (int) (age / (double) Display.stepNumber * 150);
        Color backgroundAgeColor = new Color(255 - ageColorComponent, 255 - ageColorComponent, 255 - ageColorComponent);
        Color serverGenerationColor;
        if (Display.maxServerGeneration != 0) {
            serverGenerationColor = new Color((int) (255 * (1 - generation / (double) Display.maxServerGeneration)), 0, 0);
        } else {
            serverGenerationColor = new Color(255, 0, 0);
        }
        Color applicationGenerationColor;
        if (Display.maxApplicationGeneration != 0) {
            applicationGenerationColor = new Color(0, (int) (255 * (1 - generation / (double) Display.maxApplicationGeneration)), 0);
        } else {
            applicationGenerationColor = new Color(0, 255, 0);
        }
        if (state == LIMBO) {
            backgroundAgeColor = Color.magenta;
        } else if (state == DEAD) {
            backgroundAgeColor = Color.red;
        }
        g.setColor(backgroundAgeColor);
        g.fillRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
        if (type == ActorComponent.SERVER) {
            g.setColor(serverGenerationColor);
            componentSize = (int) ((4 * services.size() / (double) Display.maxServerSize + 1) / 5f * (Display.componentBaseSize - offset * 2));
        } else if (type == ActorComponent.APPLICATION) {
            g.setColor(applicationGenerationColor);
            componentSize = (int) ((4 * services.size() / (double) Display.maxApplicationSize + 1) / 5f * (Display.componentBaseSize - offset * 2));
        }
        if (shape == ActorComponent.CIRCLE) {
            int arc = (int) ((1 - actorConnections / (double) maxConnections) * 360);
            if (type != ActorComponent.EMPTY) {
                g.fillOval((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
            if (type == ActorComponent.SERVER) {
                g.setColor(backgroundAgeColor);
                g.fillArc((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize, 90 - arc / 2, arc);
                g.setColor(serverGenerationColor);
                g.drawOval((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
        } else {
            int height = (int) ((1 - actorConnections / (double) maxConnections) * componentSize);
            if (type != ActorComponent.EMPTY) {
                g.fillRect((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
            if (type == ActorComponent.SERVER) {
                g.setColor(backgroundAgeColor);
                g.fillRect((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, height);
                g.setColor(serverGenerationColor);
                g.drawRect((Display.componentBaseSize - componentSize) / 2, (Display.componentBaseSize - componentSize) / 2,
                        componentSize, componentSize);
            }
        }
        if (highlighted && !neighbor) {
            g.setColor(Color.black);
            ((Graphics2D) g).setStroke(new BasicStroke(10));
            g.drawRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
        } else if (neighbor && !highlighted) {
            g.setColor(Color.yellow);
            ((Graphics2D) g).setStroke(new BasicStroke(10));
            g.drawRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
        } else if (highlighted && neighbor) {
            g.setColor(Color.black);
            ((Graphics2D) g).setStroke(new BasicStroke(10));
            g.drawRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
            g.setColor(Color.yellow);
            ((Graphics2D) g).setStroke(new BasicStroke(10, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
                    new float[]{Display.componentBaseSize / 3f, Display.componentBaseSize * 2 / 3f}, 0.0f));
            g.drawRect(0, 0, Display.componentBaseSize, Display.componentBaseSize);
        }
        ((Graphics2D) g).setStroke(new BasicStroke(1));
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
