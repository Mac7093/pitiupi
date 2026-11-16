package pitiupi.batalha_naval.control;

import java.util.List;
import java.util.Objects;
import javax.swing.Timer;
import java.util.UUID;

import javax.swing.SwingUtilities;

import pitiupi.batalha_naval.net.InviteMessage;
import pitiupi.batalha_naval.net.MoveMessage;
import pitiupi.batalha_naval.net.MoveResponseMessage;
import pitiupi.batalha_naval.net.MoveResult;
import pitiupi.batalha_naval.net.ResponseMessage;
import pitiupi.batalha_naval.net.LeaveMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.User;
import pitiupi.net.tcp.TcpAttemptListener;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.presentation.shell.PeerIpv4;

public class BatalhaNavalController {
    private enum Phase {IDLE, INVITE_SENT, INVITE_RECEIVED, PLAYING, FINISHED}

    private static final int INVITE_TIMEOUT_MS = 30_000;

    private final ConnectionManager connectionManager;
    private GameUiSink uiSink;

    private Phase phase = Phase.IDLE;
    private String gameId;
    private String opponentIp;
    private String opponentName;
    private Board myBoard;
    private EnemyView enemyView;
    private boolean myTurn;
    private boolean waitingResponse;
    private int lastRow;
    private int lastCol;
    private Timer inviteTimer;

    public BatalhaNavalController(ConnectionManager connectionManager){
        this.connectionManager = connectionManager;
        connectionManager.addPeerListener(peers ->
            SwingUtilities.invokeLater(() -> onPeersChanged(peers)));
    }

    public void setUiSink(GameUiSink uiSink) {
        this.uiSink = uiSink;
    }

    public void invite(String ip, String name) {
        if (phase != Phase.IDLE) {
            return;
        }
        String normalized = PeerIpv4.normalize(ip);
        if (normalized == null || normalized.isBlank()) {
            uiSink.onInviteRejected("Informe um IP válido.");
            return;
        }
        gameId = UUID.randomUUID().toString();
        opponentIp = normalized;
        opponentName = (name == null || name.isBlank()) ? normalized : name;
        phase = Phase.INVITE_SENT;

        sendToOpponent(new InviteMessage(gameId, myName(), myIp()));
        startInviteTimer();
        uiSink.onStatusChanged("Convite enviado para " + opponentName + ". Aguardando resposta...", false);
    }

    public void answerInvite(boolean accept) {
        if (phase != Phase.INVITE_RECEIVED) {
            return;
        }
        if (!accept) {
            sendToOpponent(new ResponseMessage(gameId, myName(), myIp(), false, "Convite recusado."));
            resetState();
            return;
        }
        sendToOpponent(new ResponseMessage(gameId, myName(), myIp(), true, null));
        startGame(false);
    }

    public void shoot(int row, int col) {
        if (phase != Phase.PLAYING || !myTurn || waitingResponse) {
            return;
        }
        if (!Board.isInside(row, col) || enemyView.alreadyShot(row, col)) {
            return;
        }
        waitingResponse = true;
        lastRow = row;
        lastCol = col;
        sendToOpponent(new MoveMessage(gameId, myName(), myIp(), row, col));
        uiSink.onStatusChanged("Tiro enviado. Aguardando resultado...", false);
    }

