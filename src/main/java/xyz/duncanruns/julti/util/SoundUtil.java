package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;

import javax.sound.sampled.*;
import java.io.File;

public final class SoundUtil {
    private SoundUtil() {
    }

    /**
     * Plays the sound found at soundPath at the specified volume.
     *
     * @param soundPath the path of the sound to be played (should be a .wav file)
     * @param volume    the volume of the played sound (between 0.0 and 1.0)
     */
    public static void playSound(String soundPath, float volume) {
        soundPath = soundPath.trim();
        if (soundPath.isEmpty()) {
            return;
        }
        playSound(new File(soundPath), volume);
    }

    /**
     * Plays the sound found at soundPath at the specified volume.
     *
     * @param soundFile the file of the sound to be played (should be a .wav file)
     * @param volume    the volume of the played sound (between 0.0 and 1.0)
     *
     * @author Draconix
     * @author DuncanRuns
     */
    public static void playSound(File soundFile, float volume) {
        if (volume <= 0.0f) {
            return;
        }
        try {
            volume = Math.min(1.0f, Math.max(0.0f, volume));
            final AudioInputStream stream = AudioSystem.getAudioInputStream(soundFile);
            final Clip clip = AudioSystem.getClip();
            clip.open(stream);

            // https://stackoverflow.com/questions/40514910/set-volume-of-java-clip
            // Thanks to the anonymous user7106805
            FloatControl gainControl = ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN));
            gainControl.setValue(((gainControl.getMaximum() - gainControl.getMinimum()) * volume) + gainControl.getMinimum());

            clip.loop(0);
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to play sound:\n" + ExceptionUtil.toDetailedString(e));
        }
    }
}
