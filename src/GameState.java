public class GameState {
    public static boolean gameOver = false;
    public static boolean victory = false;
    public static boolean player1GameOver = false;
    public static boolean player2GameOver = false;
    public static int player1GameOverFrame = 0;
    public static int player2GameOverFrame = 0;

    public static void reset() {
        gameOver = false;
        victory = false;
        player1GameOver = false;
        player2GameOver = false;
        player1GameOverFrame = 0;
        player2GameOverFrame = 0;
        Helper.player1Collided = false;
        Helper.player2Collided = false;
    }
}
