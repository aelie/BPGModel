package individual.gui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aelie on 14/12/15.
 */
class BoldListCellRenderer extends DefaultListCellRenderer {

    Map<String, Double> boldItems = new HashMap<>();
    Map<String, Double> itemsDemand = new HashMap<>();

    public void setBoldItems(Map<String, Double> boldItems) {
        this.boldItems = boldItems;
    }

    public void setItemsDemand(Map<String, Double> itemsDemand) {
        this.itemsDemand = itemsDemand;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component item = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
            if (itemsDemand.keySet().contains(value)) {
                String demand = Double.toString(itemsDemand.get(value));
                ((BoldListCellRenderer) item).setText(((BoldListCellRenderer) item).getText()
                        + " (" + demand.substring(0, Math.min(5, demand.length())) + ")");
            }
            if (boldItems.keySet().contains(value)) {
                item.setBackground(new Color((int) (boldItems.get(value) * 255), 0, 0));
                item.setForeground(Color.white);
                String demand = Double.toString(boldItems.get(value));
                ((BoldListCellRenderer) item).setText(((BoldListCellRenderer) item).getText()
                        + ": " + demand.substring(0, Math.min(5, demand.length())));
            }
        }
        return item;
    }
}
