import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

// Displays the level selection screen
public class LevelSelectScreen extends JPanel {
    // Images used for UI elements
    private BufferedImage snowflakeImage, frameImage, unlockedImage, lockedImage, lockIcon, backButtonImage, dripImage;

    // Snowflake animation list
    private final List<Snowflake> snowflakes;

    // Status of each level: "locked", "unlocked", "completed"
    public static final String[] levelStatus = new String[40];

    // Defines the clickable area for the "Back" button
    private final Rectangle backButtonRect = new Rectangle(230, 540, 200, 60);

    // Snowflake layout configuration
    private static final int snowflakeRow = 9, snowflakeCol = 10, spacingX = 130, spacingY = 130;

    // Controls vertical position of drip animation during transition
    private int dripY = -600;

    private Timer dripTimer;
    private boolean transitioning = false;

    public static class Snowflake {
        Point position;
        double angle;

        // Class to represent individual animated snowflakes
        public Snowflake(int x, int y) {
            this.position = new Point(x, y);
            this.angle = Math.random() * 360;
        }
    }

    public LevelSelectScreen(String player1Flavour, String player2Flavour) {
        // Load all necessary images
        try {
            snowflakeImage = ImageIO.read(new File("../graphics/images/map/frames/snowflake.png"));
            frameImage = ImageIO.read(new File("../graphics/images/map/frames/large_blank_rectangle_frame.png"));
            unlockedImage = ImageIO.read(new File("../graphics/images/map/buttons/button_frame.png"));
            lockedImage = ImageIO.read(new File("../graphics/images/map/buttons/locked_button.png"));
            lockIcon = ImageIO.read(new File("../graphics/images/map/buttons/lock.png"));
            backButtonImage = ImageIO.read(new File("../graphics/images/map/buttons/button_frame.png"));
            dripImage = ImageIO.read(new File("../graphics/images/map/frames/drip_animation.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        // Initialize snowflakes in a diagonal grid pattern
        snowflakes = new ArrayList<>();
        for (int row = 0; row < snowflakeRow; row++) {
            for (int col = 0; col < snowflakeCol; col++) {
                int x = col * spacingX;
                int y = row * spacingY - col * spacingY;
                snowflakes.add(new Snowflake(x, y));
            }
        }

        // Initialize level statuses if not yet set
        if (levelStatus[0] == null) {
            for (int i = 0; i < 40; i++) levelStatus[i] = "locked";
            levelStatus[0] = "unlocked";
        }

        // Timer to animate snowflakes falling diagonally
        Timer snowTimer = new Timer(100, _ -> {
            for (Snowflake flake : snowflakes) {
                flake.position.x -= 2;
                flake.position.y += 2;
                flake.angle = (flake.angle + 5) % 360;
                // Reset snowflake if it exits the screen
                if (flake.position.x < -65 || flake.position.y > getHeight()) {
                    flake.position.x = getWidth();
                    flake.position.y -= getHeight() + snowflakeRow * spacingY;
                }
            }
            // Refresh screen to show new positions
            repaint();
        });
        snowTimer.start();

        // Handle mouse clicks for buttons
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                // Handle "Back" button
                if (backButtonRect.contains(p)) {
                    Container parent = LevelSelectScreen.this.getParent();
                    parent.removeAll();
                    parent.add(new FlavourSelectScreen());
                    parent.revalidate();
                    parent.repaint();
                }

                // Determine location of level buttons and detect clicks
                int frameW = 575;
                int frameH = 600;
                int frameX = (getWidth() - frameW) / 2;
                int frameY = (getHeight() - frameH) / 2;
                int buttonSize = 60;
                int gap = 35;

                for (int i = 0; i < 25; i++) {
                    int row = i / 5;
                    int col = i % 5;
                    int x = frameX + 70 + col * (buttonSize + gap);
                    int y = frameY + 80 + row * (buttonSize + gap);
                    Rectangle rect = new Rectangle(x, y, buttonSize, buttonSize);

                    // Only allow clicking on unlocked or completed levels
                    if (rect.contains(p) && (levelStatus[i].equals("unlocked") || levelStatus[i].equals("completed")) && i < 3) {
                        // Start level transition
                        transition(i, player1Flavour, player2Flavour);
                        break;
                    }
                }
            }
        });
    }

