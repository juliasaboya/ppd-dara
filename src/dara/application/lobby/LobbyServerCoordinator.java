package dara.application.lobby;

import dara.comunication.network.PlayerSlot;
import dara.comunication.protocol.GameAction;
import dara.comunication.protocol.GameActionType;
import dara.comunication.rmi.DaraClientRemote;
import dara.comunication.rmi.JoinResponse;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LobbyServerCoordinator {
    private final Map<PlayerSlot, Session> occupiedSlots = new EnumMap<>(PlayerSlot.class);
    private final Map<String, Session> sessionsById = new LinkedHashMap<>();
    private boolean matchStarted;

    public synchronized JoinResponse occupy(DaraClientRemote client) {
        PlayerSlot slot = nextAvailableSlot();
        if (slot == null) {
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, slot, client);
        occupiedSlots.put(slot, session);
        sessionsById.put(sessionId, session);
        return new JoinResponse(sessionId, slot);
    }

    public synchronized void release(String sessionId) {
        Session session = sessionsById.remove(sessionId);
        if (session == null) {
            return;
        }

        occupiedSlots.remove(session.slot);
        List<Session> remainingSessions = new ArrayList<>(occupiedSlots.values());
        occupiedSlots.clear();
        sessionsById.clear();
        matchStarted = false;

        for (Session remainingSession : remainingSessions) {
            try {
                remainingSession.client.onSessionClosed();
            } catch (Exception ignored) {
            }
        }
    }

    private boolean hasTwoClients() {
        return occupiedSlots.size() == PlayerSlot.values().length;
    }

    public synchronized void startMatchIfReady() {
        if (!hasTwoClients() || matchStarted) {
            return;
        }

        for (Session session : occupiedSlots.values()) {
            try {
                session.client.onMatchStarted();
            } catch (Exception ignored) {
            }
        }
        matchStarted = true;
    }

    public synchronized void sendChat(String sessionId, String text) {
        Session sender = sessionsById.get(sessionId);
        if (sender == null || text == null || text.isBlank()) {
            return;
        }

        for (Map.Entry<PlayerSlot, Session> entry : occupiedSlots.entrySet()) {
            if (entry.getKey() == sender.slot) {
                continue;
            }

            try {
                entry.getValue().client.onChatReceived(sender.slot, text.trim());
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void sendGameAction(String sessionId, GameAction action) {
        Session sender = sessionsById.get(sessionId);
        if (sender == null || action == null) {
            return;
        }

        GameAction sanitizedAction = new GameAction(
                action.type() == null ? GameActionType.PLACE : action.type(),
                sender.slot,
                action.row(),
                action.column(),
                action.fromRow(),
                action.fromColumn(),
                action.toRow(),
                action.toColumn()
        );

        for (Map.Entry<PlayerSlot, Session> entry : occupiedSlots.entrySet()) {
            if (entry.getKey() == sender.slot) {
                continue;
            }

            try {
                entry.getValue().client.onGameActionReceived(sanitizedAction);
            } catch (Exception ignored) {
            }
        }
    }

    private PlayerSlot nextAvailableSlot() {
        for (PlayerSlot slot : PlayerSlot.values()) {
            if (!occupiedSlots.containsKey(slot)) {
                return slot;
            }
        }
        return null;
    }

    private static final class Session {
        private final String sessionId;
        private final PlayerSlot slot;
        private final DaraClientRemote client;

        private Session(String sessionId, PlayerSlot slot, DaraClientRemote client) {
            this.sessionId = sessionId;
            this.slot = slot;
            this.client = client;
        }
    }
}
