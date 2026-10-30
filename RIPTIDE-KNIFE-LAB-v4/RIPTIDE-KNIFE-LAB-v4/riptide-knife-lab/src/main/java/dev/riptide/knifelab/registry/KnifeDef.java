package dev.riptide.knifelab.registry;

public record KnifeDef(String id, String name, Category category, String description) {
    public enum Category {
        FOLDING("Folding"), CURVED("Curved"), BAYONET("Bayonets"), HUNTING("Hunting"), DAGGER("Daggers");

        public final String label;
        Category(String label) { this.label = label; }
    }
}
