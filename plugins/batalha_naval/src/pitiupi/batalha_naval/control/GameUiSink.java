package pitiupi.batalha_naval.control;

public interface GameUiSink {

    void onInviteReceived(String senderName, String senderIp);

    void onInviteRejected(String reason);

    void onGameStarted(Board myBoard, EnemyView enemyView, boolean myTurn);

    void onBoardsChanged();

    void onStatusChanged(String text, boolean myTurn);

    void onGameEnded(boolean won, String message);

    void onGameAborted(String reason);
}
