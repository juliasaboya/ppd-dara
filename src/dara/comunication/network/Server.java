package dara.comunication.network;

import dara.application.lobby.LobbyServerCoordinator;
import dara.comunication.protocol.GameAction;
import dara.comunication.rmi.DaraClientRemote;
import dara.comunication.rmi.DaraServerRemote;
import dara.comunication.rmi.JoinResponse;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server extends UnicastRemoteObject implements Closeable, DaraServerRemote {
    public static final int DEFAULT_PORT = 1024;
    public static final String SERVICE_NAME = "DaraServer";

    private final LobbyServerCoordinator coordinator;

    public Server(int ignoredPort) throws RemoteException {
        super();
        this.coordinator = new LobbyServerCoordinator();
    }

    @Override
    public JoinResponse joinQueue(DaraClientRemote client) throws RemoteException {
        JoinResponse response = coordinator.occupy(client);
        if (response == null) {
            return null;
        }
        coordinator.startMatchIfReady();
        return response;
    }

    @Override
    public void sendChat(String sessionId, String text) {
        coordinator.sendChat(sessionId, text);
    }

    @Override
    public void sendGameAction(String sessionId, GameAction action) {
        coordinator.sendGameAction(sessionId, action);
    }

    @Override
    public void leaveSession(String sessionId) {
        coordinator.release(sessionId);
    }

    @Override
    public void close() throws RemoteException {
        UnicastRemoteObject.unexportObject(this, true);
        System.out.println("Servidor Dara encerrado.");
    }

    public static void main(String[] args) {
        ServerRegistrar.main(args);
    }
}
