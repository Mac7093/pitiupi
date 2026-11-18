package pitiupi.batalha_naval.control;

import java.net.InetAddress;
import java.util.UUID;

import pitiupi.batalha_naval.net.InviteMessage;
import pitiupi.batalha_naval.net.LeaveMessage;
import pitiupi.batalha_naval.net.MoveMessage;
import pitiupi.batalha_naval.net.MoveResponseMessage;
import pitiupi.batalha_naval.net.MoveResult;
import pitiupi.batalha_naval.net.ResponseMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.presentation.shell.PeerIpv4;

public class BatalhaNavalController {
    private enum Phase {IDLE, INVITE_SENT, INVITE_RECEIVED, PLAYING, FINISHED}

    private final ConnectionManager connectionManager;
    private GameUiSink uiSink;

    private Phase phase = Phase.IDLE;
    private String gameId;
    private InetAddress opponentAddress;
    private String opponentName;
    private Board myBoard;
    private EnemyView enemyView;
    private boolean myTurn;
    private boolean waitingResponse;

    public BatalhaNavalController(ConnectionManager connectionManager){
        this.connectionManager = connectionManager;
    }

    public void setUiSink(GameUiSink uiSink) {
        this.uiSink = uiSink;
    }

    public void invite(String ip, String name) {
        if (phase != Phase.IDLE) return;

        InetAddress addr = resolveAddress(ip);
        if (addr == null) {
            uiSink.onInviteRejected("Informe um IP válido.");
            return;
        }

        gameId = UUID.randomUUID().toString();
        opponentAddress = addr;
        opponentName = (name == null || name.isBlank()) ? addr.getHostAddress() : name;
        phase = Phase.INVITE_SENT;

        send(new InviteMessage(gameId, myName(), myIp()));
        uiSink.onStatusChanged("Convite enviado para " + opponentName, false);
    }

    public void answerInvite(boolean accept) {
        if (phase != Phase.INVITE_RECEIVED) {
            return;
        }
        if (accept){
            send(new ResponseMessage(gameId, myName(), myIp(), accept, null));
            startGame(false);
        }
        else{
            send(new ResponseMessage(gameId, myName(), myIp(), accept, "Convite Recusado."));
            resetState();
        }
    }

    public void shoot(int row, int col) {
        if (phase != Phase.PLAYING || !myTurn || waitingResponse) {
            return;
        }
        if (!Board.isInside(row, col) || enemyView.alreadyShot(row, col)) {
            return;
        }
        waitingResponse = true;
        send(new MoveMessage(gameId, myName(), myIp(), row, col));
        uiSink.onStatusChanged("Tiro enviado.", false);
    }

    public void giveUp() {
        if (phase == Phase.IDLE) {
            return;
        }
        boolean wasFinished = phase == Phase.FINISHED;
        if (!wasFinished) {
            send(new LeaveMessage(gameId, myName(), myIp()));
        }
        resetState();
        if (!wasFinished) {
            uiSink.onGameAborted("Você saiu da partida.");
        }
    }

    public void returnToLobby() {
        if (phase == Phase.FINISHED) {
            resetState();
        }
    }

    public void onInvite(InviteMessage msg) {
        InetAddress senderAddr = resolveAddress(msg.getSenderIp());
        if (msg.getGameId() == null || senderAddr == null) return;

        if (phase != Phase.IDLE) {
            Message busy = new ResponseMessage(msg.getGameId(), myName(), myIp(), false, "Ocupado em outra partida.");
            MessageDispatcher.send(connectionManager, busy, SendTarget.singlePeer(senderAddr), TransportMode.TCP);
            return;
        }

        gameId = msg.getGameId();
        opponentAddress = senderAddr;
        String senderName = msg.getSenderName();
        opponentName = (senderName == null || senderName.isBlank()) ? senderAddr.getHostAddress() : senderName;
        phase = Phase.INVITE_RECEIVED;

        uiSink.onInviteReceived(opponentName, senderAddr.getHostAddress());
    }

    public void onResponse(ResponseMessage msg) {
        if (phase != Phase.INVITE_SENT || !isCurrentGame(msg.getGameId())) {
            return;
        }
        if (msg.isAccepted()) {
            startGame(true);
        } else {
            String reason = (msg.getReason() == null || msg.getReason().isBlank())
                    ? "Convite recusado." : msg.getReason();
            resetState();
            uiSink.onInviteRejected(reason);
        }
    }

