package dev.riptide.knifelab.core;

/**
 * Paints original procedural finishes into a 128x128 ARGB buffer.
 * Rows 0..63 hold the blade finish (u along the blade, v across it); rows 64..127 hold the handle.
 * No external image assets are used.
 */
public final class SkinPainter {
    private SkinPainter() {}

    public static final int W = 128, H = 128;

    public enum Style {
        DOPPLER, GEM, FADE, MARBLE, TIGER, SLAUGHTER, CASE, BLUESTEEL, DAMASCUS, NIGHT, UV,
        WEB, LORE, AUTOTRONIC, FREEHAND, WATER, VANILLA, DDPAT
    }

    public static void paint(Style style, int[] pal, int handleColor, int seed, int[] px) {
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < W; x++) {
                float u = x / (W - 1f), v = y / 63f;
                int c = blade(style, pal, seed, u, v);
                // fine brushed-metal grain along the blade
                float grain = 0.94f + 0.12f * Noise.value(x * 0.35f, y * 9f, seed + 5);
                px[y * W + x] = 0xFF000000 | M.scale(c, grain);
            }
        }
        for (int y = 64; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float weave = ((x / 4 + y / 4) & 1) == 0 ? 1.0f : 0.84f;
                float n = 0.82f + 0.3f * Noise.fbm(x * 0.2f, y * 0.2f, seed + 31);
                px[y * W + x] = 0xFF000000 | M.scale(handleColor, weave * n);
            }
        }
    }

    private static int blade(Style st, int[] p, int s, float u, float v) {
        switch (st) {
            case DOPPLER: {
                float wx = Noise.fbm(u * 3f, v * 3f, s);
                float sw = Noise.fbm(u * 4f + wx * 2f, v * 4f + wx * 2f, s + 3);
                int c = p[0];
                c = M.mix(c, p[1], M.smoothstep(0.42f, 0.58f, sw));
                c = M.mix(c, p[2 % p.length], M.smoothstep(0.55f, 0.72f, Noise.fbm(u * 5f + 3f, v * 5f - 2f + wx, s + 9)));
                if (p.length > 3) c = M.mix(c, p[3], M.smoothstep(0.62f, 0.82f, Noise.fbm(u * 6f - 4f, v * 6f + wx * 2f, s + 21)));
                return c;
            }
            case GEM: {
                float f = Noise.fbm(u * 4f, v * 4f, s);
                float t = 0.55f * f + 0.45f * (1f - Math.abs(v - 0.5f) * 2f);
                int c = M.mix(p[0], p[1], t);
                float facet = Math.abs(M.sin((u * 7f + v * 3f + f) * M.PI));
                c = M.mix(c, p[2 % p.length], M.smoothstep(0.82f, 0.98f, facet) * 0.7f);
                return c;
            }
            case FADE: {
                float t = u * 0.9f + (v - 0.5f) * 0.25f + (Noise.fbm(u * 4f, v * 4f, s) - 0.5f) * 0.08f;
                return M.grad(p, t);
            }
            case MARBLE: {
                float sx = Noise.fbm(u * 2f + v * 0.6f, v * 14f, s);
                int streak = M.grad(p, sx * 1.3f - 0.1f);
                float mask = M.smoothstep(0.25f, 0.55f, Noise.fbm(u * 3f, v * 3f, s + 8) + u * 0.25f);
                return M.mix(p[0], streak, mask);
            }
            case TIGER: {
                float w = Noise.fbm(u * 4f, v * 4f, s) * 4f;
                float st2 = M.sin((u * 18f + v * 9f + w) * M.PI) * 0.5f + 0.5f;
                int c = M.mix(p[0], p[1], M.smoothstep(0.42f, 0.58f, st2));
                return M.mix(c, p[2 % p.length], Noise.fbm(u * 8f, v * 8f, s + 2) * 0.25f);
            }
            case SLAUGHTER: {
                float f = Noise.cellF1(u * 9f, v * 5f, s);
                float t = 1f - f + 0.25f * Noise.fbm(u * 10f, v * 10f, s + 4);
                return M.grad(p, t);
            }
            case CASE: {
                float n = Noise.fbm(u * 5f, v * 5f, s);
                int c = p[0];
                c = M.mix(c, p[1], M.smoothstep(0.5f, 0.62f, n));
                c = M.mix(c, p[2 % p.length], M.smoothstep(0.4f, 0.28f, n));
                return M.mix(c, p[0], Noise.fbm(u * 12f, v * 12f, s + 6) * 0.3f);
            }
            case BLUESTEEL:
                return M.mix(p[0], p[1], v * 0.7f + 0.3f * Noise.fbm(u * 3f, v * 3f, s));
            case DAMASCUS: {
                float w = Noise.fbm(u * 3f, v * 3f, s) * 2.2f;
                float b1 = M.sin((v * 9f + w) * 2f * M.PI) * 0.5f + 0.5f;
                float b2 = M.sin((v * 31f + w * 3f) * 2f * M.PI) * 0.5f + 0.5f;
                int c = M.mix(p[0], p[1], M.smoothstep(0.3f, 0.7f, b1));
                return M.mix(c, p[2 % p.length], M.smoothstep(0.85f, 1f, b2) * 0.5f);
            }
            case NIGHT: {
                int c = M.mix(p[0], p[1], Noise.fbm(u * 6f, v * 6f, s) * 0.8f);
                if (Noise.rnd((int) (u * 127), (int) (v * 63), s) > 0.993f) c = p[2 % p.length];
                return c;
            }
            case UV: {
                int c = M.mix(p[0], p[1], u * 0.6f + 0.4f * Noise.fbm(u * 3f, v * 3f, s));
                float line = M.smoothstep(0.88f, 1f, Noise.fbm(u * 2f, v * 22f, s + 3));
                return M.mix(c, p[2 % p.length], line * 0.8f);
            }
            case WEB: {
                float e = Noise.cellEdge(u * 7f, v * 4f, s);
                int c = M.mix(p[0], M.scale(p[0], 1.4f), Noise.fbm(u * 8f, v * 8f, s));
                float web = 1f - M.smoothstep(0.02f, 0.14f, e);
                c = M.mix(c, p[1], web);
                float e2 = Noise.cellEdge(u * 15f + 4f, v * 8f, s + 5);
                return M.mix(c, p[2 % p.length], (1f - M.smoothstep(0.01f, 0.07f, e2)) * 0.5f);
            }
            case LORE: {
                int c = M.mix(p[0], p[1], u);
                float line = Math.abs(M.sin((v * 20f + M.sin(u * 6f) * 3f) * M.PI));
                return M.mix(c, p[2 % p.length], (1f - M.smoothstep(0.04f, 0.16f, line)) * 0.85f);
            }
            case AUTOTRONIC: {
                float gx = u * 14f, gy = v * 7f;
                int cx = (int) gx, cy = (int) gy;
                float fx = Math.abs(gx - cx - 0.5f), fy = Math.abs(gy - cy - 0.5f);
                boolean horiz = Noise.rnd(cx, cy, s) > 0.5f;
                float lineD = horiz ? fy : fx;
                int c = p[0];
                if (lineD < 0.05f) c = p[1];
                if (fx < 0.12f && fy < 0.12f) c = p[2 % p.length];
                return c;
            }
            case FREEHAND: {
                float n = Noise.fbm(u * 5f, v * 5f, s);
                int c = n > 0.6f ? p[2 % p.length] : p[0];
                float ln = Math.abs(M.sin(n * 30f));
                return M.mix(c, p[1], 1f - M.smoothstep(0.05f, 0.2f, ln));
            }
            case WATER: {
                int c = M.mix(p[0], p[1], v);
                float e = Noise.cellEdge(u * 8f, v * 5f, s);
                float caustic = 1f - M.smoothstep(0.0f, 0.18f, e);
                return M.mix(c, p[2 % p.length], caustic * 0.8f);
            }
            case DDPAT: {
                float n1 = Noise.fbm(u * 5f, v * 3f, s), n2 = Noise.fbm(u * 6f + 11f, v * 4f, s + 13);
                int c = p[0];
                if (n1 > 0.5f) c = p[1];
                if (n2 > 0.56f) c = p[2 % p.length];
                if (p.length > 3 && n1 + n2 < 0.82f) c = p[3];
                return c;
            }
            case VANILLA:
            default:
                return M.mix(p[0], p[1 % p.length], 0.35f * Noise.fbm(u * 3f, v * 3f, s) + 0.1f * v);
        }
    }
}
