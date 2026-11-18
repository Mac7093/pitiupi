package pitiupi.batalha_naval.GUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pitiupi.application.presence.UserPeerView;
import pitiupi.batalha_naval.control.Board;
import pitiupi.batalha_naval.control.CellState;
import pitiupi.batalha_naval.control.EnemyView;
import pitiupi.batalha_naval.control.BatalhaNavalController;
import pitiupi.batalha_naval.control.GameUiSink;
import pitiupi.presentation.shell.PeerIpv4;

public class BatalhaNavalPanel extends JPanel implements GameUiSink {
    private static final String CARD_LOBBY = "lobby";
    private static final String CARD_GAME = "game";
    private static final Color RED = new Color(170, 0, 0);

    private final BatalhaNavalController controller;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);

    private final JLabel lblOpponent = new JLabel("Nenhum oponente escolhido.", SwingConstants.CENTER);
    private final JLabel lblLobbyMessage = new JLabel(" ", SwingConstants.CENTER);
    private final JButton btnInvite = new JButton("Enviar convite");
    private final JButton btnCancelInvite = new JButton("Cancelar convite");
    private final JPanel inviteBox = new JPanel();
    private final JLabel lblInvite = new JLabel(" ");
    private final JButton btnAccept = new JButton("Aceitar");
    private final JButton btnDecline = new JButton("Recusar");

    private final JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel gridsPanel = new JPanel();
    private final JButton btnLeave = new JButton("Desistir");
    private GradePanel enemyGrid;
    private GradePanel myGrid;

    private String selectedIp;
    private String selectedName;
    private boolean waitingAnswer;   
    private boolean inGame;        
    private boolean gameFinished;

    public BatalhaNavalPanel(BatalhaNavalController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        cardPanel.add(buildLobby(), CARD_LOBBY);
        cardPanel.add(buildGame(), CARD_GAME);
        add(cardPanel, BorderLayout.CENTER);
        updateLobbyButtons();
        cards.show(cardPanel, CARD_LOBBY);
    }

    private JPanel buildLobby() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Batalha Naval");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Convide um jogador para jogar batalha naval");
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblOpponent.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblOpponent.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnInvite.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancelInvite.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancelInvite.setVisible(false);

        lblLobbyMessage.setAlignmentX(Component.CENTER_ALIGNMENT);

        inviteBox.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        inviteBox.setBorder(BorderFactory.createTitledBorder("Convite recebido"));
        inviteBox.add(lblInvite);
        inviteBox.add(btnAccept);
        inviteBox.add(btnDecline);
        inviteBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        inviteBox.setVisible(false);

        btnInvite.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendInvite();
            }
        });

        btnCancelInvite.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.giveUp();
            }
        });

        btnAccept.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inviteBox.setVisible(false);
                controller.answerInvite(true);
            }
        });

        btnDecline.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inviteBox.setVisible(false);
                lblLobbyMessage.setText(" ");
                controller.answerInvite(false);
                updateLobbyButtons();
            }
        });

        box.add(title);
        box.add(Box.createVerticalStrut(10));
        box.add(hint);
        box.add(Box.createVerticalStrut(20));
        box.add(lblOpponent);
        box.add(Box.createVerticalStrut(10));
        box.add(btnInvite);
        box.add(btnCancelInvite);
        box.add(Box.createVerticalStrut(10));
        box.add(lblLobbyMessage);
        box.add(Box.createVerticalStrut(15));
        box.add(inviteBox);

        JPanel lobby = new JPanel(new GridBagLayout());
        lobby.add(box);
        return lobby;
    }

    private JPanel buildGame() {
        JPanel game = new JPanel(new BorderLayout(5, 5));
        game.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 16));
        game.add(lblStatus, BorderLayout.NORTH);

        gridsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
        game.add(gridsPanel, BorderLayout.CENTER);

        btnLeave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameFinished) {
                    controller.returnToLobby();
                    leaveGameScreen();
                } else {
                    controller.giveUp();
                }
            }
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(btnLeave);
        game.add(bottom, BorderLayout.SOUTH);

        return game;
    }

    private void buildGrids(final Board myBoard, final EnemyView enemyView) {
        gridsPanel.removeAll();

        enemyGrid = new GradePanel(new GradePanel.CellProvider() {
            @Override
            public CellState stateAt(int row, int col) {
                return enemyView.getState(row, col);
            }
        }, 36, new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer row, Integer col) {
                controller.shoot(row, col);
            }
        });

        myGrid = new GradePanel(new GradePanel.CellProvider() {
            @Override
            public CellState stateAt(int row, int col) {
                return myBoard.getState(row, col);
            }
        }, 22, null);

        gridsPanel.add(withTitle("Tabuleiro inimigo (clique para atirar)", enemyGrid));
        gridsPanel.add(withTitle("Sua frota", myGrid));
        gridsPanel.revalidate();
        gridsPanel.repaint();
    }

    private JPanel withTitle(String title, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    public void prepareChallenge(UserPeerView peer) {
        if (peer == null || peer.isSelf() || peer.isBroadcastTarget()) {
            return;
        }
        if (!peer.getInetAddress().isPresent()) {
            return;
        }
        InetAddress address = peer.getInetAddress().get();
        selectedIp = PeerIpv4.fromInetAddress(address);
        selectedName = peer.getDisplayName();
        lblOpponent.setText("Oponente: " + selectedName + " (" + selectedIp + ")");
        updateLobbyButtons();
    }

    private void sendInvite() {
        if (selectedIp == null) {
            return;
        }
        waitingAnswer = true;
        updateLobbyButtons();
        controller.invite(selectedIp, selectedName);
    }

    private void updateLobbyButtons() {
        boolean receiving = inviteBox.isVisible();
        btnInvite.setEnabled(selectedIp != null && !waitingAnswer && !receiving);
        btnCancelInvite.setVisible(waitingAnswer);
    }

    private void showLobby() {
        cards.show(cardPanel, CARD_LOBBY);
    }

    private void leaveGameScreen() {
        inGame = false;
        gameFinished = false;
        waitingAnswer = false;
        inviteBox.setVisible(false);
        lblLobbyMessage.setText(" ");
        showLobby();
        updateLobbyButtons();
    }

    @Override
    public void onInviteReceived(String senderName, String senderIp) {
        lblInvite.setText(senderName + " (" + senderIp + ") convidou você para jogar.");
        inviteBox.setVisible(true);
        showLobby();
        updateLobbyButtons();
    }

    @Override
    public void onInviteRejected(String reason) {
        waitingAnswer = false;
        inviteBox.setVisible(false);
        lblLobbyMessage.setText(reason);
        showLobby();
        updateLobbyButtons();
    }

    @Override
    public void onGameStarted(Board myBoard, EnemyView enemyView, boolean myTurn) {
        inGame = true;
        gameFinished = false;
        waitingAnswer = false;
        inviteBox.setVisible(false);
        lblLobbyMessage.setText(" ");
        btnLeave.setText("Desistir");
        buildGrids(myBoard, enemyView);
        cards.show(cardPanel, CARD_GAME);
    }

    @Override
    public void onBoardsChanged() {
        if (enemyGrid != null) {
            enemyGrid.repaint();
        }
        if (myGrid != null) {
            myGrid.repaint();
        }
    }

    @Override
    public void onStatusChanged(String text, boolean myTurn) {
        if (inGame) {
            lblStatus.setText(text);
            lblStatus.setForeground(Color.DARK_GRAY);
        } else {
            lblLobbyMessage.setText(text);
        }
    }

    @Override
    public void onGameEnded(boolean won, String message) {
        gameFinished = true;
        lblStatus.setText(message);
        if (won) {
            lblStatus.setForeground(Color.DARK_GRAY);
        } else {
            lblStatus.setForeground(RED);
        }
        btnLeave.setText("Voltar ao lobby");
    }

    @Override
    public void onGameAborted(String reason) {
        leaveGameScreen();
        lblLobbyMessage.setText(reason);
    }
}