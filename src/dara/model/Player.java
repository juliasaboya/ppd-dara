package dara.model;

public enum Player {
    COLOR_ONE('1'),
    COLOR_TWO('2');

    private final char symbol;

    Player(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public Player opponent() {
        return this == COLOR_ONE ? COLOR_TWO : COLOR_ONE;
    }
}
