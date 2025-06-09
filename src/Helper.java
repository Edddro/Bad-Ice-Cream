import java.awt.*;

public class Helper {
    public static boolean player1Collided = false;
    public static boolean player2Collided = false;

    public static void checkGameOver() {
        if (player1Collided && !GameState.player1GameOver && !GameState.victory) {
            GameState.player1GameOver = true;
        }
        if (player2Collided && !GameState.player2GameOver && !GameState.victory) {
            GameState.player2GameOver = true;
        }
        if (player1Collided && player2Collided) {
            GameState.gameOver = true;
            Main.playSound("../graphics/sounds/LoseMusic.wav", false);
        }
    }

    public static void storeFruit(int[][] map, int x, int y) {
        int currentTile = map[y][x];
        if (currentTile / 100 == 5 || currentTile / 10 == 4) {
            Enemy.storedFruits.put(new Point(x, y), currentTile);
        }
    }

    public static void restoreFruit(int[][] map, int x, int y) {
        Point key = new Point(x, y);
        if (Enemy.storedFruits.containsKey(key)) {
            map[y][x] = Enemy.storedFruits.remove(key);
        } else {
            map[y][x] = 6;
        }
    }
}
