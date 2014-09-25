package individual;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by aelie on 23/09/14.
 */
public class SimulationManager {
    private JSlider slider1;
    private JButton a1StepProgressButton;
    private JButton a10ProgressButton;
    private JButton runButton;
    private JButton pauseButton;
    private JTextField a0TextField;
    private JPanel mainPanel;

    public SimulationManager(int maxTime) {
        slider1.setMaximum(maxTime);
        a0TextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int desiredTime = Integer.parseInt(a0TextField.getText());
                Simulator.getInstance().runUntil(desiredTime);
            }
        });
        a1StepProgressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Simulator.getInstance().runSteps(1);
            }
        });
        a10ProgressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Simulator.getInstance().runSteps((int)(Simulator.getInstance().getMaxTime() * 0.1));
            }
        });
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Simulator.getInstance().setOnPause(false);
                Simulator.getInstance().start();
            }
        });
    }

    public void display() {
        JFrame frame = new JFrame("SimulationManager");
        frame.setContentPane(this.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void updateSliderPosition(int position) {
        slider1.setValue(position);
        a0TextField.setText(Integer.toString(position));
    }
}
