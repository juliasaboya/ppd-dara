package dara.comunication.network;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;

public final class ServerRegistrar {
    private ServerRegistrar() {
    }

    public static void main(String[] args) {
        try {
            int port = parsePort(args);
            ensureRegistry(port);

            Server server = new Server(port);
            registerServer(server, port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownServer(server), "dara-rmi-shutdown"));

            System.out.println("Servidor Dara RMI registrado como " + Server.SERVICE_NAME + " na porta " + port + ".");
            System.out.println("Pressione Ctrl+C para encerrar.");
            Thread.sleep(Long.MAX_VALUE);
        } catch (IllegalArgumentException exception) {
            System.err.println("Argumento invalido: " + exception.getMessage());
            printUsage();
            System.exit(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Inicializacao do servidor interrompida.");
            System.exit(1);
        } catch (RemoteException exception) {
            System.err.println("Falha ao iniciar ou registrar o servidor RMI: " + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        } catch (MalformedURLException exception) {
            System.err.println("URL invalida para registro do servidor RMI: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return Server.DEFAULT_PORT;
        }

        try {
            int port = Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("porta fora do intervalo permitido: " + port);
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("porta invalida: " + args[0], exception);
        }
    }

    private static void ensureRegistry(int port) throws RemoteException {
        try {
            LocateRegistry.createRegistry(port);
            System.out.println("rmiregistry iniciado na porta " + port + ".");
        } catch (ExportException exception) {
            System.out.println("rmiregistry ja estava ativo na porta " + port + ".");
        }
    }

    private static void registerServer(Server server, int port) throws RemoteException, MalformedURLException {
        Naming.rebind("//localhost:" + port + "/" + Server.SERVICE_NAME, server);
    }

    private static void shutdownServer(Server server) {
        try {
            server.close();
        } catch (RemoteException exception) {
            System.err.println("Falha ao encerrar o servidor RMI: " + exception.getMessage());
        }
    }

    private static void printUsage() {
        System.err.println("Uso: java dara.comunication.network.ServerRegistrar [porta]");
    }
}
