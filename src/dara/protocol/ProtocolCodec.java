package dara.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class ProtocolCodec {
    private static final String PART_SEPARATOR = "\\|";
    private static final String ASSIGNMENT = "=";

    private ProtocolCodec() {
    }

    public static String encode(ProtocolMessage message) {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(message.getType().name());
        for (Map.Entry<String, String> entry : message.getFields().entrySet()) {
            joiner.add(entry.getKey() + ASSIGNMENT + entry.getValue());
        }
        return joiner.toString();
    }

    public static ProtocolMessage decode(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("Mensagem de protocolo vazia.");
        }

        String[] parts = rawMessage.split(PART_SEPARATOR);
        MessageType type = MessageType.valueOf(parts[0]);
        Map<String, String> fields = new LinkedHashMap<>();

        for (int index = 1; index < parts.length; index++) {
            String part = parts[index];
            int assignmentIndex = part.indexOf(ASSIGNMENT);
            if (assignmentIndex <= 0 || assignmentIndex == part.length() - 1) {
                continue;
            }

            String key = part.substring(0, assignmentIndex);
            String value = part.substring(assignmentIndex + 1);
            fields.put(key, value);
        }

        return new ProtocolMessage(type, fields);
    }
}
