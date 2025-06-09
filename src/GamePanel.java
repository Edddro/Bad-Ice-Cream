import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Timer;

// Main game class, handles game screen and controls
public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener  {
    // Constants for tile size and grid dimensions
    private final int TILE_SIZE = 35;
    private final int ROWS = 18;
    private final int COLS = 18;

    // Fruit counts for UI display logic
    private int fruitACount = 0;
    private int fruitBCount = 0;

    // 2D map representing the level's tile data
    private final int[][] map = new int[ROWS][COLS];

    // Animation frame tracker
    private int animFrame = 0;

    // Level identifier and player identifiers
    int level;
    private final String player1;
    private final String player2;

    // Player scores
    private int player1Score = 0;
    private int player2Score = 0;

    // Player positions and movement directions
    private int player1X, player1Y, player2X, player2Y;
    private String player1Dir = "down";
    private String player2Dir = "down";
    boolean player1Right = false;
    boolean player2Right = false;

    // Currently used fruit types
    private String fruitA, fruitB;

    // Tracks currently pressed keys for smoother movement
    private final Set<Integer> pressedKeys = new HashSet<>();

    // Timing control for player movement delay
    private long lastMoveTimeP1 = 0;
    private long lastMoveTimeP2 = 0;
    private static final int MOVE_DELAY_MS = 150;

    // Stores what's under players so tiles can be restored
    int[][] tileUnderPlayer = new int[map.length][map[0].length];

    // Clickable UI bounds for pause/restart/menu
    private Rectangle pauseBounds, restartBounds, resumeRect, menuRect;

    // Pause flag for game state
    public static boolean isPaused = false;

    // Maps for animations and images
    private final Map<String, List<BufferedImage>> fruitAnimations = new HashMap<>();
    private final Map<String, List<BufferedImage>> playerAnimations = new HashMap<>();
    private final Map<String, BufferedImage> staticImages = new HashMap<>();
    private final List<int[]> snowBumpPositions = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    // UI elements
    BufferedImage pauseIcon, restartIcon, footerFrame, frameImage;
    Map<String, BufferedImage> timerIcon = new HashMap<>();
    Map<String, BufferedImage> fruitDisplayImages = new HashMap<>();

    // Time management variables
    long levelStartTime = System.currentTimeMillis();
    long levelDuration = 2 * 60 * 1000;
    long pauseStartTime = 0;
    long totalPausedTime = 0;

    // Constructor initializes level, players, images, animations, and timers
    public GamePanel(int level, String player1, String player2) {
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        requestFocusInWindow();

        // Assign constructor arguments and reset game state
        this.player1 = player1;
        this.player2 = player2;
        this.level = level;

        // Reset variables for new level
        player1Score = 0;
        player2Score = 0;
        player1Dir = "down";
        player2Dir = "down";
        player1Right = false;
        player2Right = false;
        fruitACount = 0;
        fruitBCount = 0;
        GameState.reset();
        lastMoveTimeP1 = 0;
        lastMoveTimeP2 = 0;
        levelStartTime = System.currentTimeMillis();
        pressedKeys.clear();
        enemies.clear();
        snowBumpPositions.clear();

        // Load game assets and level data
        loadImages();
        generateSnowBumpPositions();
        loadLevelFromFile(level);

        // Start animation and movement timers
        int ANIM_DELAY = 200;
        Timer animationTimer = new Timer(ANIM_DELAY, this);
        animationTimer.start();
        Timer movementTimer = new Timer(50, _ -> updatePlayerMovement());
        movementTimer.start();

        // Play background music
        Main.stopSound();
        Main.playSound("../graphics/sounds/GameMusic.wav", true);
    }

    // Load all required images and animations from disk
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
            staticImages.put("footerFrame", trimWhitespace(ImageIO.read(new File("../graphics/images/map/frames/small_wide_frame.png"))));
            staticImages.put("pauseIcon", trimWhitespace(ImageIO.read(new File("../graphics/images/map/display/pause.png"))));
            staticImages.put("restartIcon", trimWhitespace(ImageIO.read(new File("../graphics/images/map/display/restart.png"))));
            footerFrame = staticImages.get("footerFrame");
            pauseIcon = staticImages.get("pauseIcon");
            restartIcon = staticImages.get("restartIcon");
            frameImage = ImageIO.read(new File("../graphics/images/map/frames/blank_rectangle_frame.png"));

            // Load animated fruits and their static display versions
            String[] fruitTypes = {"banana", "grapes", "pineapple", "watermelon"};
            for (String fruit : fruitTypes) {
                List<BufferedImage> frames = loadAnimationFrames("../graphics/images/fruit/" + fruit);

                List<BufferedImage> trimmedFrames = new ArrayList<>();
                for (BufferedImage frame : frames) {
                    trimmedFrames.add(trimWhitespace(frame));
                }
                fruitAnimations.put(fruit, trimmedFrames);
                fruitDisplayImages.put(fruit, ImageIO.read(new File("../graphics/images/fruit/" + fruit + "_consumed_display.png")));
            }

            // Load animated timer frames
            List<BufferedImage> timerFrames = loadAnimationFrames("../graphics/images/map/timer");
            for (int i = 0; i < timerFrames.size(); i++) {
                timerIcon.put("frame" + i, timerFrames.get(i));
            }

            // Load animated player states for each type and direction
            String[] playerTypes = {"vanilla", "chocolate", "strawberry"};
            String[] playerStates = {"up", "down", "side", "game_over", "victory"};
            for (String player : playerTypes) {
                for (String state : playerStates) {
                    List<BufferedImage> frames = loadAnimationFrames("../graphics/images/players/" + player + "/" + state);
                    playerAnimations.put(player + "/" + state, frames);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // Loads all PNG frames in a given folder as animation frames
    private List<BufferedImage> loadAnimationFrames(String dirPath) throws IOException {
        File dir = new File(dirPath);
        File[] allFiles = dir.listFiles();
        List<File> imageFiles = new ArrayList<>();

        if (allFiles != null) {
            for (File file : allFiles) {
                String name = file.getName();
                if (name.endsWith(".png")) {
                    imageFiles.add(file);
                }
            }
        }

        // Ensure animation frames are loaded in correct sequence
        imageFiles.sort(Comparator.comparing(File::getName));
        List<BufferedImage> frames = new ArrayList<>();
        for (File f : imageFiles) {
            frames.add(ImageIO.read(f));
        }
        return frames;
    }

    // Loads the map and objects from a level file (e.g., level1.txt)
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

                    // Add enemy objects based on tile encoding
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

                    // Count fruits to determine footer animation behavior
                    if (type / 10 == 5) {
                        if (subtype % 10 == 0) {
                            fruitACount++;
                            fruitA = fruitType(type);
                        } else if (subtype % 10 == 1) {
                            fruitBCount++;
                            fruitB = fruitType(type);
                        }
                    }

                    // Initialize player positions based on spawn tiles
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
            // Fallback to blank map
        } catch (FileNotFoundException e) {
            System.err.println("Level file not found: " + filename);
            for (int row = 0; row < ROWS; row++)
                Arrays.fill(map[row], 6);
        }
    }

    // Displays player score and timer at the top of the screen
    private void drawHeader(Graphics g) {
        int baseY = 10;
        int padding = TILE_SIZE + 5;

        Graphics2D g2d = (Graphics2D) g;
        Font scoreFont = new Font("Arial", Font.BOLD, 20);
        Font timerFont = new Font("Arial", Font.BOLD, 18);

        // Draw player 1 avatar
        List<BufferedImage> p1Frames = playerAnimations.get(player1 + "/down");
        BufferedImage p1Frame = p1Frames.get(animFrame % p1Frames.size());
        g2d.drawImage(p1Frame, padding, baseY, TILE_SIZE, TILE_SIZE, null);

        // Draw player 1 score (6 digits)
        String score1 = String.format("%06d", player1Score);
        int score1X = padding + TILE_SIZE + 5;
        int score1Y = baseY + TILE_SIZE / 2 + baseY;
        drawOutlinedText(g2d, score1, score1X, score1Y, Color.YELLOW, scoreFont);

        // Draw player 2 avatar
        List<BufferedImage> p2Frames = playerAnimations.get(player2 + "/down");
        BufferedImage p2Frame = p2Frames.get(animFrame % p2Frames.size());
        g2d.drawImage(p2Frame, padding + TILE_SIZE * 4, baseY, TILE_SIZE, TILE_SIZE, null);

        // Draw player 1 score (6 digits)
        String score2 = String.format("%06d", player2Score);
        int score2X = padding + TILE_SIZE * 4 + padding + 5;
        int score2Y = baseY + TILE_SIZE / 2 + baseY;
        drawOutlinedText(g2d, score2, score2X, score2Y, Color.PINK, scoreFont);

        // Calculate remaining time
        long timeLeft;
        int seconds;
        if (isPaused) {
            timeLeft = Math.max(0, levelDuration - (pauseStartTime - levelStartTime - totalPausedTime));
        } else {
            timeLeft = Math.max(0, levelDuration - (System.currentTimeMillis() - levelStartTime - totalPausedTime));
        }
        seconds = (int) (timeLeft / 1000);
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);

        // Draw animated timer icon
        BufferedImage timerFrame = timerIcon.get("frame" + (animFrame % timerIcon.size()));
        int timerX = getWidth() / 2 - TILE_SIZE / 2;
        g2d.drawImage(timerFrame, timerX - padding / 9 + 15, baseY, TILE_SIZE, TILE_SIZE, null);

        // Draw remaining time text beside timer
        int timerTextX = timerX - padding / 9 + TILE_SIZE + 25;
        int timerTextY = baseY + TILE_SIZE / 2 + baseY;
        drawOutlinedText(g2d, timeStr, timerTextX, timerTextY, Color.WHITE, timerFont);

        // Define bounds and draw restart and pause icons (used for click detection)
        int iconSize = TILE_SIZE - 15;
        int restartX = getWidth() - 2 * iconSize - 50;
        int pauseX = getWidth() - iconSize - 100;

        restartBounds = new Rectangle(restartX, baseY + 8, iconSize, iconSize);
        pauseBounds = new Rectangle(pauseX, baseY + 8, iconSize, iconSize);
        g2d.drawImage(restartIcon, restartX, baseY + 8, iconSize, iconSize, null);
        g2d.drawImage(pauseIcon, pauseX, baseY + 8, iconSize, iconSize, null);
    }

    // Displays current fruit on the map at the bottom of the screen
    private void drawFooter(Graphics g) {
        // Draw the wooden frame of the footer UI
        int frameWidth = TILE_SIZE * 12;
        int frameHeight = (int)(TILE_SIZE * 1.5);
        int frameX = getWidth() / 2 - TILE_SIZE * 6;
        int frameY = getHeight() - footerFrame.getHeight() - (int)(TILE_SIZE * 0.4);

        g.drawImage(footerFrame, frameX, frameY, frameWidth, frameHeight, null);

        // Determine which fruit type should animate
        List<BufferedImage> fruitAFrames;
        List<BufferedImage> fruitBFrames;

        // If fruitA is on the map, animate fruitA, fruitB is static
        if (fruitACount > 0) {
            fruitAFrames = fruitAnimations.get(fruitA);
            fruitBFrames = List.of(fruitDisplayImages.get(fruitB));
            // Otherwise, fruitA is static and fruitB is animated
        } else {
            fruitAFrames = List.of(fruitDisplayImages.get(fruitA));
            fruitBFrames = fruitAnimations.get(fruitB);
        }

        // Get current animation frame for each fruit
        BufferedImage fruitAFrame = fruitAFrames.get(animFrame % fruitAFrames.size());
        BufferedImage fruitBFrame = fruitBFrames.get(animFrame % fruitBFrames.size());

        // Calculate position and spacing to make fruits centered in the frame
        int fruitSize = TILE_SIZE;
        int spacing = 10;
        int totalWidth = fruitSize * 2 + spacing;

        int startX = frameX + (frameWidth - totalWidth) / 2;
        int fruitY = frameY + (frameHeight - fruitSize) / 2;

        // Draw fruits side by side
        g.drawImage(fruitAFrame, startX, fruitY, fruitSize, fruitSize, null);
        g.drawImage(fruitBFrame, startX + fruitSize + spacing, fruitY, fruitSize, fruitSize, null);
    }

    private void drawOutlinedText(Graphics2D g, String text, int x, int y, Color fillColor, Font font) {
        // Draws text with black outline by rendering multiple offsets around the text
        g.setFont(font);
        g.setColor(Color.BLACK);

        // Offsets for 8 directions to simulate outline
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x + 1, y + 1);
        g.drawString(text, x - 1, y);
        g.drawString(text, x + 1, y);
        g.drawString(text, x, y - 1);
        g.drawString(text, x, y + 1);

        // Draw filled center text
        g.setColor(fillColor);
        g.drawString(text, x, y);
    }

    // Returns the fruit name based on its type value.
    private String fruitType(int type) {
        // Uses last digit to determine the fruit
        return switch (type % 10) {
            case 1 -> "grapes";
            case 2 -> "pineapple";
            case 3 -> "watermelon";
            default -> "banana";
        };
    }

    // Randomly generates positions on the screen for decorative snow bump images.
    private void generateSnowBumpPositions() {
        BufferedImage bump = staticImages.get("snow_bump");
        if (bump != null) {
            int bumpWidth = bump.getWidth();
            int bumpHeight = bump.getHeight();

            // Total number of snow bumps to place
            int numBumps = 20;
            for (int i = 0; i < numBumps; i++) {
                // Adds the position of the snow bump to snowBumpPositions
                int randX = (int)(Math.random() * (COLS * TILE_SIZE - bumpWidth));
                int randY = (int)(Math.random() * (ROWS * TILE_SIZE - bumpHeight));
                snowBumpPositions.add(new int[]{randX, randY});
            }
        }
    }

    // Main rendering method for the game panel.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Set background color
        g.setColor(new Color(246, 254, 254, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw all randomly placed snow bump decorations
        BufferedImage bump = staticImages.get("snow_bump");
        if (bump != null) {
            for (int[] pos : snowBumpPositions) {
                g.drawImage(bump, pos[0], pos[1], null);
            }
        }

        // Draw the tile map
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;
                int tile = map[row][col];

                int type, subtype;
                if (tile < 10) {
                    type = tile;
                    subtype = 0;
                    // Fruit tile
                } else if (tile >= 100) {
                    type = tile / 100;
                    subtype = tile % 100;
                    // Player tile, enemy tile, wall tile
                } else {
                    type = tile / 10;
                    subtype = tile % 10;
                }

                // Draw tile content based on type
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

        // UI overlay
        drawFooter(g);
        drawHeader(g);

        // Game state overlays
        if (isPaused) pause(g);
        if (GameState.victory) victory(g);
        else if (GameState.gameOver) gameOver(g);
    }

    // Draws a specific image from the static image map
    private void drawImage(Graphics g, String key, int x, int y, int w, int h) {
        BufferedImage original = staticImages.get(key);
        if (original != null) {
            g.drawImage(original, x, y, w, h, null);
        }
    }

    // Removes surrounding transparent pixels from an image
    private BufferedImage trimWhitespace(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int top = 0, left = 0, right = width - 1, bottom = height - 1;
        boolean found = false;

        // Find top boundary
        for (int y = 0; y < height && !found; y++)
            for (int x = 0; x < width; x++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { top = y; found = true; break; }

        // Find bottom boundary
        found = false;
        for (int y = height - 1; y >= 0 && !found; y--)
            for (int x = 0; x < width; x++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { bottom = y; found = true; break; }

        // Find left boundary
        found = false;
        for (int x = 0; x < width && !found; x++)
            for (int y = 0; y < height; y++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { left = x; found = true; break; }

        // Find right boundary
        found = false;
        for (int x = width - 1; x >= 0 && !found; x--)
            for (int y = 0; y < height; y++)
                if ((image.getRGB(x, y) >> 24) != 0x00) { right = x; found = true; break; }

        // Crop the image
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    // Updates and draws all enemies on the screen
    private void drawEnemy(Graphics g) {
        // Loops through all the enemies
        for (Enemy enemy : enemies) {
            enemy.update(map, player1X, player1Y, player2X, player2Y);
            enemy.draw(g);
        }
    }

    // Draws fruit with animated frames based on the subtype
    private void drawFruit(Graphics g, int subtype, int x, int y) {
        int fruitIndex = subtype / 10;
        int fruitSet = subtype % 10;

        // Determine which fruit set to draw based on game logic
        if ((fruitSet == 0 && fruitACount > 0) || (fruitSet == 1 && fruitACount == 0)) {
            String[] fruits = {"banana", "grapes", "pineapple", "watermelon"};
            if (fruitIndex >= 0 && fruitIndex < fruits.length) {
                List<BufferedImage> frames = fruitAnimations.get(fruits[fruitIndex]);
                if (frames != null && !frames.isEmpty()) {
                    BufferedImage frame = frames.get(animFrame % frames.size());

                    // Special offset for watermelon
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

    // Draws player character with animation frames depending on state
    private void drawPlayer(Graphics g, int subtype, int x, int y) {
        String playerType = (subtype == 1) ? player2 : player1;
        String direction = (subtype == 1) ? player2Dir : player1Dir;
        boolean facingRight = (subtype == 1) ? player2Right : player1Right;

        boolean isGameOver = (subtype == 0) ? GameState.player1GameOver : GameState.player2GameOver;
        int gameOverFrame = (subtype == 0) ? GameState.player1GameOverFrame : GameState.player2GameOverFrame;

        // Draw game-over animation
        if (isGameOver) {
            String animationKey = playerType + "/game_over";
            List<BufferedImage> frames = playerAnimations.get(animationKey);
            if (frames != null && frames.size() >= 15) {
                int frameIndex = Math.min(gameOverFrame / 5, 14);
                g.drawImage(frames.get(frameIndex), x, y, TILE_SIZE, TILE_SIZE, null);

                // Advance animation frame counter
                if (frameIndex < 14) {
                    if (subtype == 0) GameState.player1GameOverFrame++;
                    else GameState.player2GameOverFrame++;
                }
            }
            return;
        }

        // Draw victory animation
        if (GameState.victory) {
            String animationKey = playerType + "/victory";
            List<BufferedImage> frames = playerAnimations.get(animationKey);
            if (frames != null && !frames.isEmpty()) {
                BufferedImage frame = frames.get(animFrame % frames.size());
                g.drawImage(frame, x, y, TILE_SIZE, TILE_SIZE, null);
            }
            return;
        }

        // Draw regular movement animation
        String animKey = playerType + "/" + direction;
        List<BufferedImage> frames = playerAnimations.get(animKey);
        if (frames != null && !frames.isEmpty()) {
            BufferedImage frame = frames.get(animFrame % frames.size());

            // Flip sprite horizontally if not facing right
            if (direction.equals("side") && !facingRight) {
                frame = flipImage(frame);
            }
            g.drawImage(frame, x, y, TILE_SIZE, TILE_SIZE, null);
        }
    }

    // Flips an image horizontally (used for rendering sprites facing opposite directions)
    protected BufferedImage flipImage(BufferedImage img) {
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = flipped.createGraphics();
        // Draw the image with negative width to flip it
        g2.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
        g2.dispose();
        return flipped;
    }

    // Checks if a player has collided with a fruit tile, adds score, and updates map
    private void checkFruitCollision(int player, int playerX, int playerY) {
        int pRow = playerY / TILE_SIZE;
        int pCol = playerX / TILE_SIZE;

        if (isValidPosition(pRow, pCol)) {
            int tile = map[pRow][pCol];

            // Checks if tile is fruit and if that fruit is currently valid to collect
            if (tile / 100 == 5 && ((tile % 10 == 0 && fruitACount > 0) || (tile % 10 == 1 && fruitACount == 0))) {
                long elapsedMillis = System.currentTimeMillis() - levelStartTime - totalPausedTime;
                int minutesElapsed = (int) (elapsedMillis / 60000);
                int scoreToAdd = 100 * Math.max(1, minutesElapsed);

                // Add score to appropriate player
                if (player == 1) player1Score += scoreToAdd;

                // Replace fruit tile with regular ground and update counters
                else player2Score += scoreToAdd;
                map[pRow][pCol] = 6;
                if (tile % 10 == 0) fruitACount--;
                else fruitBCount--;
                Main.playSound("../graphics/sounds/FoodCollect.wav", false);

                // Trigger victory if all fruit collected
                if(fruitACount == 0 && fruitBCount == 0) {
                    GameState.victory = true;
                }
            }
        }
    }

    // Converts direction and facing side into x, y movement vector
    protected int[] directionVector(String direction, Boolean facingRight) {
        return switch (direction) {
            case "up" -> new int[]{0, -1};
            case "side" -> facingRight ? new int[]{1, 0} : new int[]{-1, 0};
            // Down
            default -> new int[]{0, 1};
        };
    }

    // Handles logic for player movement and tile updates
    private void movePlayer(int player, String playerDir, boolean facingRight) {
        // Stop movement if the player is game-over or game is won
        if ((player == 1 && GameState.player1GameOver) || (player == 2 && GameState.player2GameOver) || GameState.victory) return;

        // Get players current coordinates and direction
        int[] d = directionVector(playerDir, facingRight);
        int x = (player == 1) ? player1X : player2X;
        int y = (player == 1) ? player1Y : player2Y;

        // Calculate new tile coordinates
        int newCol = (x + d[0] * TILE_SIZE) / TILE_SIZE;
        int newRow = (y + d[1] * TILE_SIZE) / TILE_SIZE;

        if (!isValidPosition(newRow, newCol)) return;

        int tile = map[newRow][newCol];

        // Only allow moving onto ground, fruit, or frozen players (if allowed)
        if (tile / 100 == 5 || tile == 6 || (tile / 10 == 4 && tile % 10 == 0 && GameState.player1GameOver) || (tile / 10 == 4 && tile % 10 == 1 && GameState.player2GameOver)) {
            int oldCol = x / TILE_SIZE;
            int oldRow = y / TILE_SIZE;

            // Restore original tile under old player position
            if (tileUnderPlayer[oldRow][oldCol] != 0) {
                map[oldRow][oldCol] = tileUnderPlayer[oldRow][oldCol];
            } else {
                map[oldRow][oldCol] = 6;
            }

            // Update player position and mark new tile
            if (player == 1) {
                player1X = newCol * TILE_SIZE;
                player1Y = newRow * TILE_SIZE;
                checkFruitCollision(1, player1X, player1Y);
                tileUnderPlayer[newRow][newCol] = map[newRow][newCol];
                map[newRow][newCol] = 40;
            } else {
                player2X = newCol * TILE_SIZE;
                player2Y = newRow * TILE_SIZE;
                checkFruitCollision(2, player2X, player2Y);
                tileUnderPlayer[newRow][newCol] = map[newRow][newCol];
                map[newRow][newCol] = 41;
            }
        }
    }

    // Updates movement for both players based on key input and timing
    private void updatePlayerMovement() {
        long currentTime = System.currentTimeMillis();

        // Player 1 input
        if (!GameState.player1GameOver  && currentTime - lastMoveTimeP1 >= MOVE_DELAY_MS && !isPaused) {
            if (pressedKeys.contains(KeyEvent.VK_LEFT)) {
                player1Dir = "side";
                player1Right = false;
                movePlayer(1, player1Dir, false);
                lastMoveTimeP1 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_RIGHT)) {
                player1Dir = "side";
                player1Right = true;
                movePlayer(1, player1Dir, true);
                lastMoveTimeP1 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_UP)) {
                player1Dir = "up";
                movePlayer(1, player1Dir, false);
                lastMoveTimeP1 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_DOWN)) {
                player1Dir = "down";
                movePlayer(1, player1Dir, false);
                lastMoveTimeP1 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_SPACE)) {
                int[] d1 = directionVector(player1Dir, player1Right);
                int targetTile1 = map[(player1Y + d1[1] * TILE_SIZE) / TILE_SIZE][(player1X + d1[0] * TILE_SIZE) / TILE_SIZE];
                // If next tile is ice, break the ice
                if (targetTile1 == 2) {
                    Ice.breakIce(player1X, player1Y, d1[0], d1[1], map, TILE_SIZE);
                    // Otherwise form ice
                } else {
                    Ice.formIce(player1X, player1Y, d1[0], d1[1], map, TILE_SIZE);
                }
                lastMoveTimeP1 = currentTime;
            }
        }

        // Player 2 input
        if (!GameState.player2GameOver && currentTime - lastMoveTimeP2 >= MOVE_DELAY_MS && !isPaused) {
            if (pressedKeys.contains(KeyEvent.VK_A)) {
                player2Dir = "side";
                player2Right = false;
                movePlayer(2, player2Dir, false);
                lastMoveTimeP2 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_D)) {
                player2Dir = "side";
                player2Right = true;
                movePlayer(2, player2Dir, true);
                lastMoveTimeP2 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_W)) {
                player2Dir = "up";
                movePlayer(2, player2Dir, false);
                lastMoveTimeP2 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_S)) {
                player2Dir = "down";
                movePlayer(2, player2Dir, false);
                lastMoveTimeP2 = currentTime;
            } else if (pressedKeys.contains(KeyEvent.VK_F)) {
                int[] d2 = directionVector(player2Dir, player2Right);
                int targetTile2 = map[(player2Y + d2[1] * TILE_SIZE) / TILE_SIZE][(player2X + d2[0] * TILE_SIZE) / TILE_SIZE];
                // If next tile is ice, break the ice
                if (targetTile2 == 2) {
                    Ice.breakIce(player2X, player2Y, d2[0], d2[1], map, TILE_SIZE);
                    // Otherwise form ice
                } else {
                    Ice.formIce(player2X, player2Y, d2[0], d2[1], map, TILE_SIZE);
                }
                lastMoveTimeP2 = currentTime;
            }
        }

        repaint();
    }

    // Draws the pause menu overlay with buttons
    private void pause(Graphics g) {
        int frameW = 400;
        int frameH = 200;
        int frameX = (getWidth() - frameW) / 2;
        int frameY = (getHeight() - frameH) / 2;
        Graphics2D g2d = (Graphics2D) g;

        g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

        Font timerFont = new Font("Arial", Font.BOLD, 24);
        g2d.setFont(timerFont);
        FontMetrics fm = g2d.getFontMetrics();

        String resume = "Resume";
        String menu = "Return to Menu";

        int resumeWidth = fm.stringWidth(resume);
        int menuWidth = fm.stringWidth(menu);

        int playX = getWidth() / 2 - resumeWidth / 2;
        int playY = frameY + 80;
        int menuX = getWidth() / 2 - menuWidth / 2;
        int menuY = frameY + 140;

        drawOutlinedText(g2d, resume, playX, playY, Color.WHITE, timerFont);
        drawOutlinedText(g2d, menu, menuX, menuY, Color.WHITE, timerFont);

        // Define clickable areas
        resumeRect = new Rectangle(playX, playY - 24, resumeWidth, 30);
        menuRect = new Rectangle(menuX, menuY - 24, menuWidth, 30);
    }

    // Resets the level and game state, replacing the current panel
    private void restartLevel(int restartLevel) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) {
            // Resets variables
            levelStartTime = System.currentTimeMillis();
            totalPausedTime = 0;
            pauseStartTime = 0;

            // Removes all contents of the game
            topFrame.getContentPane().removeAll();

            // Stops any sound and resets static variables
            Main.stopSound();
            GameState.reset();

            // Creates and displays a new GamePanel
            GamePanel newGame = new GamePanel(restartLevel, player1, player2);
            topFrame.getContentPane().add(newGame);
            topFrame.revalidate();
            topFrame.repaint();
            newGame.requestFocusInWindow();
        }
    }

    // Draws the game over screen and buttons
    private void gameOver(Graphics g) {
        // Define dimensions and position for the frame in the center of the screen
        int frameW = 400;
        int frameH = 250;
        int frameX = (getWidth() - frameW) / 2;
        int frameY = (getHeight() - frameH) / 2;
        Graphics2D g2d = (Graphics2D) g;

        // Draw the background image for the game over frame
        g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

        // Define fonts for the header and subtext
        Font titleFont = new Font("Arial", Font.BOLD, 28);
        Font subFont = new Font("Arial", Font.BOLD, 24);

        // Define the main header and the score to display
        String headerText = "Total Meltdown!";
        String combinedScore = "Combined Score: " + (player1Score + player2Score);

        // Calculate position to center the header text within the frame
        FontMetrics fmTitle = g2d.getFontMetrics(titleFont);
        int winnerX = getWidth() / 2 - fmTitle.stringWidth(headerText) / 2;
        int winnerY = frameY + 40;

        // Draw the game over header with an outline for better visibility
        drawOutlinedText(g2d, headerText, winnerX, winnerY, Color.YELLOW, titleFont);

        // Calculate position to center the score text below the header
        FontMetrics fmSub = g2d.getFontMetrics(subFont);
        int scoreX = getWidth() / 2 - fmSub.stringWidth(combinedScore) / 2;
        int scoreY = winnerY + 35;

        // Draw the combined score below the header
        drawOutlinedText(g2d, combinedScore, scoreX, scoreY, Color.WHITE, subFont);

        // Define button labels
        String resume = "Restart";
        String menu = "Back to Menu";

        // Measure button widths for centering
        int resumeWidth = fmSub.stringWidth(resume);
        int menuWidth = fmSub.stringWidth(menu);

        // Calculate X/Y positions for "Restart" and "Back to Menu" buttons
        int playX = getWidth() / 2 - resumeWidth / 2;
        int playY = scoreY + 60;
        int menuX = getWidth() / 2 - menuWidth / 2;
        int menuY = playY + 80;

        // Draw buttons with outlined text
        drawOutlinedText(g2d, resume, playX, playY, Color.WHITE, subFont);
        drawOutlinedText(g2d, menu, menuX, menuY, Color.WHITE, subFont);

        // Define clickable rectangular areas for each button
        resumeRect = new Rectangle(playX, playY - 24, resumeWidth, 30);
        menuRect = new Rectangle(menuX, menuY - 24, menuWidth, 30);
    }

    // Displays the victory frame
    private void victory(Graphics g) {
        // Dimensions for the victory frame
        int frameW = 400;
        int frameH = 250;
        int frameX = (getWidth() - frameW) / 2;
        int frameY = (getHeight() - frameH) / 2;
        Graphics2D g2d = (Graphics2D) g;

        // Draw background frame image
        g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

        // Fonts for text display
        Font titleFont = new Font("Arial", Font.BOLD, 28);
        Font subFont = new Font("Arial", Font.BOLD, 24);

        // Determine winner based on score
        String winnerText;
        if (player1Score > player2Score) {
            winnerText = "Player 1 wins!";
        } else if (player2Score > player1Score) {
            winnerText = "Player 2 wins!";
        } else {
            winnerText = "It's a tie!";
        }

        // Display total combined score
        String combinedScore = "Combined Score: " + (player1Score + player2Score);

        // Centered positioning for text
        FontMetrics fmTitle = g2d.getFontMetrics(titleFont);
        int winnerX = getWidth() / 2 - fmTitle.stringWidth(winnerText) / 2;
        int winnerY = frameY + 40;

        drawOutlinedText(g2d, winnerText, winnerX, winnerY, Color.YELLOW, titleFont);

        FontMetrics fmSub = g2d.getFontMetrics(subFont);
        int scoreX = getWidth() / 2 - fmSub.stringWidth(combinedScore) / 2;
        int scoreY = winnerY + 35;

        drawOutlinedText(g2d, combinedScore, scoreX, scoreY, Color.WHITE, subFont);

        // Draw "Continue" and "Back to Menu" buttons
        String resume = "Continue";
        String menu = "Back to Menu";

        int resumeWidth = fmSub.stringWidth(resume);
        int menuWidth = fmSub.stringWidth(menu);

        int playX = getWidth() / 2 - resumeWidth / 2;
        int playY = scoreY + 60;
        int menuX = getWidth() / 2 - menuWidth / 2;
        int menuY = playY + 80;

        drawOutlinedText(g2d, resume, playX, playY, Color.WHITE, subFont);
        drawOutlinedText(g2d, menu, menuX, menuY, Color.WHITE, subFont);

        // Store clickable bounds for interaction
        resumeRect = new Rectangle(playX, playY - 24, resumeWidth, 30);
        menuRect = new Rectangle(menuX, menuY - 24, menuWidth, 30);

        // Play victory sound and unlock next level
        Main.playSound("../graphics/sounds/WinMusic.wav", false);
        LevelSelectScreen.updateLevel(this.level);
    }


    // Helper to validate row and column bounds
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    // Handle keyboard input when key is pressed
    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
    }

    // Handle keyboard input when key is released
    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    // Handle keyboard input when key is typed
    @Override
    public void keyTyped(KeyEvent e) {}

    // Handle mouse input when mouse is clicked
    @Override
    public void mouseClicked(MouseEvent e) {
        // Pause game if pause button clicked
        if (pauseBounds != null && pauseBounds.contains(e.getPoint()) && !isPaused) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();

            // Resume game from pause
        } else if (resumeRect != null && resumeRect.contains(e.getPoint()) && isPaused) {
                isPaused = false;
                totalPausedTime += System.currentTimeMillis() - pauseStartTime;

            // Return to main menu from pause, victory, or game over, depending on the game state
        } else if (menuRect != null && menuRect.contains(e.getPoint()) && (isPaused || GameState.victory || GameState.gameOver)) {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(GamePanel.this);
            topFrame.getContentPane().removeAll();
            topFrame.getContentPane().add(new MenuScreen());
            topFrame.revalidate();
            topFrame.repaint();

            // Restart level if restart button or game over resume is clicked
        } else if ((restartBounds != null && restartBounds.contains(e.getPoint())) || (resumeRect != null && resumeRect.contains(e.getPoint()) && GameState.gameOver)) {
            restartLevel(this.level);

            // Advance to next level or return to level select if last level
        } else if (resumeRect != null && resumeRect.contains(e.getPoint()) && GameState.victory) {
            if (this.level < 2) restartLevel(this.level + 1);
            else {
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(GamePanel.this);
                topFrame.getContentPane().removeAll();
                topFrame.getContentPane().add(new LevelSelectScreen(this.player1, this.player2));
                topFrame.revalidate();
                topFrame.repaint();
            }
        }
    }

    // Unused mouse events for MouseListener
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // Handles timer-based animation updates
    @Override
    public void actionPerformed(ActionEvent e) {
        // Advance animation frame
        animFrame++;
        // Refresh screen
        repaint();
    }
}