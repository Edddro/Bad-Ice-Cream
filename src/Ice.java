import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

public class Ice {
    private static final int OBSTACLE = 1;

    private static final Map<Integer, List<BufferedImage>> iceFormAnimations = new HashMap<>();
    private static final Map<Integer, List<BufferedImage>> iceBreakAnimations = new HashMap<>();

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

    // Converts (dx, dy) direction to index: 0 = up, 1 = right, 2 = down, 3 = left
    private static int directionToIndex(int dx, int dy) {
        if (dx == 0 && dy == -1) return 0; // up
        if (dx == 1 && dy == 0) return 1;  // right
        if (dx == 0 && dy == 1) return 2;  // down
        if (dx == -1 && dy == 0) return 3; // left
        return 2; // default to down
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

    public static void formIce(int startX, int startY, int dx, int dy, int[][] map) {
        int x = startX + dx;
        int y = startY + dy;
        while (isValid(map, y, x) && canFormIce(map[y][x])) {
            map[y][x] = 2;
            x += dx;
            y += dy;
        }
    }

    public static void breakIce(int startX, int startY, int dx, int dy, int[][] map) {
        int x = startX + dx;
        int y = startY + dy;
        while (isValid(map, y, x) && map[y][x] == 2) {
            map[y][x] = 6;
            x += dx;
            y += dy;
        }
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
        return tile == 3 || tile == 6;
    }
}