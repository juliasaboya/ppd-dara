package dara.model;

import java.util.Arrays;

public class Board {
    public static final int ROWS = 5;
    public static final int COLUMNS = 6;

    private final Player[][] grid;

    public Board() {
        this.grid = new Player[ROWS][COLUMNS];
    }

    public int getRows() {
        return ROWS;
    }

    public int getColumns() {
        return COLUMNS;
    }

    public boolean isValidPosition(int row, int column) {
        return row >= 0 && row < ROWS && column >= 0 && column < COLUMNS;
    }

    public boolean isEmpty(int row, int column) {
        validatePosition(row, column);
        return grid[row][column] == null;
    }

    public Player getPiece(int row, int column) {
        validatePosition(row, column);
        return grid[row][column];
    }

    public void placePiece(int row, int column, Player player) {
        validatePosition(row, column);
        validatePlayer(player);
        if (!isEmpty(row, column)) {
            throw new IllegalStateException("Posicao ocupada no tabuleiro.");
        }
        grid[row][column] = player;
    }

    public void movePiece(int fromRow, int fromColumn, int toRow, int toColumn) {
        validatePosition(fromRow, fromColumn);
        validatePosition(toRow, toColumn);
        if (isEmpty(fromRow, fromColumn)) {
            throw new IllegalStateException("Nao existe peca na origem.");
        }
        if (!isEmpty(toRow, toColumn)) {
            throw new IllegalStateException("Destino ocupado.");
        }
        if (!isOrthogonallyAdjacent(fromRow, fromColumn, toRow, toColumn)) {
            throw new IllegalArgumentException("Movimento invalido para o tabuleiro Dara.");
        }

        grid[toRow][toColumn] = grid[fromRow][fromColumn];
        grid[fromRow][fromColumn] = null;
    }

    public Player removePiece(int row, int column) {
        validatePosition(row, column);
        Player removed = grid[row][column];
        grid[row][column] = null;
        return removed;
    }

    public boolean isOrthogonallyAdjacent(int fromRow, int fromColumn, int toRow, int toColumn) {
        validatePosition(fromRow, fromColumn);
        validatePosition(toRow, toColumn);
        int distance = Math.abs(fromRow - toRow) + Math.abs(fromColumn - toColumn);
        return distance == 1;
    }

    public boolean createsLine(int row, int column) {
        validatePosition(row, column);
        Player player = grid[row][column];
        if (player == null) {
            return false;
        }
        return countInDirection(row, column, 0, -1, player) + countInDirection(row, column, 0, 1, player) - 1 >= 3
                || countInDirection(row, column, -1, 0, player) + countInDirection(row, column, 1, 0, player) - 1 >= 3;
    }

    public int countPieces(Player player) {
        validatePlayer(player);
        int count = 0;
        for (Player[] line : grid) {
            for (Player current : line) {
                if (current == player) {
                    count++;
                }
            }
        }
        return count;
    }

    public Player[][] snapshot() {
        Player[][] copy = new Player[ROWS][COLUMNS];
        for (int row = 0; row < ROWS; row++) {
            copy[row] = Arrays.copyOf(grid[row], COLUMNS);
        }
        return copy;
    }

    public void clear() {
        for (Player[] line : grid) {
            Arrays.fill(line, null);
        }
    }

    private int countInDirection(int row, int column, int rowStep, int columnStep, Player player) {
        int count = 0;
        int currentRow = row;
        int currentColumn = column;

        while (isValidPosition(currentRow, currentColumn) && grid[currentRow][currentColumn] == player) {
            count++;
            currentRow += rowStep;
            currentColumn += columnStep;
        }

        return count;
    }

    private void validatePosition(int row, int column) {
        if (!isValidPosition(row, column)) {
            throw new IndexOutOfBoundsException("Posicao fora do tabuleiro: (" + row + ", " + column + ").");
        }
    }

    private void validatePlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Jogador nao pode ser nulo.");
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                Player player = grid[row][column];
                builder.append(player == null ? '.' : player.getSymbol());
                if (column < COLUMNS - 1) {
                    builder.append(' ');
                }
            }
            if (row < ROWS - 1) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }
}
