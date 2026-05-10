package dara.ui;

import dara.model.Game;
import dara.model.Player;
import dara.comunication.network.Client;
import dara.comunication.network.PlayerSlot;
import dara.comunication.network.Server;
import dara.comunication.protocol.GameAction;
import dara.comunication.protocol.GameActionType;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class DaraFrame extends JFrame {
    private static final String LOCAL_PLAYER_NAME = "Voce";
    private static final String OPPONENT_PLAYER_NAME = "Adversario";
    private static final int FRAME_WIDTH = 920;
    private static final int FRAME_HEIGHT = 768;

    private final String host;
    private final int port;
    private final Point initialLocation;

    private LobbyPanel lobbyPanel;
    private Client client;
    private PlayerSlot localSlot;
    private DaraPanel boardPanel;
    private Timer repaintTimer;
    private Timer searchTimer;
    private int waitingSeconds;

    public DaraFrame() {
        this("localhost", Server.DEFAULT_PORT);
    }

    public DaraFrame(String host, int port) {
        super("Dara");
        this.host = host;
        this.port = port;
        this.initialLocation = resolveInitialLocation();
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

    private void beginMatchmaking() {
        if (client != null && client.isConnected()) {
            return;
        }

        lobbyPanel.showConnecting();

        Thread connectionThread = new Thread(() -> {
            Client nextClient = new Client(host, port);
            nextClient.setMatchStartListener(() -> SwingUtilities.invokeLater(this::handleMatchStarted));
            nextClient.setChatListener((senderSlot, text) ->
                    SwingUtilities.invokeLater(() -> handleChatReceived(senderSlot, text)));
            nextClient.setGameActionListener(action ->
                    SwingUtilities.invokeLater(() -> handleRemoteGameAction(action)));

            try {
                if (!nextClient.connect()) {
                    nextClient.close();
                    SwingUtilities.invokeLater(() -> {
                        client = null;
                        lobbyPanel.showConnectionError("NAO FOI POSSIVEL ENTRAR NA FILA");
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> onClientConnected(nextClient));
            } catch (IOException exception) {
                try {
                    nextClient.close();
                } catch (IOException ignored) {
                }
                SwingUtilities.invokeLater(() -> {
                    client = null;
                    lobbyPanel.showConnectionError("SERVIDOR INDISPONIVEL EM " + host + ":" + port);
                });
            }
        }, "dara-client-connect");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void onClientConnected(Client connectedClient) {
        client = connectedClient;
        localSlot = connectedClient.getAssignedSlot();
        waitingSeconds = 0;
        lobbyPanel.showSearching(localSlot);
        startSearchTimer();
        if (connectedClient.isMatchStarted()) {
            handleMatchStarted();
        }
    }

    private void handleMatchStarted() {
        if (client == null) {
            return;
        }

        if (localSlot == null) {
            localSlot = client.getAssignedSlot();
        }

        stopSearchTimer();
        lobbyPanel.showMatchFound(localSlot);
        showBoard();
    }

    private void startSearchTimer() {
        stopSearchTimer();
        searchTimer = new Timer(1000, event -> {
            waitingSeconds++;
            lobbyPanel.updateSearchSeconds(waitingSeconds);
        });
        searchTimer.setInitialDelay(1000);
        searchTimer.start();
    }

    private void stopSearchTimer() {
        if (searchTimer != null) {
            searchTimer.stop();
            searchTimer = null;
        }
    }

    private void showBoard() {
        if (boardPanel != null || localSlot == null) {
            return;
        }

        Game game = new Game(resolvePlayerOneName(), resolvePlayerTwoName());
        boardPanel = new DaraPanel(game, localSlot, this::sendChatMessage, this::sendGameAction, this::restartToFreshLobby);
        setTitle("Dara - " + LOCAL_PLAYER_NAME);

        setContentPane(boardPanel);
        pack();
        revalidate();
        repaint();
        bringWindowToFront();

        repaintTimer = new Timer(1000, event -> boardPanel.repaint());
        repaintTimer.start();
    }

    private void sendChatMessage(String text) {
        try {
            if (client != null) {
                client.sendChat(text);
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
        boardPanel.appendChatMessage(resolveChatSenderName(senderSlot), text);
    }

    private void sendGameAction(GameAction action) {
        try {
            if (client != null) {
                client.sendGameAction(action);
            }
        } catch (IOException exception) {
            if (boardPanel != null) {
                boardPanel.appendChatMessage("Sistema", "Nao foi possivel sincronizar a jogada.");
            }
        }

        disconnectAfterMatchEnded();
    }

    private void handleRemoteGameAction(GameAction action) {
        if (boardPanel == null || action == null) {
            return;
        }

        Game game = boardPanel.getGame();
        Player player = action.slot().getControlledPlayer();

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
        disconnectAfterMatchEnded();
    }

    private void restartToFreshLobby() {
        shutdownNetwork();
        client = null;
        localSlot = null;
        boardPanel = null;
        startFreshLobby();
    }

    private void startFreshLobby() {
        setTitle("Dara");
        lobbyPanel = new LobbyPanel(this::beginMatchmaking);

        setContentPane(lobbyPanel);
        pack();
        setLocation(initialLocation);
        revalidate();
        repaint();
        bringWindowToFront();
    }

    private void disconnectAfterMatchEnded() {
        if (boardPanel == null || boardPanel.getGame().getState() != dara.model.GameState.FINISHED || client == null) {
            return;
        }

        try {
            client.close();
        } catch (IOException ignored) {
        } finally {
            client = null;
        }
    }

    private String resolvePlayerOneName() {
        return localSlot != null && localSlot.getControlledPlayer() == Player.COLOR_TWO
                ? LOCAL_PLAYER_NAME
                : OPPONENT_PLAYER_NAME;
    }

    private String resolvePlayerTwoName() {
        return localSlot != null && localSlot.getControlledPlayer() == Player.COLOR_ONE
                ? LOCAL_PLAYER_NAME
                : OPPONENT_PLAYER_NAME;
    }

    private String resolveChatSenderName(PlayerSlot senderSlot) {
        if (localSlot != null && senderSlot != null
                && senderSlot.getControlledPlayer() == localSlot.getControlledPlayer()) {
            return LOCAL_PLAYER_NAME;
        }
        return OPPONENT_PLAYER_NAME;
    }

    private void shutdownNetwork() {
        try {
            stopSearchTimer();
            if (repaintTimer != null) {
                repaintTimer.stop();
                repaintTimer = null;
            }
            if (client != null) {
                client.close();
            }
        } catch (IOException ignored) {
        }
    }

    private Point resolveInitialLocation() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int baseX = Math.max(0, (screenSize.width - FRAME_WIDTH) / 2);
        int baseY = Math.max(0, (screenSize.height - FRAME_HEIGHT) / 2);
        long pid = ProcessHandle.current().pid();
        int offsetX = (int) ((pid % 5) * 36);
        int offsetY = (int) ((pid % 4) * 28);
        int x = Math.min(Math.max(0, baseX + offsetX), Math.max(0, screenSize.width - FRAME_WIDTH));
        int y = Math.min(Math.max(0, baseY + offsetY), Math.max(0, screenSize.height - FRAME_HEIGHT));
        return new Point(x, y);
    }

    private void bringWindowToFront() {
        setAlwaysOnTop(true);
        toFront();
        requestFocus();
        setAlwaysOnTop(false);
    }
}
