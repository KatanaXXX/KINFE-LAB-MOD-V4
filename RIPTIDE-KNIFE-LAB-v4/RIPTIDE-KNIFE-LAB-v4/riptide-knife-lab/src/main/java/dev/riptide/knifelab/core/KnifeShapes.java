package dev.riptide.knifelab.core;

import static dev.riptide.knifelab.core.Mesh.*;

/**
 * Original procedural geometry for every knife. Each builder defines its own blade outline
 * (width/curve functions), handle construction, guard and pommel, so silhouettes are distinct.
 */
public final class KnifeShapes {
    private KnifeShapes() {}

    private static final int[] FOUR_HANDLE = {HANDLE, HANDLE, HANDLE, HANDLE};

    /** Handle loft: each row is {y, cx, halfWidth, halfDepth}. */
    private static void handle(MeshBuilder b, float[][] rows, int mat) {
        float[][][] s = new float[rows.length][][];
        for (int i = 0; i < rows.length; i++) {
            float c = 0.3f * Math.min(rows[i][2], rows[i][3]);
            s[i] = MeshBuilder.rrect(rows[i][0], rows[i][1], rows[i][2], rows[i][3], 0f, c);
        }
        int[] sm = new int[8];
        java.util.Arrays.fill(sm, mat);
        b.loft(s, sm, true);
    }

    private static void groove(MeshBuilder b, float x, float y0, float len, float z) {
        b.box(x, y0 + len / 2, z, 0.22f, len, 0.05f, GROOVE);
        b.box(x, y0 + len / 2, -z, 0.22f, len, 0.05f, GROOVE);
    }

    /** Wrapped-cord bands along a handle. */
    private static void wraps(MeshBuilder b, float yTop, float yBot, int count, float hw, float hd) {
        float step = (yTop - yBot) / count;
        for (int i = 0; i < count; i++) {
            float y = yBot + step * (i + 0.5f);
            b.box(0f, y, 0f, hw * 2f + 0.16f, step * 0.42f, hd * 2f + 0.16f, i % 2 == 0 ? GUARD : HANDLE);
        }
    }

    public static Mesh build(String id) {
        MeshBuilder b = new MeshBuilder();
        switch (id) {
            case "butterfly" -> butterfly(b);
            case "karambit" -> karambit(b);
            case "m9_bayonet" -> m9(b);
            case "bayonet" -> bayonet(b);
            case "flip" -> flip(b);
            case "huntsman" -> huntsman(b);
            case "falchion" -> falchion(b);
            case "gut" -> gut(b);
            case "bowie" -> bowie(b);
            case "shadow_daggers" -> shadow(b);
            case "navaja" -> navaja(b);
            case "stiletto" -> stiletto(b);
            case "talon" -> talon(b);
            case "ursus" -> ursus(b);
            case "skeleton" -> skeleton(b);
            case "nomad" -> nomad(b);
            case "paracord" -> paracord(b);
            case "survival" -> survival(b);
            case "classic" -> classic(b);
            default -> throw new IllegalArgumentException("Unknown knife " + id);
        }
        return b.build();
    }

    // ---------------------------------------------------------------- Classic
    private static void classic(MeshBuilder b) {
        handle(b, new float[][]{{-4.6f, 0, .5f, .42f}, {-3.4f, 0, .58f, .48f}, {-1.5f, 0, .6f, .5f}, {0f, 0, .5f, .42f}}, HANDLE);
        b.box(0, 0.12f, 0, 2.2f, 0.3f, 1.0f, GUARD);
        b.box(0, -4.75f, 0, 1.1f, 0.35f, 0.85f, GUARD);
        b.blade(0.25f, 8f, 16, t -> 1.5f * M.taper(t, 0.72f), null, 0.2f, false, -0.75f);
        b.grip(0, -2.3f, 0);
    }

    // ---------------------------------------------------------------- Bayonet
    private static void bayonet(MeshBuilder b) {
        handle(b, new float[][]{{-5f, 0, .52f, .4f}, {-3f, 0, .6f, .46f}, {0f, 0, .5f, .4f}}, HANDLE);
        b.box(0, 0.1f, 0, 2.4f, 0.4f, 1.1f, GUARD);
        b.ring(-1.55f, 0.1f, 0, 0.55f, 0.3f, 0.28f, 12, GUARD);        // muzzle ring
        b.box(0, -5.15f, 0, 1.0f, 0.5f, 0.8f, GUARD);
        b.box(0.62f, -5.15f, 0, 0.5f, 0.5f, 0.5f, GUARD);               // lug (fused with pommel)
        b.blade(0.3f, 9.2f, 18, t -> 1.45f * M.taper(t, 0.78f), t -> 0.35f * t * t, 0.2f, false, -0.7f);
        groove(b, -0.2f, 0.9f, 5.5f, 0.2f);
        b.grip(0, -2.5f, 0);
    }

