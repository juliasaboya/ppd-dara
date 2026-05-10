package dara.comunication.network;

import dara.application.lobby.LobbyClientSession;
import dara.comunication.protocol.GameAction;
import dara.comunication.rmi.DaraServerRemote;
import dara.comunication.rmi.JoinResponse;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.util.Scanner;

public class Client implements Closeable {
    public interface ChatListener {
        void onChatReceived(PlayerSlot senderSlot, String text);
    }

    public interface MatchStartListener {
        void onMatchStarted();
    }

    public interface GameActionListener {
        void onGameActionReceived(GameAction action);
    }

    private final String host;
    private final int port;

    private DaraServerRemote server;
    private LobbyClientSession session;
    private PlayerSlot assignedSlot;
    private ChatListener chatListener;
    private MatchStartListener matchStartListener;
    private GameActionListener gameActionListener;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() throws IOException {
        if (isConnected()) {
            return true;
        }

        System.out.println("Tentando conectar em " + host + ":" + port + " via RMI");
        try {
            server = (DaraServerRemote) Naming.lookup("//" + host + ":" + port + "/" + Server.SERVICE_NAME);
            session = new LobbyClientSession();
        } catch (Exception exception) {
            throw new IOException("Falha ao localizar o servidor RMI.", exception);
        }

        applyRegisteredListeners();
        JoinResponse response = server.joinQueue(session);
        boolean accepted = session.connect(response);
        assignedSlot = session.getAssignedSlot();
        System.out.println("Resposta do servidor: " + accepted);
        if (!accepted) {
            close();
            return false;
        }

        System.out.println("Cliente conectado com sucesso como " + assignedSlot.name() + ".");
        return true;
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public PlayerSlot getAssignedSlot() {
        return assignedSlot;
    }

    public boolean isMatchStarted() {
        return session != null && session.isMatchStarted();
    }

    public void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
        if (session != null) {
            session.setChatListener(chatListener::onChatReceived);
        }
    }

    public void setMatchStartListener(MatchStartListener matchStartListener) {
        this.matchStartListener = matchStartListener;
        if (session != null) {
            session.setMatchStartListener(matchStartListener::onMatchStarted);
        }
    }

    public void setGameActionListener(GameActionListener gameActionListener) {
        this.gameActionListener = gameActionListener;
        if (session != null) {
            session.setGameActionListener(gameActionListener::onGameActionReceived);
        }
    }

    public void sendChat(String text) throws IOException {
        if (server != null && session != null && session.getSessionId() != null) {
            server.sendChat(session.getSessionId(), text);
        }
    }

    public void sendGameAction(GameAction action) throws IOException {
        if (server != null && session != null && session.getSessionId() != null) {
            server.sendGameAction(session.getSessionId(), action);
        }
    }

    @Override
    public void close() throws IOException {
        if (server != null && session != null && session.getSessionId() != null) {
            try {
                server.leaveSession(session.getSessionId());
            } catch (Exception ignored) {
            }
        }
        if (session != null) {
            session.close();
        }
        session = null;
        server = null;
        assignedSlot = null;
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

        return new LaunchOptions(host, port);
    }

    private static void runStandalone(LaunchOptions options) {
        tryStandaloneConnection(options.host, options.port);
    }

    private static boolean tryStandaloneConnection(String host, int port) {
        try (Client client = new Client(host, port)) {
            boolean connected = client.connect();
            System.out.println("Cliente standalone conectado: " + connected);
            if (!connected) {
                return false;
            }
            System.out.println("Slot atribuido: " + client.getAssignedSlot().getLabel());
            client.setChatListener((senderSlot, text) -> System.out.println(senderSlot.getLabel() + ": " + text));
            runTerminalChat(client);
            return true;
        } catch (UnknownHostException exception) {
            System.err.println("Host nao encontrado: " + host);
        } catch (IOException exception) {
            System.err.println("Falha de conexao com " + host + ":" + port + ": " + exception.getMessage());
        }
        return false;
    }

    private static void runTerminalChat(Client client) throws IOException {
        System.out.println("Digite mensagens para enviar ao outro jogador.");
        System.out.println("Use /sair para encerrar o cliente.");

        Scanner scanner = new Scanner(System.in);
        while (client.isConnected()) {
            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine();
            if (line == null) {
                break;
            }

            String text = line.trim();
            if (text.isEmpty()) {
                continue;
            }

            if ("/sair".equalsIgnoreCase(text)) {
                System.out.println("Cliente encerrado pelo usuario.");
                break;
            }

            client.sendChat(text);
        }
    }

    private static void printUsage() {
        System.err.println("Uso: java dara.sockets_comunication.network.Client [host] [porta]");
    }

    private void applyRegisteredListeners() {
        if (session == null) {
            return;
        }
        if (chatListener != null) {
            session.setChatListener(chatListener::onChatReceived);
        }
        if (matchStartListener != null) {
            session.setMatchStartListener(matchStartListener::onMatchStarted);
        }
        if (gameActionListener != null) {
            session.setGameActionListener(gameActionListener::onGameActionReceived);
        }
    }

    private record LaunchOptions(String host, int port) {
    }
}