    public void giveUp() {
        if (phase == Phase.IDLE) {
            return;
        }
        boolean wasFinished = phase == Phase.FINISHED;
        if (!wasFinished) {
            sendToOpponent(new LeaveMessage(gameId, myName(), myIp()));
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
        String senderIp = msg.getSenderIp() == null ? null : PeerIpv4.normalize(msg.getSenderIp());
        if (msg.getGameId() == null || senderIp == null || senderIp.isBlank()) {
            return;
        }
        if (phase != Phase.IDLE) {
            send(new ResponseMessage(msg.getGameId(), myName(), myIp(), false,
                    "Ocupado em outra partida."), senderIp, null);
            return;
        }
        gameId = msg.getGameId();
        opponentIp = senderIp;
        opponentName = (msg.getSenderName() == null || msg.getSenderName().isBlank())
                ? senderIp : msg.getSenderName();
        phase = Phase.INVITE_RECEIVED;
        uiSink.onInviteReceived(opponentName, opponentIp);
    }

    public void onResponse(ResponseMessage msg) {
        if (phase != Phase.INVITE_SENT || !isCurrentGame(msg.getGameId(), msg.getSenderIp())) {
            return;
        }
        cancelInviteTimer();
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
        if (phase != Phase.PLAYING || !isCurrentGame(msg.getGameId(), msg.getSenderIp())) {
            return;
        }
        if (myTurn) {
            return; // não é a vez do oponente
        }
        int row = msg.getRow();
        int col = msg.getColumn();
        if (!Board.isInside(row, col) || myBoard.alreadyShot(row, col)) {
            return;
        }

        MoveResult result = myBoard.receiveShot(row, col);
        int[][] sunkCells = result == MoveResult.SUNK ? myBoard.getShipCells(row, col) : new int[0][];
        boolean gameOver = myBoard.allSunk();
        if (result == MoveResult.WATER) {
            myTurn = true;
        }

        sendToOpponent(new MoveResponseMessage(gameId, myName(), myIp(),
                row, col, result, sunkCells, gameOver));
        uiSink.onBoardsChanged();

        if (gameOver) {
            phase = Phase.FINISHED;
            uiSink.onGameEnded(false, "Todos os seus navios foram afundados. Você perdeu.");
            return;
        }
        switch (result) {
            case WATER -> uiSink.onStatusChanged(opponentName + " errou. Sua vez!", true);
            case HIT -> uiSink.onStatusChanged(opponentName + " acertou um navio seu e joga de novo.", false);
            case SUNK -> uiSink.onStatusChanged(opponentName + " afundou um navio seu e joga de novo.", false);
        }
    }

    public void onMoveResponse(MoveResponseMessage msg) {
        if (phase != Phase.PLAYING || !isCurrentGame(msg.getGameId(), msg.getSenderIp())) {
            return;
        }
        if (!waitingResponse || msg.getResult() == null
                || msg.getRow() != lastRow || msg.getColumn() != lastCol) {
            return;
        }
        waitingResponse = false;
        enemyView.register(lastRow, lastCol, msg.getResult(), msg.getSunkCells());
        uiSink.onBoardsChanged();

        if (msg.isGameOver()) {
            phase = Phase.FINISHED;
            uiSink.onGameEnded(true, "Você afundou todos os navios! Vitória!");
            return;
        }
        switch (msg.getResult()) {
            case WATER -> {
                myTurn = false;
                uiSink.onStatusChanged("Água! Vez de " + opponentName + ".", false);
            }
            case HIT -> uiSink.onStatusChanged("Acertou! Jogue novamente.", true);
            case SUNK -> uiSink.onStatusChanged("Você afundou um navio! Jogue novamente.", true);
        }
    }

    public void onLeave(LeaveMessage msg) {
        if (!isCurrentGame(msg.getGameId(), msg.getSenderIp())) {
            return;
        }
        if (phase == Phase.INVITE_RECEIVED) {
            resetState();
            uiSink.onGameAborted("O convite foi cancelado.");
        } else if (phase == Phase.PLAYING || phase == Phase.INVITE_SENT) {
            String who = opponentName;
            resetState();
            uiSink.onGameAborted(who + " saiu da partida.");
        }
    }

    private void onPeersChanged(List<User> peers) {
        if (phase != Phase.PLAYING && phase != Phase.INVITE_SENT) {
            return;
        }
        if (peers != null) {
            for (User peer : peers) {
                if (peer == null || peer.isBroadcastTarget() || peer.getAddress() == null) {
                    continue;
                }
                if (opponentIp.equals(PeerIpv4.normalize(PeerIpv4.fromInetAddress(peer.getAddress())))) {
                    return; 
                }
            }
        }
        abort(opponentName + " saiu da rede.");
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

    private boolean isCurrentGame(String id, String senderIp) {
        return id != null && id.equals(gameId)
                && senderIp != null
                && opponentIp != null
                && opponentIp.equals(PeerIpv4.normalize(senderIp));
    }

    private boolean isActive() {
        return phase != Phase.IDLE && phase != Phase.FINISHED;
    }

    private void abort(String reason) {
        resetState();
        uiSink.onGameAborted(reason);
    }

    private void resetState() {
        cancelInviteTimer();
        phase = Phase.IDLE;
        gameId = null;
        opponentIp = null;
        opponentName = null;
        myBoard = null;
        enemyView = null;
        myTurn = false;
        waitingResponse = false;
    }

    private void startInviteTimer() {
        cancelInviteTimer();
        inviteTimer = new Timer(INVITE_TIMEOUT_MS, e -> {
            if (phase == Phase.INVITE_SENT) {
                String who = opponentName;
                sendToOpponent(new LeaveMessage(gameId, myName(), myIp()));
                resetState();
                uiSink.onInviteRejected("Sem resposta de " + who + ".");
            }
        });
        inviteTimer.setRepeats(false);
        inviteTimer.start();
    }

    private void cancelInviteTimer() {
        if (inviteTimer != null) {
            inviteTimer.stop();
            inviteTimer = null;
        }
    }

    private String myName() {
        return connectionManager.getUserName();
    }

    private String myIp() {
        return PeerIpv4.normalize(LocalNetworkInfo.bestLocalIpv4());
    }

    private void sendToOpponent(Message message) {
        final String id = gameId;
        send(message, opponentIp, () -> {
            if (isActive() && Objects.equals(id, gameId)) {
                abort("Falha de conexão com " + opponentName + ".");
            }
        });
    }

    private void send(Message message, String ip, Runnable failureAction) {
        connectionManager.sendTcpAsync(message, ip, new TcpAttemptListener() {
            @Override
            public void onConnecting(String endpoint) {
            }

            @Override
            public void onSuccess(TcpConnection connection) {
            }

            @Override
            public void onFailure(String error) {
                if (failureAction != null) {
                    SwingUtilities.invokeLater(failureAction);
                }
            }
        });
    }
}
