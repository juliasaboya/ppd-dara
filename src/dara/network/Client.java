package dara.network;

import dara.application.lobby.LobbyClientSession;
import dara.protocol.GameAction;
import dara.transport.socket.SocketMessageChannel;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class Client implements Closeable {
    public interface ChatListener {
        void onChatReceived(PlayerSlot senderSlot, String text);
    }

    public interface GameActionListener {
        void onGameActionReceived(GameAction action);
    }

    private final String host;
    private final int port;
    private final PlayerSlot desiredSlot;

    private LobbyClientSession session;
    private boolean connected;

    public Client(String host, int port, PlayerSlot desiredSlot) {
        this.host = host;
        this.port = port;
        this.desiredSlot = desiredSlot;
    }

    public boolean connect() throws IOException {
        if (connected) {
            return true;
        }

        System.out.println("Tentando conectar " + desiredSlot.name() + " em " + host + ":" + port);
        Socket socket = new Socket(host, port);
        session = new LobbyClientSession(new SocketMessageChannel(socket), desiredSlot);
        boolean accepted = session.connect();
        System.out.println("Resposta do servidor para " + desiredSlot.name() + ": " + accepted);
        if (!accepted) {
            close();
            return false;
        }

        connected = true;
        System.out.println("Cliente " + desiredSlot.name() + " conectado com sucesso.");
        return true;
    }

    public boolean isConnected() {
        return connected;
    }

    public PlayerSlot getDesiredSlot() {
        return desiredSlot;
    }

    public void setChatListener(ChatListener chatListener) {
        if (session != null) {
            session.setChatListener(chatListener::onChatReceived);
        }
    }

    public void setGameActionListener(GameActionListener gameActionListener) {
        if (session != null) {
            session.setGameActionListener(gameActionListener::onGameActionReceived);
        }
    }

    public void sendChat(String text) throws IOException {
        if (session != null) {
            session.sendChat(text);
        }
    }

    public void sendGameAction(GameAction action) throws IOException {
        if (session != null) {
            session.sendGameAction(action);
        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        if (session != null) {
            session.close();
        }
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Server.DEFAULT_PORT;
        PlayerSlot slot = args.length > 2 ? PlayerSlot.valueOf(args[2].toUpperCase()) : PlayerSlot.PLAYER_1;

        try (Client client = new Client(host, port, slot)) {
            boolean connected = client.connect();
            System.out.println("Cliente standalone conectado: " + connected);
            if (connected) {
                System.out.println("Pressione Ctrl+C para encerrar.");
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
