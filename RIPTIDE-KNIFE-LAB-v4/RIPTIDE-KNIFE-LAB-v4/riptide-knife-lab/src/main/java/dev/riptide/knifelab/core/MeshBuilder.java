package dev.riptide.knifelab.core;

import java.util.Arrays;

/**
 * Builds a {@link Mesh}. Every face is added with an "outward hint" vector so that winding and
 * normals are always consistent, regardless of the order the corners were supplied in.
 */
public final class MeshBuilder {
    private float[] p = new float[9 * 512];
    private float[] n = new float[3 * 512];
    private byte[] m = new byte[512];
    private byte[] g = new byte[512];
    private int t;
    private byte grp;
    private final float[][] pivots = new float[3][3];
    private final float[] grip = new float[3];
    private final float[] ring = new float[3];
    private boolean hasRing;

    public MeshBuilder group(int id) { grp = (byte) id; return this; }

    public MeshBuilder pivot(int id, float x, float y, float z) {
        pivots[id][0] = x; pivots[id][1] = y; pivots[id][2] = z;
        return this;
    }

    public MeshBuilder grip(float x, float y, float z) {
        grip[0] = x; grip[1] = y; grip[2] = z;
        return this;
    }

    public MeshBuilder ringPivot(float x, float y, float z) {
        ring[0] = x; ring[1] = y; ring[2] = z;
        hasRing = true;
        return this;
    }

    private void grow() {
        if (t < m.length) return;
        int c = m.length * 2;
        p = Arrays.copyOf(p, c * 9);
        n = Arrays.copyOf(n, c * 3);
        m = Arrays.copyOf(m, c);
        g = Arrays.copyOf(g, c);
    }

