import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.font.TextLayout;

public class TutorialScreen extends JPanel {
    private final int totalPages = 4;
    private int currentPage = 0;

    private BufferedImage frameImage, prevButtonImage, nextButtonImage, snowflakeImage;
    private final ImageIcon[] tutorialGifs = new ImageIcon[totalPages + 1];

    private final List<Snowflake> snowflakes;
    private final int rows = 9, cols = 10, spacingX = 130, spacingY = 130;

    private static class Snowflake {
        Point position;
        double angle;

        public Snowflake(int x, int y) {
            this.position = new Point(x, y);
            this.angle = Math.random() * 360;
        }
    }

    public TutorialScreen() {
        try {
            frameImage = ImageIO.read(new File("../graphics/images/map/frames/image_frame.png"));
            prevButtonImage = ImageIO.read(new File("../graphics/images/map/buttons/previous_button.png"));
            nextButtonImage = ImageIO.read(new File("../graphics/images/map/buttons/next_button.png"));
            snowflakeImage = ImageIO.read(new File("../graphics/images/map/frames/snowflake.png"));

            for (int i = 0; i < totalPages + 1; i++) {
                tutorialGifs[i] = new ImageIcon("../graphics/images/map/tutorial/tutorial" + (i + 1) + ".gif");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        snowflakes = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * spacingX;
                int y = row * spacingY - col * spacingY;
                snowflakes.add(new Snowflake(x, y));
            }
        }

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
            repaint();
        });
        snowTimer.start();

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();

                int buttonW = 100, buttonH = 50;
                int centerX = getWidth() / 2;

                Rectangle backRect = new Rectangle(centerX - 120, getHeight() - 100, buttonW, buttonH);
                Rectangle nextRect = new Rectangle(centerX + 20, getHeight() - 100, buttonW, buttonH);

                if (backRect.contains(mx, my)) {
                    if (currentPage == 0) {
                        Container parent = TutorialScreen.this.getParent();
                        parent.removeAll();
                        parent.add(new MenuScreen());
                        parent.revalidate();
                        parent.repaint();
                    } else {
                        currentPage--;
                        repaint();
                    }
                }

                if (currentPage < totalPages && nextRect.contains(mx, my)) {
                    currentPage++;
                    repaint();
                }
            }
        });
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

        int frameW = 600;
        int frameH = 500;
        int frameX = (getWidth() - frameW) / 2;
        int frameY = 100;

        g2d.drawImage(frameImage, frameX, frameY, frameW, frameH, null);

        Image tutorialImage = tutorialGifs[currentPage].getImage();
        g2d.drawImage(tutorialImage, (getWidth() - 500) / 2 + 60, frameY + 100,
                380, 200, this);

        int buttonW = 100;
        int buttonH = 60;
        int centerX = getWidth() / 2;

        g2d.drawImage(prevButtonImage, centerX - 120, getHeight() - 120, buttonW, buttonH, null);
        if (currentPage < totalPages) {
            g2d.drawImage(nextButtonImage, centerX + 20, getHeight() - 120, buttonW, buttonH, null);
        }

        Font font = new Font("Arial", Font.BOLD, 28);
        String[] description = {"Player 1 walk with the arrow keys.\nPlayer 2 walk with the WASD keys.", "Player 1 shoot with SPACE.\nPlayer 2 shoot with the F key.", "Shoot ice blocks to break them again.", "Pick up all fruit to complete the level.", "Avoid being flattened by the enemies!"};
        int frameCenterX = getWidth() / 2;
        int frameTextStartY = 440;
        drawOutlinedText(g2d, description[currentPage], frameCenterX, frameTextStartY, font);
    }

    private void drawOutlinedText(Graphics2D g2d, String text, int centerX, int startY, Font font) {
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(2f));
        FontMetrics metrics = g2d.getFontMetrics();
        int lineHeight = metrics.getHeight();

        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int textWidth = metrics.stringWidth(line);
            int x = centerX - textWidth / 2;
            int y = startY + i * lineHeight;

            Shape outline = new TextLayout(line, font, g2d.getFontRenderContext())
                    .getOutline(AffineTransform.getTranslateInstance(x, y));
            g2d.setColor(Color.BLACK);
            g2d.draw(outline);

            g2d.setColor(Color.WHITE);
            g2d.drawString(line, x, y);
        }
    }
}