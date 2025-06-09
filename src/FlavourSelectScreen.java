import javax.swing.*;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

// Screen to handle both players' flavour selection
public class FlavourSelectScreen extends JPanel {
    // UI images and font
    private BufferedImage snowflakeImage, frameImage, backButtonImage, dripImage;
    private Font customFont;

    // List of animated snowflakes
    public List<Snowflake> snowflakes;

    // Track which flavour is hovered or selected
    private int hoveredFlavour1 = -1;
    private int hoveredFlavour2 = -1;
    public String player1Flavour = null;
    public String player2Flavour = null;

    // Transitioning state and animation position
    private boolean transitioning = false;
    private int dripY = -600;
    private Timer dripTimer;

    // Flavour data and animation frames
    private final String[] flavours = {"chocolate", "vanilla", "strawberry"};
    private final List<BufferedImage[]> flavourAnimations = new ArrayList<>();

    // UI button/selection areas
    private final Rectangle flavour1Rect = new Rectangle(100, 200, 150, 215);
    private final Rectangle flavour2Rect = new Rectangle(400, 200, 150, 215);
    private final Rectangle flavour1RectChocolate = new Rectangle(100, 160, 50, 90);
    private final Rectangle flavour1RectVanilla = new Rectangle(150, 170, 50, 90);
    private final Rectangle flavour1RectStrawberry = new Rectangle(200, 160, 50, 90);
    private final Rectangle flavour2RectChocolate = new Rectangle(400, 160, 50, 90);
    private final Rectangle flavour2RectVanilla = new Rectangle(450, 170, 50, 90);
    private final Rectangle flavour2RectStrawberry = new Rectangle(500, 160, 50, 90);
    private final Rectangle backButtonRect = new Rectangle(230, 490, 200, 60);

    private int animFrame = 0;

    // Snowflake grid config
    private final int rows = 9;
    private final int cols = 10;
    private final int spacingX = 130;
    private final int spacingY = 130;

    public static class Snowflake {
        Point position;
        double angle;

        public Snowflake(int x, int y) {
            this.position = new Point(x, y);
            this.angle = Math.random() * 360;
        }
    }

