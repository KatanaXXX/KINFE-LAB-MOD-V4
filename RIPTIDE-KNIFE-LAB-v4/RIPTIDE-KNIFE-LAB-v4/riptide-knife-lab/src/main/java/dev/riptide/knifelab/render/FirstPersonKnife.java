package dev.riptide.knifelab.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riptide.knifelab.config.KnifeConfig;
import dev.riptide.knifelab.core.M;
import dev.riptide.knifelab.core.Mesh;
import dev.riptide.knifelab.core.Pose;
import dev.riptide.knifelab.core.PoseApplier;
import dev.riptide.knifelab.registry.Knives;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;

/**
 * Draws the equipped knife in first person in place of the held sword. The inventory item is
 * untouched; this only replaces what is drawn. Vertex transforms (pose, animation) are applied on
 * the CPU into preallocated buffers, so the render path performs no per-frame allocation besides
 * one small lambda.
 */
public final class FirstPersonKnife {
    private FirstPersonKnife() {}

    private static float[] tp = new float[600 * 9];
    private static float[] tn = new float[600 * 9];

    // key light, normalised
    private static final float LX = -0.4f / 1.0f, LY = 0.7f, LZ = 0.6f;

    public static boolean shouldReplace(Player player, InteractionHand hand, ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND
                && KnifeConfig.get().replaceEnabled
                && !stack.isEmpty()
                && stack.is(ItemTags.SWORDS);
    }

    public static void render(PoseStack ps, SubmitNodeCollector collector, int light, float swing, boolean leftHanded) {
        KnifeConfig c = KnifeConfig.get();
        Mesh mesh = Knives.mesh(c.knife);
        if (tp.length < mesh.tris * 9) { tp = new float[mesh.tris * 9]; tn = new float[mesh.tris * 9]; }

        Pose pose = KnifeRuntime.frame(swing);
        float side = leftHanded ? -1f : 1f;

        // base hold orientation, then the user's rotation offsets
        // (left-handed view is mirrored by the negative X scale below, so no per-side sign here)
        pose.rx += -32f + c.fpRotX;
        pose.ry += -35f + c.fpRotY;
        pose.rz += 12f + c.fpRotZ;
        PoseApplier.apply(mesh, pose, tp, tn);

        Identifier tex = SkinTextures.get(c.skin, c.variant);
        final boolean fancy = !c.lowOverhead;
        final boolean fx = c.enhancedFx;

        ps.pushPose();
        ps.translate(0.52f * side + c.fpPosX * side, -0.5f + c.fpPosY, -0.78f + c.fpPosZ);
        float s = 0.052f * c.fpScale;
        ps.scale(s * side, s, s);   // mirrored for left hand; two-sided rendering hides the winding flip
        final Mesh m = mesh;
        // The geometry callback runs later in the render pipeline: capture the buffers filled above
        // so a reallocation (larger mesh selected) cannot swap them out from under this submission.
        final float[] fp = tp, fn = tn;
        McCompat.submit(collector, ps, tex, (pose2, vc) -> emit(m, fp, fn, pose2, vc, light, fancy, fx));
        ps.popPose();
    }

    private static void emit(Mesh m, float[] tp, float[] tn, PoseStack.Pose pose,
                             com.mojang.blaze3d.vertex.VertexConsumer vc,
                             int light, boolean fancy, boolean fx) {
        for (int t = 0; t < m.tris; t++) {
            int r = 255, g = 255, b = 255;
            float nx = tn[t * 9], ny = tn[t * 9 + 1], nz = tn[t * 9 + 2];
            if (fancy) {
                if (nz < 0f) { nx = -nx; ny = -ny; nz = -nz; }
                int mat = m.mat[t];
                float diff = Math.max(0f, nx * LX + ny * LY + nz * LZ);
                float l = 0.55f + 0.5f * diff;
                if (fx && Mesh.isMetal(mat)) {
                    float nh = Math.max(0f, nz * 0.6f + ny * 0.5f - nx * 0.3f);
                    l += 0.45f * M.pow(nh, 20f);
                    if (mat == Mesh.EDGE) l += 0.2f;
                }
                if (mat == Mesh.GROOVE) l *= 0.6f;
                int v = M.clamp((int) (l * 255f), 0, 255);
                r = g = b = v;
            }
            // Entity render types draw QUADS (4 vertices per primitive). Emitting only 3 per
            // triangle shifts every following primitive by one vertex and shreds the model into
            // holes and missing faces, so each triangle is sent as a degenerate quad (last corner
            // repeated).
            for (int v = 0; v < 4; v++) {
                int i = t * 9 + Math.min(v, 2) * 3;
                int vi = Math.min(v, 2);
                McCompat.vertex(vc, pose, tp[i], tp[i + 1], tp[i + 2],
                        m.uv[t * 6 + vi * 2], m.uv[t * 6 + vi * 2 + 1], r, g, b, light, tn[i], tn[i + 1], tn[i + 2]);
            }
        }
    }
}
