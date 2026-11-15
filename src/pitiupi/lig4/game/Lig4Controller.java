package pitiupi.lig4.game;

import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.application.presence.UserPeerView;
import pitiupi.control.ConnectionManager;
import pitiupi.lig4.net.Lig4Message;
import pitiupi.lig4.ui.Lig4Panel;
import pitiupi.presentation.shell.LocalNetworkInfo;

import javax.swing.SwingUtilities;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

/** Coordena regras, estado da partida e troca de mensagens pelo transporte do Pitiupi. */
public final class Lig4Controller {
    private final ConnectionManager connectionManager;
    private final PluginWorkspace workspace;
    private final Lig4Panel panel;
    private Lig4Game game = new Lig4Game();
    private String gameId;
    private String opponentName;
    private InetAddress opponentAddress;
    private boolean host;
    private boolean active;
    private boolean waitingForAnswer;
    private int localPlayer = Lig4Game.RED;

    public Lig4Controller(ConnectionManager connectionManager, PluginWorkspace workspace, Lig4Panel panel) {
        this.connectionManager = connectionManager;
        this.workspace = workspace;
        this.panel = panel;
        panel.setActions(this::inviteSelectedPeer, this::refreshPeers, this::requestRestart, this::playColumn);
        refreshPeers();
        render();
    }

    public void refreshPeers() {
        panel.setPeers(connectionManager.getPresenceService().listPeerViews());
    }

    public void inviteSelectedPeer() {
        InetAddress address = panel.getSelectedPeerAddress();
        String name = panel.getSelectedPeerName();
        if (address == null) {
            panel.showError("Nenhum usuário conectado foi selecionado.");
            return;
        }
        invite(address, name);
    }

    public void invite(UserPeerView peer) {
        if (peer == null || peer.getInetAddress().isEmpty()) return;
        invite(peer.getInetAddress().get(), peer.getDisplayName());
    }

    private void invite(InetAddress address, String name) {
        if (active || waitingForAnswer) {
            panel.showInfo("Finalize a partida atual antes de convidar outra pessoa.");
            return;
        }
        if (address == null || address.isAnyLocalAddress()) {
            panel.showError("Endereço do jogador inválido.");
            return;
        }
        gameId = UUID.randomUUID().toString();
        opponentAddress = address;
        opponentName = name;
        host = true;
        localPlayer = Lig4Game.RED;
        waitingForAnswer = true;
        Lig4Message message = message(Lig4Message.INVITE);
        if (!send(message)) {
            waitingForAnswer = false;
            panel.showError("Não foi possível enviar o convite por TCP. Verifique se o usuário está conectado e se a rede permite a comunicação.");
        } else {
            panel.setWaiting("Convite enviado para " + name + ". Aguardando resposta…");
        }
    }

    public void receive(Lig4Message message) {
        if (message == null || message.getType() == null) return;
        switch (message.getType()) {
            case Lig4Message.INVITE -> receiveInvite(message);
            case Lig4Message.ACCEPT -> receiveAccept(message);
            case Lig4Message.REJECT -> receiveReject(message);
            case Lig4Message.GAME_START -> receiveGameStart(message);
            case Lig4Message.MOVE -> receiveMove(message);
            case Lig4Message.GAME_STATE -> receiveState(message);
            case Lig4Message.RESTART_REQUEST -> receiveRestartRequest(message);
            default -> { }
        }
    }

    private void receiveInvite(Lig4Message message) {
        InetAddress sender = parseAddress(message.getSenderIp());
        if (sender == null || active || waitingForAnswer) {
            sendReply(message, Lig4Message.REJECT, sender);
            return;
        }
        workspace.openOrFocusTab("Pitiupi.Lig4", "Lig 4");
        boolean accepted = panel.confirmInvite(message.getSenderName() == null ? "Um usuário" : message.getSenderName());
        if (!accepted) {
            sendReply(message, Lig4Message.REJECT, sender);
            return;
        }
        gameId = message.getGameId();
        opponentName = message.getSenderName();
        opponentAddress = sender;
        host = false;
        localPlayer = Lig4Game.YELLOW;
        active = true;
        waitingForAnswer = false;
        game = new Lig4Game();
        sendReply(message, Lig4Message.ACCEPT, sender);
        panel.setWaiting("Convite aceito. Aguardando o anfitrião iniciar a partida…");
        render();
    }

    private void sendReply(Lig4Message original, String type, InetAddress destination) {
        if (destination == null) return;
        Lig4Message reply = message(type);
        reply.setGameId(original.getGameId());
        sendTo(reply, destination);
    }

