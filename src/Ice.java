import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

public class Ice {
    // Caches for animation frames based on direction
    private static final Map<Integer, List<BufferedImage>> iceFormAnimations = new HashMap<>();
    private static final Map<Integer, List<BufferedImage>> iceBreakAnimations = new HashMap<>();

    // Stores fruit tiles that were covered by ice, keyed by position
    private static final Map<Point, Integer> storedFruits = new HashMap<>();

    // Loads a list of 3 animation frames from a specified path
    private static List<BufferedImage> loadFrames(String path) {
        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            try {
                frames.add(ImageIO.read(new File(path + i + ".png")));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return frames;
    }

    // Converts a direction (dx, dy) to a unique index (used as key in animation maps)
    private static int directionToIndex(int dx, int dy) {
        // Up
        if (dx == 0 && dy == -1) return 0;
        // Right
        if (dx == 1 && dy == 0) return 1;
        // Down
        if (dx == 0 && dy == 1) return 2;
        // Left
        if (dx == -1 && dy == 0) return 3;
        // Fallback default direction (down)
        return 2;
    }

    // Retrieves (and caches) ice formation animation frames for a given direction
    public static List<BufferedImage> getIceFormFrames(int dx, int dy) {
        int dir = directionToIndex(dx, dy);
        if (!iceFormAnimations.containsKey(dir)) {
            iceFormAnimations.put(dir, loadFrames("../graphics/images/map/ice/ice"));
        }
        return iceFormAnimations.get(dir);
    }

    // Retrieves (and caches) ice breaking animation frames for a given direction
    public static List<BufferedImage> getIceBreakFrames(int dx, int dy) {
        int dir = directionToIndex(dx, dy);
        if (!iceBreakAnimations.containsKey(dir)) {
            iceBreakAnimations.put(dir, loadFrames("../graphics/images/map/ice/ice_break"));
        }
        return iceBreakAnimations.get(dir);
    }

    // Forms a trail of ice in a direction until an invalid or blocked tile is encountered
    public static void formIce(int x, int y, int dx, int dy, int[][] map, int tileSize) {
        Timer timer = new Timer();
        Queue<Point> queue = new LinkedList<>();
        // Start at the tile-based coordinate
        queue.add(new Point(x / tileSize, y / tileSize));

        // Repeated task every 100ms to grow the ice trail
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    timer.cancel();
                    return;
                }

                Point current = queue.poll();
                int x = current.x;
                int y = current.y;

                int nextX = x + dx;
                int nextY = y + dy;

                // If the next tile is out of bounds or invalid, stop forming
                if (isValid(map, nextY, nextX) || !canFormIce(map[nextY][nextX])) {
                    timer.cancel();
                    return;
                }

                int currentTile = map[nextY][nextX];
                Point key = new Point(nextX, nextY);

                // Store any fruit tile so it can be restored later
                if (currentTile / 100 == 5 || (currentTile == 40 && GameState.player1GameOver) || (currentTile == 41 && GameState.player2GameOver)) {
                    storedFruits.put(key, currentTile);
                }

                // Mark tile as ice (value 2)
                map[nextY][nextX] = 2;
                queue.add(new Point(nextX, nextY));
            }
        }, 0, 100);
    }

    // Breaks a trail of ice in a direction, revealing underlying fruit or setting to normal tile
    public static void breakIce(int x, int y, int dx, int dy, int[][] map, int tileSize) {
        Timer timer = new Timer();
        Queue<Point> queue = new LinkedList<>();
        // Start at the tile-based coordinate
        queue.add(new Point(x / tileSize, y / tileSize));

        // Repeated task every 100ms to break the ice trail
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    timer.cancel();
                    return;
                }

                Point current = queue.poll();
                int x = current.x;
                int y = current.y;

                int nextX = x + dx;
                int nextY = y + dy;

                // If the next tile is out of bounds or not ice, stop breaking
                if (isValid(map, nextY, nextX) || map[nextY][nextX] != 2) {
                    timer.cancel();
                    return;
                }

                Point key = new Point(nextX, nextY);
                // Restore fruit tile if one was stored, otherwise mark as broken ice (value 6)
                if (storedFruits.containsKey(key)) {
                    map[nextY][nextX] = storedFruits.remove(key);
                } else {
                    map[nextY][nextX] = 6;
                }

                queue.add(new Point(nextX, nextY));
            }
        }, 0, 100);
    }

    // Draws the current frame of the ice formation animation
    public static void drawFormAnimation(Graphics g, int x, int y, int tileSize, int frameIndex, int[] direction) {
        List<BufferedImage> frames = getIceFormFrames(direction[0], direction[1]);
        if (!frames.isEmpty()) {
            BufferedImage frame = frames.get(frameIndex % frames.size());
            g.drawImage(frame, x, y, tileSize, tileSize, null);
        }
    }

    // Draws the current frame of the ice breaking animation
    public static void drawBreakAnimation(Graphics g, int x, int y, int tileSize, int frameIndex, int[] direction) {
        List<BufferedImage> frames = getIceBreakFrames(direction[0], direction[1]);
        if (!frames.isEmpty()) {
            BufferedImage frame = frames.get(frameIndex % frames.size());
            g.drawImage(frame, x, y, tileSize, tileSize, null);
        }
    }

    // Checks if the row/col are out of bounds of the map
    private static boolean isValid(int[][] map, int row, int col) {
        return row < 0 || col < 0 || row >= map.length || col >= map[0].length;
    }

    // Checks if the tile can be turned into ice (fruit, breakable, or finished player tile)
    private static boolean canFormIce(int tile) {
        return tile / 100 == 5 || tile == 6 || (tile == 40 && GameState.player1GameOver) || (tile == 41 && GameState.player2GameOver);
    }
}