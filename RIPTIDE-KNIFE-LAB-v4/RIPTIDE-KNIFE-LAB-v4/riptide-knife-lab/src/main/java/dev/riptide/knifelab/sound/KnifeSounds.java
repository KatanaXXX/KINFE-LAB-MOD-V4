package dev.riptide.knifelab.sound;

import dev.riptide.knifelab.config.KnifeConfig;
import dev.riptide.knifelab.render.McCompat;
import net.minecraft.sounds.SoundEvents;

/**
 * Cosmetic client-side sounds built from vanilla sound events with custom pitch and volume.
 * (No audio files are shipped; none of the sounds come from any other game.)
 */
public final class KnifeSounds {
    private KnifeSounds() {}

    public enum Kind { DRAW, INSPECT, HANDLING, SWING }

    public static void play(Kind kind) {
        KnifeConfig c = KnifeConfig.get();
        if (!c.soundEnabled || c.soundVolume <= 0.01f) return;
        float v = c.soundVolume;
        switch (kind) {
            case DRAW -> { if (c.soundDraw) McCompat.playUi(SoundEvents.IRON_TRAPDOOR_OPEN, 1.7f, 0.45f * v); }
            case INSPECT -> { if (c.soundInspect) McCompat.playUi(SoundEvents.CHAIN_STEP, 1.5f, 0.6f * v); }
            case HANDLING -> { if (c.soundHandling) McCompat.playUi(SoundEvents.CHAIN_HIT, 1.9f, 0.4f * v); }
            case SWING -> { if (c.soundSwing) McCompat.playUi(SoundEvents.CHAIN_PLACE, 1.8f, 0.3f * v); }
        }
    }
}