    private void receiveAccept(Lig4Message message) {
        if (!waitingForAnswer || !sameGame(message) || !host) return;
        if (!isExpectedPeer(message)) return;
        waitingForAnswer = false;
        active = true;
        game = new Lig4Game();
        Lig4Message start = message(Lig4Message.GAME_START);
        start.setGameId(gameId);
        send(start);
        sendState();
        render();
    }

    private void receiveReject(Lig4Message message) {
        if (!waitingForAnswer || !sameGame(message) || !isExpectedPeer(message)) return;
        waitingForAnswer = false;
        panel.setWaiting(opponentName + " recusou o convite.");
    }

    private void receiveGameStart(Lig4Message message) {
        if (host || !sameGame(message) || !isExpectedPeer(message)) return;
        active = true;
        game = new Lig4Game();
        render();
    }

    private void receiveMove(Lig4Message message) {
        if (!host || !active || !sameGame(message) || !isExpectedPeer(message)) return;
        if (message.getTurnNumber() != game.getTurnNumber() || game.getCurrentPlayer() != Lig4Game.YELLOW) return;
        int row = game.dropPiece(message.getColumn(), Lig4Game.YELLOW);
        if (row >= 0) sendState();
    }

    private void receiveState(Lig4Message message) {
        if (host || !active || !sameGame(message) || !isExpectedPeer(message)) return;
        boolean accepted = game.setState(message.getBoard(), message.getCurrentPlayer(), message.getTurnNumber(),
                message.getWinner(), message.isDraw());
        if (accepted) render();
    }

    private void receiveRestartRequest(Lig4Message message) {
        if (!host || !active || !sameGame(message) || !isExpectedPeer(message) || !game.isFinished()) return;
        game.reset();
        sendState();
        render();
    }

    private void playColumn(int column) {
        if (!active || game.isFinished() || game.getCurrentPlayer() != localPlayer) return;
        if (host) {
            int row = game.dropPiece(column, Lig4Game.RED);
            if (row >= 0) {
                sendState();
                render();
            }
        } else {
            Lig4Message move = message(Lig4Message.MOVE);
            move.setGameId(gameId);
            move.setColumn(column);
            move.setTurnNumber(game.getTurnNumber());
            if (!send(move)) panel.showError("Não foi possível enviar sua jogada. Verifique a conexão.");
        }
    }

    private void requestRestart() {
        if (!active || !game.isFinished()) return;
        if (host) {
            game.reset();
            sendState();
            render();
        } else {
            Lig4Message request = message(Lig4Message.RESTART_REQUEST);
            request.setGameId(gameId);
            if (!send(request)) panel.showError("Não foi possível solicitar uma nova partida.");
        }
    }

    private void sendState() {
        Lig4Message state = message(Lig4Message.GAME_STATE);
        state.setGameId(gameId);
        state.setBoard(game.getBoard());
        state.setCurrentPlayer(game.getCurrentPlayer());
        state.setTurnNumber(game.getTurnNumber());
        state.setWinner(game.getWinner());
        state.setDraw(game.isDraw());
        if (!send(state)) panel.setWaiting("Aviso: não foi possível sincronizar o estado com " + opponentName + ".");
    }

    private Lig4Message message(String type) {
        String localName = connectionManager.getUserName();
        String localIp = LocalNetworkInfo.bestLocalIpv4();
        return new Lig4Message(type, gameId, localName, localIp);
    }

    private boolean send(Lig4Message message) {
        return opponentAddress != null && sendTo(message, opponentAddress);
    }

    private boolean sendTo(Lig4Message message, InetAddress address) {
        int sent = MessageDispatcher.send(connectionManager, message,
                SendTarget.singlePeer(address), TransportMode.TCP);
        return sent > 0;
    }

    private boolean sameGame(Lig4Message message) {
        return gameId != null && gameId.equals(message.getGameId());
    }

    private boolean isExpectedPeer(Lig4Message message) {
        if (message.getSenderName() != null && opponentName != null && !opponentName.equals(message.getSenderName())) return false;
        InetAddress from = parseAddress(message.getSenderIp());
        return from != null && opponentAddress != null && from.equals(opponentAddress);
    }

    private InetAddress parseAddress(String ip) {
        try { return ip == null || ip.isBlank() ? null : InetAddress.getByName(ip); }
        catch (Exception ex) { return null; }
    }

    private void render() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::render);
            return;
        }
        panel.render(game.getBoard(), game.getCurrentPlayer(), localPlayer, game.getWinner(), game.isDraw(),
                active, opponentName == null ? "adversário" : opponentName, host);
    }

    public void inviteFromPeer(UserPeerView peer) { invite(peer); }
}
