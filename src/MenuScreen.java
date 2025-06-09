import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

public class MenuScreen extends JPanel {
    private BufferedImage homeImage, buttonImage, snowflakeImage, frameImage;
    private boolean showStartButton = true;
    private boolean flashOn = true;
    private Font customFont;
    private final List<Snowflake> snowflakes;
    private final int rows = 9;
    private final int cols = 10;
    private final int spacingX = 130;
    private final int spacingY = 130;

    private static class Snowflake {
        Point position;
        double angle;

        public Snowflake(int x, int y) {
            this.position = new Point(x, y);
            this.angle = Math.random() * 360;
        }
    }

    public MenuScreen() {
        Main.stopSound();
        Main.playSound("../graphics/sounds/MenuMusic.wav", true);
        try {
            homeImage = ImageIO.read(new File("../graphics/images/map/frames/home_screen_image.png"));
            buttonImage = ImageIO.read(new File("../graphics/images/map/buttons/button_frame_wide.png"));
            snowflakeImage = ImageIO.read(new File("../graphics/images/map/frames/snowflake.png"));
            frameImage = ImageIO.read(new File("../graphics/images/map/frames/blank_rectangle_frame.png"));
            customFont = Font.createFont(Font.TRUETYPE_FONT, new File("../graphics/fonts/4409_FFF Neostandard Bold_8pt_st.ttf")).deriveFont(24f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            System.out.println(e.getMessage());
            customFont = new Font("SansSerif", Font.BOLD, 24);
        }

        snowflakes = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * spacingX;
                int y = row * spacingY - col * spacingY;
                snowflakes.add(new Snowflake(x, y));
            }
        }

        JPanel panel = new JPanel() {
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

                int homeW = 400;
                int homeH = 300;
                int homeX = (getWidth() - homeW) / 2;
                int homeY = 60;
                g2d.drawImage(homeImage, homeX, homeY, homeW, homeH, null);

                if (showStartButton) {
                    int buttonW = 300;
                    int buttonH = 70;
                    int buttonX = (getWidth() - buttonW) / 2;
                    int buttonY = homeY + homeH + 30;
                    g2d.drawImage(buttonImage, buttonX, buttonY, buttonW, buttonH, null);

                    String text = "click to lick";
                    FontMetrics fm = g2d.getFontMetrics(customFont);
                    int textWidth = fm.stringWidth(text);
                    int textX = buttonX + (buttonW - textWidth) / 2;
                    int textY = buttonY + buttonH / 2 + fm.getAscent() / 2 - 5;
                    if (flashOn) drawOutlinedText(g2d, text, textX, textY, customFont);
                } else {
                    int frameW = 400;
                    int frameH = 200;
                    int frameX = (getWidth() - frameW) / 2;
                    int frameY = (getHeight() - frameH) / 2;
                    g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

                    Font largeFont = customFont.deriveFont(30f);
                    g2d.setFont(largeFont);
                    FontMetrics fm = g2d.getFontMetrics();

                    String play = "play";
                    String tutorial = "tutorial";

                    int playWidth = fm.stringWidth(play);
                    int tutorialWidth = fm.stringWidth(tutorial);

                    drawOutlinedText(g2d, play, getWidth() / 2 - playWidth / 2, frameY + 80, largeFont);
                    drawOutlinedText(g2d, tutorial, getWidth() / 2 - tutorialWidth / 2, frameY + 140, largeFont);
                }
            }
        };

        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (showStartButton) {
                    showStartButton = false;
                    repaint();
                } else {
                    int mx = e.getX();
                    int my = e.getY();
                    if (new Rectangle(250, 250, 150, 50).contains(mx, my)) {
                        Container parent = MenuScreen.this.getParent();
                        parent.removeAll();
                        parent.add(new FlavourSelectScreen());
                        parent.revalidate();
                        parent.repaint();
                    } else if (new Rectangle(250, 310, 150, 50).contains(mx, my)) {
                        Container parent = MenuScreen.this.getParent();
                        parent.removeAll();
                        parent.add(new TutorialScreen());
                        parent.revalidate();
                        parent.repaint();
                    }
                }
            }
        });

        Timer flashTimer = new Timer(500, _ -> {
            if (showStartButton) {
                flashOn = !flashOn;
                panel.repaint();
            }
        });
        flashTimer.start();

        Timer snowTimer = new Timer(100, _ -> {
            for (Snowflake flake : snowflakes) {
                flake.position.x -= 2;
                flake.position.y += 2;
                flake.angle = (flake.angle + 5) % 360;
                if (flake.position.x < -65 || flake.position.y > getHeight()) {
                    flake.position.x = getWidth();
                    flake.position.y -= getHeight() + rows * spacingY;
                }
            }
            panel.repaint();
        });
        snowTimer.start();

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    private void drawOutlinedText(Graphics2D g2d, String text, int x, int y, Font font) {
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(Color.BLACK);
        g2d.draw(new TextLayout(text, font, g2d.getFontRenderContext())
                .getOutline(AffineTransform.getTranslateInstance(x, y)));
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }
}