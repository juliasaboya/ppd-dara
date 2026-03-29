package dara.ui;

import dara.model.Game;
import dara.network.Client;
import dara.network.PlayerSlot;
import dara.network.Server;

import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class DaraFrame extends JFrame {
    private final Server server;
    private final LobbyPanel lobbyPanel;
    private Client playerOneClient;
    private Client playerTwoClient;
    private Timer repaintTimer;
    private Timer countdownTimer;

    public DaraFrame() {
        super("Dara");
        this.server = new Server();
        this.lobbyPanel = new LobbyPanel(
                () -> connectPlayer(PlayerSlot.PLAYER_1),
                () -> connectPlayer(PlayerSlot.PLAYER_2)
        );

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(lobbyPanel);
        pack();
        setLocationRelativeTo(null);

        try {
            server.start();
            System.out.println("Lobby iniciado. Servidor local ativo.");
        } catch (IOException exception) {
            lobbyPanel.showConnectionError("NAO FOI POSSIVEL INICIAR O SERVIDOR");
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                shutdownNetwork();
            }
        });
    }

    private void connectPlayer(PlayerSlot slot) {
        try {
            System.out.println("Tentando conectar via lobby: " + slot.name());
            Client client = new Client("localhost", server.getPort(), slot);
            if (!client.connect()) {
                lobbyPanel.showConnectionError("ESSA VAGA JA ESTA EM USO");
                return;
            }

            if (slot == PlayerSlot.PLAYER_1) {
                playerOneClient = client;
            } else {
                playerTwoClient = client;
            }

            lobbyPanel.markConnected(slot);

            if (playerOneClient != null && playerTwoClient != null) {
                System.out.println("Dois clientes conectados. Iniciando contagem.");
                beginCountdown();
            }
        } catch (IOException exception) {
            lobbyPanel.showConnectionError("ERRO AO CONECTAR AO SERVIDOR");
        }
    }

    private void beginCountdown() {
        lobbyPanel.startCountdown();

        final int[] countdown = {3};
        countdownTimer = new Timer(1000, event -> {
            countdown[0]--;
            if (countdown[0] > 0) {
                lobbyPanel.updateCountdown(countdown[0]);
                return;
            }

            countdownTimer.stop();
            showBoard();
        });
        countdownTimer.setInitialDelay(1000);
        countdownTimer.start();
    }

    private void showBoard() {
        System.out.println("Contagem concluida. Abrindo tabuleiro.");
        Game game = new Game(lobbyPanel.getPlayerOneName(), lobbyPanel.getPlayerTwoName());
        DaraPanel boardPanel = new DaraPanel(game);

        setContentPane(boardPanel);
        pack();
        revalidate();
        repaint();

        repaintTimer = new Timer(1000, event -> boardPanel.repaint());
        repaintTimer.start();
    }

    private void shutdownNetwork() {
        try {
            if (playerOneClient != null) {
                playerOneClient.close();
            }
            if (playerTwoClient != null) {
                playerTwoClient.close();
            }
            server.close();
        } catch (IOException ignored) {
        }
    }
}