    // Called when a level is clicked; begins the drip transition
    private void transition(int level, String player1Flavour, String player2Flavour) {
        transitioning = true;
        dripTimer = new Timer(15, _ -> {
            dripY += 10;
            // Animate drip image falling
            repaint();
            if (dripY >= getHeight()) {
                dripTimer.stop();
                // Create new JFrame
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(LevelSelectScreen.this);
                // Removes content on screen
                topFrame.getContentPane().removeAll();
                // Stop background music
                Main.stopSound();
                // Creates and displays the GamePanel screen
                GamePanel game = new GamePanel(level, player1Flavour, player2Flavour);
                topFrame.getContentPane().add(game);
                topFrame.revalidate();
                topFrame.repaint();
                game.requestFocusInWindow();
            }
        });
        dripTimer.start();
    }

    // Paint the entire screen including background, snowflakes, buttons, and transition
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(246, 254, 254, 255));

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw animated snowflakes
        for (Snowflake flake : snowflakes) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(flake.position.x + 32, flake.position.y + 32);
            g2d.rotate(Math.toRadians(flake.angle));
            g2d.drawImage(snowflakeImage, -32, -32, 65, 65, null);
            g2d.setTransform(old);
        }

        // Draw central frame
        int frameW = 575;
        int frameH = 610;
        int frameX = (getWidth() - frameW) / 2;
        int frameY = (getHeight() - frameH) / 2;
        g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

        // Draw title
        String title = "level selection";
        Font font = new Font("Arial", Font.BOLD, 28);
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        drawOutlinedText(g2d, title, (frameW - titleWidth) / 2, frameY + 40, font);

        // Draw each level button
        int buttonSize = 60;
        int gap = 35;

        for (int i = 0; i < 25; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = frameX + 70 + col * (buttonSize + gap);
            int y = frameY + 80 + row * (buttonSize + gap);

            // Draw locked button and lock icon
            if (levelStatus[i].equals("locked")) {
                int unlockedW = buttonSize + 20;
                g2d.drawImage(unlockedImage, x, y, unlockedW, buttonSize, null);

                int margin = 8;
                int lockedW = unlockedW - 2 * margin + 5;
                int lockedH = buttonSize - 2 * margin;
                int lockedX = x + margin;
                int lockedY = y + margin;

                g2d.drawImage(lockedImage, lockedX - 3, lockedY, lockedW, lockedH, null);

                int iconSize = 28;
                g2d.drawImage(lockIcon,
                        x + (unlockedW - iconSize) / 2,
                        y + (buttonSize - iconSize) / 2,
                        iconSize, iconSize, null);
        } else {
                // Draw unlocked/completed level number
                g2d.drawImage(unlockedImage, x, y, buttonSize + 20, buttonSize, null);
                String text = String.valueOf(i + 1);
                int textWidth = g2d.getFontMetrics().stringWidth(text);
                g2d.setColor(Color.WHITE);
                Font levelFont = new Font("Arial", Font.PLAIN, 16);
                drawOutlinedText(g2d, text, x + (buttonSize - textWidth) / 2 + 16, y + buttonSize / 2 + 6, levelFont);
            }
        }
        // Draw back button
        g2d.drawImage(backButtonImage, backButtonRect.x, backButtonRect.y, backButtonRect.width, backButtonRect.height, null);
        drawOutlinedText(g2d, "back", backButtonRect.x + 75, backButtonRect.y + 35, font.deriveFont(20f));

        // If transitioning, draw falling drip effect
        if (transitioning) {
            g2d.drawImage(dripImage, 0, dripY, 650, 1080, null);
        }
    }

    // Helper method to draw outlined text (for readability against any background)
    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y, Font font) {
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(Color.BLACK);
        g2d.draw(new TextLayout(text, font, g2d.getFontRenderContext())
                .getOutline(AffineTransform.getTranslateInstance(x, y)));
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

    // Call this when a level is completed to unlock the next one
    public static void updateLevel(int levelCompleted) {
        levelStatus[levelCompleted] = "completed";
        levelStatus[levelCompleted + 1] = "unlocked";
    }
}