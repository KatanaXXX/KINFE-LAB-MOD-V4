package dev.riptide.knifelab.registry;

import dev.riptide.knifelab.core.SkinPainter.Style;

public record SkinDef(String id, String name, Style style, int handleColor, Variant[] variants) {
    /** A selectable finish variant: name plus the palette the painter uses. */
    public record Variant(String name, int[] palette) {}

    public int variantCount() { return variants.length; }

    public Variant variant(int i) { return variants[Math.floorMod(i, variants.length)]; }
}
