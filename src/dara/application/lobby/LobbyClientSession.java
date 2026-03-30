package dara.application.lobby;

import dara.network.PlayerSlot;
import dara.protocol.LobbyProtocol;
import dara.protocol.MessageType;
import dara.protocol.ProtocolCodec;
import dara.protocol.ProtocolMessage;
import dara.transport.MessageChannel;

import java.io.Closeable;
import java.io.IOException;

public class LobbyClientSession implements Closeable {
    public interface ChatListener {
        void onChatReceived(PlayerSlot senderSlot, String text);
    }

    public interface GameActionListener {
        void onGameActionReceived(dara.protocol.GameAction action);
    }

    private final MessageChannel channel;
    private final PlayerSlot desiredSlot;
    private ChatListener chatListener;
    private GameActionListener gameActionListener;
    private boolean connected;
    private Thread listenerThread;

    public LobbyClientSession(MessageChannel channel, PlayerSlot desiredSlot) {
        this.channel = channel;
        this.desiredSlot = desiredSlot;
    }

    public boolean connect() throws IOException {
        if (connected) {
            return true;
        }

        ProtocolMessage joinRequest = LobbyProtocol.join(desiredSlot.name());
        channel.send(ProtocolCodec.encode(joinRequest));

        ProtocolMessage response = ProtocolCodec.decode(channel.receive());
        if (response.getType() != MessageType.ACCEPT) {
            close();
            return false;
        }

        connected = true;
        startListenerLoop();
        return true;
    }

    public boolean isConnected() {
        return connected;
    }

    public PlayerSlot getDesiredSlot() {
        return desiredSlot;
    }

    public void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    public void setGameActionListener(GameActionListener gameActionListener) {
        this.gameActionListener = gameActionListener;
    }

    public void sendChat(String text) throws IOException {
        if (!connected || text == null || text.isBlank()) {
            return;
        }
        channel.send(ProtocolCodec.encode(LobbyProtocol.chat(desiredSlot.name(), text.trim())));
    }

    public void sendGameAction(dara.protocol.GameAction action) throws IOException {
        if (!connected || action == null) {
            return;
        }
        channel.send(ProtocolCodec.encode(LobbyProtocol.gameAction(action)));
    }

    @Override
    public void close() throws IOException {
        connected = false;
        channel.close();
    }

    private void startListenerLoop() {
        listenerThread = new Thread(() -> {
            while (connected) {
                try {
                    ProtocolMessage message = ProtocolCodec.decode(channel.receive());
                    if (message.getType() == MessageType.CHAT && chatListener != null) {
                        chatListener.onChatReceived(
                                PlayerSlot.fromName(message.getField(LobbyProtocol.FIELD_SLOT)),
                                message.getField(LobbyProtocol.FIELD_TEXT)
                        );
                    } else if (message.getType() == MessageType.GAME_ACTION && gameActionListener != null) {
                        gameActionListener.onGameActionReceived(LobbyProtocol.parseGameAction(message));
                    }
                } catch (IOException exception) {
                    connected = false;
                }
            }
        }, "dara-lobby-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}
