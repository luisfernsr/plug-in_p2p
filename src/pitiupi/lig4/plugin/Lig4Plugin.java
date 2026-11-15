package pitiupi.lig4.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.control.ConnectionManager;
import pitiupi.lig4.game.Lig4Controller;
import pitiupi.lig4.net.Lig4Message;
import pitiupi.lig4.ui.Lig4Panel;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;

import javax.swing.SwingUtilities;
import java.util.List;

/** Plugin Lig 4 para o Pitiupi. */
public final class Lig4Plugin extends Plugin {
    private PluginWorkspace workspace;
    private Lig4Panel panel;
    private Lig4Controller controller;

    @Override public String getName() { return "Lig 4"; }
    @Override public String getDisplayName() { return "Lig 4"; }
    @Override public String getAuthor() { return "Luís Fernando"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getIdentifier() { return "Pitiupi.Lig4"; }
    @Override public String getDescription() { return "Jogo de tabuleiro para dois jogadores pela rede P2P."; }

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.workspace = workspace;
        this.panel = new Lig4Panel();
        this.controller = new Lig4Controller(connectionManager, workspace, panel);
        bindShellWorkspaceContent(panel);
        installPeerActions(workspace.peerActions());
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "lig4.invite",
                "Convidar para jogar Lig 4",
                PeerActionTarget.SINGLE_PEER,
                target -> {
                    if (target instanceof PeerTarget.Single single && controller != null) {
                        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
                        controller.inviteFromPeer(single.peer());
                    }
                }
        ));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(Lig4Message.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (!(message instanceof Lig4Message lig4Message) || controller == null) return;
        SwingUtilities.invokeLater(() -> controller.receive(lig4Message));
    }
}
