package pitiupi.lig4.ui;

import pitiupi.application.presence.UserPeerView;
import pitiupi.lig4.game.Lig4Game;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/** Interface Swing do Lig 4. */
public final class Lig4Panel extends JPanel {
    private final JComboBox<PeerChoice> peers = new JComboBox<>();
    private final JLabel status = new JLabel("Escolha uma pessoa conectada e envie um convite.", SwingConstants.CENTER);
    private final JLabel turn = new JLabel("Aguardando partida", SwingConstants.CENTER);
    private final JPanel boardPanel = new JPanel(new GridLayout(Lig4Game.ROWS, Lig4Game.COLUMNS, 5, 5));
    private final JButton inviteButton = new JButton("Convidar para jogar");
    private final JButton refreshButton = new JButton("Atualizar usuários");
    private final JButton restartButton = new JButton("Nova partida");
    private final List<JButton> cells = new ArrayList<>();
    private Runnable inviteAction = () -> { };
    private Runnable refreshAction = () -> { };
    private Runnable restartAction = () -> { };
    private java.util.function.IntConsumer moveAction = column -> { };

    public Lig4Panel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("LIG 4", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        top.add(title, BorderLayout.NORTH);
        JPanel controls = new JPanel();
        controls.add(peers);
        controls.add(refreshButton);
        controls.add(inviteButton);
        controls.add(restartButton);
        top.add(controls, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        boardPanel.setBackground(new Color(27, 78, 150));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (int row = 0; row < Lig4Game.ROWS; row++) {
            for (int col = 0; col < Lig4Game.COLUMNS; col++) {
                final int selectedColumn = col;
                JButton cell = new JButton("●");
                cell.setFont(cell.getFont().deriveFont(Font.BOLD, 29f));
                cell.setFocusPainted(false);
                cell.setOpaque(true);
                cell.setBackground(new Color(235, 239, 245));
                cell.setForeground(new Color(190, 195, 202));
                cell.setBorder(BorderFactory.createLineBorder(new Color(20, 58, 110), 2));
                cell.setPreferredSize(new Dimension(58, 58));
                cell.addActionListener(e -> moveAction.accept(selectedColumn));
                cells.add(cell);
                boardPanel.add(cell);
            }
        }
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(turn, BorderLayout.NORTH);
        center.add(boardPanel, BorderLayout.CENTER);
        center.add(status, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        inviteButton.addActionListener(e -> inviteAction.run());
        refreshButton.addActionListener(e -> refreshAction.run());
        restartButton.addActionListener(e -> restartAction.run());
        restartButton.setEnabled(false);
    }

    public void setActions(Runnable invite, Runnable refresh, Runnable restart, java.util.function.IntConsumer move) {
        inviteAction = invite;
        refreshAction = refresh;
        restartAction = restart;
        moveAction = move;
    }

    public void setPeers(List<UserPeerView> peerViews) {
        PeerChoice selected = (PeerChoice) peers.getSelectedItem();
        peers.removeAllItems();
        if (peerViews != null) {
            for (UserPeerView peer : peerViews) {
                if (peer == null || peer.isSelf() || peer.isBroadcastTarget() || peer.getInetAddress().isEmpty()) continue;
                peers.addItem(new PeerChoice(peer.getDisplayName(), peer.getInetAddress().get()));
            }
        }
        if (selected != null) {
            for (int i = 0; i < peers.getItemCount(); i++) {
                PeerChoice item = peers.getItemAt(i);
                if (item.address.equals(selected.address)) {
                    peers.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public InetAddress getSelectedPeerAddress() {
        PeerChoice choice = (PeerChoice) peers.getSelectedItem();
        return choice == null ? null : choice.address;
    }

    public String getSelectedPeerName() {
        PeerChoice choice = (PeerChoice) peers.getSelectedItem();
        return choice == null ? null : choice.name;
    }

    public boolean confirmInvite(String playerName) {
        return JOptionPane.showConfirmDialog(this,
                playerName + " convidou você para jogar Lig 4.\nDeseja aceitar?",
                "Convite para Lig 4", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Lig 4", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lig 4", JOptionPane.ERROR_MESSAGE);
    }

    public void render(int[][] board, int currentPlayer, int localPlayer, int winner, boolean draw,
                       boolean active, String opponentName, boolean host) {
        for (int row = 0; row < Lig4Game.ROWS; row++) {
            for (int col = 0; col < Lig4Game.COLUMNS; col++) {
                JButton cell = cells.get(row * Lig4Game.COLUMNS + col);
                int value = board[row][col];
                cell.setForeground(value == Lig4Game.RED ? new Color(205, 45, 45)
                        : value == Lig4Game.YELLOW ? new Color(220, 170, 15) : new Color(190, 195, 202));
                cell.setEnabled(active && winner == 0 && !draw && currentPlayer == localPlayer);
            }
        }
        if (!active) {
            turn.setText("Aguardando partida");
            status.setText("Escolha uma pessoa conectada e envie um convite.");
        } else if (winner != 0) {
            turn.setText(winner == localPlayer ? "Você venceu!" : "Você perdeu.");
            status.setText("Partida encerrada contra " + opponentName + ".");
        } else if (draw) {
            turn.setText("Empate!");
            status.setText("A partida terminou sem vencedor.");
        } else {
            String color = currentPlayer == Lig4Game.RED ? "vermelha" : "amarela";
            turn.setText(currentPlayer == localPlayer ? "Sua vez — peça " + color : "Vez de " + opponentName);
            status.setText((host ? "Você é o anfitrião." : "Você entrou na partida.") + " Adversário: " + opponentName);
        }
        restartButton.setEnabled(active && (winner != 0 || draw));
        inviteButton.setEnabled(!active);
    }

    public void setWaiting(String text) {
        status.setText(text);
    }

    private static final class PeerChoice {
        private final String name;
        private final InetAddress address;
        private PeerChoice(String name, InetAddress address) { this.name = name; this.address = address; }
        @Override public String toString() { return name + " (" + address.getHostAddress() + ")"; }
    }
}
