package dara.comunication.network;

import dara.model.Player;

public enum PlayerSlot {
    PLAYER_1("PLAYER 1", Player.COLOR_TWO),
    PLAYER_2("PLAYER 2", Player.COLOR_ONE);

    private final String label;
    private final Player controlledPlayer;

    PlayerSlot(String label, Player controlledPlayer) {
        this.label = label;
        this.controlledPlayer = controlledPlayer;
    }

    public String getLabel() {
        return label;
    }

    public Player getControlledPlayer() {
        return controlledPlayer;
    }

    public static PlayerSlot fromName(String rawSlot) {
        if (rawSlot == null || rawSlot.isBlank()) {
            return null;
        }
        for (PlayerSlot slot : values()) {
            if (slot.name().equals(rawSlot)) {
                return slot;
            }
        }
        return null;
    }

    public static PlayerSlot fromPlayer(Player player) {
        if (player == null) {
            return null;
        }
        for (PlayerSlot slot : values()) {
            if (slot.controlledPlayer == player) {
                return slot;
            }
        }
        return null;
    }
}
