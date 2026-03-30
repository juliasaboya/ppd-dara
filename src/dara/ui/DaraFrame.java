package dara.ui;

import dara.model.Game;
import dara.model.Player;
import dara.network.Client;
import dara.network.PlayerSlot;
import dara.network.Server;
import dara.protocol.GameAction;
import dara.protocol.GameActionType;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class DaraFrame extends JFrame {
    private Server server;
    private LobbyPanel lobbyPanel;
    private Client playerOneClient;
    private Client playerTwoClient;
    private DaraPanel boardPanel;
    private Timer repaintTimer;
    private Timer countdownTimer;

    public DaraFrame() {
        super("Dara");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        startFreshLobby();

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

            client.setChatListener((senderSlot, text) -> SwingUtilities.invokeLater(() -> handleChatReceived(senderSlot, text)));
            client.setGameActionListener(action -> SwingUtilities.invokeLater(() -> handleRemoteGameAction(action)));

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
        boardPanel = new DaraPanel(game, this::sendChatMessage, this::sendGameAction, this::restartToFreshLobby);

        setContentPane(boardPanel);
        pack();
        revalidate();
        repaint();

        repaintTimer = new Timer(1000, event -> boardPanel.repaint());
        repaintTimer.start();
    }

    private void sendChatMessage(PlayerSlot slot, String text) {
        try {
            if (slot == PlayerSlot.PLAYER_1 && playerOneClient != null) {
                playerOneClient.sendChat(text);
            } else if (slot == PlayerSlot.PLAYER_2 && playerTwoClient != null) {
                playerTwoClient.sendChat(text);
            }
        } catch (IOException exception) {
            if (boardPanel != null) {
                boardPanel.appendChatMessage("Sistema", "Nao foi possivel enviar a mensagem.");
            }
        }
    }

    private void handleChatReceived(PlayerSlot senderSlot, String text) {
        if (boardPanel == null || text == null || text.isBlank()) {
            return;
        }
        String senderName = senderSlot == PlayerSlot.PLAYER_1 ? lobbyPanel.getPlayerOneName() : lobbyPanel.getPlayerTwoName();
        boardPanel.appendChatMessage(senderName, text);
    }

    private void sendGameAction(GameAction action) {
        try {
            if (action.slot() == PlayerSlot.PLAYER_1 && playerOneClient != null) {
                playerOneClient.sendGameAction(action);
            } else if (action.slot() == PlayerSlot.PLAYER_2 && playerTwoClient != null) {
                playerTwoClient.sendGameAction(action);
            }
        } catch (IOException exception) {
            if (boardPanel != null) {
                boardPanel.appendChatMessage("Sistema", "Nao foi possivel sincronizar a jogada.");
            }
        }
    }

    private void handleRemoteGameAction(GameAction action) {
        if (boardPanel == null || action == null) {
            return;
        }

        // Na UI atual de janela unica existem dois clientes locais. Reaplicar aqui duplicaria a jogada.
        if (playerOneClient != null && playerTwoClient != null) {
            return;
        }

        Game game = boardPanel.getGame();
        Player player = action.slot() == PlayerSlot.PLAYER_1 ? Player.COLOR_TWO : Player.COLOR_ONE;

        if (action.type() == GameActionType.PLACE) {
            game.applyRemotePlace(player, action.row(), action.column());
        } else if (action.type() == GameActionType.MOVE) {
            game.applyRemoteMove(player, action.fromRow(), action.fromColumn(), action.toRow(), action.toColumn());
        } else if (action.type() == GameActionType.REMOVE) {
            game.applyRemoteRemove(player, action.row(), action.column());
        } else if (action.type() == GameActionType.SURRENDER) {
            game.applyRemoteSurrender(player);
        }

        boardPanel.refreshStatus();
        boardPanel.repaint();
    }

    private void restartToFreshLobby() {
        shutdownNetwork();
        playerOneClient = null;
        playerTwoClient = null;
        boardPanel = null;
        startFreshLobby();
    }

    private void startFreshLobby() {
        server = new Server();
        lobbyPanel = new LobbyPanel(
                () -> connectPlayer(PlayerSlot.PLAYER_1),
                () -> connectPlayer(PlayerSlot.PLAYER_2)
        );

        setContentPane(lobbyPanel);
        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();

        try {
            server.start();
            System.out.println("Lobby iniciado. Servidor local ativo.");
        } catch (IOException exception) {
            lobbyPanel.showConnectionError("NAO FOI POSSIVEL INICIAR O SERVIDOR");
        }
    }

    private void shutdownNetwork() {
        try {
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            if (repaintTimer != null) {
                repaintTimer.stop();
            }
            if (playerOneClient != null) {
                playerOneClient.close();
            }
            if (playerTwoClient != null) {
                playerTwoClient.close();
            }
            if (server != null) {
                server.close();
            }
        } catch (IOException ignored) {
        }
    }
}
