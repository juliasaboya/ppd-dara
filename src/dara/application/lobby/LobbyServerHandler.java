package dara.application.lobby;

import dara.network.PlayerSlot;
import dara.protocol.LobbyProtocol;
import dara.protocol.MessageType;
import dara.protocol.ProtocolCodec;
import dara.protocol.ProtocolMessage;
import dara.transport.MessageChannel;
import dara.transport.MessageChannelHandler;

import java.io.IOException;

public class LobbyServerHandler implements MessageChannelHandler {
    private final LobbyServerCoordinator coordinator;

    public LobbyServerHandler(LobbyServerCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void handle(MessageChannel channel) throws IOException {
        Object token = new Object();
        PlayerSlot assignedSlot = null;

        try {
            ProtocolMessage request = ProtocolCodec.decode(channel.receive());
            assignedSlot = request.getType() == MessageType.JOIN
                    ? PlayerSlot.fromName(request.getField(LobbyProtocol.FIELD_SLOT))
                    : null;

            if (assignedSlot == null || !coordinator.occupy(assignedSlot, token, channel)) {
                channel.send(ProtocolCodec.encode(LobbyProtocol.reject(LobbyProtocol.REASON_SLOT_UNAVAILABLE)));
                return;
            }

            channel.send(ProtocolCodec.encode(LobbyProtocol.accept(assignedSlot.name())));

            while (true) {
                ProtocolMessage message = ProtocolCodec.decode(channel.receive());
                if (message.getType() == MessageType.CHAT) {
                    coordinator.broadcast(
                            assignedSlot,
                            ProtocolCodec.encode(LobbyProtocol.chat(
                                    assignedSlot.name(),
                                    message.getField(LobbyProtocol.FIELD_TEXT)
                            ))
                    );
                } else if (message.getType() == MessageType.GAME_ACTION) {
                    coordinator.broadcast(assignedSlot, ProtocolCodec.encode(message));
                }
            }
        } finally {
            coordinator.release(assignedSlot, token);
        }
    }
}
