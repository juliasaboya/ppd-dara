package dara.transport.socket;

import dara.transport.MessageChannel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketMessageChannel implements MessageChannel {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    public SocketMessageChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void send(String payload) throws IOException {
        output.writeUTF(payload);
        output.flush();
    }

    @Override
    public String receive() throws IOException {
        return input.readUTF();
    }

    @Override
    public void close() throws IOException {
        input.close();
        output.close();
        socket.close();
    }
}
