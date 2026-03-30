package dara.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProtocolMessage {
    private final MessageType type;
    private final Map<String, String> fields;

    public ProtocolMessage(MessageType type, Map<String, String> fields) {
        if (type == null) {
            throw new IllegalArgumentException("Tipo da mensagem nao pode ser nulo.");
        }
        this.type = type;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public MessageType getType() {
        return type;
    }

    public String getField(String key) {
        return fields.get(key);
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public static ProtocolMessage of(MessageType type) {
        return new ProtocolMessage(type, Map.of());
    }

    public static ProtocolMessage of(MessageType type, String key, String value) {
        return new ProtocolMessage(type, Map.of(key, value));
    }
}
