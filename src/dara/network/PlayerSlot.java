package dara.network;

public enum PlayerSlot {
    PLAYER_1("PLAYER 1"),
    PLAYER_2("PLAYER 2");

    private final String label;

    PlayerSlot(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
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
}
