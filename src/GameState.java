// Stores all static variables used in the game for resetting or moving to the next game
public class GameState {
    // Indicates the game state
    public static boolean gameOver = false;
    public static boolean victory = false;

    // Indicates the player states
    public static boolean player1GameOver = false;
    public static boolean player2GameOver = false;
    public static int player1GameOverFrame = 0;
    public static int player2GameOverFrame = 0;

    // Sets the variables to the original values
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
