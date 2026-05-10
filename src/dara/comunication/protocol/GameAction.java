package dara.comunication.protocol;

import dara.comunication.network.PlayerSlot;

import java.io.Serial;
import java.io.Serializable;

public record GameAction(
        GameActionType type,
        PlayerSlot slot,
        int row,
        int column,
        int fromRow,
        int fromColumn,
        int toRow,
        int toColumn
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
