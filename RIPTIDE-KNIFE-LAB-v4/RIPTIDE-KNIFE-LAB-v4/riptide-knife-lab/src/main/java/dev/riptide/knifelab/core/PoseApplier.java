package dev.riptide.knifelab.core;

/** Applies a {@link Pose} to a mesh on the CPU, writing transformed vertices and normals. */
public final class PoseApplier {
    private PoseApplier() {}

    public static void apply(Mesh m, Pose p, float[] outP, float[] outN) {
        float gx = m.grip[0], gy = m.grip[1], gz = m.grip[2];
        float sx = m.hasRing ? m.ring[0] : gx, sy = m.hasRing ? m.ring[1] : gy;
        float cSp = M.cos(M.rad(p.spin)), sSp = M.sin(M.rad(p.spin));
        float c1 = M.cos(M.rad(p.g1rot)), s1 = M.sin(M.rad(p.g1rot));
        float c2 = M.cos(M.rad(p.g2rot)), s2 = M.sin(M.rad(p.g2rot));
        float cz = M.cos(M.rad(p.rz)), sz = M.sin(M.rad(p.rz));
        float cx = M.cos(M.rad(p.rx)), sxr = M.sin(M.rad(p.rx));
        float cy = M.cos(M.rad(p.ry)), sy2 = M.sin(M.rad(p.ry));

        for (int tri = 0; tri < m.tris; tri++) {
            int g = m.group[tri];
            for (int v = 0; v < 3; v++) {
                int i = tri * 9 + v * 3;
                float x = m.pos[i], y = m.pos[i + 1], z = m.pos[i + 2];
                float nx = m.nrm[tri * 3], ny = m.nrm[tri * 3 + 1], nz = m.nrm[tri * 3 + 2];
                float t;

                if (g == 1 || g == 2) {
                    float c = g == 1 ? c1 : c2, s = g == 1 ? s1 : s2;
                    float ox = m.pivot[g][0], oy = m.pivot[g][1];
                    t = ox + (x - ox) * c - (y - oy) * s;
                    y = oy + (x - ox) * s + (y - oy) * c;
                    x = t;
                    t = nx * c - ny * s;
                    ny = nx * s + ny * c;
                    nx = t;
                    if (g == 1) x += p.g1x;
                }

                // spin about Z through ring pivot
                t = sx + (x - sx) * cSp - (y - sy) * sSp;
                y = sy + (x - sx) * sSp + (y - sy) * cSp;
                x = t;
                t = nx * cSp - ny * sSp;
                ny = nx * sSp + ny * cSp;
                nx = t;

                // about grip: Z, then X, then Y
                x -= gx; y -= gy; z -= gz;
                t = x * cz - y * sz; y = x * sz + y * cz; x = t;
                t = nx * cz - ny * sz; ny = nx * sz + ny * cz; nx = t;

                t = y * cx - z * sxr; z = y * sxr + z * cx; y = t;
                t = ny * cx - nz * sxr; nz = ny * sxr + nz * cx; ny = t;

                t = x * cy + z * sy2; z = -x * sy2 + z * cy; x = t;
                t = nx * cy + nz * sy2; nz = -nx * sy2 + nz * cy; nx = t;

                outP[i] = x + gx + p.px;
                outP[i + 1] = y + gy + p.py;
                outP[i + 2] = z + gz + p.pz;
                outN[i] = nx; outN[i + 1] = ny; outN[i + 2] = nz;
            }
        }
    }
}
