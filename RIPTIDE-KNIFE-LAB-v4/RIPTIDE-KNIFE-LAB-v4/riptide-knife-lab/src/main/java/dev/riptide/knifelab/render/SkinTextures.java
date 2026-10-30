package dev.riptide.knifelab.render;

import dev.riptide.knifelab.core.SkinPainter;
import dev.riptide.knifelab.registry.Skins;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** GPU textures for painted skins. Bounded: at most MAX textures live, oldest is released. */
public final class SkinTextures {
    private SkinTextures() {}

    private static final int MAX = 4;
    private static final List<String> KEYS = new ArrayList<>();
    private static final List<McCompat.Tex> TEXS = new ArrayList<>();
    private static int counter;

    public static net.minecraft.resources.Identifier get(String skinId, int variant) {
        String key = skinId + "#" + variant;
        int i = KEYS.indexOf(key);
        if (i >= 0) {
            // move to most-recent end
            String k = KEYS.remove(i);
            McCompat.Tex t = TEXS.remove(i);
            KEYS.add(k);
            TEXS.add(t);
            return t.id;
        }
        if (KEYS.size() >= MAX) {
            KEYS.remove(0);
            McCompat.destroy(TEXS.remove(0));
        }
        McCompat.Tex t = McCompat.createTexture("skin/" + (counter++), SkinPainter.W, SkinPainter.H);
        McCompat.upload(t, Skins.pixels(skinId, variant));
        KEYS.add(key);
        TEXS.add(t);
        return t.id;
    }

    /** Releases every GPU texture (called on client stop). */
    public static void releaseAll() {
        for (McCompat.Tex t : TEXS) McCompat.destroy(t);
        TEXS.clear();
        KEYS.clear();
    }
}
