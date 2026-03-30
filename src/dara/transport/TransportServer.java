package dara.transport;

import java.io.Closeable;
import java.io.IOException;

public interface TransportServer extends Closeable {
    void start() throws IOException;

    int getPort();

    @Override
    void close() throws IOException;
}
