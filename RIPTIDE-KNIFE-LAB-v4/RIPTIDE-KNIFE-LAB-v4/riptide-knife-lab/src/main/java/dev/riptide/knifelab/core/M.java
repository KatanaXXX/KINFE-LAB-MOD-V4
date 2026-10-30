package dev.riptide.knifelab.core;

/** Small float math helpers shared by the geometry, painter and animation code. */
public final class M {
    private M() {}

    public static final float PI = (float) Math.PI;

    public static float sin(float a) { return (float) Math.sin(a); }
    public static float cos(float a) { return (float) Math.cos(a); }
    public static float sqrt(float a) { return (float) Math.sqrt(a); }
    public static float pow(float a, float b) { return (float) Math.pow(a, b); }
    public static float rad(float deg) { return deg * (PI / 180f); }
    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : Math.min(v, hi); }
    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }
    public static float sat(float v) { return clamp(v, 0f, 1f); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float sm(float x) { x = sat(x); return x * x * (3f - 2f * x); }
    public static float smoothstep(float e0, float e1, float x) { return sm((x - e0) / (e1 - e0)); }

    /** Blade tip taper: 1 until {@code start}, then falls to 0 at t = 1. */
    public static float taper(float t, float start) {
        if (t <= start) return 1f;
        return Math.max(0f, 1f - pow((t - start) / (1f - start), 1.4f));
    }

    // ---- colour helpers (0xRRGGBB ints) ----
    public static int mix(int a, int b, float t) {
        t = sat(t);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int scale(int c, float f) {
        int r = clamp((int) (((c >> 16) & 255) * f), 0, 255);
        int g = clamp((int) (((c >> 8) & 255) * f), 0, 255);
        int b = clamp((int) ((c & 255) * f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** Multi-stop gradient across a palette, t in [0,1]. */
    public static int grad(int[] pal, float t) {
        if (pal.length == 1) return pal[0];
        t = sat(t) * (pal.length - 1);
        int i = Math.min((int) t, pal.length - 2);
        return mix(pal[i], pal[i + 1], t - i);
    }
}