    public void onMove(MoveMessage msg) {
        if (phase != Phase.PLAYING || myTurn || !isCurrentGame(msg.getGameId())) {
            return;
        }

        int row = msg.getRow();
        int col = msg.getColumn();

        if (!Board.isInside(row, col) || myBoard.alreadyShot(row, col)) return;

        MoveResult result = myBoard.receiveShot(row, col);
        int[][] sunkCells = result == MoveResult.SUNK ? myBoard.getShipCells(row, col) : new int[0][];
        boolean gameOver = myBoard.allSunk();

        if (result == MoveResult.WATER) {
            myTurn = true;
        }

        send(new MoveResponseMessage(gameId, myName(), myIp(), row, col, result, sunkCells, gameOver));
        uiSink.onBoardsChanged();

        if (gameOver) {
            phase = Phase.FINISHED;
            uiSink.onGameEnded(false, "Todos os seus navios foram afundados. Você perdeu.");
        } else if (result == MoveResult.WATER) {
            uiSink.onStatusChanged(opponentName + " errou. Sua vez!", true);
        } else {
            String acao = (result == MoveResult.SUNK) ? "afundou um navio seu" : "acertou um navio seu";
            uiSink.onStatusChanged(opponentName + " " + acao + " e joga de novo.", false);
        }
    }

    public void onMoveResponse(MoveResponseMessage msg) {
        if (phase != Phase.PLAYING || !waitingResponse || !isCurrentGame(msg.getGameId())) {
            return;
        }
        if (msg.getResult() == null) return;

        waitingResponse = false;
        enemyView.register(msg.getRow(), msg.getColumn(), msg.getResult(), msg.getSunkCells());
        uiSink.onBoardsChanged();

        if (msg.isGameOver()) {
            phase = Phase.FINISHED;
            uiSink.onGameEnded(true, "Você afundou todos os navios! Vitória!");
            return;
        } else if (msg.getResult() == MoveResult.WATER) {
            myTurn = false;
            uiSink.onStatusChanged("Água! Vez de " + opponentName + ".", false);
        } else {
            String texto = (msg.getResult() == MoveResult.SUNK) ? "Você afundou um navio!" : "Acertou!";
            uiSink.onStatusChanged(texto + " Jogue novamente.", true);
        }
    }

    public void onLeave(LeaveMessage msg) {
        if (!isCurrentGame(msg.getGameId())) {
            return;
        }
        
        String who = opponentName;
        Phase previousPhase = phase;
        resetState();

        if (previousPhase == Phase.INVITE_RECEIVED) {
            uiSink.onGameAborted("O convite foi cancelado.");
        } else if (previousPhase == Phase.PLAYING || previousPhase == Phase.INVITE_SENT) {
            uiSink.onGameAborted(who + " saiu da partida.");
        }
    }

    
    private void startGame(boolean firstToPlay) {
        phase = Phase.PLAYING;
        myBoard = new Board();
        enemyView = new EnemyView();
        myTurn = firstToPlay;
        waitingResponse = false;

        uiSink.onGameStarted(myBoard, enemyView, myTurn);
        uiSink.onStatusChanged(myTurn
                ? "Sua vez! Clique no tabuleiro inimigo para atirar."
                : "Vez de " + opponentName + ". Aguarde...", myTurn);
    }

    private boolean isCurrentGame(String id) {
        return id != null && id.equals(gameId);
    }

    private void resetState() {
        phase = Phase.IDLE;
        gameId = null;
        opponentAddress = null;
        opponentName = null;
        myBoard = null;
        enemyView = null;
        myTurn = false;
        waitingResponse = false;
    }

    private InetAddress resolveAddress(String ip) {
        try {
            String normalized = PeerIpv4.normalize(ip);
            if (normalized == null || normalized.isBlank()) return null;
            return InetAddress.getByName(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private String myName() {
        return connectionManager.getUserName();
    }

    private String myIp() {
        return PeerIpv4.normalize(LocalNetworkInfo.bestLocalIpv4());
    }

    private void send(Message message) {
        if (opponentAddress != null) {
            MessageDispatcher.send(connectionManager, message, SendTarget.singlePeer(opponentAddress), TransportMode.TCP);
        }
    }
}
