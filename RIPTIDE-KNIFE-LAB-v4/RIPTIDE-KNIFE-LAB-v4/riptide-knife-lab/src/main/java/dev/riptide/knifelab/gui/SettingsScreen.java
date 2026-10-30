package dev.riptide.knifelab.gui;

import dev.riptide.knifelab.config.KnifeConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Persistent settings. Keybinds are changed in Options > Controls (they use Minecraft's own system). */
public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final KnifeConfig cfg = KnifeConfig.get();
    private int page;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Knife Lab Settings"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        KnifeConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    protected void init() {
        int colW = 190, gap = 8, x0 = width / 2 - colW - gap / 2, x1 = width / 2 + gap / 2, y = 40, step = 22;

        addRenderableWidget(Button.builder(Component.literal("General"), b -> { page = 0; rebuildWidgets(); })
                .bounds(width / 2 - 100, 12, 98, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Position / Sound"), b -> { page = 1; rebuildWidgets(); })
                .bounds(width / 2 + 2, 12, 98, 18).build());

        if (page == 0) {
            toggle(x0, y, colW, "Knife replacement", () -> cfg.replaceEnabled, v -> cfg.replaceEnabled = v);
            toggle(x0, y + step, colW, "Draw animation", () -> cfg.drawAnim, v -> cfg.drawAnim = v);
            toggle(x0, y + 2 * step, colW, "Idle animation", () -> cfg.idleAnim, v -> cfg.idleAnim = v);
            toggle(x0, y + 3 * step, colW, "Inspect animation", () -> cfg.inspectAnim, v -> cfg.inspectAnim = v);
            toggle(x0, y + 4 * step, colW, "Enhanced visual effects", () -> cfg.enhancedFx, v -> cfg.enhancedFx = v);
            toggle(x0, y + 5 * step, colW, "Low-overhead rendering", () -> cfg.lowOverhead, v -> cfg.lowOverhead = v);
            slider(x1, y, colW, "Animation speed", 0.25, 3.0, () -> cfg.animSpeed, v -> cfg.animSpeed = (float) v);
            addRenderableWidget(Button.builder(qualityLabel(), b -> {
                cfg.previewQuality = (cfg.previewQuality + 1) % 3;
                b.setMessage(qualityLabel());
            }).bounds(x1, y + step, colW, 20).build());
            slider(x1, y + 2 * step, colW, "First-person scale", 0.5, 2.0, () -> cfg.fpScale, v -> cfg.fpScale = (float) v);
        } else {
            slider(x0, y, colW, "Position X", -1, 1, () -> cfg.fpPosX, v -> cfg.fpPosX = (float) v);
            slider(x0, y + step, colW, "Position Y", -1, 1, () -> cfg.fpPosY, v -> cfg.fpPosY = (float) v);
            slider(x0, y + 2 * step, colW, "Position Z", -1, 1, () -> cfg.fpPosZ, v -> cfg.fpPosZ = (float) v);
            slider(x0, y + 3 * step, colW, "Rotation X", -180, 180, () -> cfg.fpRotX, v -> cfg.fpRotX = (float) v);
            slider(x0, y + 4 * step, colW, "Rotation Y", -180, 180, () -> cfg.fpRotY, v -> cfg.fpRotY = (float) v);
            slider(x0, y + 5 * step, colW, "Rotation Z", -180, 180, () -> cfg.fpRotZ, v -> cfg.fpRotZ = (float) v);
            toggle(x1, y, colW, "Sounds", () -> cfg.soundEnabled, v -> cfg.soundEnabled = v);
            slider(x1, y + step, colW, "Volume", 0, 1, () -> cfg.soundVolume, v -> cfg.soundVolume = (float) v);
            toggle(x1, y + 2 * step, colW, "Draw sound", () -> cfg.soundDraw, v -> cfg.soundDraw = v);
            toggle(x1, y + 3 * step, colW, "Inspect sound", () -> cfg.soundInspect, v -> cfg.soundInspect = v);
            toggle(x1, y + 4 * step, colW, "Handling sound", () -> cfg.soundHandling, v -> cfg.soundHandling = v);
            toggle(x1, y + 5 * step, colW, "Swing sound", () -> cfg.soundSwing, v -> cfg.soundSwing = v);
        }

        addRenderableWidget(Button.builder(Component.literal("Reset all settings"), b -> {
            KnifeConfig fresh = new KnifeConfig();
            fresh.knife = cfg.knife; fresh.skin = cfg.skin; fresh.variant = cfg.variant;
            fresh.favorites = cfg.favorites; fresh.recent = cfg.recent;
            copy(fresh);
            rebuildWidgets();
        }).bounds(width / 2 - 100, height - 50, 200, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private void copy(KnifeConfig f) {
        cfg.replaceEnabled = f.replaceEnabled; cfg.drawAnim = f.drawAnim; cfg.idleAnim = f.idleAnim;
        cfg.inspectAnim = f.inspectAnim; cfg.animSpeed = f.animSpeed; cfg.fpScale = f.fpScale;
        cfg.fpPosX = f.fpPosX; cfg.fpPosY = f.fpPosY; cfg.fpPosZ = f.fpPosZ;
        cfg.fpRotX = f.fpRotX; cfg.fpRotY = f.fpRotY; cfg.fpRotZ = f.fpRotZ;
        cfg.previewQuality = f.previewQuality; cfg.enhancedFx = f.enhancedFx; cfg.lowOverhead = f.lowOverhead;
        cfg.soundEnabled = f.soundEnabled; cfg.soundVolume = f.soundVolume; cfg.soundDraw = f.soundDraw;
        cfg.soundInspect = f.soundInspect; cfg.soundHandling = f.soundHandling; cfg.soundSwing = f.soundSwing;
        KnifeConfig.save();
    }

    private Component qualityLabel() {
        String[] n = {"Low", "Medium", "High"};
        return Component.literal("Preview lighting: " + n[cfg.previewQuality]);
    }

    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
            KnifeConfig.save();
        }).bounds(x, y, w, 20).build());
    }

    private void slider(int x, int y, int w, String label, double min, double max, DoubleSupplier get, DoubleConsumer set) {
        addRenderableWidget(new Slider(x, y, w, label, min, max, get.getAsDouble(), set));
    }

    private static final class Slider extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final DoubleConsumer set;

        Slider(int x, int y, int w, String label, double min, double max, double value, DoubleConsumer set) {
            super(x, y, w, 20, Component.empty(), (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.set = set;
            updateMessage();
        }

        private double real() { return min + (max - min) * value; }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + String.format(java.util.Locale.ROOT, "%.2f", real())));
        }

        @Override
        protected void applyValue() {
            set.accept(real());
            KnifeConfig.save();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0xFF08101E, 0xFF0C1C2C);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, "Keybinds: change Open (J) and Inspect (G) in Options > Controls", width / 2, height - 64, 0xFF8494AC);
    }
}
