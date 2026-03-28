package dara.network;

import java.io.*;
import java.net.Socket;

class ClientHandler extends Thread {
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    int id;

    public ClientHandler(Socket socket, int id) {
        try {
            this.socket = socket;
            this.id = id;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            System.out.println("Cliente " + id + " conectado");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        while (true) {
            try {
                String msg = in.readUTF();

                System.out.println("Cliente " + id + ": " + msg);
                for (ClientHandler c : Server.clients) {
                    if (c != this) {
                        c.out.writeUTF("Cliente " + id + ": " + msg);
                        c.out.flush();
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}