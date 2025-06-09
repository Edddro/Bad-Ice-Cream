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

// Enemy class to serve as a blueprint for all types of enemies (i.e. draw, update, collide with player)
public abstract class Enemy {
    // Variables to store enemy direction, animation files, and fruits on the map
    protected int x, y, tileSize;
    protected String direction;
    protected boolean facingRight;
    protected Map<String, List<BufferedImage>> animations;
    protected int animFrame;
    protected long lastMoveTime;
    protected static final Map<Point, Integer> storedFruits = new HashMap<>();

    // Constructor for enemy to initialize the position and direction of the enemy
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

    // Abstract method for updating enemy state, implemented by subclasses (specific enemy type)
    public abstract void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y);

    // Abstract method for drawing the enemy on screen
    public abstract void draw(Graphics g);

    // Utility method to horizontally flip an image (used when the enemy faces left)
    protected BufferedImage flipImage(BufferedImage img) {
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = flipped.createGraphics();
        g2.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
        g2.dispose();
        return flipped;
    }

    // Returns the direction vector based on the current direction and facing state
    protected int[] directionVector() {
        return switch (direction) {
            case "up" -> new int[]{0, -1};
            // Left or right
            case "side" -> facingRight ? new int[]{1, 0} : new int[]{-1, 0};
            default -> new int[]{0, 1};
        };
    }

    // Determines if the tile can be walked on by the enemy (empty tile, fruit tile, or player)
    protected boolean isPassable(int tileValue) {
        return tileValue == 6 || tileValue / 100 == 5 || tileValue / 10 == 4;
    }

    // Checks if the enemy collides with a player
    protected boolean collidesWithPlayer(int playerX, int playerY) {
        Rectangle enemyRect = new Rectangle(x, y, tileSize, tileSize);
        Rectangle playerRect = new Rectangle(playerX, playerY, tileSize, tileSize);
        return enemyRect.intersects(playerRect);
    }

    // Loads a list of animation frames (images) from a given directory
    protected List<BufferedImage> loadFrames(String path) {
        List<BufferedImage> frames = new ArrayList<>();
        File dir = new File(path);
        if (dir.exists()) {
            File[] files = dir.listFiles((_, name) -> name.endsWith(".png"));
            if (files != null) {
                // Ensures correct order of animation frames
                Arrays.sort(files);
                for (File f : files) {
                    try {
                        frames.add(ImageIO.read(f));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        return frames;
    }

    // Finds the closest player who has not been collided with
    protected Point closestPlayer(int player1X, int player1Y, int player2X, int player2Y) {
        List<Point> alivePlayers = new ArrayList<>();
        if (!Helper.player1Collided) alivePlayers.add(new Point(player1X, player1Y));
        if (!Helper.player2Collided) alivePlayers.add(new Point(player2X, player2Y));

        // Both players have been collided with
        if (alivePlayers.isEmpty()) return null;

        // Calculates the closest player (through both of their distances from the enemy)
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

// Monster subclass of Enemy
class Monster extends Enemy {
    public Monster(int x, int y, int tileSize) {
        super(x, y, tileSize);
        // Load animation frames for all directions, down by default
        animations.put("up", loadFrames("../graphics/images/enemies/monster/up"));
        animations.put("down", loadFrames("../graphics/images/enemies/monster/down"));
        animations.put("side", loadFrames("../graphics/images/enemies/monster/side"));
        this.direction = "down";
    }

    // Update enemy position and direction on the map
    @Override
    public void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y) {
        // Cancels movement if game is paused
        if (GamePanel.isPaused) return;

        // Move only if enough time has passed
        if (System.currentTimeMillis() - lastMoveTime < 275) return;

        // Restore any fruit at the current position
        Helper.restoreFruit(map, x / tileSize, y / tileSize);

        // Calculate next position based on direction
        int[] d = directionVector();
        int newX = x + d[0] * tileSize;
        int newY = y + d[1] * tileSize;

        int col = newX / tileSize;
        int row = newY / tileSize;

        // If the next tile is within bounds and passable, move to it, otherwise turn to the right
        if (row >= 0 && col >= 0 && row < map.length && col < map[0].length && isPassable(map[row][col])) {
            x = newX;
            y = newY;
        } else {
            turnRight();
        }

        // Store fruit at new location and update map to show enemy's new position
        Helper.storeFruit(map, x / tileSize, y / tileSize);
        map[y / tileSize][x / tileSize] = 32;
        // Next animation frame
        animFrame++;
        lastMoveTime = System.currentTimeMillis();

        // Collision detection with players
        if (collidesWithPlayer(player1X, player1Y)) {
            Helper.player1Collided = true;
        }
        if (collidesWithPlayer(player2X, player2Y)) {
            Helper.player2Collided = true;
        }
        // Check if the game should end
        Helper.checkGameOver();
    }

    // Determines new direction if movement is blocked
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

    // Draws the monster on screen with appropriate animation frame and facing direction
    public void draw(Graphics g) {
        List<BufferedImage> frames = animations.get(direction);
        if (frames != null && !frames.isEmpty()) {
            BufferedImage frame = frames.get(animFrame % frames.size());

            // Flip the frame horizontally if facing left
            if (direction.equals("side") && !facingRight) {
                frame = flipImage(frame);
            }
            // Draw enemy slightly larger than tile size for better visibility
            g.drawImage(frame, x, y, (int)(tileSize * 1.2), (int)(tileSize * 1.2), null);
        }
    }
}

// Halo subclass of Enemy
class Halo extends Enemy {
    // Timestamp of the last pathfinding computation and path the enemy follows
    private long lastPathTime;
    private Queue<Point> path;

    // Constructor to initialize halo
    public Halo(int x, int y, int tileSize) {
        super(x, y, tileSize);
        // Load animations for each direction
        animations.put("up", loadFrames("../graphics/images/enemies/halo/up"));
        animations.put("down", loadFrames("../graphics/images/enemies/halo/down"));
        animations.put("side", loadFrames("../graphics/images/enemies/halo/side"));
        // Initial direction
        this.direction = "side";
        // Initialize empty path and last path computation
        this.lastPathTime = System.currentTimeMillis();
        this.path = new LinkedList<>();
    }

    @Override
    public void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y) {
        // Cancels movement if game is paused
        if (GamePanel.isPaused) return;
        // Find the closest player (skips if both are collided)
        Point target = closestPlayer(player1X, player1Y, player2X, player2Y);
        if (target == null) return;

        // Only allow movement every 275 ms
        if (System.currentTimeMillis() - lastMoveTime < 275) return;

        int targetX = target.x;
        int targetY = target.y;

        // Recalculate path every 5 seconds or if path is empty
        if (System.currentTimeMillis() - lastPathTime > 5000 || path.isEmpty()) {
            Queue<Point> newPath = findPath(map, x / tileSize, y / tileSize, targetX / tileSize, targetY / tileSize);
            path = (!newPath.isEmpty()) ? newPath : new LinkedList<>();
            lastPathTime = System.currentTimeMillis();
        }

        // Follow the next point in the path
        if (!path.isEmpty()) {
            // Checks if there is a next tile
            Point next = path.peek();
            if (next != null) {
                int row = next.y;
                int col = next.x;

                // Move only if tile is passable and adjacent
                if (isPassable(map[row][col]) && Math.abs(col - x / tileSize) + Math.abs(row - y / tileSize) == 1) {
                    // Consume the next step
                    path.poll();
                    int dx = col - x / tileSize;
                    int dy = row - y / tileSize;

                    // Update direction based on movement
                    if (dx != 0) {
                        direction = "side";
                        facingRight = dx > 0;
                    } else if (dy < 0) {
                        direction = "up";
                    } else {
                        direction = "down";
                    }

                    // Update position and animation state
                    Helper.restoreFruit(map, x / tileSize, y / tileSize);
                    x = col * tileSize;
                    y = row * tileSize;
                    Helper.storeFruit(map, col, row);
                    map[row][col] = 30;
                    animFrame++;
                    lastMoveTime = System.currentTimeMillis();
                } else {
                    // Invalid move, clear the path
                    path.clear();
                }
            }
        }

        // If no path to follow, walk randomly
        if (path.isEmpty()) {
            randomWalk(map);
        }

        // Check for collisions with players (within 1 tile distance)
        int haloCol = x / tileSize;
        int haloRow = y / tileSize;

        int player1Col = player1X / tileSize;
        int player1Row = player1Y / tileSize;

        int player2Col = player2X / tileSize;
        int player2Row = player2Y / tileSize;

        if (Math.abs(haloCol - player1Col) + Math.abs(haloRow - player1Row) <= 1) {
            Helper.player1Collided = true;
        }

        if (Math.abs(haloCol - player2Col) + Math.abs(haloRow - player2Row) <= 1) {
            Helper.player2Collided = true;
        }
        // Check if the game ended
        Helper.checkGameOver();
    }

    private void randomWalk(int[][] map) {
        // Try random directions until a valid move is found
        int[][] directions = {{0,1},{1,0},{0,-1},{-1,0}};
        List<int[]> shuffled = new ArrayList<>(Arrays.asList(directions));
        java.util.Collections.shuffle(shuffled);

        for (int[] d : shuffled) {
            int newX = x + d[0] * tileSize;
            int newY = y + d[1] * tileSize;
            int row = newY / tileSize;
            int col = newX / tileSize;

            if (row >= 0 && col >= 0 && row < map.length && col < map[0].length && isPassable(map[row][col])) {
                direction = (d[1] < 0) ? "up" : (d[1] > 0) ? "down" : "side";
                facingRight = d[0] > 0;

                Helper.restoreFruit(map, x / tileSize, y / tileSize);
                x = newX;
                y = newY;
                Helper.storeFruit(map, x / tileSize, y / tileSize);
                map[y / tileSize][x / tileSize] = 30;
                animFrame++;
                lastMoveTime = System.currentTimeMillis();
                return;
            }
        }
    }

    private Queue<Point> findPath(int[][] map, int sx, int sy, int ex, int ey) {
        // Breadth-First Search (BFS) to find the shortest path on tile grid
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
                // Ensures next tile is valid
                if (nx >= 0 && ny >= 0 && nx < cols && ny < rows && !visited[ny][nx] && isPassable(map[ny][nx])) {
                    q.add(new Point(nx, ny));
                    visited[ny][nx] = true;
                    prev[ny][nx] = curr;
                }
            }
        }

        // Reconstruct path from end to start
        Point curr = new Point(ex, ey);
        Stack<Point> stack = new Stack<>();
        while (curr != null && !(curr.x == sx && curr.y == sy)) {
            stack.push(curr);
            curr = prev[curr.y][curr.x];
        }

        // Removes tile from stack
        while (!stack.isEmpty()) path.add(stack.pop());
        return path;
    }

    public void draw(Graphics g) {
        List<BufferedImage> frames = animations.get(direction);
        if (frames != null && !frames.isEmpty()) {
            BufferedImage frame = frames.get(animFrame % frames.size());

            // Flip image if facing left
            if (direction.equals("side") && !facingRight) {
                frame = flipImage(frame);
            }
            // Draw the sprite slightly lower and bigger for visual effect
            g.drawImage(frame, x, y + (int)(tileSize * 0.5), (int)(tileSize * 1.5), (int)(tileSize * 1.5), null);
        }
    }
}

// IceBreaker subclass of Enemy
class IceBreaker extends Enemy {
    // Variables to keep track of ice breaks and animations
    private boolean breaking = false;
    private long breakStartTime;
    private List<BufferedImage> breakFrames;
    private int breakRow = -1, breakCol = -1;

    public IceBreaker(int x, int y, int tileSize) {
        super(x, y, tileSize);
        // Load directional animations
        animations.put("up", loadFrames("../graphics/images/enemies/icebreaker/up"));
        animations.put("down", loadFrames("../graphics/images/enemies/icebreaker/down"));
        animations.put("side", loadFrames("../graphics/images/enemies/icebreaker/side"));
        direction = "side";
    }

    @Override
    public void update(int[][] map, int player1X, int player1Y, int player2X, int player2Y) {
        // Cancels movement if game is paused
        if (GamePanel.isPaused) return;
        // If currently breaking a tile, wait until break is complete
        if (breaking) {
            if (System.currentTimeMillis() - breakStartTime >= 750) {
                // Finish breaking and replace ice tile with an empty tile
                breaking = false;
                if (breakRow >= 0 && breakCol >= 0 && breakRow < map.length && breakCol < map[0].length) {
                    map[breakRow][breakCol] = 6;
                }
            }
            animFrame++;
            // Skip rest of update while breaking
            return;
        }

        // Movement cooldown every 275ms
        if (System.currentTimeMillis() - lastMoveTime < 275) {
            return;
        }

        // Find the closest player
        Point target = closestPlayer(player1X, player1Y, player2X, player2Y);
        if (target == null) return;

        int targetX = target.x;
        int targetY = target.y;

        // Calculate movement direction based on closest axis
        int dx = 0, dy = 0;
        int xDist = Math.abs(targetX - x);
        int yDist = Math.abs(targetY - y);

        if (xDist >= yDist) {
            dx = Integer.compare(targetX, x);
        } else {
            dy = Integer.compare(targetY, y);
        }

        // Set direction and sprite facing
        if (dx != 0) {
            direction = "side";
            facingRight = dx > 0;
        } else if (dy < 0) direction = "up";
        else direction = "down";

        // Calculate target position
        int newX = x + dx * tileSize;
        int newY = y + dy * tileSize;

        int col = newX / tileSize;
        int row = newY / tileSize;

        // Check bounds and interact with target tile
        if (row >= 0 && col >= 0 && row < map.length && col < map[0].length) {
            int nextTile = map[row][col];
            // Ice tile to break
            if (nextTile == 2) {
                breaking = true;
                breakStartTime = System.currentTimeMillis();
                breakRow = row;
                breakCol = col;
                breakFrames = loadFrames("../graphics/images/enemies/icebreaker/" + direction + "/break_ice");
                // Move to passable tile
            } else if (isPassable(nextTile)) {
                Helper.restoreFruit(map, x / tileSize, y / tileSize);
                x = newX;
                y = newY;
            }
        }

        // Update map and state after move
        Helper.storeFruit(map, x / tileSize, y / tileSize);
        // Mark tile as occupied by IceBreaker
        map[y / tileSize][x / tileSize] = 31;
        animFrame++;
        lastMoveTime = System.currentTimeMillis();

        // Check for collisions with players
        if (collidesWithPlayer(player1X, player1Y)) {
            Helper.player1Collided = true;
        }
        if (collidesWithPlayer(player2X, player2Y)) {
            Helper.player2Collided = true;
        }
        Helper.checkGameOver();
    }

    @Override
    public void draw(Graphics g) {
        // Draw breaking animation
        if (breaking) {
            BufferedImage img = breakFrames.get((animFrame / 5) % breakFrames.size());
            if (direction.equals("side") && !facingRight) {
                img = flipImage(img);
            }
            g.drawImage(img, x, y - tileSize, tileSize, tileSize * 2, null);

            // Draw ice breaking effect
            if (breakRow >= 0 && breakCol >= 0) {
                int drawX = breakCol * tileSize;
                int drawY = breakRow * tileSize;
                int[] iceDirection = directionVector();
                Ice.drawBreakAnimation(g, drawX, drawY, tileSize, animFrame / 5, iceDirection);
            }
            // Draw walking animation
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