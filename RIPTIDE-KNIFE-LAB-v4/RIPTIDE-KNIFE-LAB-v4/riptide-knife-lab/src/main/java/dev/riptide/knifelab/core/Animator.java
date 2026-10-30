package dev.riptide.knifelab.core;

/**
 * Frame-time based knife animation state machine (draw / switch / idle / inspect + swing).
 * Pure Java and allocation-free after construction: it mutates one reused {@link Pose}.
 * Knife-specific behaviour is selected by knife id for draw and inspect.
 */
public final class Animator {
    public enum State { DRAW, SWITCH, IDLE, INSPECT }

    private State state = State.IDLE;
    private float t, clock;
    private String id = "classic";
    private final Pose pose = new Pose();

    public State state() { return state; }

    public void setKnife(String newId) {
        if (!newId.equals(id)) { id = newId; reset(); }
    }

    /** Stops whatever is playing and returns to idle (safe on item change / GUI close). */
    public void reset() { state = State.IDLE; t = 0f; }

    public void startDraw(boolean fast) { state = fast ? State.SWITCH : State.DRAW; t = 0f; }

    public boolean startInspect() {
        if (state == State.INSPECT || state == State.DRAW || state == State.SWITCH) return false;
        state = State.INSPECT;
        t = 0f;
        return true;
    }

    public boolean isInspecting() { return state == State.INSPECT; }

    /** 0..1 progress of the running inspect, or -1 when not inspecting. */
    public float inspectProgress() { return state == State.INSPECT ? t / inspectDuration(id) : -1f; }

    public static float inspectDuration(String id) {
        return switch (id) {
            case "butterfly" -> 3.6f;
            case "karambit" -> 3.0f;
            case "m9_bayonet" -> 3.2f;
            case "skeleton" -> 2.8f;
            case "shadow_daggers" -> 2.8f;
            default -> 2.4f;
        };
    }

    /**
     * Advances the animation.
     * @param dt     real seconds since the previous call
     * @param swing  vanilla attack progress 0..1 (purely visual; never touches combat)
     */
    public Pose update(float dt, float swing, boolean draw, boolean idle, boolean inspect, float speed) {
        dt = M.clamp(dt, 0f, 0.1f) * speed;
        clock += dt;
        pose.reset();

        if (!inspect && state == State.INSPECT) state = State.IDLE;
        if (!draw && (state == State.DRAW || state == State.SWITCH)) state = State.IDLE;

        switch (state) {
            case DRAW -> {
                t += dt;
                float p = t / 0.55f;
                if (p >= 1f) state = State.IDLE; else drawPose(p, 1f);
            }
            case SWITCH -> {
                t += dt;
                float p = t / 0.32f;
                if (p >= 1f) state = State.IDLE; else drawPose(p, 0.6f);
            }
            case INSPECT -> {
                t += dt;
                float p = t / inspectDuration(id);
                if (p >= 1f) state = State.IDLE; else inspectPose(p);
            }
            default -> { }
        }
        if (state == State.IDLE && idle) {
            pose.py += M.sin(clock * 1.7f) * 0.05f;
            pose.rz += M.sin(clock * 0.9f) * 0.9f;
            pose.rx += M.sin(clock * 1.3f) * 0.6f;
        }
        if (swing > 0f) swingPose(swing);
        return pose;
    }

    private static float outBack(float x) {
        float c1 = 1.5f, c3 = c1 + 1f;
        x = M.sat(x) - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
    }

    private void drawPose(float p, float amp) {
        float e = outBack(p);
        float inv = 1f - M.sat(e);
        pose.py -= 3.2f * inv * amp;
        pose.rx -= 65f * inv * amp;
        pose.rz += 14f * inv * amp;
        switch (id) {
            case "butterfly" -> {      // starts closed, flips open
                float o = M.sm(p * 1.25f);
                pose.g1rot = 180f * (1f - o);
                pose.g2rot = -180f * (1f - o);
            }
            case "karambit" -> pose.spin -= 360f * (1f - M.sm(p));
            case "shadow_daggers" -> {
                pose.g1x = -2.6f * (1f - M.sm(p * 1.2f));
                pose.g1rot = 25f * (1f - M.sm(p * 1.2f));
            }
            default -> { }
        }
    }

    private void inspectPose(float p) {
        float env = M.sm(p / 0.15f) * M.sm((1f - p) / 0.15f);
        switch (id) {
            case "butterfly" -> {
                float close = M.sm((p - 0.05f) / 0.22f) * (1f - M.sm((p - 0.72f) / 0.22f));
                pose.g1rot = 180f * close;
                pose.g2rot = -180f * close;
                pose.rz += 720f * M.sm((p - 0.25f) / 0.5f);
                pose.rx += 22f * env;
                pose.px -= 0.5f * env;
            }
            case "karambit" -> {
                pose.spin += 720f * M.sm((p - 0.12f) / 0.7f);
                pose.rx += 28f * env;
                pose.ry += 25f * env;
                pose.px -= 0.6f * env;
            }
            case "m9_bayonet" -> {
                float e = p * p * (3f - 2f * p);
                pose.ry += 360f * e;
                pose.rx += 25f * env;
                pose.rz += 14f * M.sin(p * 2f * M.PI) * env;
                pose.px -= 0.5f * env;
            }
            case "skeleton" -> {
                pose.rx += 78f * env;
                pose.ry += 20f * env;
                pose.rz += 360f * M.sm((p - 0.62f) / 0.3f);
                pose.px -= 0.4f * env;
            }
            case "shadow_daggers" -> {
                pose.g1x = -2.0f * env;
                pose.g1rot = 28f * env;
                pose.ry += 360f * M.sm((p - 0.2f) / 0.65f);
                pose.rx += 18f * env;
            }
            default -> {
                pose.ry += 360f * M.sm((p - 0.1f) / 0.8f);
                pose.rx += 22f * env;
                pose.px -= 0.5f * env;
                pose.py += 0.2f * env;
            }
        }
    }

    private void swingPose(float s) {
        float a = M.sin(s * M.PI);
        pose.rx -= 48f * M.sin(M.sqrt(s) * M.PI);
        pose.rz += 32f * a;
        pose.ry -= 18f * a;
        pose.pz -= 1.2f * a;
        pose.py -= 0.4f * a;
    }
}
