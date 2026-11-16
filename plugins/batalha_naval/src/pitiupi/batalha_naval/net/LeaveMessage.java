package pitiupi.batalha_naval.net;

import pitiupi.models.Messages.Message;

public class LeaveMessage extends Message{
    private String gameId;
    private String senderName;
    private String senderIp;

    public LeaveMessage(){

    }

    public LeaveMessage(String gameId, String senderName, String senderIp){
        this.gameId = gameId;
        this.senderName = senderName;
        this.senderIp = senderIp;
    }

    public String getGameId(){
        return gameId;
    }

    public void setGameId(String gameId){
        this.gameId = gameId;
    }

    public String getSenderName(){
        return senderName;
    }

    public void setSenderName(String senderName){
        this.senderName = senderName;
    }

    public String getSenderIp(){
        return senderIp;
    }

    public void setSenderIp(String senderIp){
        this.senderIp = senderIp;
    }
}
