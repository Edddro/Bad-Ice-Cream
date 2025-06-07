import javax.swing.*;
import javax.sound.sampled.*;
import java.io.File;

public class Main {
    public static Clip clip;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bad Ice Cream");
            frame.setTitle("Bad Ice Cream");
            frame.setSize(650, 670);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            playSound("../graphics/sounds/MenuMusic.wav");

            MenuScreen menu = new MenuScreen();
            frame.add(menu);
            frame.setVisible(true);
        });
    }

    public static void playSound(String filePath) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopSound() {
        if(clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
}