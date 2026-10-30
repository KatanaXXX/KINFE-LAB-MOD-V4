package dev.riptide.knifelab.core;

/**
 * Small software 3D rasterizer used for the Knife Lab preview. It renders a posed mesh with a
 * z-buffer, textured albedo, key-light diffuse, specular and a fake environment reflection on
 * metal. Buffers are allocated once; render() does not allocate.
 */
public final class Rasterizer {
    private final int size, ss, big;
    private final float[] z;
    private final int[] hi;
    private float[] tp = new float[0], tn = new float[0];
    private final Pose pose = new Pose();

    public Rasterizer(int size, boolean supersample) {
        this.size = size;
        this.ss = supersample ? 2 : 1;
        this.big = size * ss;
        this.z = new float[big * big];
        this.hi = new int[big * big];
    }

    public int size() { return size; }

    /**
     * @param quality 0 = diffuse only, 1 = + specular, 2 = + environment reflection
     * @param out     size*size ARGB pixels (transparent background)
     */
    public void render(Mesh m, Pose p, int[] skin, float yaw, float pitch, float zoom, int quality, int[] out) {
        if (tp.length < m.tris * 9) { tp = new float[m.tris * 9]; tn = new float[m.tris * 3 * 3]; }
        PoseApplier.apply(m, p, tp, tn);

        java.util.Arrays.fill(hi, 0);
        java.util.Arrays.fill(z, -1e9f);

        float cx = m.centerX(), cy = m.centerY(), cz = m.centerZ();
        float cyw = M.cos(M.rad(yaw)), syw = M.sin(M.rad(yaw));
        float cpi = M.cos(M.rad(pitch)), spi = M.sin(M.rad(pitch));
        float scale = zoom * big / (m.extent() * 1.2f);
        float half = big / 2f;

        float lx = -0.4f, ly = 0.7f, lz = 0.6f;
        float ll = M.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        float hx = lx, hy = ly, hz = lz + 1f;
        float hl = M.sqrt(hx * hx + hy * hy + hz * hz);
        hx /= hl; hy /= hl; hz /= hl;

        float[] sx = new float[3], sy = new float[3], sz = new float[3];
        for (int t = 0; t < m.tris; t++) {
            for (int v = 0; v < 3; v++) {
                int i = t * 9 + v * 3;
                float x = tp[i] - cx, y = tp[i + 1] - cy, zz = tp[i + 2] - cz;
                float t1 = x * cyw + zz * syw; zz = -x * syw + zz * cyw; x = t1;
                t1 = y * cpi - zz * spi; zz = y * spi + zz * cpi; y = t1;
                sx[v] = half + x * scale;
                sy[v] = half - y * scale;
                sz[v] = zz;
            }
            float nx = tn[t * 9], ny = tn[t * 9 + 1], nz = tn[t * 9 + 2];
            float t1 = nx * cyw + nz * syw; nz = -nx * syw + nz * cyw; nx = t1;
            t1 = ny * cpi - nz * spi; nz = ny * spi + nz * cpi; ny = t1;
            if (nz < 0f) { nx = -nx; ny = -ny; nz = -nz; }

            int mat = m.mat[t];
            boolean metal = Mesh.isMetal(mat);
            float diff = Math.max(0f, nx * lx + ny * ly + nz * lz);
            float nh = Math.max(0f, nx * hx + ny * hy + nz * hz);
            float spec = quality >= 1 ? M.pow(nh, metal ? 60f : 12f) * (metal ? 0.85f : 0.12f) : 0f;
            float env = 1f;
            if (quality >= 2 && metal) {
                float ry = 2f * nz * ny;
                env = 0.72f + 0.55f * M.smoothstep(-0.3f, 0.8f, ry);
            }
            float light = (0.36f + 0.78f * diff) * env;
            if (mat == Mesh.EDGE) { light *= 1.22f; spec *= 1.5f; }
            if (mat == Mesh.GROOVE) light *= 0.55f;

            float minX = Math.min(sx[0], Math.min(sx[1], sx[2])), maxX = Math.max(sx[0], Math.max(sx[1], sx[2]));
            float minY = Math.min(sy[0], Math.min(sy[1], sy[2])), maxY = Math.max(sy[0], Math.max(sy[1], sy[2]));
            int x0 = Math.max(0, (int) Math.floor(minX)), x1 = Math.min(big - 1, (int) Math.ceil(maxX));
            int y0 = Math.max(0, (int) Math.floor(minY)), y1 = Math.min(big - 1, (int) Math.ceil(maxY));
            float den = (sy[1] - sy[2]) * (sx[0] - sx[2]) + (sx[2] - sx[1]) * (sy[0] - sy[2]);
            if (Math.abs(den) < 1e-6f) continue;
            float inv = 1f / den;
            int uvo = t * 6;
            for (int py = y0; py <= y1; py++) {
                for (int px = x0; px <= x1; px++) {
                    float fx = px + 0.5f, fy = py + 0.5f;
                    float w0 = ((sy[1] - sy[2]) * (fx - sx[2]) + (sx[2] - sx[1]) * (fy - sy[2])) * inv;
                    float w1 = ((sy[2] - sy[0]) * (fx - sx[2]) + (sx[0] - sx[2]) * (fy - sy[2])) * inv;
                    float w2 = 1f - w0 - w1;
                    if (w0 < -0.001f || w1 < -0.001f || w2 < -0.001f) continue;
                    float depth = w0 * sz[0] + w1 * sz[1] + w2 * sz[2];
                    int idx = py * big + px;
                    if (depth <= z[idx]) continue;
                    z[idx] = depth;
                    float u = w0 * m.uv[uvo] + w1 * m.uv[uvo + 2] + w2 * m.uv[uvo + 4];
                    float vv = w0 * m.uv[uvo + 1] + w1 * m.uv[uvo + 3] + w2 * m.uv[uvo + 5];
                    int tx = M.clamp((int) (u * SkinPainter.W), 0, SkinPainter.W - 1);
                    int ty = M.clamp((int) (vv * SkinPainter.H), 0, SkinPainter.H - 1);
                    int c = skin[ty * SkinPainter.W + tx];
                    float r = ((c >> 16) & 255) * light + spec * 255f;
                    float g = ((c >> 8) & 255) * light + spec * 255f;
                    float b = (c & 255) * light + spec * 255f;
                    hi[idx] = 0xFF000000 | (M.clamp((int) r, 0, 255) << 16) | (M.clamp((int) g, 0, 255) << 8) | M.clamp((int) b, 0, 255);
                }
            }
        }

        if (ss == 1) {
            System.arraycopy(hi, 0, out, 0, size * size);
            return;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int a = 0, r = 0, g = 0, b = 0;
                for (int dy = 0; dy < ss; dy++) {
                    for (int dx = 0; dx < ss; dx++) {
                        int c = hi[(y * ss + dy) * big + x * ss + dx];
                        a += c >>> 24; r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255;
                    }
                }
                int n = ss * ss;
                // un-premultiplied average over covered samples keeps edges from darkening
                int cov = Math.max(1, a / 255);
                out[y * size + x] = ((a / n) << 24) | ((r / cov) << 16) | ((g / cov) << 8) | (b / cov);
            }
        }
    }
}