    // ---------------------------------------------------------------- M9 Bayonet
    private static void m9(MeshBuilder b) {
        float[][] rows = new float[9][];
        for (int i = 0; i < 9; i++) rows[i] = new float[]{-5f + i * 0.625f, 0, i % 2 == 0 ? .5f : .62f, i % 2 == 0 ? .4f : .5f};
        handle(b, rows, HANDLE);
        b.box(0, 0.15f, 0, 2.7f, 0.45f, 1.2f, GUARD);
        b.ring(-1.7f, 0.15f, 0, 0.6f, 0.34f, 0.3f, 14, GUARD);
        b.box(0, -5.2f, 0, 1.1f, 0.5f, 0.9f, GUARD);
        final float w0 = 2.0f;
        Fn w = t -> w0 * M.taper(t, 0.55f);
        b.blade(0.4f, 9.6f, 20, w, t -> 0.8f * (w0 - w0 * M.taper(t, 0.55f)), 0.27f, false, -0.9f);
        groove(b, -0.15f, 1.1f, 5.2f, 0.27f);
        b.grip(0, -2.5f, 0);
    }

    // ---------------------------------------------------------------- Flip Knife
    private static void flip(MeshBuilder b) {
        b.box(0.025f, -2.6f, 0.4f, 1.5f, 5.2f, 0.4f, HANDLE);
        b.box(0.025f, -2.6f, -0.4f, 1.5f, 5.2f, 0.4f, HANDLE);
        b.box(0.025f, -2.6f, 0, 1.5f, 5.1f, 0.5f, GUARD);
        b.box(0.025f, -0.15f, 0, 0.3f, 0.3f, 1.4f, PIN);
        b.box(0.025f, -4.7f, 0, 0.3f, 0.3f, 1.4f, PIN);
        b.blade(0f, 7.6f, 18, t -> 1.45f * (t < 0.55f ? 1f : M.taper((t - 0.55f) / 0.45f, 0f) * 0.98f + 0.0f), t -> -0.25f * t * t, 0.2f, false, -0.7f);
        b.grip(0, -2.6f, 0);
    }

    // ---------------------------------------------------------------- Huntsman
    private static void huntsman(MeshBuilder b) {
        handle(b, new float[][]{{-4.2f, 0, .65f, .55f}, {-3.2f, 0, .8f, .62f}, {-2.2f, 0, .68f, .58f}, {-1.2f, 0, .8f, .62f}, {0f, 0, .62f, .5f}}, HANDLE);
        b.box(-0.05f, 0.1f, 0, 2.2f, 0.4f, 1.2f, GUARD);
        b.box(0.1f, -4.35f, 0, 1.3f, 0.4f, 0.9f, GUARD);
        b.box(0.7f, -4.6f, 0, 0.5f, 0.5f, 0.5f, GUARD);
        b.blade(0.3f, 6.4f, 18, t -> 2.3f * M.pow(Math.max(0f, M.sin(M.PI * (0.12f + 0.88f * t))), 0.55f), t -> 0f, 0.26f, false, -1.1f);
        b.grip(0, -2.1f, 0);
    }

    // ---------------------------------------------------------------- Falchion
    private static void falchion(MeshBuilder b) {
        handle(b, new float[][]{{-4.2f, 0, .46f, .38f}, {-2f, 0, .52f, .42f}, {0f, 0, .46f, .38f}}, HANDLE);
        b.box(0, 0.12f, 0, 1.9f, 0.3f, 0.9f, GUARD);
        b.box(0, -4.35f, 0, 0.95f, 0.3f, 0.75f, GUARD);
        b.blade(0.25f, 10.6f, 24, t -> (1.1f + 1.4f * t * t) * M.taper(t, 0.88f), t -> 1.2f * t * t, 0.2f, false, -0.6f);
        b.grip(0, -2.1f, 0);
    }

