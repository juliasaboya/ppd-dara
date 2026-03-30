package dara.network;

import dara.application.lobby.LobbyClientSession;
import dara.protocol.GameAction;
import dara.transport.socket.SocketMessageChannel;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

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
        boolean slotProvided = false;

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
                slotProvided = true;
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("slot invalido: " + args[2] + ". Use PLAYER_1 ou PLAYER_2.");
            }
        }

        return new LaunchOptions(host, port, slot, slotProvided);
    }

    private static void runStandalone(LaunchOptions options) {
        if (!options.slotProvided) {
            PlayerSlot[] fallbackOrder = {PlayerSlot.PLAYER_1, PlayerSlot.PLAYER_2};
            for (PlayerSlot slot : fallbackOrder) {
                if (tryStandaloneConnection(options.host, options.port, slot)) {
                    return;
                }
            }
            System.err.println("Nenhuma vaga disponivel para conexao standalone.");
            return;
        }

        tryStandaloneConnection(options.host, options.port, options.slot);
    }

    private static boolean tryStandaloneConnection(String host, int port, PlayerSlot slot) {
        try (Client client = new Client(host, port, slot)) {
            boolean connected = client.connect();
            System.out.println("Cliente standalone conectado: " + connected);
            if (!connected) {
                return false;
            }
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
        System.err.println("Uso: java dara.network.Client [host] [porta] [PLAYER_1|PLAYER_2]");
    }

    private record LaunchOptions(String host, int port, PlayerSlot slot, boolean slotProvided) {
    }
}
