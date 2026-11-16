package pitiupi.batalha_naval.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import pitiupi.batalha_naval.net.MoveResult;

public class Board {
    public static final int DIMENSION = 10;
    private CellState[][] cells = new CellState[DIMENSION][DIMENSION];
    private Ship[][] shipAt = new Ship[DIMENSION][DIMENSION];
    private List<Ship> ships = new ArrayList<>();
    private Random random = new Random();
    
    private static final int[][][] shapes = {
        {{0, 0}, {1, 0}},
        {{0, 0}, {1, 0}, {2, 0}},
        {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
        {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}},
        {{0, 0}, {1, 0}, {2, 0}, {1, 1}},
        {{0, 0}, {1, 0}, {1, 1}, {2, 0}, {2, 1}, {3, 1}}
    };

    public Board() {
        for (CellState[] row : cells){
            Arrays.fill(row, CellState.UNKNOWN);
        }
        for (int[][] shape : shapes)
        {
            placeShip(shape);
        }
    }

    public MoveResult receiveShot(int row, int col){
        if (!isInside(row, col) || alreadyShot(row, col)) {
            throw new IllegalArgumentException("Tiro inválido: " + row + "," + col);
        }
        Ship ship = shipAt[row][col];
        if (ship == null) {
            cells[row][col] = CellState.MISS;
            return MoveResult.WATER;
        }
        ship.registerHit();
        if (ship.isSunk()){
            for (int[] c: ship.getCells()) {
                cells[c[0]][c[1]] = CellState.SUNK;
            }
            return MoveResult.SUNK;
        }
        cells[row][col] = CellState.HIT;
        return MoveResult.HIT;
    }

    private void placeShip(int[][] shape){
        while(true){
            int[][] f = rotate(shape, random.nextInt(4));

            int maxL = 0;
            int maxC = 0;
            for (int[] p : f){
                maxL = Math.max(maxL, p[0]);
                maxC = Math.max(maxC, p[1]);
            }
            
            int l0 = random.nextInt(DIMENSION - maxL);
            int c0 = random.nextInt(DIMENSION - maxC);

            int[][] absolute = new int[f.length][2];
            boolean livre = true;
            for (int i = 0; i < f.length; i++){
                absolute[i][0] = l0 + f[i][0];
                absolute[i][1] = c0 + f[i][1];
                if (shipAt[absolute[i][0]][absolute[i][1]] != null){
                    livre = false;
                    break;
                }
            }
            if (!livre) continue;

            Ship ship = new Ship(absolute);
            for (int[] c : absolute){
                shipAt[c[0]][c[1]] = ship;
                cells[c[0]][c[1]] = CellState.SHIP;
            }
            ships.add(ship);
            return;
        }
    }

    private static int[][] rotate(int[][] shape, int times) {
        int[][] result = new int[shape.length][];
        for (int i = 0; i < shape.length; i++) {
            result[i] = shape[i].clone();
        }
        for (int k = 0; k < times; k++) {
            for (int[] p : result) {
                int row = p[0];
                p[0] = p[1];
                p[1] = -row;
            }
        }
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        for (int[] p : result) {
            minRow = Math.min(minRow, p[0]);
            minCol = Math.min(minCol, p[1]);
        }
        for (int[] p : result) {
            p[0] -= minRow;
            p[1] -= minCol;
        }
        return result;
    }

    public static boolean isInside(int row, int col) {
        return row >= 0 && row < DIMENSION && col >= 0 && col < DIMENSION;
    }

    public int[][] getShipCells(int row, int col) {
        Ship ship = shipAt[row][col];
        return ship != null ? ship.getCells() : new int[0][];
    }

    public boolean allSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public CellState getState(int row, int col) {
        return cells[row][col];
    }

    public boolean alreadyShot(int row, int col) {
        CellState s = cells[row][col];
        return s == CellState.MISS || s == CellState.HIT || s == CellState.SUNK;
    }
}
