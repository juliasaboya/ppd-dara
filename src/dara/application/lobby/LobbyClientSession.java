package dara.application.lobby;

import dara.comunication.network.PlayerSlot;
import dara.comunication.protocol.GameAction;
import dara.comunication.rmi.DaraClientRemote;
import dara.comunication.rmi.JoinResponse;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class LobbyClientSession extends UnicastRemoteObject implements DaraClientRemote {
    public interface ChatListener {
        void onChatReceived(PlayerSlot senderSlot, String text);
    }

    public interface MatchStartListener {
        void onMatchStarted();
    }

    public interface GameActionListener {
        void onGameActionReceived(GameAction action);
    }

    private ChatListener chatListener;
    private MatchStartListener matchStartListener;
    private GameActionListener gameActionListener;
    private boolean connected;
    private boolean matchStarted;
    private PlayerSlot assignedSlot;
    private String sessionId;

    public LobbyClientSession() throws RemoteException {
        super();
    }

    public boolean connect(JoinResponse response) {
        if (connected) {
            return true;
        }
        if (response == null || response.sessionId() == null || response.assignedSlot() == null) {
            return false;
        }
        sessionId = response.sessionId();
        assignedSlot = response.assignedSlot();
        connected = true;
        return true;
    }

    public boolean isConnected() {
        return connected;
    }

    public PlayerSlot getAssignedSlot() {
        return assignedSlot;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isMatchStarted() {
        return matchStarted;
    }

    public void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    public void setMatchStartListener(MatchStartListener matchStartListener) {
        this.matchStartListener = matchStartListener;
    }

    public void setGameActionListener(GameActionListener gameActionListener) {
        this.gameActionListener = gameActionListener;
    }

    public void close() {
        connected = false;
        sessionId = null;
        assignedSlot = null;
        matchStarted = false;
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException ignored) {
        }
    }

    @Override
    public void onMatchStarted() {
        matchStarted = true;
        if (matchStartListener != null) {
            matchStartListener.onMatchStarted();
        }
    }

    @Override
    public void onChatReceived(PlayerSlot senderSlot, String text) {
        if (chatListener != null) {
            chatListener.onChatReceived(senderSlot, text);
        }
    }

    @Override
    public void onGameActionReceived(GameAction action) {
        if (gameActionListener != null) {
            gameActionListener.onGameActionReceived(action);
        }
    }

    @Override
    public void onSessionClosed() {
        connected = false;
    }
}
