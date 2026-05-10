package dara.comunication.rmi;

import dara.comunication.network.PlayerSlot;

import java.io.Serial;
import java.io.Serializable;

public record JoinResponse(String sessionId, PlayerSlot assignedSlot) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
