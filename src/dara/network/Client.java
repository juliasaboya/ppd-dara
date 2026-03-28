package dara.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {

    static DataOutputStream out_stream = null;
    DataInputStream in_stream = null;

    static String host = "";
    static int port = 1024;
    Socket socket = null;
    String mensRecebida = "";

    Client(){
        try {
            socket = new Socket(host, port);
            System.out.println("Conectado....");
            this.start();
            out_stream = new DataOutputStream(socket.getOutputStream());
            in_stream = new DataInputStream(socket.getInputStream());
            Scanner console = new Scanner(System.in);
            while(true){
                System.out.println("Você: ");
                String msg = console.nextLine();
                out_stream.writeUTF(msg);
                out_stream.flush();
            }
        } catch(Exception e) {System.out.println(e);}
    }

    public void run(){
        while (true) {
            try {
                mensRecebida = in_stream.readUTF();
                System.out.println(mensRecebida);
            } catch(Exception e) {}
        }
    }

    public static void main(String args[]){
        host = args.length == 0 ? "localhost" : args[0];
        new Client();
    }
}
