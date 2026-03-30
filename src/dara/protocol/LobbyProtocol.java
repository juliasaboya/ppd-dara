package dara.protocol;

public final class LobbyProtocol {
    public static final String FIELD_SLOT = "slot";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_ACTION = "action";
    public static final String FIELD_ROW = "row";
    public static final String FIELD_COLUMN = "column";
    public static final String FIELD_FROM_ROW = "fromRow";
    public static final String FIELD_FROM_COLUMN = "fromColumn";
    public static final String FIELD_TO_ROW = "toRow";
    public static final String FIELD_TO_COLUMN = "toColumn";
    public static final String REASON_SLOT_UNAVAILABLE = "slot_unavailable";

    private LobbyProtocol() {
    }

    public static ProtocolMessage join(String slot) {
        return ProtocolMessage.of(MessageType.JOIN, FIELD_SLOT, slot);
    }

    public static ProtocolMessage accept(String slot) {
        return ProtocolMessage.of(MessageType.ACCEPT, FIELD_SLOT, slot);
    }

    public static ProtocolMessage reject(String reason) {
        return ProtocolMessage.of(MessageType.REJECT, FIELD_REASON, reason);
    }

    public static ProtocolMessage chat(String slot, String text) {
        return new ProtocolMessage(MessageType.CHAT, java.util.Map.of(
                FIELD_SLOT, slot,
                FIELD_TEXT, text
        ));
    }

    public static ProtocolMessage gameAction(GameAction action) {
        java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put(FIELD_SLOT, action.slot().name());
        fields.put(FIELD_ACTION, action.type().name());
        fields.put(FIELD_ROW, String.valueOf(action.row()));
        fields.put(FIELD_COLUMN, String.valueOf(action.column()));
        fields.put(FIELD_FROM_ROW, String.valueOf(action.fromRow()));
        fields.put(FIELD_FROM_COLUMN, String.valueOf(action.fromColumn()));
        fields.put(FIELD_TO_ROW, String.valueOf(action.toRow()));
        fields.put(FIELD_TO_COLUMN, String.valueOf(action.toColumn()));
        return new ProtocolMessage(MessageType.GAME_ACTION, fields);
    }

    public static GameAction parseGameAction(ProtocolMessage message) {
        return new GameAction(
                GameActionType.valueOf(message.getField(FIELD_ACTION)),
                dara.network.PlayerSlot.fromName(message.getField(FIELD_SLOT)),
                parseInt(message.getField(FIELD_ROW)),
                parseInt(message.getField(FIELD_COLUMN)),
                parseInt(message.getField(FIELD_FROM_ROW)),
                parseInt(message.getField(FIELD_FROM_COLUMN)),
                parseInt(message.getField(FIELD_TO_ROW)),
                parseInt(message.getField(FIELD_TO_COLUMN))
        );
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        return Integer.parseInt(value);
    }
}
