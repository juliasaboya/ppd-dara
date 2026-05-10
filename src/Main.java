import dara.ui.DaraFrame;
import dara.comunication.network.Server;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String host = args.length > 0 ? args[0] : "localhost";
            int port = args.length > 1 ? Integer.parseInt(args[1]) : Server.DEFAULT_PORT;
            DaraFrame frame = new DaraFrame(host, port);
            frame.setVisible(true);
        });
    }
}
