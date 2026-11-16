package pitiupi.batalha_naval.control;

public class Ship {
    private final int [][] cells;
    private int hits = 0;

    public Ship(int[][] cells) {
        this.cells = cells;
    }

    public int getSize() {
        return cells.length;
    }

    public int[][] getCells() {
        return cells;
    }

    public void registerHit() {
        hits++;
    }

    public boolean isSunk() {
        return hits >= cells.length;
    }
}
