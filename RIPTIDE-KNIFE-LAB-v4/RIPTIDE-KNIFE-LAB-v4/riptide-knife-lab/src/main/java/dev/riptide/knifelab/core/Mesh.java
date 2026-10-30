package dev.riptide.knifelab.core;

/**
 * Immutable triangle mesh of one knife. Model space: blade along +Y, edge toward +X,
 * blade flat normal along Z, origin at the guard. Units are arbitrary "knife units".
 * Triangle data is stored in flat arrays so rendering loops allocate nothing.
 */
public final class Mesh {
    // Material ids.
    public static final int BLADE = 0, EDGE = 1, HANDLE = 2, GUARD = 3, GROOVE = 4, PIN = 5;

    public final int tris;
    /** 9 floats per triangle (3 vertices x xyz). */
    public final float[] pos;
    /** 3 floats per triangle: outward unit normal. */
    public final float[] nrm;
    /** 6 floats per triangle: u,v per vertex in 0..1 over the 128x128 skin texture. */
    public final float[] uv;
    public final byte[] mat;
    /** 0 = main body, 1 and 2 = movable groups (butterfly handles, second dagger). */
    public final byte[] group;
    /** Pivot per group (index = group id), xyz. */
    public final float[][] pivot;
    public final float[] grip;
    public final float[] ring;
    public final boolean hasRing;
    public final float minX, maxX, minY, maxY, minZ, maxZ;

    Mesh(int tris, float[] pos, float[] nrm, float[] uv, byte[] mat, byte[] group,
         float[][] pivot, float[] grip, float[] ring, boolean hasRing) {
        this.tris = tris;
        this.pos = pos;
        this.nrm = nrm;
        this.uv = uv;
        this.mat = mat;
        this.group = group;
        this.pivot = pivot;
        this.grip = grip;
        this.ring = ring;
        this.hasRing = hasRing;
        float a = Float.MAX_VALUE, b = -a, c = a, d = -a, e = a, f = -a;
        for (int i = 0; i < tris * 3; i++) {
            float x = pos[i * 3], y = pos[i * 3 + 1], z = pos[i * 3 + 2];
            a = Math.min(a, x); b = Math.max(b, x);
            c = Math.min(c, y); d = Math.max(d, y);
            e = Math.min(e, z); f = Math.max(f, z);
        }
        minX = a; maxX = b; minY = c; maxY = d; minZ = e; maxZ = f;
    }

    public static boolean isMetal(int material) {
        return material == BLADE || material == EDGE || material == GROOVE;
    }

    public float centerX() { return (minX + maxX) * 0.5f; }
    public float centerY() { return (minY + maxY) * 0.5f; }
    public float centerZ() { return (minZ + maxZ) * 0.5f; }
    public float extent() { return Math.max(Math.max(maxX - minX, maxY - minY), 1f); }
}
