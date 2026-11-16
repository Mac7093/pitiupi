package pitiupi.batalha_naval.net;

import pitiupi.models.Messages.Message;

public class MoveResponseMessage extends Message {
    private String gameId;
    private String senderName;
    private String senderIp;
    private int row;
    private int column;
    private MoveResult result;
    private int[][] sunkCells;
    private boolean gameOver;

    public MoveResponseMessage() {

    }

    public MoveResponseMessage(
        String gameId,
        String senderName,
        String senderIp,
        int row,
        int column,
        MoveResult result,
        int[][] sunkCells,
        boolean gameOver
    ) {
        this.gameId = gameId;
        this.senderName = senderName;
        this.senderIp = senderIp;
        this.row = row;
        this.column = column;
        this.result = result;
        this.sunkCells = sunkCells;
        this.gameOver = gameOver;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public MoveResult getResult(){
        return result;
    }

    public int[][] getSunkCells(){
        return sunkCells;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}