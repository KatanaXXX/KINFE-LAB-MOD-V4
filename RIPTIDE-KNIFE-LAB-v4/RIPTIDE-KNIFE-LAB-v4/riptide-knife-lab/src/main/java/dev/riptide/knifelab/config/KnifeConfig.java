package dev.riptide.knifelab.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.riptide.knifelab.registry.Knives;
import dev.riptide.knifelab.registry.Skins;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Persistent client settings. Missing, malformed or outdated files fall back to safe defaults. */
public final class KnifeConfig {
    public static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static KnifeConfig instance;

    public int configVersion = CURRENT_VERSION;

    public boolean replaceEnabled = true;
    public boolean drawAnim = true;
    public boolean idleAnim = true;
    public boolean inspectAnim = true;
    public float animSpeed = 1.0f;

    public float fpScale = 1.0f;
    public float fpPosX = 0f, fpPosY = 0f, fpPosZ = 0f;
    public float fpRotX = 0f, fpRotY = 0f, fpRotZ = 0f;

    /** 0 = low (diffuse), 1 = medium (+specular), 2 = high (+reflection, supersampled). */
    public int previewQuality = 2;
    public boolean enhancedFx = true;
    /** Skips per-triangle lighting in first person for minimum per-frame cost. */
    public boolean lowOverhead = false;
    public boolean autoRotate = true;

    public boolean soundEnabled = true;
    public float soundVolume = 0.7f;
    public boolean soundDraw = true, soundInspect = true, soundHandling = true, soundSwing = true;

    public String knife = "classic";
    public String skin = "vanilla";
    public int variant = 0;
    public List<String> favorites = new ArrayList<>();
    public List<String> recent = new ArrayList<>();

    public static synchronized KnifeConfig get() {
        if (instance == null) load();
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("riptide_knife_lab.json");
    }

    public static synchronized void load() {
        Path p = path();
        KnifeConfig loaded = null;
        if (Files.exists(p)) {
            try {
                String json = Files.readString(p, StandardCharsets.UTF_8);
                loaded = GSON.fromJson(json, KnifeConfig.class);
            } catch (IOException | JsonParseException | IllegalStateException e) {
                try {
                    Files.move(p, p.resolveSibling("riptide_knife_lab.json.bad"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) { }
            }
        }
        instance = loaded != null ? loaded : new KnifeConfig();
        instance.sanitize();
        save();
    }

    public static synchronized void save() {
        if (instance == null) return;
        Path p = path();
        Path tmp = p.resolveSibling("riptide_knife_lab.json.tmp");
        try {
            Files.writeString(tmp, GSON.toJson(instance), StandardCharsets.UTF_8);
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // a failed save must never crash the client
        }
    }

    /** Clamps every value and repairs invalid ids, including configs from older versions. */
    public void sanitize() {
        if (configVersion < CURRENT_VERSION) configVersion = CURRENT_VERSION;
        animSpeed = clamp(animSpeed, 0.25f, 3f);
        fpScale = clamp(fpScale, 0.5f, 2f);
        fpPosX = clamp(fpPosX, -1f, 1f); fpPosY = clamp(fpPosY, -1f, 1f); fpPosZ = clamp(fpPosZ, -1f, 1f);
        fpRotX = clamp(fpRotX, -180f, 180f); fpRotY = clamp(fpRotY, -180f, 180f); fpRotZ = clamp(fpRotZ, -180f, 180f);
        previewQuality = Math.max(0, Math.min(2, previewQuality));
        soundVolume = clamp(soundVolume, 0f, 1f);
        if (knife == null || !Knives.exists(knife)) knife = "classic";
        if (skin == null || !Skins.exists(skin)) skin = "vanilla";
        variant = Math.max(0, Math.min(Skins.get(skin).variantCount() - 1, variant));
        if (favorites == null) favorites = new ArrayList<>();
        if (recent == null) recent = new ArrayList<>();
        favorites.removeIf(id -> id == null || !Knives.exists(id));
        recent.removeIf(id -> id == null || !Knives.exists(id));
        while (recent.size() > 8) recent.remove(recent.size() - 1);
    }

    public void pushRecent(String id) {
        recent.remove(id);
        recent.add(0, id);
        while (recent.size() > 8) recent.remove(recent.size() - 1);
    }

    public boolean toggleFavorite(String id) {
        if (favorites.remove(id)) return false;
        favorites.add(id);
        return true;
    }

    private static float clamp(float v, float lo, float hi) {
        if (Float.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
