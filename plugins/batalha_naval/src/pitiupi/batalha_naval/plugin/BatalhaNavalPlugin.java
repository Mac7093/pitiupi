package pitiupi.batalha_naval.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.batalha_naval.GUI.BatalhaNavalPanel;
import pitiupi.batalha_naval.control.BatalhaNavalController;
import pitiupi.batalha_naval.net.InviteMessage;
import pitiupi.batalha_naval.net.LeaveMessage;
import pitiupi.batalha_naval.net.MoveMessage;
import pitiupi.batalha_naval.net.MoveResponseMessage;
import pitiupi.batalha_naval.net.ResponseMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;

public class BatalhaNavalPlugin extends Plugin{

    private PluginWorkspace workspace;
    private BatalhaNavalController controller;
    private BatalhaNavalPanel panel;

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.workspace = workspace;
        this.controller = new BatalhaNavalController(connectionManager);
        this.panel = new BatalhaNavalPanel(controller);
        controller.setUiSink(panel);
        bindShellWorkspaceContent(panel);
        installPeerActions(workspace.peerActions());
    }

    @Override 
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
            "batalhanaval.desafiar",
            "Desafiar para Batalha Naval",
            PeerActionTarget.SINGLE_PEER,
            this::openChallenge
        ));
    }

    private void openChallenge(PeerTarget target) {
        if (!(target instanceof PeerTarget.Single)) {
            return;
        }
        PeerTarget.Single single = (PeerTarget.Single) target;
        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
        panel.prepareChallenge(single.peer());
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        List<Class<? extends Message>> messages = new ArrayList<Class<? extends Message>>();
        messages.add(InviteMessage.class);
        messages.add(ResponseMessage.class);
        messages.add(MoveMessage.class);
        messages.add(MoveResponseMessage.class);
        messages.add(LeaveMessage.class);
        return messages;
    }

    @Override
    public void receiveMessage(Message message) {
        if (controller == null) {
            return;
        }
        final Message received = message;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                dispatch(received);
            }
        });
    }

    private void dispatch(Message message) {
        if (message instanceof InviteMessage) {
            controller.onInvite((InviteMessage) message);
        } else if (message instanceof ResponseMessage) {
            controller.onResponse((ResponseMessage) message);
        } else if (message instanceof MoveMessage) {
            controller.onMove((MoveMessage) message);
        } else if (message instanceof MoveResponseMessage) {
            controller.onMoveResponse((MoveResponseMessage) message);
        } else if (message instanceof LeaveMessage) {
            controller.onLeave((LeaveMessage) message);
        }
    }

    @Override
    public void onShellTabClosed() {
        if (controller != null) {
            controller.giveUp();
        }
    }

    @Override
    public String getName() {
        return "Batalha Naval";
    }

    @Override
    public String getDisplayName() {
        return "Batalha Naval";
    }

    @Override
    public String getAuthor() {
        return "Miguel Agostini";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getIdentifier() {
        return "Pitiupi.BatalhaNaval";
    }
}
