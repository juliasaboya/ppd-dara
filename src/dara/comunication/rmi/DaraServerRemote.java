package dara.comunication.rmi;

import dara.comunication.protocol.GameAction;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DaraServerRemote extends Remote {
    JoinResponse joinQueue(DaraClientRemote client) throws RemoteException;

    void sendChat(String sessionId, String text) throws RemoteException;

    void sendGameAction(String sessionId, GameAction action) throws RemoteException;

    void leaveSession(String sessionId) throws RemoteException;
}
