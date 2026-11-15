package pitiupi.lig4.game;

import java.util.Arrays;

/** Regras puras do Lig 4: 6 linhas, 7 colunas e quatro peças para vencer. */
public final class Lig4Game {
    public static final int ROWS = 6;
    public static final int COLUMNS = 7;
    public static final int EMPTY = 0;
    public static final int RED = 1;
    public static final int YELLOW = 2;

    private final int[][] board = new int[ROWS][COLUMNS];
    private int currentPlayer = RED;
    private int turnNumber = 0;
    private int winner = EMPTY;
    private boolean draw;

    /** @return linha onde a peça caiu ou -1 se a coluna for inválida/cheia ou a partida terminou. */
    public int dropPiece(int column, int player) {
        if (column < 0 || column >= COLUMNS || (player != RED && player != YELLOW)
                || player != currentPlayer || winner != EMPTY || draw) {
            return -1;
        }
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == EMPTY) {
                board[row][column] = player;
                turnNumber++;
                if (hasFour(row, column, player)) {
                    winner = player;
                } else if (isBoardFull()) {
                    draw = true;
                } else {
                    currentPlayer = player == RED ? YELLOW : RED;
                }
                return row;
            }
        }
        return -1;
    }

    private boolean hasFour(int row, int column, int player) {
        return count(row, column, 0, 1, player) + count(row, column, 0, -1, player) - 1 >= 4
                || count(row, column, 1, 0, player) + count(row, column, -1, 0, player) - 1 >= 4
                || count(row, column, 1, 1, player) + count(row, column, -1, -1, player) - 1 >= 4
                || count(row, column, 1, -1, player) + count(row, column, -1, 1, player) - 1 >= 4;
    }

    private int count(int row, int column, int dr, int dc, int player) {
        int total = 0;
        int r = row;
        int c = column;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLUMNS && board[r][c] == player) {
            total++;
            r += dr;
            c += dc;
        }
        return total;
    }

    private boolean isBoardFull() {
        for (int c = 0; c < COLUMNS; c++) {
            if (board[0][c] == EMPTY) return false;
        }
        return true;
    }

    public void reset() {
        for (int[] row : board) Arrays.fill(row, EMPTY);
        currentPlayer = RED;
        turnNumber = 0;
        winner = EMPTY;
        draw = false;
    }

    public int[][] getBoard() {
        int[][] copy = new int[ROWS][COLUMNS];
        for (int r = 0; r < ROWS; r++) copy[r] = Arrays.copyOf(board[r], COLUMNS);
        return copy;
    }

    /** Substitui o estado local por um estado recebido do host, validando suas dimensões e valores. */
    public boolean setState(int[][] incoming, int nextPlayer, int incomingTurn, int incomingWinner, boolean incomingDraw) {
        if (incoming == null || incoming.length != ROWS || (nextPlayer != RED && nextPlayer != YELLOW)
                || incomingTurn < 0 || (incomingWinner < EMPTY || incomingWinner > YELLOW)) return false;
        int pieces = 0;
        for (int r = 0; r < ROWS; r++) {
            if (incoming[r] == null || incoming[r].length != COLUMNS) return false;
            for (int c = 0; c < COLUMNS; c++) {
                int value = incoming[r][c];
                if (value < EMPTY || value > YELLOW) return false;
                if (value != EMPTY) pieces++;
                if (value != EMPTY && r < ROWS - 1 && incoming[r + 1][c] == EMPTY) return false;
            }
        }
        if (pieces != incomingTurn) return false;
        for (int r = 0; r < ROWS; r++) System.arraycopy(incoming[r], 0, board[r], 0, COLUMNS);
        currentPlayer = nextPlayer;
        turnNumber = incomingTurn;
        winner = incomingWinner;
        draw = incomingDraw;
        return true;
    }

    public int getCurrentPlayer() { return currentPlayer; }
    public int getTurnNumber() { return turnNumber; }
    public int getWinner() { return winner; }
    public boolean isDraw() { return draw; }
    public boolean isFinished() { return winner != EMPTY || draw; }
}
