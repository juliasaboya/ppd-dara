package dara.application.lobby;

import dara.network.PlayerSlot;
import dara.transport.MessageChannel;

import java.util.EnumMap;
import java.util.Map;

public class LobbyServerCoordinator {
    private final Map<PlayerSlot, Session> occupiedSlots = new EnumMap<>(PlayerSlot.class);

    public synchronized boolean occupy(PlayerSlot slot, Object token, MessageChannel channel) {
        if (slot == null || occupiedSlots.containsKey(slot)) {
            return false;
        }
        occupiedSlots.put(slot, new Session(token, channel));
        return true;
    }

    public synchronized void release(PlayerSlot slot, Object token) {
        if (slot != null && occupiedSlots.containsKey(slot) && occupiedSlots.get(slot).token == token) {
            occupiedSlots.remove(slot);
        }
    }

    public synchronized boolean hasTwoClients() {
        return occupiedSlots.size() == PlayerSlot.values().length;
    }

    public synchronized int connectedCount() {
        return occupiedSlots.size();
    }

    public synchronized void broadcast(PlayerSlot sender, String payload) {
        for (Map.Entry<PlayerSlot, Session> entry : occupiedSlots.entrySet()) {
            if (entry.getKey() == sender) {
                continue;
            }

            try {
                entry.getValue().channel.send(payload);
            } catch (Exception ignored) {
            }
        }
    }

    private static final class Session {
        private final Object token;
        private final MessageChannel channel;

        private Session(Object token, MessageChannel channel) {
            this.token = token;
            this.channel = channel;
        }
    }
}