    // ---------------------------------------------------------------- Gut Knife
    private static void gut(MeshBuilder b) {
        handle(b, new float[][]{{-4.4f, 0, .42f, .4f}, {-3f, 0, .55f, .5f}, {-1f, 0, .52f, .46f}, {0f, 0, .42f, .38f}}, HANDLE);
        b.box(-0.15f, 0.08f, 0, 1.95f, 0.3f, 0.9f, GUARD);
        b.box(0, -4.55f, 0, 0.9f, 0.3f, 0.8f, GUARD);
        // the belly curve starts from a real ricasso width instead of a razor-thin sliver at the spine
        b.blade(0.2f, 7.8f, 20, t -> Math.max(0.95f * (1f - t), 2.2f * M.pow(Math.max(t, 0.001f), 0.38f)) * Math.max(0f, 1f - M.pow(t, 2.6f)), null, 0.2f, false, -1.0f);
        b.grip(0, -2.2f, 0);
    }

    // ---------------------------------------------------------------- Bowie
    private static void bowie(MeshBuilder b) {
        handle(b, new float[][]{{-4.8f, 0, .5f, .46f}, {-3.4f, 0, .62f, .52f}, {-1.4f, 0, .62f, .52f}, {0f, 0, .52f, .42f}}, HANDLE);
        b.box(0, 0.15f, 0, 3.0f, 0.4f, 1.1f, GUARD);
        b.box(0, -5f, 0, 1.2f, 0.4f, 1.0f, GUARD);
        final float w0 = 2.3f;
        Fn w = t -> t < 0.62f ? w0 : w0 * M.taper((t - 0.62f) / 0.38f, 0.05f);
        b.blade(0.4f, 11.2f, 24, w, t -> 0.88f * (w0 - w.f(t)), 0.24f, false, -1.1f);
        groove(b, -0.3f, 1.2f, 6.2f, 0.24f);
        b.grip(0, -2.4f, 0);
    }

    // ---------------------------------------------------------------- Shadow Daggers (two push daggers)
    private static void pushDagger(MeshBuilder b, float x, int g) {
        b.group(g);
        b.box(x, -1.1f, 0, 2.3f, 0.55f, 0.6f, HANDLE);                    // horizontal punch bar
        b.box(x, -0.35f, 0, 2.5f, 0.3f, 1.0f, GUARD);                     // base plate
        b.box(x, -0.7f, 0, 0.8f, 0.8f, 0.5f, GUARD);                      // neck: plate to punch bar
        b.box(x, -1.9f, 0, 0.6f, 1.3f, 0.5f, HANDLE);                    // rear post
        b.blade(-0.2f, 5.2f, 14, t -> 1.9f * (1f - M.pow(t, 0.9f)) + 0.02f, null, 0.25f, true, x);
    }

    private static void shadow(MeshBuilder b) {
        pushDagger(b, -1.3f, 0);
        pushDagger(b, 1.3f, 1);
        b.pivot(1, 1.3f, -1f, 0f);
        b.grip(0, -1.1f, 0);
    }

    // ---------------------------------------------------------------- Navaja
    private static void navaja(MeshBuilder b) {
        float[][] rows = new float[9][];
        for (int i = 0; i < 9; i++) {
            float y = -5.2f + i * 0.65f;
            float k = (y + 5.2f) / 5.2f;
            rows[i] = new float[]{y, 0.35f * M.sin(k * M.PI), 0.5f + 0.12f * M.sin(k * M.PI), 0.38f};
        }
        handle(b, rows, HANDLE);
        b.box(0.03f, 0.05f, 0, 1.3f, 0.3f, 0.95f, GUARD);
        b.box(0, -5.3f, 0, 0.8f, 0.3f, 0.85f, GUARD);
        b.ring(0.03f, 0.05f, 0f, 0.28f, 0.1f, 0.5f, 8, PIN);
        b.blade(0.15f, 8.8f, 22, t -> 1.15f * M.taper(t, 0.8f), t -> 0.75f * t * t, 0.17f, false, -0.55f);
        b.grip(0, -2.6f, 0);
    }

