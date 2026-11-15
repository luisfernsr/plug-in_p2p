package pitiupi.lig4.game;

/** Testes executáveis sem dependências externas: ant -f plugins/lig4/build.xml test. */
public final class Lig4GameTest {
    public static void main(String[] args) {
        verticalWin();
        horizontalWin();
        diagonalWin();
        invalidMoves();
        stateTransfer();
        System.out.println("Todos os testes de regras do Lig 4 passaram.");
    }

    private static void verticalWin() {
        Lig4Game game = new Lig4Game();
        play(game, new int[][]{{0,1},{1,2},{0,1},{1,2},{0,1},{1,2},{0,1}});
        check(game.getWinner() == Lig4Game.RED, "vitória vertical");
    }

    private static void horizontalWin() {
        Lig4Game game = new Lig4Game();
        play(game, new int[][]{{0,1},{0,2},{1,1},{1,2},{2,1},{2,2},{3,1}});
        check(game.getWinner() == Lig4Game.RED, "vitória horizontal");
    }

    private static void diagonalWin() {
        Lig4Game game = new Lig4Game();
        play(game, new int[][]{
                {0,1},{1,2},{1,1},{2,2},{6,1},{2,2},{2,1},
                {3,2},{5,1},{3,2},{4,1},{3,2},{3,1}
        });
        check(game.getWinner() == Lig4Game.RED, "vitória diagonal");
    }

    private static void invalidMoves() {
        Lig4Game game = new Lig4Game();
        check(game.dropPiece(-1, Lig4Game.RED) == -1, "coluna negativa");
        check(game.dropPiece(7, Lig4Game.RED) == -1, "coluna fora do tabuleiro");
        check(game.dropPiece(0, Lig4Game.YELLOW) == -1, "jogada fora do turno");
        for (int i = 0; i < 6; i++) {
            int player = i % 2 == 0 ? Lig4Game.RED : Lig4Game.YELLOW;
            check(game.dropPiece(0, player) >= 0, "preenchimento de coluna");
        }
        check(game.dropPiece(0, Lig4Game.RED) == -1, "coluna cheia");
    }

    private static void stateTransfer() {
        Lig4Game source = new Lig4Game();
        play(source, new int[][]{{0,1},{1,2},{0,1}});
        Lig4Game copy = new Lig4Game();
        check(copy.setState(source.getBoard(), source.getCurrentPlayer(), source.getTurnNumber(), source.getWinner(), source.isDraw()),
                "receber estado válido");
        check(copy.getTurnNumber() == 3 && copy.getCurrentPlayer() == Lig4Game.YELLOW, "estado sincronizado");
        int[][] invalid = source.getBoard();
        invalid[0][6] = Lig4Game.RED;
        check(!copy.setState(invalid, Lig4Game.YELLOW, 4, 0, false), "rejeitar peça flutuante/contador inconsistente");
    }

    private static void play(Lig4Game game, int[][] moves) {
        for (int[] move : moves) check(game.dropPiece(move[0], move[1]) >= 0, "jogada válida aceita");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Falhou: " + label);
    }
}
