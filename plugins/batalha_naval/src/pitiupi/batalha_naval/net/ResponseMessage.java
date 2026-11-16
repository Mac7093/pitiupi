package pitiupi.batalha_naval.net;

import pitiupi.models.Messages.Message;

public class ResponseMessage extends Message{
    private String gameId;
    private String senderName;
    private String senderIp;
    private boolean accepted;
    private String reason;

    public ResponseMessage(){

    }

    public ResponseMessage(String gameId, String senderName, String senderIp, boolean accepted, String reason){
        this.gameId = gameId;
        this.senderName = senderName;
        this.senderIp = senderIp;
        this.accepted = accepted;
        this.reason = reason;
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

    public boolean isAccepted(){
        return accepted;
    }

    public String getReason(){
        return reason;
    }
}
