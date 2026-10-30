package dev.riptide.knifelab.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

/**
 * Every Minecraft API call whose signature changed across the 1.21.x rendering rewrite lives in
 * this one class. If a 1.21.11 signature differs from what is written here, this is the only file
 * (besides the mixin) that needs adjusting. See KNOWN_LIMITATIONS.md for the unverified list.
 */
public final class McCompat {
    private McCompat() {}

    public static final String NS = "riptide_knife_lab";

    /** UNVERIFIED: whether NativeImage.setPixel takes ARGB (true) or ABGR (false). */
    private static final boolean PIXEL_IS_ARGB = true;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    /** A dynamically uploaded texture. */
    public static final class Tex {
        public final Identifier id;
        final DynamicTexture texture;
        final int w, h;

        Tex(Identifier id, DynamicTexture texture, int w, int h) {
            this.id = id;
            this.texture = texture;
            this.w = w;
            this.h = h;
        }
    }

    public static Tex createTexture(String path, int w, int h) {
        NativeImage img = new NativeImage(w, h, true);
        DynamicTexture tex = new DynamicTexture(() -> path, img);
        Identifier id = id(path);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return new Tex(id, tex, w, h);
    }

    public static void upload(Tex t, int[] argb) {
        NativeImage img = t.texture.getPixels();
        if (img == null) return;
        for (int y = 0; y < t.h; y++) {
            for (int x = 0; x < t.w; x++) {
                int c = argb[y * t.w + x];
                if (!PIXEL_IS_ARGB) {
                    c = (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
                }
                img.setPixel(x, y, c);
            }
        }
        t.texture.upload();
    }

    public static void destroy(Tex t) {
        Minecraft.getInstance().getTextureManager().release(t.id);
    }

    // ------------------------------------------------------------------ world rendering

    public interface Emitter {
        void emit(PoseStack.Pose pose, VertexConsumer vc);
    }

    public static void submit(SubmitNodeCollector collector, PoseStack ps, Identifier texture, Emitter e) {
        collector.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(texture), e::emit);
    }

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                              float u, float v, int r, int g, int b, int light, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    // ------------------------------------------------------------------ GUI

    public static void blit(GuiGraphics g, Tex t, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, t.id, x, y, 0f, 0f, w, h, t.w, t.h, t.w, t.h);
    }

    // ------------------------------------------------------------------ sound

    public static void playUi(SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }

    public static boolean isMainHandSword(Player p) {
        return p.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS);
    }
}
