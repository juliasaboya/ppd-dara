package dara.network;

import dara.application.lobby.LobbyClientSession;
import dara.protocol.GameAction;
import dara.transport.socket.SocketMessageChannel;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
        try {
            LaunchOptions options = parseLaunchOptions(args);
            runStandalone(options);
        } catch (IllegalArgumentException exception) {
            System.err.println("Erro nos argumentos: " + exception.getMessage());
            printUsage();
        }
    }

    private static LaunchOptions parseLaunchOptions(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = Server.DEFAULT_PORT;
        PlayerSlot slot = PlayerSlot.PLAYER_1;

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("porta invalida: " + args[1]);
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("porta fora do intervalo permitido: " + port);
            }
        }

        if (args.length > 2) {
            try {
                slot = PlayerSlot.valueOf(args[2].trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("slot invalido: " + args[2] + ". Use PLAYER_1 ou PLAYER_2.");
            }
        }

        return new LaunchOptions(host, port, slot);
    }

    private static void runStandalone(LaunchOptions options) {
        try (Client client = new Client(options.host, options.port, options.slot)) {
            boolean connected = client.connect();
            System.out.println("Cliente standalone conectado: " + connected);
            if (connected) {
                System.out.println("Pressione Ctrl+C para encerrar.");
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (UnknownHostException exception) {
            System.err.println("Host nao encontrado: " + options.host);
        } catch (IOException exception) {
            System.err.println("Falha de conexao com " + options.host + ":" + options.port + ": " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Cliente encerrado.");
        }
    }

    private static void printUsage() {
        System.err.println("Uso: java dara.network.Client [host] [porta] [PLAYER_1|PLAYER_2]");
    }

    private record LaunchOptions(String host, int port, PlayerSlot slot) {
    }
}
