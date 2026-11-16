package pitiupi.batalha_naval.control;

import java.util.Arrays;

import pitiupi.batalha_naval.net.MoveResult;

public class EnemyView {
    private CellState[][] cells = new CellState[Board.DIMENSION][Board.DIMENSION];
    
    public EnemyView(){
        for (CellState[] row : cells){
            Arrays.fill(row, CellState.UNKNOWN);
        }
    }

    public void register(int row, int col, MoveResult result, int[][] sunkCells){
        if (!Board.isInside(row, col)) {
            return;
        }
        switch(result){
            case WATER -> cells[row][col] = CellState.MISS;
            case HIT -> cells[row][col] = CellState.HIT;
            case SUNK -> {
                cells[row][col] = CellState.SUNK;
                if (sunkCells != null){
                    for (int[] c : sunkCells){
                        if (c.length == 2 && Board.isInside(c[0], c[1])) {
                            cells[c[0]][c[1]] = CellState.SUNK;
                        }
                    }
                }
            }
        }
    }

    public boolean alreadyShot(int row, int col) {
        return cells[row][col] != CellState.UNKNOWN;
    }

    public CellState getState(int row, int col) {
        return cells[row][col];
    }
}