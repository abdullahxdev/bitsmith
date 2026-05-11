package aluviz;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ALU Internals Visualizer — Computer Architecture Final Lab");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new MainPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setMinimumSize(new Dimension(1100, 720));
            frame.setVisible(true);
        });
    }
}
