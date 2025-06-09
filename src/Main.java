import javax.swing.*;
import javax.sound.sampled.*;
import java.io.File;

public class Main {
    // Shared Clip instance used for playing background music or sound effects
    public static Clip clip;
    public static void main(String[] args) {
        // Ensures that GUI creation happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Creates the main game window (JFrame)
            JFrame frame = new JFrame("Bad Ice Cream");
            frame.setTitle("Bad Ice Cream");
            frame.setSize(650, 670);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            // Loads the menu screen and adds it to the frame
            MenuScreen menu = new MenuScreen();
            frame.add(menu);
            frame.setVisible(true);
        });
    }

    // Plays a sound from the specified file path
    public static void playSound(String filePath, boolean loop) {
        try {
            // Loads audio from the file
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            // If loop is true, the sound will repeat continuously
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            clip.start();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // Stops and closes the currently playing sound clip, if it exists and is running.
    public static void stopSound() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
}