    public FlavourSelectScreen() {
        // Load images and font
        try {
            snowflakeImage = ImageIO.read(new File("../graphics/images/map/frames/snowflake.png"));
            frameImage = ImageIO.read(new File("../graphics/images/map/frames/two_rectangle_frames.png"));
            backButtonImage = ImageIO.read(new File("../graphics/images/map/buttons/button_frame.png"));
            dripImage = ImageIO.read(new File("../graphics/images/map/frames/drip_animation.png"));
            customFont = Font.createFont(Font.TRUETYPE_FONT, new File("../graphics/fonts/4409_FFF Neostandard Bold_8pt_st.ttf")).deriveFont(28f);
        } catch (IOException | FontFormatException e) {
            System.out.println(e.getMessage());
            customFont = new Font("SansSerif", Font.BOLD, 28);
        }

        // Load animated flavour images
        for (String flavour : flavours) {
            File folder = new File("../graphics/images/map/frames/flavour_selection/" + flavour + "/");
            File[] files = folder.listFiles((_, name) -> name.endsWith(".png"));
            if (files == null) continue;
            // Ensure consistent frame order
            Arrays.sort(files);
            BufferedImage[] frames = new BufferedImage[files.length];
            for (int i = 0; i < files.length; i++) {
                try {
                    frames[i] = ImageIO.read(files[i]);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            flavourAnimations.add(frames);
        }

        // Create snowflake grid for animated background
        snowflakes = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * spacingX;
                int y = row * spacingY - col * spacingY;
                snowflakes.add(new Snowflake(x, y));
            }
        }

        // Timer to animate falling snowflakes
        Timer snowTimer = new Timer(100, _ -> {
            for (Snowflake flake : snowflakes) {
                flake.position.x -= 2;
                flake.position.y += 2;
                flake.angle = (flake.angle + 5) % 360;

                // Reset snowflake when it moves off-screen
                if (flake.position.x < -65 || flake.position.y > getHeight()) {
                    flake.position.x = getWidth();
                    flake.position.y -= getHeight() + rows * spacingY;
                }
            }
            repaint();
        });
        snowTimer.start();

        // Timer to update flavour animations
        Timer animationTimer = new Timer(200, _ -> {
            animFrame++;
            repaint();
        });
        animationTimer.start();

        // Mouse hover detection for flavour boxes
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                hoveredFlavour1 = hoveredFlavour2 = -1;

                // Check if mouse is over any flavour button for Player 1
                if (flavour1RectChocolate.contains(p)) hoveredFlavour1 = 0;
                else if (flavour1RectVanilla.contains(p)) hoveredFlavour1 = 1;
                else if (flavour1RectStrawberry.contains(p)) hoveredFlavour1 = 2;

                // Check if mouse is over any flavour button for Player 2
                if (flavour2RectChocolate.contains(p)) hoveredFlavour2 = 0;
                else if (flavour2RectVanilla.contains(p)) hoveredFlavour2 = 1;
                else if (flavour2RectStrawberry.contains(p)) hoveredFlavour2 = 2;

                repaint();
            }
        });

        // Mouse click detection for selecting flavours and navigating back
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();

                if (backButtonRect.contains(p)) {
                    Container parent = FlavourSelectScreen.this.getParent();
                    parent.removeAll();
                    parent.add(new MenuScreen());
                    parent.revalidate();
                    parent.repaint();
                }

                // Select flavour for Player 1
                if (player1Flavour == null) {
                    if (flavour1RectChocolate.contains(p)) {
                        player1Flavour = "chocolate";
                    } else if (flavour1RectVanilla.contains(p)) {
                        player1Flavour = "vanilla";
                    } else if (flavour1RectStrawberry.contains(p)) {
                        player1Flavour = "strawberry";
                    }
                }

                // Select flavour for Player 2
                if (player2Flavour == null) {
                    if (flavour2RectChocolate.contains(p)) {
                        player2Flavour = "chocolate";
                    } else if (flavour2RectVanilla.contains(p)) {
                        player2Flavour = "vanilla";
                    } else if (flavour2RectStrawberry.contains(p)) {
                        player2Flavour = "strawberry";
                    }
                }

                // If both flavours selected, start transition
                if (player1Flavour != null && player2Flavour != null && !transitioning) {
                    transition();
                }

                repaint();
            }
        });

    }

    // Starts the animated drip transition before changing screen
    private void transition() {
        transitioning = true;
        dripTimer = new Timer(15, _ -> {
            dripY += 10;
            repaint();
            if (dripY >= getHeight()) {
                dripTimer.stop();
                JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(FlavourSelectScreen.this);
                topFrame.getContentPane().removeAll();
                topFrame.getContentPane().add(new LevelSelectScreen(player1Flavour, player2Flavour));
                topFrame.revalidate();
                topFrame.repaint();
            }
        });
        dripTimer.start();
    }

    // Draw text with black outline and white fill
    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y, Font font) {
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(Color.BLACK);
        g2d.draw(new TextLayout(text, font, g2d.getFontRenderContext())
                .getOutline(AffineTransform.getTranslateInstance(x, y)));
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(246, 254, 254));
        Graphics2D g2d = (Graphics2D) g;

        // Draw rotating snowflakes
        for (Snowflake flake : snowflakes) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(flake.position.x + 32, flake.position.y + 32);
            g2d.rotate(Math.toRadians(flake.angle));
            g2d.drawImage(snowflakeImage, -32, -32, 65, 65, null);
            g2d.setTransform(old);
        }

        // Draw frame
        g2d.drawImage(frameImage, 40, 80, 570, 500, null);

        // Draw text titles and labels
        String title = "choose your flavour";
        String playerText = "player one";
        String playerText2 = "player two";
        FontMetrics fm = g2d.getFontMetrics(customFont);
        int tx = getWidth() / 2 - fm.stringWidth(title) / 2;
        int ty = 80 + 50;
        drawOutlinedText(g2d, title, tx, ty, customFont);
        drawOutlinedText(g2d, playerText, flavour1Rect.x, flavour1Rect.y + flavour1Rect.height - 5, customFont);
        drawOutlinedText(g2d, playerText2, flavour2Rect.x, flavour2Rect.y + flavour2Rect.height - 5, customFont);

        // Draw selected/hovered flavour animations
        BufferedImage flavour1 = getFlavourImage(player1Flavour, hoveredFlavour1, flavourAnimations, animFrame);
        BufferedImage flavour2 = getFlavourImage(player2Flavour, hoveredFlavour2, flavourAnimations, animFrame);

        g2d.drawImage(flavour1, flavour1Rect.x, flavour1Rect.y - 40, flavour1Rect.width, flavour1Rect.height, null);
        g2d.drawImage(flavour2, flavour2Rect.x, flavour2Rect.y - 40, flavour2Rect.width, flavour2Rect.height, null);

        // Draw back button
        g2d.drawImage(backButtonImage, backButtonRect.x, backButtonRect.y, backButtonRect.width, backButtonRect.height, null);
        drawOutlinedText(g2d, "back", backButtonRect.x + 75, backButtonRect.y + 35, customFont.deriveFont(20f));

        // If transitioning, draw drip effect
        if (transitioning) {
            g2d.drawImage(dripImage, 0, dripY, 650, 1080, null);
        }
    }

    // Helper method to get the current frame of flavour animation or static image
    private BufferedImage getFlavourImage(String playerFlavour, int hoveredFlavour, List<BufferedImage[]> flavourAnimations, int animFrame) {
        if (playerFlavour != null) {
            // Get animation frames
            int index = Arrays.asList(flavours).indexOf(playerFlavour);
            if (index != -1 && flavourAnimations.size() > index) {
                BufferedImage[] frames = flavourAnimations.get(index);
                return frames.length > 1 ? frames[1] : frames[0];
            }
        } else if (hoveredFlavour != -1 && hoveredFlavour < flavourAnimations.size()) {
            return flavourAnimations.get(hoveredFlavour)[animFrame % flavourAnimations.get(hoveredFlavour).length];
        }
        return flavourAnimations.getFirst()[0];
    }
}