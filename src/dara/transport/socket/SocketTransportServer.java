package dara.transport.socket;

import dara.transport.MessageChannel;
import dara.transport.MessageChannelHandler;
import dara.transport.TransportServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketTransportServer implements TransportServer {
    private final int port;
    private final MessageChannelHandler handler;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public SocketTransportServer(int port, MessageChannelHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "dara-socket-transport");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                runHandler(socket);
            } catch (IOException exception) {
                if (running) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void runHandler(Socket socket) {
        Thread thread = new Thread(() -> {
            try (MessageChannel channel = new SocketMessageChannel(socket)) {
                handler.handle(channel);
            } catch (IOException exception) {
                if (running) {
                    exception.printStackTrace();
                }
            }
        }, "dara-socket-channel");
        thread.setDaemon(true);
        thread.start();
    }
}
