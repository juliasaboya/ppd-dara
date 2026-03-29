import dara.ui.DaraFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DaraFrame frame = new DaraFrame();
            frame.setVisible(true);
        });
    }
}
