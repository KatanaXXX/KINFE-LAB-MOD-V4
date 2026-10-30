package dev.riptide.knifelab.core;

/** A knife pose: whole-knife transform about the grip plus per-group articulation. Mutable, reused. */
public final class Pose {
    public float px, py, pz;       // translation in knife units
    public float rx, ry, rz;       // degrees, about the grip point
    public float spin;             // degrees about Z through the ring pivot (or grip if no ring)
    public float g1rot, g2rot;     // degrees about Z through each group's pivot
    public float g1x;              // group 1 x translation

    public void reset() {
        px = py = pz = rx = ry = rz = spin = g1rot = g2rot = g1x = 0f;
    }
}
