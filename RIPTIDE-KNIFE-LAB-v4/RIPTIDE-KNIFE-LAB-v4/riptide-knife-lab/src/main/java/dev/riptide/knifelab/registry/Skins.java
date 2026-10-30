package dev.riptide.knifelab.registry;

import dev.riptide.knifelab.core.SkinPainter;
import dev.riptide.knifelab.core.SkinPainter.Style;
import dev.riptide.knifelab.registry.SkinDef.Variant;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of all original finishes, plus a bounded cache of painted 128x128 ARGB textures. */
public final class Skins {
    private Skins() {}

    private static Variant v(String name, int... pal) { return new Variant(name, pal); }

    private static SkinDef s(String id, String name, Style st, int handle, Variant... vs) {
        return new SkinDef(id, name, st, handle, vs);
    }

    public static final List<SkinDef> ALL = List.of(
            s("doppler", "Doppler", Style.DOPPLER, 0x16161E,
                    v("Phase 1", 0x14141C, 0x7A1FA0, 0x2B55D9, 0xC8307A),
                    v("Phase 2", 0x14141C, 0xE0407A, 0x2E8BFF, 0x7B2FC0),
                    v("Phase 3", 0x12161A, 0x1E9E52, 0x2B4FD0, 0x1B1B26),
                    v("Phase 4", 0x101018, 0x1E56D6, 0x2A8CF0, 0x0B1B7A)),
            s("gamma_doppler", "Gamma Doppler", Style.DOPPLER, 0x10201A,
                    v("Phase 1", 0x0A1A18, 0x13A58C, 0x1E6FC8, 0x72F0D0),
                    v("Phase 2", 0x0A1A18, 0x1EC878, 0x0E7C9E, 0x2B52C0),
                    v("Phase 3", 0x0B1814, 0x2FD080, 0x1A9C5A, 0x0F5E3A),
                    v("Phase 4", 0x08161A, 0x18B0C8, 0x4EE6B0, 0x1A5A8A)),
            s("sapphire", "Sapphire", Style.GEM, 0x0A1630,
                    v("Polished", 0x07102A, 0x1553E0, 0x7CB6FF),
                    v("Deep", 0x040A1E, 0x0B3AA8, 0x4A8CEA)),
            s("ruby", "Ruby", Style.GEM, 0x2A0A10,
                    v("Polished", 0x2A0508, 0xD0102A, 0xFF7088),
                    v("Deep", 0x1A0305, 0x9A0A1E, 0xE04058)),
            s("emerald", "Emerald", Style.GEM, 0x0A2A18,
                    v("Polished", 0x04140C, 0x0F9D4E, 0x7DFFB0),
                    v("Deep", 0x030C08, 0x0A6E36, 0x4FE08A)),
            s("black_pearl", "Black Pearl", Style.GEM, 0x08080E,
                    v("Polished", 0x050509, 0x2B2B4A, 0x8E6FD0),
                    v("Deep", 0x030306, 0x1A1A30, 0x5A4A90)),
            s("fade", "Fade", Style.FADE, 0x2A1A30,
                    v("Full Fade", 0xFFC83C, 0xFF7A3C, 0xE8407C, 0x6A3CC8),
                    v("Cool Fade", 0x3CD0C8, 0x3C8CFF, 0x7A4CE8, 0xC84CB0),
                    v("Sunset", 0xFFE05C, 0xFF9040, 0xE04070)),
            s("marble_fade", "Marble Fade", Style.MARBLE, 0x14141A,
                    v("Fire & Ice", 0x0E0E14, 0xE03030, 0xF0D040, 0xF0F4FF, 0x3C8CFF),
                    v("Max Blue", 0x0E0E14, 0x1A3CC8, 0x3C8CFF, 0xDDEEFF),
                    v("Max Red", 0x0E0E14, 0x8A0A14, 0xE03030, 0xFFA060)),
            s("tiger_tooth", "Tiger Tooth", Style.TIGER, 0x3A2A10,
                    v("Gold", 0xE8B438, 0x4A2A08, 0xFFE080),
                    v("Bronze", 0xB87838, 0x2A1608, 0xE8B070)),
            s("slaughter", "Slaughter", Style.SLAUGHTER, 0x2A0A0A,
                    v("Classic", 0x3A0A0A, 0xB01818, 0xE86020, 0xF0C040),
                    v("Ember", 0x1A0404, 0x8A1010, 0xD04010, 0xE08030)),
            s("case_hardened", "Case Hardened", Style.CASE, 0x1E2A44,
                    v("Blue Gem", 0x2A4C9A, 0x5A9CF0, 0x6E7890),
                    v("Standard", 0x3A4466, 0xB89040, 0x6E7890),
                    v("Heavy Gold", 0x4A3A28, 0xE0B848, 0x6E7890)),
            s("blue_steel", "Blue Steel", Style.BLUESTEEL, 0x1A2438,
                    v("Standard", 0x2A3E64, 0x7C98C0),
                    v("Dark", 0x1A2842, 0x587CA8)),
            s("damascus_steel", "Damascus Steel", Style.DAMASCUS, 0x2A2A2E,
                    v("Steel", 0x8A8E96, 0x4A4E58, 0xD0D4DC),
                    v("Dark", 0x50545C, 0x24262C, 0x9A9EA8)),
            s("night", "Night", Style.NIGHT, 0x0C0C10,
                    v("Standard", 0x101014, 0x2A2A34, 0x7A7A92),
                    v("Pitch", 0x08080A, 0x16161C, 0x484856)),
            s("ultraviolet", "Ultraviolet", Style.UV, 0x1A1030,
                    v("Standard", 0x1A1030, 0x5A2AA8, 0xC090FF),
                    v("Blacklight", 0x0C0820, 0x3A1A88, 0xE060FF)),
            s("crimson_web", "Crimson Web", Style.WEB, 0x2A1010,
                    v("Standard", 0x4A4E58, 0xB01020, 0xE04050),
                    v("Heavy", 0x30343C, 0x8A0A18, 0xD03040)),
            s("lore", "Lore", Style.LORE, 0x2A2010,
                    v("Gold", 0x3A2A66, 0x6A3A88, 0xE8C050),
                    v("Silver", 0x2A2E48, 0x58649A, 0xD8DCEC)),
            s("autotronic", "Autotronic", Style.AUTOTRONIC, 0x14100C,
                    v("Standard", 0x161410, 0xE87A10, 0xFFC060),
                    v("Cyan", 0x0C1416, 0x18C0D8, 0x9AF0FF)),
            s("freehand", "Freehand", Style.FREEHAND, 0x181410,
                    v("Standard", 0x14141A, 0xE0B040, 0xF0F0F4),
                    v("Crimson", 0x14100E, 0xD83030, 0xF0D0A0)),
            s("bright_water", "Bright Water", Style.WATER, 0x103050,
                    v("Standard", 0x1E78C8, 0x58C0F0, 0xE8FAFF),
                    v("Lagoon", 0x0E8A9A, 0x40E0D0, 0xE0FFF8)),
            s("vanilla", "Vanilla", Style.VANILLA, 0x1A1A1E,
                    v("Standard", 0xB8BCC4, 0x8A8E98)),
            s("forest_ddpat", "Forest DDPAT", Style.DDPAT, 0x2A3A1E,
                    v("Forest", 0x2E3E22, 0x5A4630, 0x6E7A3C, 0x1E2A16),
                    v("Dark", 0x1E2A16, 0x3A2E20, 0x44502A, 0x121A0C))
    );

    private static final Map<String, SkinDef> BY_ID = new HashMap<>();

    static {
        for (SkinDef d : ALL) BY_ID.put(d.id(), d);
    }

    public static SkinDef get(String id) {
        SkinDef d = BY_ID.get(id);
        return d != null ? d : BY_ID.get("vanilla");
    }

    public static boolean exists(String id) { return BY_ID.containsKey(id); }

    private static final int CACHE_MAX = 12;
    private static final Map<String, int[]> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, int[]> e) { return size() > CACHE_MAX; }
    };

    /** Painted 128x128 ARGB pixels. Do not modify the returned array. Bounded LRU cache. */
    public static synchronized int[] pixels(String skinId, int variant) {
        SkinDef d = get(skinId);
        int vi = Math.floorMod(variant, d.variantCount());
        String key = d.id() + "#" + vi;
        int[] px = CACHE.get(key);
        if (px == null) {
            px = new int[SkinPainter.W * SkinPainter.H];
            SkinPainter.paint(d.style(), d.variant(vi).palette(), d.handleColor(), (d.id().hashCode() & 0xFFFF) + vi * 101, px);
            CACHE.put(key, px);
        }
        return px;
    }
}
