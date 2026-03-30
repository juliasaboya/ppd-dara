package dara.transport;

import java.io.IOException;

public interface MessageChannelHandler {
    void handle(MessageChannel channel) throws IOException;
}
