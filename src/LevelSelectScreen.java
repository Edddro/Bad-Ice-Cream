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

public class LevelSelectScreen extends JPanel {
    private BufferedImage snowflakeImage, frameImage, unlockedImage, lockedImage, lockIcon, backButtonImage, dripImage;
    private final List<Snowflake> snowflakes;
    public static final String[] levelStatus = new String[40];
    private final Rectangle backButtonRect = new Rectangle(230, 540, 200, 60);

    private static final int snowflakeRow = 9, snowflakeCol = 10, spacingX = 130, spacingY = 130;
    private int dripY = -600;
    private Timer dripTimer;
    private boolean transitioning = false;

    public static class Snowflake {
        Point position;
        double angle;

        public Snowflake(int x, int y) {
            this.position = new Point(x, y);
            this.angle = Math.random() * 360;
        }
    }

    public LevelSelectScreen(String player1Flavour, String player2Flavour) {

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

        snowflakes = new ArrayList<>();
        for (int row = 0; row < snowflakeRow; row++) {
            for (int col = 0; col < snowflakeCol; col++) {
                int x = col * spacingX;
                int y = row * spacingY - col * spacingY;
                snowflakes.add(new Snowflake(x, y));
            }
        }

        if (levelStatus[0] == null) {
            for (int i = 0; i < 40; i++) levelStatus[i] = "locked";
            levelStatus[0] = "unlocked";
        }

        Timer snowTimer = new Timer(100, _ -> {
            for (Snowflake flake : snowflakes) {
                flake.position.x -= 2;
                flake.position.y += 2;
                flake.angle = (flake.angle + 5) % 360;
                if (flake.position.x < -65 || flake.position.y > getHeight()) {
                    flake.position.x = getWidth();
                    flake.position.y -= getHeight() + snowflakeRow * spacingY;
                }
            }
            repaint();
        });
        snowTimer.start();

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                if (backButtonRect.contains(p)) {
                    Container parent = LevelSelectScreen.this.getParent();
                    parent.removeAll();
                    parent.add(new FlavourSelectScreen());
                    parent.revalidate();
                    parent.repaint();
                }

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
                    if (rect.contains(p) && (levelStatus[i].equals("unlocked") || levelStatus[i].equals("completed")) && i < 3) {
                        transition(i, player1Flavour, player2Flavour);
                        break;
                    }
                }
            }
        });
    }

    private void transition(int level, String player1Flavour, String player2Flavour) {
        transitioning = true;
        dripTimer = new Timer(15, _ -> {
            dripY += 10;
            repaint();
            if (dripY >= getHeight()) {
                dripTimer.stop();
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(LevelSelectScreen.this);
                topFrame.getContentPane().removeAll();
                Main.stopSound();
                GamePanel game = new GamePanel(level, player1Flavour, player2Flavour);
                topFrame.getContentPane().add(game);
                topFrame.revalidate();
                topFrame.repaint();
                game.requestFocusInWindow();
            }
        });
        dripTimer.start();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(246, 254, 254, 255));

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Snowflake flake : snowflakes) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(flake.position.x + 32, flake.position.y + 32);
            g2d.rotate(Math.toRadians(flake.angle));
            g2d.drawImage(snowflakeImage, -32, -32, 65, 65, null);
            g2d.setTransform(old);
        }

        int frameW = 575;
        int frameH = 610;
        int frameX = (getWidth() - frameW) / 2;
        int frameY = (getHeight() - frameH) / 2;
        g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

        String title = "level selection";
        Font font = new Font("Arial", Font.BOLD, 28);
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        drawOutlinedText(g2d, title, (frameW - titleWidth) / 2, frameY + 40, font);

        int buttonSize = 60;
        int gap = 35;

        for (int i = 0; i < 25; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = frameX + 70 + col * (buttonSize + gap);
            int y = frameY + 80 + row * (buttonSize + gap);

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
                g2d.drawImage(unlockedImage, x, y, buttonSize + 20, buttonSize, null);
                String text = String.valueOf(i + 1);
                int textWidth = g2d.getFontMetrics().stringWidth(text);
                g2d.setColor(Color.WHITE);
                Font levelFont = new Font("Arial", Font.PLAIN, 16);
                drawOutlinedText(g2d, text, x + (buttonSize - textWidth) / 2 + 16, y + buttonSize / 2 + 6, levelFont);
            }
        }
        g2d.drawImage(backButtonImage, backButtonRect.x, backButtonRect.y, backButtonRect.width, backButtonRect.height, null);
        drawOutlinedText(g2d, "back", backButtonRect.x + 75, backButtonRect.y + 35, font.deriveFont(20f));

        if (transitioning) {
            g2d.drawImage(dripImage, 0, dripY, 650, 1080, null);
        }
    }

    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y, Font font) {
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(Color.BLACK);
        g2d.draw(new TextLayout(text, font, g2d.getFontRenderContext())
                .getOutline(AffineTransform.getTranslateInstance(x, y)));
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

    public static void updateLevel(int levelCompleted) {
        levelStatus[levelCompleted] = "completed";
        levelStatus[levelCompleted + 1] = "unlocked";
    }
}