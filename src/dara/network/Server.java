package dara.network;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

class Server {
        static List<ClientHandler> clients = new ArrayList<>();
        static int port = 1024;
        static int client_count = 1;

        public static void main (String args[]){
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Aguardando conexão...");

                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Cliente conectado");

                    ClientHandler client = new ClientHandler(socket, client_count++);
                    clients.add(client);
                    client.start();
                }

            } catch (Exception e) {
                System.out.println("Erro no servidor");
            }
        }
    }