    // ---------------------------------------------------------------- Stiletto
    private static void stiletto(MeshBuilder b) {
        handle(b, new float[][]{{-4.4f, 0, .3f, .3f}, {-2.5f, 0, .38f, .38f}, {0f, 0, .3f, .3f}}, HANDLE);
        b.box(0, 0.1f, 0, 2.5f, 0.24f, 0.55f, GUARD);
        b.box(0, -4.6f, 0, 0.7f, 0.5f, 0.7f, GUARD);
        b.blade(0.2f, 10.4f, 20, t -> 0.85f * M.pow(Math.max(0f, 1f - t), 0.55f), null, 0.17f, true, 0f);
        b.grip(0, -2.3f, 0);
    }

    // ---------------------------------------------------------------- Talon
    private static void talon(MeshBuilder b) {
        handle(b, new float[][]{{-4.4f, 0.0f, .42f, .4f}, {-2.5f, 0.15f, .5f, .45f}, {-0.8f, 0.1f, .48f, .42f}, {0f, 0f, .4f, .36f}}, HANDLE);
        b.box(0.11f, 0.05f, 0, 1.8f, 0.3f, 0.9f, GUARD);
        b.ring(0f, -4.95f, 0f, 0.62f, 0.34f, 0.2f, 14, GUARD);
        b.blade(0.15f, 7f, 22, t -> 1.6f * (1f - M.pow(t, 1.25f)) + 0.02f, t -> 2.8f * t * t, 0.24f, false, -0.7f);
        b.grip(0, -2.4f, 0);
        b.ringPivot(0f, -4.95f, 0f);
    }

    // ---------------------------------------------------------------- Ursus
    private static void ursus(MeshBuilder b) {
        handle(b, new float[][]{{-4.6f, 0, .5f, .46f}, {-3.4f, 0, .66f, .56f}, {-1.4f, 0, .66f, .56f}, {0f, 0, .52f, .44f}}, HANDLE);
        b.box(-0.05f, 0.1f, 0, 2.0f, 0.35f, 1.0f, GUARD);
        b.ring(0f, -4.9f, 0f, 0.5f, 0.24f, 0.2f, 10, GUARD);
        b.blade(0.25f, 8.6f, 22, t -> 1.95f * M.pow(Math.max(0f, M.sin(M.PI * (0.18f + 0.82f * t))), 0.7f), t -> 0f, 0.25f, false, -0.95f);
        b.grip(0, -2.4f, 0);
    }

    // ---------------------------------------------------------------- Skeleton (open-frame handle)
    private static void skeleton(MeshBuilder b) {
        b.box(-0.55f, -2.4f, 0, 0.16f, 4.8f, 0.8f, HANDLE);
        b.box(0.55f, -2.4f, 0, 0.16f, 4.8f, 0.8f, HANDLE);
        for (int i = 0; i < 5; i++) b.box(0, -0.3f - i * 1.0f, 0, 1.28f, 0.22f, 0.7f, GUARD);
        b.box(0, -4.7f, 0, 1.28f, 0.28f, 0.7f, GUARD);                   // bottom bar: frame to pommel ring
        b.ring(0f, -5.35f, 0f, 0.7f, 0.4f, 0.28f, 14, GUARD);
        b.box(0, 0.1f, 0, 1.9f, 0.3f, 1.0f, GUARD);
        b.blade(0.25f, 7.8f, 18, t -> 1.6f * (t < 0.5f ? 1f : M.taper((t - 0.5f) / 0.5f, 0f)), t -> -0.2f * t * t, 0.2f, false, -0.8f);
        b.grip(0, -2.4f, 0);
    }

    // ---------------------------------------------------------------- Nomad
    private static void nomad(MeshBuilder b) {
        handle(b, new float[][]{{-4.4f, 0, .44f, .4f}, {0f, 0, .44f, .4f}}, HANDLE);
        wraps(b, -0.3f, -4.2f, 6, 0.44f, 0.4f);
        b.box(0, 0.1f, 0, 1.5f, 0.3f, 0.9f, GUARD);
        b.box(0, -4.6f, 0, 0.8f, 0.4f, 0.7f, GUARD);
        b.blade(0.25f, 9f, 22, t -> (1.1f + 0.9f * M.sin(M.PI * Math.min(1f, t * 1.05f))) * M.taper(t, 0.86f), t -> 0.55f * t * t, 0.21f, false, -0.7f);
        b.grip(0, -2.2f, 0);
    }

