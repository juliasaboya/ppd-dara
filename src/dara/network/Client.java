package dara.network;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client implements Closeable {
    private final String host;
    private final int port;
    private final PlayerSlot desiredSlot;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
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
        socket = new Socket(host, port);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        output.writeUTF("JOIN:" + desiredSlot.name());
        output.flush();

        String response = input.readUTF();
        System.out.println("Resposta do servidor para " + desiredSlot.name() + ": " + response);
        if (!response.startsWith("ACCEPT:")) {
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

    @Override
    public void close() throws IOException {
        connected = false;
        if (input != null) {
            input.close();
        }
        if (output != null) {
            output.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
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
