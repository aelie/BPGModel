package individual.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by aelie on 14/12/15.
 */
class JPanelMatrix extends JPanel {
    private static final int PREF_W = 2;
    private static final int PREF_H = PREF_W;
    int size = 6;
    int padding = 0;
    int totalPaddingX = 10;
    int totalPaddingY = 10;
    private Double[][] matrix;
    int matrixWidth;
    int matrixHeight;

    public void setMatrixSize(int width, int height) {
        matrixWidth = width;
        matrixHeight = height;
        matrix = new Double[matrixWidth][matrixHeight];
    }

    public void setCell(int x, int y, double intensity) {
        matrix[x][y] = intensity;
    }

    public void setMatrix(Double[][] matrix) {
        this.matrix = matrix;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(totalPaddingX * 2 + matrixWidth * (size + padding), totalPaddingY * 2 + matrixHeight * (size + padding));//this.getParent().getSize();//new Dimension(PREF_W, PREF_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        double max = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                max = matrix[i][j] > max ? matrix[i][j] : max;
            }
        }
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                double value = matrix[i][j] / max;
                if (value >= 0) {
                    int rgbValue = (int) (value == 0 ? 0 : value * 200 + 55);
                    g2.setColor(new Color(rgbValue, rgbValue, rgbValue));
                } else {
                    int rgbValue = (int) (-value < 0.00001 ? 0 : -value * 128 + 55);
                    g2.setColor(new Color(rgbValue, 0, 0));
                }
                g2.fillRect(totalPaddingX + i * (size + padding), totalPaddingY + j * (size + padding), size, size);
                g2.setColor(Color.black);
                g2.draw(new Rectangle(totalPaddingX + i * (size + padding), totalPaddingY + j * (size + padding), size, size));
            }
        }
    }
}
