package pitiupi.batalha_naval.net;

import pitiupi.models.Messages.Message;

public class MoveMessage extends Message {
    private String gameId;
    private String senderName;
    private String senderIp;
    private int row;
    private int column;

    public MoveMessage() {

    }

    public MoveMessage(
        String gameId,
        String senderName,
        String senderIp,
        int row,
        int column
    ) {
        this.gameId = gameId;
        this.senderName = senderName;
        this.senderIp = senderIp;
        this.row = row;
        this.column = column;
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
}
