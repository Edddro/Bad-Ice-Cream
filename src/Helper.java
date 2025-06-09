import java.awt.*;

// Manage collision states, handle game-over logic, and support fruit tile manipulation on the game map
public class Helper {
    // Flags to track if each player has collided
    public static boolean player1Collided = false;
    public static boolean player2Collided = false;

    // Checks the current collision status of both players, updating the game state accordingly.
    public static void checkGameOver() {
        if (player1Collided && !GameState.player1GameOver && !GameState.victory) {
            GameState.player1GameOver = true;
        }

        if (player2Collided && !GameState.player2GameOver && !GameState.victory) {
            GameState.player2GameOver = true;
        }

        // If both players have collided, set game over to true
        if (player1Collided && player2Collided) {
            GameState.gameOver = true;
            Main.playSound("../graphics/sounds/LoseMusic.wav", false);
        }
    }


    // Stores the fruit tile at the specified map location if it's a fruit tile.
    public static void storeFruit(int[][] map, int x, int y) {
        int currentTile = map[y][x];
        // Check if the tile is a fruit or player (tile codes: 5xx or 4)
        if (currentTile / 100 == 5 || currentTile / 10 == 4) {
            Enemy.storedFruits.put(new Point(x, y), currentTile);
        }
    }

    // Restores a previously stored fruit tile at the given location or defaults to a breakable ice block (6)
    public static void restoreFruit(int[][] map, int x, int y) {
        Point key = new Point(x, y);
        if (Enemy.storedFruits.containsKey(key)) {
            map[y][x] = Enemy.storedFruits.remove(key);
        } else {
            map[y][x] = 6;
        }
    }
}
