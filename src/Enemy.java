import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import javax.imageio.ImageIO;

public abstract class Enemy {
    protected int x, y, tileSize;
    protected String direction;
    protected boolean facingRight;
    protected Map<String, List<BufferedImage>> animations;
    protected int animFrame;
    protected long lastMoveTime;

    public Enemy(int x, int y, int tileSize) {
        this.x = x;
        this.y = y;
        this.tileSize = tileSize;
        this.animations = new HashMap<>();
        this.animFrame = 0;
        this.direction = "down";
        this.facingRight = true;
        this.lastMoveTime = System.currentTimeMillis();
    }

    public abstract void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y);

    public abstract void draw(Graphics g);

    protected BufferedImage flipImage(BufferedImage img) {
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = flipped.createGraphics();
        g2.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
        g2.dispose();
        return flipped;
    }

    protected int[] directionVector() {
        return switch (direction) {
            case "up" -> new int[]{0, -1};
            case "down" -> new int[]{0, 1};
            case "side" -> facingRight ? new int[]{1, 0} : new int[]{-1, 0};
            default -> new int[]{0, 1};
        };
    }

    protected boolean isObstacle(int tileValue) {
        return tileValue == 6 || tileValue / 10 == 5 || tileValue / 10 == 4 || tileValue / 10 == 3;
    }

    protected boolean collidesWithPlayer(int playerX, int playerY) {
        Rectangle enemyRect = new Rectangle(x, y, tileSize, tileSize);
        Rectangle playerRect = new Rectangle(playerX, playerY, tileSize, tileSize);
        return enemyRect.intersects(playerRect);
    }

    protected List<BufferedImage> loadFrames(String path) {
        List<BufferedImage> frames = new ArrayList<>();
        File dir = new File(path);
        if (dir.exists()) {
            File[] files = dir.listFiles((_, name) -> name.endsWith(".png"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    try {
                        frames.add(ImageIO.read(f));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return frames;
    }

    protected Point closestPlayer(int player1X, int player1Y, int player2X, int player2Y) {
        List<Point> alivePlayers = new ArrayList<>();
        if (!PlayerStatus.player1Collided) alivePlayers.add(new Point(player1X, player1Y));
        if (!PlayerStatus.player2Collided) alivePlayers.add(new Point(player2X, player2Y));

        if (alivePlayers.isEmpty()) return null;

        Point target = alivePlayers.getFirst();
        int minDist = Math.abs(x - target.x) + Math.abs(y - target.y);

        for (Point p : alivePlayers) {
            int dist = Math.abs(x - p.x) + Math.abs(y - p.y);
            if (dist < minDist) {
                target = p;
                minDist = dist;
            }
        }

        return target;
    }
}

class Monster extends Enemy {
    public Monster(int x, int y, int tileSize) {
        super(x, y, tileSize);
        animations.put("up", loadFrames("../graphics/images/enemies/monster/up"));
        animations.put("down", loadFrames("../graphics/images/enemies/monster/down"));
        animations.put("side", loadFrames("../graphics/images/enemies/monster/side"));
        this.direction = "down";
    }

    @Override
    public void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y) {
        if (System.currentTimeMillis() - lastMoveTime < 275) return;

        map[y / tileSize][x / tileSize] = 6;
        int[] d = directionVector();
        int newX = x + d[0] * tileSize;
        int newY = y + d[1] * tileSize;

        int col = newX / tileSize;
        int row = newY / tileSize;

        if (row >= 0 && col >= 0 && row < map.length && col < map[0].length && isObstacle(map[row][col])) {
            x = newX;
            y = newY;
        } else {
            turnRight();
        }

        map[y / tileSize][x / tileSize] = 32;
        animFrame++;
        lastMoveTime = System.currentTimeMillis();

        if (collidesWithPlayer(player1X, player1Y)) {
            PlayerStatus.player1Collided = true;
        }
        if (collidesWithPlayer(player2X, player2Y)) {
            PlayerStatus.player2Collided = true;
        }
        PlayerStatus.checkGameOver();
    }

    private void turnRight() {
        direction = switch (direction) {
            case "up" -> "side";
            case "side" -> {
                if (facingRight) {
                    yield "down";
                } else {
                    facingRight = true;
                    yield "up";
                }
            }
            case "down" -> {
                facingRight = false;
                yield "side";
            }
            default -> "up";
        };
    }

    public void draw(Graphics g) {
        List<BufferedImage> frames = animations.get(direction);
        if (frames != null && !frames.isEmpty()) {
            BufferedImage frame = frames.get(animFrame % frames.size());

            if (direction.equals("side") && !facingRight) {
                frame = flipImage(frame);
            }
            g.drawImage(frame, x, y, (int)(tileSize * 1.2), (int)(tileSize * 1.2), null);
        }
    }
}

class Halo extends Enemy {
    private long lastPathTime;
    private Queue<Point> path;

    public Halo(int x, int y, int tileSize) {
        super(x, y, tileSize);
        animations.put("up", loadFrames("../graphics/images/enemies/halo/up"));
        animations.put("down", loadFrames("../graphics/images/enemies/halo/down"));
        animations.put("side", loadFrames("../graphics/images/enemies/halo/side"));
        this.direction = "side";
        path = new LinkedList<>();
    }

    @Override
    public void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y) {
        Point target = closestPlayer(player1X, player1Y, player2X, player2Y);
        if (target == null) return;

        int targetX = target.x;
        int targetY = target.y;

        if (System.currentTimeMillis() - lastMoveTime < 275) return;

        if (System.currentTimeMillis() - lastPathTime > 5000 || path.isEmpty()) {
            path = findPath(map, x / tileSize, y / tileSize, targetX / tileSize, targetY / tileSize);
            lastPathTime = System.currentTimeMillis();
        }

        if (!path.isEmpty()) {
            Point next = path.poll();
            if (next != null) {
                int dx = next.x * tileSize - x;
                int dy = next.y * tileSize - y;

                if (dx != 0) {
                    direction = "side";
                    facingRight = dx > 0;
                }
                else if (dy < 0) direction = "up";
                else direction = "down";

                map[y / tileSize][x / tileSize] = 6;
                x = next.x * tileSize;
                y = next.y * tileSize;
            }
        }

        map[y / tileSize][x / tileSize] = 30;
        animFrame++;
        lastMoveTime = System.currentTimeMillis();

        if (collidesWithPlayer(player1X, player1Y)) {
            PlayerStatus.player1Collided = true;
        }
        if (collidesWithPlayer(player2X, player2Y)) {
            PlayerStatus.player2Collided = true;
        }
        PlayerStatus.checkGameOver();
    }

    private Queue<Point> findPath(int[][] map, int sx, int sy, int ex, int ey) {
        Queue<Point> path = new LinkedList<>();
        int rows = map.length;
        int cols = map[0].length;
        boolean[][] visited = new boolean[rows][cols];
        Point[][] prev = new Point[rows][cols];

        Queue<Point> q = new LinkedList<>();
        q.add(new Point(sx, sy));
        visited[sy][sx] = true;

        int[][] dirs = {{0,1},{1,0},{0,-1},{-1,0}};
        while (!q.isEmpty()) {
            Point curr = q.poll();
            if (curr.x == ex && curr.y == ey) break;
            for (int[] d : dirs) {
                int nx = curr.x + d[0], ny = curr.y + d[1];
                if (nx >= 0 && ny >= 0 && nx < cols && ny < rows && !visited[ny][nx] && isObstacle(map[ny][nx])) {
                    q.add(new Point(nx, ny));
                    visited[ny][nx] = true;
                    prev[ny][nx] = curr;
                }
            }
        }

        Point curr = new Point(ex, ey);
        Stack<Point> stack = new Stack<>();
        while (curr != null && !(curr.x == sx && curr.y == sy)) {
            stack.push(curr);
            curr = prev[curr.y][curr.x];
        }

        while (!stack.isEmpty()) path.add(stack.pop());
        return path;
    }

    public void draw(Graphics g) {
        List<BufferedImage> frames = animations.get(direction);
        if (frames != null && !frames.isEmpty()) {
            BufferedImage frame = frames.get(animFrame % frames.size());

            if (direction.equals("side") && !facingRight) {
                frame = flipImage(frame);
            }
            g.drawImage(frame, x, y + (int)(tileSize * 0.5), (int)(tileSize * 1.5), (int)(tileSize * 1.5), null);
        }
    }
}

class IceBreaker extends Enemy {
    private boolean breaking = false;
    private long breakStartTime;
    private List<BufferedImage> breakFrames;
    private int breakRow = -1, breakCol = -1;

    public IceBreaker(int x, int y, int tileSize) {
        super(x, y, tileSize);
        animations.put("up", loadFrames("../graphics/images/enemies/icebreaker/up"));
        animations.put("down", loadFrames("../graphics/images/enemies/icebreaker/down"));
        animations.put("side", loadFrames("../graphics/images/enemies/icebreaker/side"));
        direction = "side";
    }

    @Override
    public void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y) {
        if (breaking) {
            if (System.currentTimeMillis() - breakStartTime >= 750) {
                breaking = false;
                if (breakRow >= 0 && breakCol >= 0 && breakRow < map.length && breakCol < map[0].length) {
                    map[breakRow][breakCol] = 6;
                }
            }
            animFrame++;
            return;
        }

        if (System.currentTimeMillis() - lastMoveTime < 275) {
            return;
        }

        Point target = closestPlayer(player1X, player1Y, player2X, player2Y);
        if (target == null) return;

        int targetX = target.x;
        int targetY = target.y;

        int dx = Integer.compare(targetX, x);
        int dy = Integer.compare(targetY, y);

        if (dx != 0) {
            direction = "side";
            facingRight = dx > 0;
        } else if (dy < 0) direction = "up";
        else direction = "down";

        int newX = x + dx * tileSize;
        int newY = y + dy * tileSize;

        int col = newX / tileSize;
        int row = newY / tileSize;

        if (row >= 0 && col >= 0 && row < map.length && col < map[0].length) {
            int nextTile = map[row][col];
            if (nextTile == 2) {
                breaking = true;
                breakStartTime = System.currentTimeMillis();
                breakRow = row;
                breakCol = col;
                breakFrames = loadFrames("../graphics/images/enemies/icebreaker/" + direction + "/break_ice");
            } else if (isObstacle(nextTile)) {
                map[y / tileSize][x / tileSize] = 6;
                x = newX;
                y = newY;
            }
        }

        map[y / tileSize][x / tileSize] = 31;
        animFrame++;
        lastMoveTime = System.currentTimeMillis();

        if (collidesWithPlayer(player1X, player1Y)) {
            PlayerStatus.player1Collided = true;
        }
        if (collidesWithPlayer(player2X, player2Y)) {
            PlayerStatus.player2Collided = true;
        }
        PlayerStatus.checkGameOver();
    }

    @Override
    public void draw(Graphics g) {
        if (breaking) {
            BufferedImage img = breakFrames.get((animFrame / 5) % breakFrames.size());
            g.drawImage(img, x, y - tileSize, tileSize, tileSize * 2, null);

            if (breakRow >= 0 && breakCol >= 0) {
                int drawX = breakCol * tileSize;
                int drawY = breakRow * tileSize;
                int[] iceDirection = directionVector();
                Ice.drawBreakAnimation(g, drawX, drawY, tileSize, animFrame / 5, iceDirection);
            }
        } else {
            List<BufferedImage> frames = animations.get(direction);
            if (frames != null && !frames.isEmpty()) {
                BufferedImage frame = frames.get(animFrame % frames.size());
                if (direction.equals("side") && !facingRight) {
                    frame = flipImage(frame);
                }
                g.drawImage(frame, x, y - tileSize, tileSize, tileSize * 2, null);
            }
        }
    }
}

class PlayerStatus {
    public static boolean player1Collided = false;
    public static boolean player2Collided = false;

    public static void checkGameOver() {
        if (player1Collided && player2Collided) {
            System.out.println("Game Over");
        }
    }
}