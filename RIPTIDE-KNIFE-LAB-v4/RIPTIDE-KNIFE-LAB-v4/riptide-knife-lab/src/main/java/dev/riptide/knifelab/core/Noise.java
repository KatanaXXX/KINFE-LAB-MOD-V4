package dev.riptide.knifelab.core;

/** Deterministic hash-based noise used by the procedural skin painter. */
final class Noise {
    private Noise() {}

    static int hash(int x, int y, int s) {
        int n = x * 374761393 + y * 668265263 + s * 1442695041;
        n = (n ^ (n >>> 13)) * 1274126177;
        return n ^ (n >>> 16);
    }

    static float rnd(int x, int y, int s) {
        return (hash(x, y, s) & 0xFFFFFF) / 16777216f;
    }

    static float value(float x, float y, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float a = rnd(xi, yi, s), b = rnd(xi + 1, yi, s), c = rnd(xi, yi + 1, s), d = rnd(xi + 1, yi + 1, s);
        return M.lerp(M.lerp(a, b, fx), M.lerp(c, d, fx), fy);
    }

    static float fbm(float x, float y, int s) {
        float sum = 0, amp = 0.5f, norm = 0;
        for (int o = 0; o < 4; o++) {
            sum += value(x, y, s + o * 17) * amp;
            norm += amp;
            x *= 2.03f; y *= 2.03f; amp *= 0.5f;
        }
        return sum / norm;
    }

    /** Distance to nearest feature point (F1) of a jittered grid. */
    static float cellF1(float x, float y, int s) {
        return cell(x, y, s, false);
    }

    /** F2 - F1: near 0 on cell borders. */
    static float cellEdge(float x, float y, int s) {
        return cell(x, y, s, true);
    }

    private static float cell(float x, float y, int s, boolean edge) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float f1 = 9f, f2 = 9f;
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                float px = xi + i + rnd(xi + i, yi + j, s), py = yi + j + rnd(xi + i, yi + j, s + 91);
                float d = M.sqrt((px - x) * (px - x) + (py - y) * (py - y));
                if (d < f1) { f2 = f1; f1 = d; } else if (d < f2) f2 = d;
            }
        }
        return edge ? f2 - f1 : f1;
    }
}
