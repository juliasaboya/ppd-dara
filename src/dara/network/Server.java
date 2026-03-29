package dara.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.EnumMap;
import java.util.Map;

public class Server implements Closeable {
    public static final int DEFAULT_PORT = 5050;

    private final int port;
    private final Map<PlayerSlot, ClientHandler> clients;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        this.clients = new EnumMap<>(PlayerSlot.class);
    }

    public void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Servidor Dara iniciado na porta " + port + ". Aguardando dois clientes...");
        acceptThread = new Thread(this::acceptLoop, "dara-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int getPort() {
        return port;
    }

    public synchronized boolean hasClient(PlayerSlot slot) {
        return clients.containsKey(slot);
    }

    public synchronized boolean hasTwoClients() {
        return clients.size() == PlayerSlot.values().length;
    }

    synchronized boolean registerClient(PlayerSlot slot, ClientHandler handler) {
        if (slot == null || clients.containsKey(slot)) {
            return false;
        }
        clients.put(slot, handler);
        System.out.println("Slot ocupado: " + slot.name() + ". Total conectados: " + clients.size());
        return true;
    }

    synchronized void unregisterClient(PlayerSlot slot, ClientHandler handler) {
        if (slot != null && clients.get(slot) == handler) {
            clients.remove(slot);
            System.out.println("Slot liberado: " + slot.name() + ". Total conectados: " + clients.size());
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket).start();
            } catch (IOException exception) {
                if (running) {
                    exception.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        System.out.println("Servidor Dara encerrado.");
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        try (Server server = new Server(port)) {
            server.start();
            System.out.println("Pressione Ctrl+C para encerrar.");
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
