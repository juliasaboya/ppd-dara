package dara.transport;

import java.io.Closeable;
import java.io.IOException;

public interface MessageChannel extends Closeable {
    void send(String payload) throws IOException;

    String receive() throws IOException;

    @Override
    void close() throws IOException;
}
