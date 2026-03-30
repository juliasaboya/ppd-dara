package dara.network;

import dara.application.lobby.LobbyServerCoordinator;
import dara.application.lobby.LobbyServerHandler;
import dara.transport.TransportServer;
import dara.transport.socket.SocketTransportServer;

import java.io.Closeable;
import java.io.IOException;

public class Server implements Closeable {
    public static final int DEFAULT_PORT = 1024;

    private final int port;
    private final LobbyServerCoordinator coordinator;
    private final TransportServer transportServer;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        this.coordinator = new LobbyServerCoordinator();
        this.transportServer = new SocketTransportServer(port, new LobbyServerHandler(coordinator));
    }

    public void start() throws IOException {
        System.out.println("Servidor Dara iniciado na porta " + port + ". Aguardando dois clientes...");
        transportServer.start();
    }

    public int getPort() {
        return port;
    }

    public boolean hasTwoClients() {
        return coordinator.hasTwoClients();
    }

    @Override
    public void close() throws IOException {
        transportServer.close();
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