    /** Adds a triangle; its normal is flipped (by swapping two corners) to agree with the hint. */
    public void tri(float ax, float ay, float az, float bx, float by, float bz,
                    float cx, float cy, float cz, int mat, float hx, float hy, float hz) {
        float ux = bx - ax, uy = by - ay, uz = bz - az;
        float vx = cx - ax, vy = cy - ay, vz = cz - az;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float len = M.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) return;
        nx /= len; ny /= len; nz /= len;
        if (nx * hx + ny * hy + nz * hz < 0f) {
            float tx = bx, ty = by, tz = bz;
            bx = cx; by = cy; bz = cz;
            cx = tx; cy = ty; cz = tz;
            nx = -nx; ny = -ny; nz = -nz;
        }
        grow();
        int o = t * 9;
        p[o] = ax; p[o + 1] = ay; p[o + 2] = az;
        p[o + 3] = bx; p[o + 4] = by; p[o + 5] = bz;
        p[o + 6] = cx; p[o + 7] = cy; p[o + 8] = cz;
        n[t * 3] = nx; n[t * 3 + 1] = ny; n[t * 3 + 2] = nz;
        m[t] = (byte) mat;
        g[t] = grp;
        t++;
    }

    public void quad(float[] a, float[] b, float[] c, float[] d, int mat, float hx, float hy, float hz) {
        tri(a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], mat, hx, hy, hz);
        tri(a[0], a[1], a[2], c[0], c[1], c[2], d[0], d[1], d[2], mat, hx, hy, hz);
    }

    /** Rectangular cross-section helper: points at height y. */
    public static float[][] rect(float y, float cx, float hw, float hd, float cz) {
        return new float[][]{{cx - hw, y, cz - hd}, {cx + hw, y, cz - hd}, {cx + hw, y, cz + hd}, {cx - hw, y, cz + hd}};
    }

    /**
     * Lofts a prism through a list of cross-sections (each an equal-length ring of xyz points).
     * Segment j joins point j to point j+1 of each ring and uses material segMat[j].
     */
    public void loft(float[][][] s, int[] segMat, boolean caps) {
        loft(s, segMat, caps, caps);
    }

    public void loft(float[][][] s, int[] segMat, boolean capStart, boolean capEnd) {
        int sections = s.length, k = s[0].length;
        for (int i = 0; i < sections - 1; i++) {
            float cx = 0, cz = 0;
            for (float[] q : s[i]) { cx += q[0]; cz += q[2]; }
            for (float[] q : s[i + 1]) { cx += q[0]; cz += q[2]; }
            cx /= 2 * k; cz /= 2 * k;
            for (int j = 0; j < k; j++) {
                int j2 = (j + 1) % k;
                float[] a = s[i][j], b = s[i][j2], c = s[i + 1][j2], d = s[i + 1][j];
                float dx = (b[0] - a[0]) + (c[0] - d[0]);
                float dz = (b[2] - a[2]) + (c[2] - d[2]);
                float mx = (a[0] + b[0] + c[0] + d[0]) / 4f - cx;
                float mz = (a[2] + b[2] + c[2] + d[2]) / 4f - cz;
                float hx = dz, hz = -dx;
                if (hx * mx + hz * mz < 0f) { hx = -hx; hz = -hz; }
                if (hx == 0f && hz == 0f) { hx = mx; hz = mz; }
                if (hx == 0f && hz == 0f) hz = 1f;
                quad(a, b, c, d, segMat[j], hx, 0f, hz);
            }
        }
        if (capStart) cap(s[0], segMat[0], -1f);
        if (capEnd) cap(s[sections - 1], segMat[0], 1f);
    }

    private void cap(float[][] ring, int mat, float dir) {
        float cx = 0, cy = 0, cz = 0;
        for (float[] q : ring) { cx += q[0]; cy += q[1]; cz += q[2]; }
        cx /= ring.length; cy /= ring.length; cz /= ring.length;
        for (int j = 0; j < ring.length; j++) {
            float[] a = ring[j], b = ring[(j + 1) % ring.length];
            tri(cx, cy, cz, a[0], a[1], a[2], b[0], b[1], b[2], mat, 0f, dir, 0f);
        }
    }

    /** Octagonal (corner-cut) rectangular cross-section at height y. */
    public static float[][] rrect(float y, float cx, float hw, float hd, float cz, float c) {
        c = Math.min(c, Math.min(hw, hd) * 0.9f);
        if (c < 1e-4f) return rect(y, cx, hw, hd, cz);
        return new float[][]{
                {cx - hw + c, y, cz - hd}, {cx + hw - c, y, cz - hd},
                {cx + hw, y, cz - hd + c}, {cx + hw, y, cz + hd - c},
                {cx + hw - c, y, cz + hd}, {cx - hw + c, y, cz + hd},
                {cx - hw, y, cz + hd - c}, {cx - hw, y, cz - hd + c}};
    }

    /**
     * Closed box with chamfered edges (vertical edges via an octagonal section, end edges via
     * inset end rings). The chamfer is clamped to a fraction of the smallest dimension, so thin
     * boxes (grooves, pins) stay clean.
     */
    public void box(float cx, float cy, float cz, float sx, float sy, float sz, int mat) {
        float hw = sx / 2, hd = sz / 2, hh = sy / 2;
        float c = Math.min(0.06f, 0.2f * Math.min(Math.min(sx, sz), sy));
        float[][][] s = {
                rrect(cy - hh, cx, hw - c, hd - c, cz, Math.max(0f, c * 0.6f)),
                rrect(cy - hh + c, cx, hw, hd, cz, c),
                rrect(cy + hh - c, cx, hw, hd, cz, c),
                rrect(cy + hh, cx, hw - c, hd - c, cz, Math.max(0f, c * 0.6f))};
        int[] sm = new int[8];
        Arrays.fill(sm, mat);
        loft(s, sm, true);
    }

    /** Flat ring in the XY plane (a finger ring or lanyard loop) with thickness 2*hd along Z. */
    public void ring(float cx, float cy, float cz, float ro, float ri, float hd, int segs, int mat) {
        for (int j = 0; j < segs; j++) {
            float a0 = 2f * M.PI * j / segs, a1 = 2f * M.PI * (j + 1) / segs;
            float c0 = M.cos(a0), s0 = M.sin(a0), c1 = M.cos(a1), s1 = M.sin(a1);
            float[] of0 = {cx + ro * c0, cy + ro * s0, cz + hd}, of1 = {cx + ro * c1, cy + ro * s1, cz + hd};
            float[] ob0 = {cx + ro * c0, cy + ro * s0, cz - hd}, ob1 = {cx + ro * c1, cy + ro * s1, cz - hd};
            float[] if0 = {cx + ri * c0, cy + ri * s0, cz + hd}, if1 = {cx + ri * c1, cy + ri * s1, cz + hd};
            float[] ib0 = {cx + ri * c0, cy + ri * s0, cz - hd}, ib1 = {cx + ri * c1, cy + ri * s1, cz - hd};
            float mc = M.cos((a0 + a1) / 2), ms = M.sin((a0 + a1) / 2);
            quad(of0, of1, if1, if0, mat, 0, 0, 1);
            quad(ob0, ob1, ib1, ib0, mat, 0, 0, -1);
            quad(of0, of1, ob1, ob0, mat, mc, ms, 0);
            quad(if0, if1, ib1, ib0, mat, -mc, -ms, 0);
        }
    }

    /**
     * Lofts a blade. Spine is on the -X side, edge on +X (or both edges when centered).
     * The cross-section has a flat, a bevel and an edge line, which gives the beveled look.
     */
    public void blade(float y0, float len, int steps, Fn w, Fn c, float ht, boolean centered, float xOff) {
        blade(y0, len, steps, w, c, ht, centered, xOff, 0.2f);
    }

    /**
     * @param sink how far the blade root is extended below y0 (a hidden tang) and capped, so the
     *             blade is a closed solid that is embedded in the guard/handle, never an open tube.
     */
    public void blade(float y0, float len, int steps, Fn w, Fn c, float ht, boolean centered, float xOff, float sink) {
        int extra = sink > 0f ? 1 : 0;
        float[][][] secs = new float[steps + 1 + extra][][];
        for (int i = 0; i <= steps; i++) {
            float tt = i / (float) steps;
            float y = y0 + len * tt;
            float wi = Math.max(0f, w.f(tt));
            float ci = c == null ? 0f : c.f(tt);
            float h = i == steps ? 0f : ht * (1f - 0.55f * tt * tt);
            secs[i + extra] = bladeSection(y, xOff + ci - (centered ? wi * 0.5f : 0f), wi, h, centered);
        }
        if (extra == 1) {
            // tang: identical cross-section to the root, dropped straight down
            float[][] root = secs[1];
            float[][] tang = new float[root.length][];
            for (int j = 0; j < root.length; j++) tang[j] = new float[]{root[j][0], y0 - sink, root[j][2]};
            secs[0] = tang;
        }
        int[] seg = centered
                ? new int[]{Mesh.EDGE, Mesh.BLADE, Mesh.EDGE, Mesh.EDGE, Mesh.BLADE, Mesh.EDGE}
                : new int[]{Mesh.BLADE, Mesh.EDGE, Mesh.EDGE, Mesh.BLADE, Mesh.BLADE};
        loft(secs, seg, extra == 1, false);
    }

    private static float[][] bladeSection(float y, float xs, float wi, float h, boolean centered) {
        float xe = xs + wi;
        if (!centered) {
            float xb = xs + wi * 0.62f;
            return new float[][]{{xs, y, h}, {xb, y, h}, {xe, y, 0f}, {xb, y, -h}, {xs, y, -h}};
        }
        float x1 = xs + wi * 0.22f, x2 = xs + wi * 0.78f;
        return new float[][]{{xs, y, 0f}, {x1, y, h}, {x2, y, h}, {xe, y, 0f}, {x2, y, -h}, {x1, y, -h}};
    }

    public Mesh build() {
        float[] pos = Arrays.copyOf(p, t * 9);
        float[] nrm = Arrays.copyOf(n, t * 3);
        byte[] mat = Arrays.copyOf(m, t);
        byte[] grpArr = Arrays.copyOf(g, t);
        float minX = Float.MAX_VALUE, maxX = -minX, minY = minX, maxY = -minX, minZ = minX, maxZ = -minX;
        float bMinY = minX, bMaxY = -minX;
        for (int i = 0; i < t; i++) {
            for (int v = 0; v < 3; v++) {
                float x = pos[i * 9 + v * 3], y = pos[i * 9 + v * 3 + 1], z = pos[i * 9 + v * 3 + 2];
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                if (Mesh.isMetal(mat[i])) { bMinY = Math.min(bMinY, y); bMaxY = Math.max(bMaxY, y); }
            }
        }
        float rx = Math.max(maxX - minX, 1e-3f), ry = Math.max(maxY - minY, 1e-3f), rz = Math.max(maxZ - minZ, 1e-3f);
        float bry = Math.max(bMaxY - bMinY, 1e-3f);
        float[] uv = new float[t * 6];
        for (int i = 0; i < t; i++) {
            float ax = Math.abs(nrm[i * 3]), ay = Math.abs(nrm[i * 3 + 1]), az = Math.abs(nrm[i * 3 + 2]);
            boolean metal = Mesh.isMetal(mat[i]);
            for (int v = 0; v < 3; v++) {
                float x = pos[i * 9 + v * 3], y = pos[i * 9 + v * 3 + 1], z = pos[i * 9 + v * 3 + 2];
                float u, w;
                if (az >= ax && az >= ay) { u = (y - (metal ? bMinY : minY)) / (metal ? bry : ry); w = (x - minX) / rx; }
                else if (ax >= ay) { u = (y - (metal ? bMinY : minY)) / (metal ? bry : ry); w = (z - minZ) / rz; }
                else { u = (x - minX) / rx; w = (z - minZ) / rz; }
                u = M.clamp(u, 0.01f, 0.99f);
                w = M.clamp(w, 0.01f, 0.99f);
                uv[i * 6 + v * 2] = u;
                uv[i * 6 + v * 2 + 1] = metal ? w * 0.5f : 0.5f + w * 0.5f;
            }
        }
        float[][] pv = new float[3][];
        for (int i = 0; i < 3; i++) pv[i] = pivots[i].clone();
        return new Mesh(t, pos, nrm, uv, mat, grpArr, pv, grip.clone(), ring.clone(), hasRing);
    }
}
