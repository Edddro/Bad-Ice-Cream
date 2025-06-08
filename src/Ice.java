import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

public class Ice {
    private static final Map<Integer, List<BufferedImage>> iceFormAnimations = new HashMap<>();
    private static final Map<Integer, List<BufferedImage>> iceBreakAnimations = new HashMap<>();
    private static final Map<Point, Integer> storedFruits = new HashMap<>();

    private static List<BufferedImage> loadFrames(String path) {
        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            try {
                frames.add(ImageIO.read(new File(path + i + ".png")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return frames;
    }

    private static int directionToIndex(int dx, int dy) {
        if (dx == 0 && dy == -1) return 0; // up
        if (dx == 1 && dy == 0) return 1;  // right
        if (dx == 0 && dy == 1) return 2;  // down
        if (dx == -1 && dy == 0) return 3; // left
        return 2;
    }

    public static List<BufferedImage> getIceFormFrames(int dx, int dy) {
        int dir = directionToIndex(dx, dy);
        if (!iceFormAnimations.containsKey(dir)) {
            iceFormAnimations.put(dir, loadFrames("../graphics/images/map/ice/ice"));
        }
        return iceFormAnimations.get(dir);
    }

    public static List<BufferedImage> getIceBreakFrames(int dx, int dy) {
        int dir = directionToIndex(dx, dy);
        if (!iceBreakAnimations.containsKey(dir)) {
            iceBreakAnimations.put(dir, loadFrames("../graphics/images/map/ice/ice_break"));
        }
        return iceBreakAnimations.get(dir);
    }

    public static void formIce(int x, int y, int dx, int dy, int[][] map, int tileSize) {
        Timer timer = new Timer();
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x / tileSize, y / tileSize));

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

                if (!isValid(map, nextY, nextX) || !canFormIce(map[nextY][nextX])) {
                    timer.cancel();
                    return;
                }

                int currentTile = map[nextY][nextX];
                Point key = new Point(nextX, nextY);

                if (currentTile / 100 == 5 || (currentTile == 40 && GamePanel.player1GameOver) || (currentTile == 41 && GamePanel.player2GameOver)) {
                    storedFruits.put(key, currentTile);
                }

                map[nextY][nextX] = 2;
                queue.add(new Point(nextX, nextY));
            }
        }, 0, 100);
    }

    public static void breakIce(int x, int y, int dx, int dy, int[][] map, int tileSize) {
        Timer timer = new Timer();
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x / tileSize, y / tileSize));

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

                if (!isValid(map, nextY, nextX) || map[nextY][nextX] != 2) {
                    timer.cancel();
                    return;
                }

                Point key = new Point(nextX, nextY);
                if (storedFruits.containsKey(key)) {
                    map[nextY][nextX] = storedFruits.remove(key);
                } else {
                    map[nextY][nextX] = 6;
                }

                queue.add(new Point(nextX, nextY));
            }
        }, 0, 100);
    }

    public static void drawFormAnimation(Graphics g, int x, int y, int tileSize, int frameIndex, int[] direction) {
        List<BufferedImage> frames = getIceFormFrames(direction[0], direction[1]);
        if (!frames.isEmpty()) {
            BufferedImage frame = frames.get(frameIndex % frames.size());
            g.drawImage(frame, x, y, tileSize, tileSize, null);
        }
    }

    public static void drawBreakAnimation(Graphics g, int x, int y, int tileSize, int frameIndex, int[] direction) {
        List<BufferedImage> frames = getIceBreakFrames(direction[0], direction[1]);
        if (!frames.isEmpty()) {
            BufferedImage frame = frames.get(frameIndex % frames.size());
            g.drawImage(frame, x, y, tileSize, tileSize, null);
        }
    }

    private static boolean isValid(int[][] map, int row, int col) {
        return row >= 0 && col >= 0 && row < map.length && col < map[0].length;
    }

    private static boolean canFormIce(int tile) {
        return tile / 100 == 5 || tile == 6 || (tile == 40 && GamePanel.player1GameOver) || (tile == 41 && GamePanel.player2GameOver);
    }
}