    // ---------------------------------------------------------------- Paracord
    private static void paracord(MeshBuilder b) {
        handle(b, new float[][]{{-4.6f, 0, .46f, .4f}, {0f, 0, .46f, .4f}}, HANDLE);
        wraps(b, -0.2f, -4.4f, 10, 0.46f, 0.4f);
        b.box(0, 0.1f, 0, 1.95f, 0.28f, 0.8f, GUARD);
        b.ring(0f, -5.2f, 0f, 0.75f, 0.5f, 0.14f, 12, GUARD);
        b.blade(0.2f, 8.4f, 18, t -> 1.6f * M.taper(t, 0.68f), t -> 0.5f * (1.6f - 1.6f * M.taper(t, 0.68f)), 0.2f, false, -0.8f);
        b.grip(0, -2.3f, 0);
    }

    // ---------------------------------------------------------------- Survival (serrated spine)
    private static void survival(MeshBuilder b) {
        handle(b, new float[][]{{-4.8f, 0, .56f, .5f}, {-3f, 0, .66f, .58f}, {0f, 0, .56f, .48f}}, HANDLE);
        b.box(0, 0.15f, 0, 2.6f, 0.4f, 1.2f, GUARD);
        b.box(0, -5f, 0, 1.2f, 0.5f, 1.0f, GUARD);
        b.box(0, -5.35f, 0, 0.7f, 0.3f, 0.7f, PIN);
        b.blade(0.4f, 9.6f, 20, t -> 2.0f * M.taper(t, 0.8f), t -> 0.1f * t, 0.3f, false, -1.0f);
        // serrations follow the spine line (-1.0 -> -0.9 over the blade) and sink 0.09 into it
        for (int i = 0; i < 8; i++) {
            float y = 1.3f + i * 0.58f;
            float spine = -1.0f + 0.1f * ((y - 0.4f) / 9.6f);
            b.box(spine - 0.06f, y, 0, 0.3f, 0.34f, 0.4f, EDGE);
        }
        b.grip(0, -2.5f, 0);
    }

    // ---------------------------------------------------------------- Butterfly (balisong)
    private static void butterfly(MeshBuilder b) {
        b.group(0);
        b.blade(0f, 7.2f, 18, t -> 1.55f * (t < 0.6f ? 1f : M.taper((t - 0.6f) / 0.4f, 0f)), t -> -0.15f * t * t, 0.2f, false, -0.75f);
        b.box(0, -0.1f, 0, 0.3f, 0.3f, 1.5f, PIN);
        b.box(0.025f, -0.6f, 0, 1.6f, 1.1f, 0.5f, GUARD);
        b.group(1);
        b.box(0, -2.75f, 0.4f, 1.1f, 5.1f, 0.36f, HANDLE);
        b.box(0, -2.75f, 0.62f, 0.7f, 4.2f, 0.08f, GUARD);
        b.group(2);
        b.box(0, -2.75f, -0.4f, 1.1f, 5.1f, 0.36f, HANDLE);
        b.box(0, -2.75f, -0.62f, 0.7f, 4.2f, 0.08f, GUARD);
        b.box(0.62f, -3.4f, -0.4f, 0.2f, 0.9f, 0.3f, PIN);   // latch
        b.pivot(1, 0f, -0.15f, 0f);
        b.pivot(2, 0f, -0.15f, 0f);
        b.grip(0, -2.7f, 0);
    }

    // ---------------------------------------------------------------- Karambit
    private static void karambit(MeshBuilder b) {
        float[][] rows = new float[8][];
        for (int i = 0; i < 8; i++) {
            float y = -4.8f + i * (4.8f / 7f);
            float k = -y / 4.8f;
            rows[i] = new float[]{y, 0.6f * k * k, 0.46f + 0.08f * M.sin(M.PI * (1 - k)), 0.4f};
        }
        handle(b, rows, HANDLE);
        b.box(0.06f, 0.05f, 0, 1.4f, 0.3f, 0.8f, GUARD);
        b.ring(0.6f, -5.7f, 0f, 1.05f, 0.64f, 0.24f, 20, GUARD);
        b.blade(0.15f, 6.4f, 26, t -> 1.2f * M.pow(Math.max(0f, 1f - M.pow(t, 1.5f)), 0.9f) + 0.02f, t -> 3.4f * t * t, 0.22f, false, -0.55f);
        b.grip(0.1f, -2.4f, 0);
        b.ringPivot(0.6f, -5.7f, 0f);
    }
}
