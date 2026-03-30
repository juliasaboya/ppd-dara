package dara.model;

public enum GameState {
    WAITING("waiting"),
    PLACING("placing"),
    MOVIMENTATION("movimentation"),
    FINISHED("finished");

    private final String label;

    GameState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
