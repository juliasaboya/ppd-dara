package dara.comunication.rmi;

import dara.comunication.network.PlayerSlot;
import dara.comunication.protocol.GameAction;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DaraClientRemote extends Remote {
    void onMatchStarted() throws RemoteException;

    void onChatReceived(PlayerSlot senderSlot, String text) throws RemoteException;

    void onGameActionReceived(GameAction action) throws RemoteException;

    void onSessionClosed() throws RemoteException;
}
