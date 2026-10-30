package dev.riptide.knifelab.registry;

import dev.riptide.knifelab.core.KnifeShapes;
import dev.riptide.knifelab.core.Mesh;
import dev.riptide.knifelab.registry.KnifeDef.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registry of all knives. Meshes are built lazily once and cached (19 entries maximum). */
public final class Knives {
    private Knives() {}

    public static final List<KnifeDef> ALL = List.of(
            new KnifeDef("butterfly", "Butterfly Knife", Category.FOLDING, "Balisong with two swinging handle halves. Inspect flips the handles shut and spins the knife."),
            new KnifeDef("karambit", "Karambit", Category.CURVED, "Hooked blade with a finger ring. Inspect spins it around the ring."),
            new KnifeDef("m9_bayonet", "M9 Bayonet", Category.BAYONET, "Broad clip-point bayonet with a muzzle ring and fuller. Slow, controlled inspection turn."),
            new KnifeDef("bayonet", "Bayonet", Category.BAYONET, "Slim military bayonet with a mounting lug and muzzle ring."),
            new KnifeDef("flip", "Flip Knife", Category.FOLDING, "Slab-handled folder with a drop-point blade and exposed pivot pins."),
            new KnifeDef("huntsman", "Huntsman Knife", Category.HUNTING, "Short, wide-bellied hunting blade with a finger-grooved handle."),
            new KnifeDef("falchion", "Falchion Knife", Category.CURVED, "Long forward-swept blade that widens toward the tip."),
            new KnifeDef("gut", "Gut Knife", Category.HUNTING, "Rounded skinning belly on a compact blade."),
            new KnifeDef("bowie", "Bowie Knife", Category.HUNTING, "Large clip-point fighter with a heavy crossguard."),
            new KnifeDef("shadow_daggers", "Shadow Daggers", Category.DAGGER, "A pair of push daggers shown side by side. Inspect crosses them."),
            new KnifeDef("navaja", "Navaja Knife", Category.FOLDING, "Slim swept blade with a curved bone handle and pivot ring."),
            new KnifeDef("stiletto", "Stiletto Knife", Category.DAGGER, "Needle-thin double-edged blade with a thin crossguard."),
            new KnifeDef("talon", "Talon Knife", Category.CURVED, "Heavily curved claw blade with a small pommel ring."),
            new KnifeDef("ursus", "Ursus Knife", Category.HUNTING, "Rounded bear-claw belly with a lanyard loop."),
            new KnifeDef("skeleton", "Skeleton Knife", Category.DAGGER, "Open-frame handle with cross bars. Inspect tilts it to show through the frame."),
            new KnifeDef("nomad", "Nomad Knife", Category.CURVED, "Recurved belly blade with a wrapped handle."),
            new KnifeDef("paracord", "Paracord Knife", Category.HUNTING, "Clip-point blade with a cord-wrapped handle and loop."),
            new KnifeDef("survival", "Survival Knife", Category.HUNTING, "Thick blade with a serrated spine and cap pommel."),
            new KnifeDef("classic", "Classic Knife", Category.HUNTING, "Plain straight utility blade.")
    );

    private static final Map<String, KnifeDef> BY_ID = new HashMap<>();
    private static final Map<String, Mesh> MESHES = new HashMap<>();

    static {
        for (KnifeDef k : ALL) BY_ID.put(k.id(), k);
    }

    public static KnifeDef get(String id) {
        KnifeDef k = BY_ID.get(id);
        return k != null ? k : BY_ID.get("classic");
    }

    public static boolean exists(String id) { return BY_ID.containsKey(id); }

    public static Mesh mesh(String id) {
        String key = get(id).id();
        return MESHES.computeIfAbsent(key, KnifeShapes::build);
    }
}
