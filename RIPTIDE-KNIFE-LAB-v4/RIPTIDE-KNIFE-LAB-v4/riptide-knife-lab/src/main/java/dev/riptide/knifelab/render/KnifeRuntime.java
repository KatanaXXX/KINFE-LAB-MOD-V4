package dev.riptide.knifelab.render;

import dev.riptide.knifelab.config.KnifeConfig;
import dev.riptide.knifelab.core.Animator;
import dev.riptide.knifelab.core.Pose;
import dev.riptide.knifelab.sound.KnifeSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client-side runtime state for the first-person knife: one animator, driven by frame time. */
public final class KnifeRuntime {
    private KnifeRuntime() {}

    private static final Animator ANIMATOR = new Animator();
    private static long lastNanos;
    private static long lastRenderNanos;
    private static int lastSlot = -1;
    private static float lastSwing;
    private static float lastInspectProgress = -1f;

    public static Animator animator() { return ANIMATOR; }

    /** Called once per client tick: detects hotbar slot changes for the switch animation. */
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) { lastSlot = -1; ANIMATOR.reset(); return; }
        int slot = p.getInventory().getSelectedSlot();
        if (slot != lastSlot) {
            if (lastSlot != -1 && McCompat.isMainHandSword(p) && KnifeConfig.get().replaceEnabled) {
                ANIMATOR.startDraw(true);
                KnifeSounds.play(KnifeSounds.Kind.DRAW);
            }
            lastSlot = slot;
        }
    }

    /** Inspect key handler. Only runs when a knife is shown and nothing else is animating. */
    public static void requestInspect(Minecraft mc) {
        KnifeConfig c = KnifeConfig.get();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || !c.replaceEnabled || !c.inspectAnim) return;
        if (!McCompat.isMainHandSword(p)) return;
        ANIMATOR.setKnife(c.knife);
        if (ANIMATOR.startInspect()) {
            lastInspectProgress = 0f;
            KnifeSounds.play(KnifeSounds.Kind.INSPECT);
        }
    }

    /** Called when the equipped knife changes in the GUI. */
    public static void onEquipChanged() {
        ANIMATOR.setKnife(KnifeConfig.get().knife);
        if (KnifeConfig.get().drawAnim) ANIMATOR.startDraw(false);
    }

    /** Advances the animation for one rendered frame and returns the (reused) pose. */
    public static Pose frame(float swing) {
        KnifeConfig c = KnifeConfig.get();
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1e9f;
        lastNanos = now;

        // Knife was not on screen for a while (other item / GUI): play the draw animation again.
        if (lastRenderNanos != 0 && (now - lastRenderNanos) > 300_000_000L && c.drawAnim) {
            ANIMATOR.startDraw(false);
            KnifeSounds.play(KnifeSounds.Kind.DRAW);
        }
        lastRenderNanos = now;

        ANIMATOR.setKnife(c.knife);
        Pose pose = ANIMATOR.update(dt, swing, c.drawAnim, c.idleAnim, c.inspectAnim, c.animSpeed);

        if (swing > 0f && lastSwing == 0f) KnifeSounds.play(KnifeSounds.Kind.SWING);
        lastSwing = swing;

        float ip = ANIMATOR.inspectProgress();
        if (ip >= 0f && lastInspectProgress < 0.5f && ip >= 0.5f) KnifeSounds.play(KnifeSounds.Kind.HANDLING);
        lastInspectProgress = ip;
        return pose;
    }
}
