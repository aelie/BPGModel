package individual;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;
import tools.Tools;

import javax.swing.*;
import java.util.Map;
import java.util.Set;

/**
 * Created by aelie on 23/09/14.
 */
public class DisplayGraph {
    Graph graph;
    public DisplayGraph() {
        graph = new SingleGraph("embedded");
        Viewer viewer = graph.display();
        viewer.disableAutoLayout();
        //viewer.enableAutoLayout(new SpringBoxBP());
    }

    public void addServer(Server server) {
        Node serverNode;
        if((serverNode = graph.getNode(server.toString())) == null) {
            serverNode = graph.addNode(server.toString());
            serverNode.setAttribute("ui.label", serverNode.getId());
        }
    }

    public void removeServer(Server server) {
        graph.removeNode(server.toString());
    }

    public void removeServers(Set<Server> servers) {
        for(Server server : servers) {
            removeServer(server);
        }
    }

    public void removeApplication(Application application) {
        graph.removeNode(application.toString());
    }

    public void addApplication(Application application) {
        Node applicationNode;
        if((applicationNode = graph.getNode(application.toString())) == null) {
            applicationNode = graph.addNode(application.toString());
            applicationNode.setAttribute("ui.label", applicationNode.getId());
        }
    }

    public void displayGraph(Map<Server, Set<Application>> connexions) {
        graph.clear();
        if(connexions != null) {
            int serverCounter = 0;
            int applicationCounter = 0;
            for (Server server : connexions.keySet()) {
                Node serverNode;
                if((serverNode = graph.getNode(server.toString())) == null) {
                    serverNode = graph.addNode(server.toString());
                    serverNode.setAttribute("ui.label", serverNode.getId());
                    serverNode.setAttribute("xyz", serverCounter, 0, 0);
                }
                for (Application application : connexions.get(server)) {
                    Node applicationNode;
                    if(graph.getNode(application.toString()) == null) {
                        applicationNode = graph.addNode(application.toString());
                        applicationNode.setAttribute("ui.label", applicationNode.getId());
                        applicationNode.setAttribute("xyz", applicationCounter, 2, 0);
                        applicationCounter++;
                    }
                    else {
                        applicationNode = graph.getNode(application.toString());
                    }
                    String edgeName = applicationNode.getAttribute("ui.label");
                    for (Service service : Tools.getMatchingServices(server.getAvailableServices(), application.getRequiredServices())) {
                        edgeName += service.getName() + "-";
                    }
                    if(graph.getEdge(edgeName) == null) {
                        graph.addEdge(edgeName, applicationNode.getId(), serverNode.getId()).setAttribute("ui.label", edgeName);
                    }
                }
                serverCounter++;
            }
        }
    }
}
