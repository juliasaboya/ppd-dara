package dara.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class ClientHandler extends Thread {
    private final Server server;
    private final Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private PlayerSlot assignedSlot;

    ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        setName("dara-client-handler");
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            String request = input.readUTF();
            System.out.println("Requisicao recebida: " + request);
            PlayerSlot desiredSlot = PlayerSlot.fromProtocol(request);

            if (desiredSlot == null || !server.registerClient(desiredSlot, this)) {
                output.writeUTF("REJECT:SLOT_UNAVAILABLE");
                output.flush();
                System.out.println("Conexao rejeitada.");
                return;
            }

            assignedSlot = desiredSlot;
            output.writeUTF("ACCEPT:" + assignedSlot.name());
            output.flush();
            System.out.println("Cliente registrado como " + assignedSlot.name());

            while (!socket.isClosed()) {
                input.readUTF();
            }
        } catch (IOException ignored) {
            // A desconexao do cliente encerra o handler.
        } finally {
            if (assignedSlot != null) {
                System.out.println("Cliente " + assignedSlot.name() + " desconectado.");
            }
            server.unregisterClient(assignedSlot, this);
            closeSilently();
        }
    }

    private void closeSilently() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
