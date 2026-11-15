package pitiupi.lig4.net;

import pitiupi.models.Messages.Message;

/** Mensagem serializável usada pelo protocolo do plugin Lig 4. */
public class Lig4Message extends Message {
    private static final long serialVersionUID = 1L;

    public static final String INVITE = "INVITE";
    public static final String ACCEPT = "ACCEPT";
    public static final String REJECT = "REJECT";
    public static final String GAME_START = "GAME_START";
    public static final String MOVE = "MOVE";
    public static final String GAME_STATE = "GAME_STATE";
    public static final String RESTART_REQUEST = "RESTART_REQUEST";

    private String type;
    private String gameId;
    private String senderName;
    private String senderIp;
    private int column = -1;
    private int[][] board;
    private int currentPlayer;
    private int turnNumber;
    private int winner;
    private boolean draw;

    public Lig4Message() { }

    public Lig4Message(String type, String gameId, String senderName, String senderIp) {
        this.type = type;
        this.gameId = gameId;
        this.senderName = senderName;
        this.senderIp = senderIp;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderIp() { return senderIp; }
    public void setSenderIp(String senderIp) { this.senderIp = senderIp; }
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }
    public int[][] getBoard() { return board; }
    public void setBoard(int[][] board) { this.board = board; }
    public int getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(int currentPlayer) { this.currentPlayer = currentPlayer; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
    public boolean isDraw() { return draw; }
    public void setDraw(boolean draw) { this.draw = draw; }
}
