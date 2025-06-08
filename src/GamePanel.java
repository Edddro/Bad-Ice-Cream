import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int TILE_SIZE = 35;
    private final int ROWS = 18;
    private final int COLS = 18;
    private int fruitACount = 0;
    private int fruitBCount = 0;

    private final int[][] map = new int[ROWS][COLS];

    private int animFrame = 0;
    private final String player1;
    private final String player2;
    private int player1Score = 0;
    private int player2Score = 0;
    private int player1X, player1Y, player2X, player2Y;
    private String player1Dir = "down";
    private String player2Dir = "down";
    boolean player1Right = false;
    boolean player2Right = false;
    public static boolean gameOver = false;
    public boolean victory = false;
    public static boolean player1GameOver = false;
    public static boolean player2GameOver = false;
    public static int player1GameOverFrame = 0;
    public static int player2GameOverFrame = 0;

    private final Map<String, List<BufferedImage>> fruitAnimations = new HashMap<>();
    private final Map<String, List<BufferedImage>> playerAnimations = new HashMap<>();
    private final Map<String, BufferedImage> staticImages = new HashMap<>();
    private final List<int[]> snowBumpPositions = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    public GamePanel(int level, String player1, String player2) {
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        setFocusable(true);
        loadImages();
        generateSnowBumpPositions();
        loadLevelFromFile(level);
        this.player1 = player1;
        this.player2 = player2;
        setFocusable(true);
        addKeyListener(this);
        requestFocusInWindow();

        int ANIM_DELAY = 200;
        Timer animationTimer = new Timer(ANIM_DELAY, this);
        animationTimer.start();
        Main.playSound("../graphics/sounds/GameMusic.wav");
    }

    private void loadImages() {
        try {
            staticImages.put("corner", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/blue_square.png"))));
            staticImages.put("wall_0", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/blue_box.png"))));
            staticImages.put("wall_1", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/green_box.png"))));
            staticImages.put("wall_2", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/brown_box.png"))));
            staticImages.put("wall_3", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/blue_dotted_box.png"))));
            staticImages.put("snow_bump", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/snow_bumps.png"))));
            staticImages.put("ice", trimWhitespace(ImageIO.read(new File("../graphics/images/map/ice/ice10.png"))));
            staticImages.put("building_0", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/igloo.png"))));
            staticImages.put("building_1", trimWhitespace(ImageIO.read(new File("../graphics/images/map/buildings/snowman.png"))));

            String[] fruitTypes = {"banana", "grapes", "pineapple", "watermelon"};
            for (String fruit : fruitTypes) {
                List<BufferedImage> frames = loadAnimationFrames("../graphics/images/fruit/" + fruit);

                List<BufferedImage> trimmedFrames = new ArrayList<>();
                for (BufferedImage frame : frames) {
                    trimmedFrames.add(trimWhitespace(frame));
                }
                fruitAnimations.put(fruit, trimmedFrames);
            }

            String[] playerTypes = {"vanilla", "chocolate", "strawberry"};
            String[] playerStates = {"up", "down", "side", "game_over", "victory"};
            for (String player : playerTypes) {
                for (String state : playerStates) {
                    List<BufferedImage> frames = loadAnimationFrames("../graphics/images/players/" + player + "/" + state);
                    playerAnimations.put(player + "/" + state, frames);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<BufferedImage> loadAnimationFrames(String dirPath) throws IOException {
        File dir = new File(dirPath);
        File[] allFiles = dir.listFiles();
        List<File> imageFiles = new ArrayList<>();

        if (allFiles != null) {
            for (File file : allFiles) {
                String name = file.getName();
                if (name.endsWith(".png") && !name.contains("_consumed")) {
                    imageFiles.add(file);
                }
            }
        }

        imageFiles.sort(Comparator.comparing(File::getName));
        List<BufferedImage> frames = new ArrayList<>();
        for (File f : imageFiles) {
            frames.add(ImageIO.read(f));
        }
        return frames;
    }

    private void loadLevelFromFile(int levelIndex) {
        String filename = "level" + levelIndex + ".txt";
        try (Scanner scanner = new Scanner(new File(filename))) {
            for (int row = 0; row < ROWS; row++) {
                if (!scanner.hasNextLine()) break;

                String line = scanner.nextLine().trim();
                String[] tokens = line.split("\\s+");

                for (int col = 0; col < COLS && col < tokens.length; col++) {
                    try {
                        map[row][col] = Integer.parseInt(tokens[col]);
                    } catch (NumberFormatException e) {
                        map[row][col] = 0;
                    }

                    int tile = map[row][col];
                    int type = tile / 10;
                    int subtype = tile % 10;

                    if (type == 3 && subtype >= 0 && subtype < 3) {
                        int x = col * TILE_SIZE;
                        int y = row * TILE_SIZE;

                        Enemy enemy = switch (subtype) {
                            case 0 -> new Halo(x, y, TILE_SIZE);
                            case 1 -> new IceBreaker(x, y, TILE_SIZE);
                            case 2 -> new Monster(x, y, TILE_SIZE);
                            default -> null;
                        };

                        enemies.add(enemy);
                    }

                    if (type / 10 == 5) {
                        if (subtype % 10 == 0) {
                            fruitACount++;
                        } else if (subtype % 10 == 1) {
                            fruitBCount++;
                        }
                    }

                    if (type == 4) {
                        if (subtype == 0) {
                            player1X = col * TILE_SIZE;
                            player1Y = row * TILE_SIZE;
                        } else if (subtype == 1) {
                            player2X = col * TILE_SIZE;
                            player2Y = row * TILE_SIZE;
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Level file not found: " + filename);
            for (int row = 0; row < ROWS; row++)
                Arrays.fill(map[row], 0);
        }
    }

    private void generateSnowBumpPositions() {
        BufferedImage bump = staticImages.get("snow_bump");
        if (bump != null) {
            int bumpWidth = bump.getWidth();
            int bumpHeight = bump.getHeight();

            int numBumps = 20;
            for (int i = 0; i < numBumps; i++) {
                int randX = (int)(Math.random() * (COLS * TILE_SIZE - bumpWidth));
                int randY = (int)(Math.random() * (ROWS * TILE_SIZE - bumpHeight));
                snowBumpPositions.add(new int[]{randX, randY});
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(246, 254, 254, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        BufferedImage bump = staticImages.get("snow_bump");
        if (bump != null) {
            for (int[] pos : snowBumpPositions) {
                g.drawImage(bump, pos[0], pos[1], null);
            }
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;
                int tile = map[row][col];

                int type, subtype;
                if (tile < 10) {
                    type = tile;
                    subtype = 0;
                } else if (tile >= 100) {
                    type = tile / 100;
                    subtype = tile % 100;
                } else {
                    type = tile / 10;
                    subtype = tile % 10;
                }

                switch (type) {
                    case 0 -> {
                        if (row % 2 == 0 && col % 2 == 0) {
                            drawImage(g, "corner", x, y, TILE_SIZE * 2, TILE_SIZE * 2);
                        }
                    }
                    case 1 -> drawImage(g, "wall_" + subtype, x, y, TILE_SIZE, TILE_SIZE);
                    case 2 -> drawImage(g, "ice", x, y, TILE_SIZE, TILE_SIZE);
                    case 3 -> drawEnemy(g);
                    case 4 -> drawPlayer(g, subtype, x, y);
                    case 5 -> drawFruit(g, subtype, x, y);
                    case 7 -> {
                        if (row % 4 == 0 && col % 4 == 0) {
                            drawImage(g, "building_" + subtype, x - TILE_SIZE, (int)(y - TILE_SIZE * 1.5), TILE_SIZE * 4, TILE_SIZE * 4);
                        }
                    }
                }
            }
        }
    }

    private void drawImage(Graphics g, String key, int x, int y, int w, int h) {
        BufferedImage original = staticImages.get(key);
        if (original != null) {
            g.drawImage(original, x, y, w, h, null);
        }
    }

    private BufferedImage trimWhitespace(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int top = 0, left = 0, right = width - 1, bottom = height - 1;
        boolean found = false;

        for (int y = 0; y < height && !found; y++)
            for (int x = 0; x < width; x++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { top = y; found = true; break; }

        found = false;
        for (int y = height - 1; y >= 0 && !found; y--)
            for (int x = 0; x < width; x++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { bottom = y; found = true; break; }

        found = false;
        for (int x = 0; x < width && !found; x++)
            for (int y = 0; y < height; y++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { left = x; found = true; break; }

        found = false;
        for (int x = width - 1; x >= 0 && !found; x--)
            for (int y = 0; y < height; y++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { right = x; found = true; break; }

        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    private void drawEnemy(Graphics g) {
        for (Enemy enemy : enemies) {
            enemy.update(map, player1X, player1Y, player2X, player2Y);
            enemy.draw(g);
        }
    }

    private void drawFruit(Graphics g, int subtype, int x, int y) {
        int fruitIndex = subtype / 10;
        int fruitSet = subtype % 10;

        if ((fruitSet == 0 && fruitACount > 0) || (fruitSet == 1 && fruitACount == 0)) {
            String[] fruits = {"banana", "grapes", "pineapple", "watermelon"};
            if (fruitIndex >= 0 && fruitIndex < fruits.length) {
                List<BufferedImage> frames = fruitAnimations.get(fruits[fruitIndex]);
                if (frames != null && !frames.isEmpty()) {
                    BufferedImage frame = frames.get(animFrame % frames.size());

                    if (fruits[fruitIndex].equals("watermelon")) {
                        int offset = (int)(TILE_SIZE * 0.18);
                        g.drawImage(frame, x + offset, y + offset, TILE_SIZE - 10, TILE_SIZE - 10, null);
                    } else {
                        g.drawImage(frame, x, y, TILE_SIZE - 10, TILE_SIZE - 10, null);
                    }
                }
            }
        }
    }

    private void drawPlayer(Graphics g, int subtype, int x, int y) {
        String playerType = (subtype == 1) ? player2 : player1;
        String direction = (subtype == 1) ? player2Dir : player1Dir;
        boolean facingRight = (subtype == 1) ? player2Right : player1Right;

        boolean isGameOver = (subtype == 0) ? player1GameOver : player2GameOver;
        int gameOverFrame = (subtype == 0) ? player1GameOverFrame : player2GameOverFrame;

        if (isGameOver) {
            String animationKey = playerType + "/game_over";
            List<BufferedImage> frames = playerAnimations.get(animationKey);
            if (frames != null && frames.size() >= 15) {
                int frameIndex = Math.min(gameOverFrame / 5, 14);
                g.drawImage(frames.get(frameIndex), x, y, TILE_SIZE, TILE_SIZE, null);

                if (frameIndex < 14) {
                    if (subtype == 0) player1GameOverFrame++;
                    else player2GameOverFrame++;
                }
            }
            return;
        }

        String animKey = playerType + "/" + direction;
        List<BufferedImage> frames = playerAnimations.get(animKey);
        if (frames != null && !frames.isEmpty()) {
            BufferedImage frame = frames.get(animFrame % frames.size());
            if (direction.equals("side") && !facingRight) {
                frame = flipImage(frame);
            }
            g.drawImage(frame, x, y, TILE_SIZE, TILE_SIZE, null);
        }
    }

    protected BufferedImage flipImage(BufferedImage img) {
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = flipped.createGraphics();
        g2.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
        g2.dispose();
        return flipped;
    }

    private void checkFruitCollision() {
        int p1Row = player1Y / TILE_SIZE;
        int p1Col = player1X / TILE_SIZE;
        int p2Row = player2Y / TILE_SIZE;
        int p2Col = player2X / TILE_SIZE;

        if (isValidPosition(p1Row, p1Col)) {
            int tile = map[p1Row][p1Col];
            if (tile / 100 == 5) {
                player1Score++;
                map[p1Row][p1Col] = 0;
            }
        }

        if (isValidPosition(p2Row, p2Col)) {
            int tile = map[p2Row][p2Col];
            if (tile / 100 == 5) {
                player2Score++;
                map[p2Row][p2Col] = 0;
            }
        }
    }

    protected int[] directionVector(String direction, Boolean facingRight) {
        return switch (direction) {
            case "up" -> new int[]{0, -1};
            case "down" -> new int[]{0, 1};
            case "side" -> facingRight ? new int[]{1, 0} : new int[]{-1, 0};
            default -> new int[]{0, 1};
        };
    }
    private void movePlayer(int player, String playerDir, boolean facingRight) {
        if ((player == 1 && player1GameOver) || (player == 2 && player2GameOver)) return;

        int[] d = directionVector(playerDir, facingRight);

        int x = (player == 1) ? player1X : player2X;
        int y = (player == 1) ? player1Y : player2Y;

        int newCol = (x + d[0] * TILE_SIZE) / TILE_SIZE;
        int newRow = (y + d[1] * TILE_SIZE) / TILE_SIZE;

        if (!isValidPosition(newRow, newCol)) return;

        int tile = map[newRow][newCol];

        if (tile / 100 == 5 || tile == 6) {
            int oldCol = x / TILE_SIZE;
            int oldRow = y / TILE_SIZE;
            map[oldRow][oldCol] = 6;

            if (player == 1) {
                player1X = newCol * TILE_SIZE;
                player1Y = newRow * TILE_SIZE;
                map[newRow][newCol] = 40;
            } else {
                player2X = newCol * TILE_SIZE;
                player2Y = newRow * TILE_SIZE;
                map[newRow][newCol] = 41;
            }

            checkFruitCollision();
        }
    }

    public static void victory(){
        System.out.println("VICTORY");
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent e) {
        int code = e.getKeyCode();

        // Player 1 controls
        if (!player1GameOver) {
            switch (code) {
                case java.awt.event.KeyEvent.VK_LEFT:
                    player1Dir = "side";
                    player1Right = false;
                    movePlayer(1, player1Dir, player1Right);
                    break;
                case java.awt.event.KeyEvent.VK_RIGHT:
                    player1Dir = "side";
                    player1Right = true;
                    movePlayer(1, player1Dir, player1Right);
                    break;
                case java.awt.event.KeyEvent.VK_UP:
                    player1Dir = "up";
                    movePlayer(1, player1Dir, false);
                    break;
                case java.awt.event.KeyEvent.VK_DOWN:
                    player1Dir = "down";
                    movePlayer(1, player1Dir, false);
                    break;
                case java.awt.event.KeyEvent.VK_SPACE:
                    int[] d1 = directionVector(player1Dir, player1Right);
                    int targetTile1 = map[(player1Y + d1[1] * TILE_SIZE) / TILE_SIZE][(player1X + d1[0] * TILE_SIZE) / TILE_SIZE];
                    if (targetTile1 == 2) {
                        Ice.breakIce(player1X, player1Y, d1[0], d1[1], map, TILE_SIZE);
                    } else {
                        Ice.formIce(player1X, player1Y, d1[0], d1[1], map, TILE_SIZE);
                    }
                    break;
            }
        }

        // Player 2 controls
        if (!player2GameOver) {
            switch (code) {
                case java.awt.event.KeyEvent.VK_A:
                    player2Dir = "side";
                    player2Right = false;
                    movePlayer(2, player2Dir, player2Right);
                    break;
                case java.awt.event.KeyEvent.VK_D:
                    player2Dir = "side";
                    player2Right = true;
                    movePlayer(2, player2Dir, player2Right);
                    break;
                case java.awt.event.KeyEvent.VK_W:
                    player2Dir = "up";
                    movePlayer(2, player2Dir, false);
                    break;
                case java.awt.event.KeyEvent.VK_S:
                    player2Dir = "down";
                    movePlayer(2, player2Dir, false);
                    break;
                case java.awt.event.KeyEvent.VK_F:
                    int[] d2 = directionVector(player2Dir, player2Right);
                    int targetTile2 = map[(player2Y + d2[1] * TILE_SIZE) / TILE_SIZE][(player2X + d2[0] * TILE_SIZE) / TILE_SIZE];
                    if (targetTile2 == 2) {
                        Ice.breakIce(player2X, player2Y, d2[0], d2[1], map, TILE_SIZE);
                    } else {
                        Ice.formIce(player2X, player2Y, d2[0], d2[1], map, TILE_SIZE);
                    }
                    break;
            }
        }

        repaint();
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {}

    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        animFrame++;
        repaint();
    }
}