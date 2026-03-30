package dara.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    public static final int INITIAL_RESERVE_PER_PLAYER = 9;

    private final Board board;
    private final Instant startedAt;
    private final String playerOneName;
    private final String playerTwoName;
    private int playerOneReserve;
    private int playerTwoReserve;
    private GameState state;
    private Player currentTurn;
    private boolean awaitingRemoval;
    private final Random random;

    public Game(String playerOneName, String playerTwoName) {
        this.board = new Board();
        this.startedAt = Instant.now();
        this.playerOneName = normalizeName(playerOneName, "Player 1");
        this.playerTwoName = normalizeName(playerTwoName, "Player 2");
        this.playerOneReserve = INITIAL_RESERVE_PER_PLAYER;
        this.playerTwoReserve = INITIAL_RESERVE_PER_PLAYER;
        this.state = GameState.PLACING;
        this.currentTurn = Player.COLOR_TWO;
        this.awaitingRemoval = false;
        this.random = new Random();
    }

    public Board getBoard() {
        return board;
    }

    public long getElapsedSeconds() {
        return Duration.between(startedAt, Instant.now()).getSeconds();
    }

    public String getFormattedElapsedTime() {
        long elapsedSeconds = getElapsedSeconds();
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getPlayerOneName() {
        return playerOneName;
    }

    public String getPlayerTwoName() {
        return playerTwoName;
    }

    public GameState getState() {
        return state;
    }

    public int getReserveCount(Player player) {
        return player == Player.COLOR_ONE ? playerOneReserve : playerTwoReserve;
    }

    public Player getCurrentTurn() {
        return currentTurn;
    }

    public boolean isAwaitingRemoval() {
        return awaitingRemoval;
    }

    public String getCurrentTurnName() {
        return getPlayerName(currentTurn);
    }

    public String getWaitingPlayerName() {
        return getPlayerName(currentTurn.opponent());
    }

    public String getPlayerName(Player player) {
        return player == Player.COLOR_TWO ? playerOneName : playerTwoName;
    }

    public boolean canPlacePiece(Player player, int row, int column) {
        if (state != GameState.PLACING) {
            return false;
        }
        if (player != currentTurn) {
            return false;
        }
        if (getReserveCount(player) <= 0) {
            return false;
        }
        return board.isValidPosition(row, column)
                && board.isEmpty(row, column)
                && !wouldCreateLine(player, row, column);
    }

    public void placePiece(Player player, int row, int column) {
        if (!canPlacePiece(player, row, column)) {
            throw new IllegalStateException("Nao foi possivel posicionar a peca.");
        }

        board.placePiece(row, column, player);
        if (player == Player.COLOR_ONE) {
            playerOneReserve--;
        } else {
            playerTwoReserve--;
        }

        if (playerOneReserve == 0 && playerTwoReserve == 0) {
            state = GameState.MOVIMENTATION;
            currentTurn = currentTurn.opponent();
            return;
        }

        currentTurn = currentTurn.opponent();
    }

    public boolean wouldCreateLine(Player player, int row, int column) {
        if (!board.isValidPosition(row, column) || !board.isEmpty(row, column)) {
            return false;
        }

        board.placePiece(row, column, player);
        boolean createsLine = board.createsLine(row, column);
        board.removePiece(row, column);
        return createsLine;
    }

    public boolean canSelectPiece(Player player, int row, int column) {
        return state == GameState.MOVIMENTATION
                && !awaitingRemoval
                && player == currentTurn
                && board.isValidPosition(row, column)
                && board.getPiece(row, column) == player;
    }

    public boolean canMovePiece(Player player, int fromRow, int fromColumn, int toRow, int toColumn) {
        return canSelectPiece(player, fromRow, fromColumn)
                && board.isValidPosition(toRow, toColumn)
                && board.isEmpty(toRow, toColumn)
                && board.isOrthogonallyAdjacent(fromRow, fromColumn, toRow, toColumn);
    }

    public void movePiece(Player player, int fromRow, int fromColumn, int toRow, int toColumn) {
        if (!canMovePiece(player, fromRow, fromColumn, toRow, toColumn)) {
            throw new IllegalStateException("Nao foi possivel mover a peca.");
        }

        board.movePiece(fromRow, fromColumn, toRow, toColumn);
        if (board.createsLine(toRow, toColumn)) {
            awaitingRemoval = true;
            return;
        }

        currentTurn = currentTurn.opponent();
    }

    public boolean canRemoveOpponentPiece(Player player, int row, int column) {
        return state == GameState.MOVIMENTATION
                && awaitingRemoval
                && player == currentTurn
                && board.isValidPosition(row, column)
                && board.getPiece(row, column) == player.opponent();
    }

    public void removeOpponentPiece(Player player, int row, int column) {
        if (!canRemoveOpponentPiece(player, row, column)) {
            throw new IllegalStateException("Nao foi possivel remover a peca adversaria.");
        }

        board.removePiece(row, column);
        awaitingRemoval = false;

        if (board.countPieces(player.opponent()) <= 2) {
            state = GameState.FINISHED;
            return;
        }

        currentTurn = currentTurn.opponent();
    }

    public void applyRemotePlace(Player player, int row, int column) {
        board.placePiece(row, column, player);
        if (player == Player.COLOR_ONE) {
            playerOneReserve--;
        } else {
            playerTwoReserve--;
        }

        if (playerOneReserve == 0 && playerTwoReserve == 0) {
            state = GameState.MOVIMENTATION;
            currentTurn = player.opponent();
            return;
        }

        currentTurn = player.opponent();
    }

    public void applyRemoteMove(Player player, int fromRow, int fromColumn, int toRow, int toColumn) {
        board.movePiece(fromRow, fromColumn, toRow, toColumn);
        if (board.createsLine(toRow, toColumn)) {
            awaitingRemoval = true;
            currentTurn = player;
            return;
        }
        currentTurn = player.opponent();
    }

    public void applyRemoteRemove(Player player, int row, int column) {
        board.removePiece(row, column);
        awaitingRemoval = false;
        if (board.countPieces(player.opponent()) <= 2) {
            state = GameState.FINISHED;
            currentTurn = player;
            return;
        }
        currentTurn = player.opponent();
    }

    public void applyRemoteSurrender(Player player) {
        state = GameState.FINISHED;
        currentTurn = player.opponent();
        awaitingRemoval = false;
    }

    public void randomizePlacingPhase() {
        int attempts = 0;
        while (state != GameState.MOVIMENTATION && attempts < 200) {
            resetForNewMatchFlow();

            boolean completed = true;
            while (state == GameState.PLACING) {
                List<int[]> validMoves = collectValidPlacingMoves(currentTurn);
                if (validMoves.isEmpty()) {
                    completed = false;
                    break;
                }

                int[] move = validMoves.get(random.nextInt(validMoves.size()));
                placePiece(currentTurn, move[0], move[1]);
            }

            if (completed && state == GameState.MOVIMENTATION) {
                return;
            }

            attempts++;
        }

        throw new IllegalStateException("Nao foi possivel gerar um estado aleatorio valido para a fase de movimentacao.");
    }

    private String normalizeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private void resetForNewMatchFlow() {
        board.clear();
        playerOneReserve = INITIAL_RESERVE_PER_PLAYER;
        playerTwoReserve = INITIAL_RESERVE_PER_PLAYER;
        state = GameState.PLACING;
        currentTurn = Player.COLOR_TWO;
        awaitingRemoval = false;
    }

    private List<int[]> collectValidPlacingMoves(Player player) {
        List<int[]> validMoves = new ArrayList<>();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                if (canPlacePiece(player, row, column)) {
                    validMoves.add(new int[]{row, column});
                }
            }
        }
        return validMoves;
    }
}
