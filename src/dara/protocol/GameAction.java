package dara.protocol;

import dara.network.PlayerSlot;

public record GameAction(
        GameActionType type,
        PlayerSlot slot,
        int row,
        int column,
        int fromRow,
        int fromColumn,
        int toRow,
        int toColumn
) {
}
