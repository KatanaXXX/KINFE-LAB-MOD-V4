package dev.riptide.knifelab.gui;

import dev.riptide.knifelab.config.KnifeConfig;
import dev.riptide.knifelab.core.Animator;
import dev.riptide.knifelab.core.Mesh;
import dev.riptide.knifelab.core.Pose;
import dev.riptide.knifelab.core.Rasterizer;
import dev.riptide.knifelab.registry.KnifeDef;
import dev.riptide.knifelab.registry.Knives;
import dev.riptide.knifelab.registry.SkinDef;
import dev.riptide.knifelab.registry.Skins;
import dev.riptide.knifelab.render.KnifeRuntime;
import dev.riptide.knifelab.render.McCompat;
import dev.riptide.knifelab.sound.KnifeSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** The Knife Lab: browse, preview, equip and inspect knives. Every control is functional. */
public final class KnifeLabScreen extends Screen {
    private static final int BG = 0xFF08101E, PANEL = 0xF0101C30, EDGE = 0xFF1C5C66, AQUA = 0xFF40E0D0,
            SILVER = 0xFFC8D0DC, DIM = 0xFF8494AC, WHITE = 0xFFFFFFFF;
    private static final int LW = 150, RW = 176, MARGIN = 8, TOP = 30, PREVIEW = 256;

    private final Screen parent;
    private final KnifeConfig cfg = KnifeConfig.get();
    private final Random random = new Random();

    private String selKnife, selSkin;
    private int selVariant;
    private KnifeDef.Category category;          // null = all
    private boolean favOnly;
    private String search = "";
    private int knifePage, skinPage;

    private final List<AbstractWidget> dynamic = new ArrayList<>();
    private EditBox searchBox;

    // preview
    private Rasterizer rasterizer;
    private int[] pixels;
    private McCompat.Tex texture;
    private final Animator anim = new Animator();
    private float yaw = -20f, pitch = 6f, zoom = 1f;
    private boolean autoRotate;
    private long lastNs, lastRenderNs;
    private float dtAcc;

    // layout (computed in init)
    private int barY, pvX, pvY, pvS, rightX, rowsKnife, rowsSkin, listY, skinY;

    public KnifeLabScreen(Screen parent) {
        super(Component.literal("Knife Lab"));
        this.parent = parent;
        selKnife = cfg.knife;
        selSkin = cfg.skin;
        selVariant = cfg.variant;
        autoRotate = cfg.autoRotate;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        cfg.autoRotate = autoRotate;
        KnifeConfig.save();
        if (texture != null) { McCompat.destroy(texture); texture = null; }
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        dynamic.clear();
        barY = height - 24;
        rightX = width - MARGIN - RW;
        listY = 100;
        rowsKnife = Math.max(3, (barY - 58 - listY) / 15);
        skinY = TOP + 88;
        rowsSkin = Math.max(3, (barY - 52 - skinY) / 14);

        int cx0 = MARGIN + LW + MARGIN, cx1 = rightX - MARGIN, cw = cx1 - cx0;
        pvY = TOP + 14;
        pvS = Math.max(64, Math.min(cw, barY - 34 - pvY));
        pvX = cx0 + (cw - pvS) / 2;

        if (rasterizer == null) {
            rasterizer = new Rasterizer(PREVIEW, cfg.previewQuality >= 2);
            pixels = new int[PREVIEW * PREVIEW];
        }
        if (texture == null) texture = McCompat.createTexture("preview", PREVIEW, PREVIEW);
        anim.setKnife(selKnife);

        // left: search, category, favourites
        searchBox = new EditBox(font, MARGIN, TOP + 4, LW, 16, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search knives..."));
        searchBox.setMaxLength(24);
        searchBox.setValue(search);
        searchBox.setResponder(s -> { search = s.toLowerCase(Locale.ROOT); knifePage = 0; rebuildDynamic(); });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(categoryLabel(), b -> {
            KnifeDef.Category[] all = KnifeDef.Category.values();
            if (category == null) category = all[0];
            else if (category.ordinal() == all.length - 1) category = null;
            else category = all[category.ordinal() + 1];
            b.setMessage(categoryLabel());
            knifePage = 0;
            rebuildDynamic();
        }).bounds(MARGIN, TOP + 24, LW, 16).build());

        addRenderableWidget(Button.builder(favLabel(), b -> {
            favOnly = !favOnly;
            b.setMessage(favLabel());
            knifePage = 0;
            rebuildDynamic();
        }).bounds(MARGIN, TOP + 44, LW, 16).build());

        // preview controls
        int cy = barY - 28, bw = Math.max(34, (pvS - 5 * 3) / 6);
        int cx = pvX;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { autoRotate = false; yaw -= 20f; })
                .bounds(cx, cy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { autoRotate = false; yaw += 20f; })
                .bounds(cx + (bw + 3), cy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Zoom-"), b -> zoom = Math.max(0.5f, zoom - 0.15f))
                .bounds(cx + 2 * (bw + 3), cy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Zoom+"), b -> zoom = Math.min(2.5f, zoom + 0.15f))
                .bounds(cx + 3 * (bw + 3), cy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Auto"), b -> autoRotate = !autoRotate)
                .bounds(cx + 4 * (bw + 3), cy, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Spin"), b -> { anim.startInspect(); })
                .bounds(cx + 5 * (bw + 3), cy, bw, 18).build());

        // bottom bar: Equip, Inspect, Randomize, Favorite, Reset, Settings, Done
        String[] names = {"Equip", "Inspect", "Randomize", "Favorite", "Reset", "Settings", "Done"};
        int total = width - 2 * MARGIN, bwid = (total - 6 * 4) / 7;
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(names[i]), b -> barAction(idx))
                    .bounds(MARGIN + i * (bwid + 4), barY, bwid, 20).build());
        }
        rebuildDynamic();
    }

    private Component categoryLabel() {
        return Component.literal("Category: " + (category == null ? "All" : category.label));
    }

    private Component favLabel() {
        return Component.literal("Favorites only: " + (favOnly ? "ON" : "OFF"));
    }

    private void barAction(int i) {
        switch (i) {
            case 0 -> equip();
            case 1 -> { if (!anim.startInspect()) { anim.reset(); anim.startInspect(); } }
            case 2 -> randomize();
            case 3 -> { cfg.toggleFavorite(selKnife); KnifeConfig.save(); rebuildDynamic(); }
            case 4 -> { selKnife = cfg.knife; selSkin = cfg.skin; selVariant = cfg.variant; yaw = -20f; pitch = 6f; zoom = 1f; onSelectionChanged(false); }
            case 5 -> minecraft.setScreen(new SettingsScreen(this));
            default -> onClose();
        }
    }

    private void equip() {
        cfg.knife = selKnife;
        cfg.skin = selSkin;
        cfg.variant = selVariant;
        cfg.pushRecent(selKnife);
        KnifeConfig.save();
        KnifeRuntime.onEquipChanged();
        KnifeSounds.play(KnifeSounds.Kind.HANDLING);
        rebuildDynamic();
    }

    private void randomize() {
        selKnife = Knives.ALL.get(random.nextInt(Knives.ALL.size())).id();
        SkinDef s = Skins.ALL.get(random.nextInt(Skins.ALL.size()));
        selSkin = s.id();
        selVariant = random.nextInt(s.variantCount());
        onSelectionChanged(true);
    }

    private void onSelectionChanged(boolean playDraw) {
        anim.setKnife(selKnife);
        if (playDraw) anim.startDraw(false);
        skinPage = Math.max(0, indexOfSkin(selSkin) / rowsSkin);
        rebuildDynamic();
    }

    private int indexOfSkin(String id) {
        for (int i = 0; i < Skins.ALL.size(); i++) if (Skins.ALL.get(i).id().equals(id)) return i;
        return 0;
    }

    // ------------------------------------------------------------------ dynamic widgets

    private List<KnifeDef> filtered() {
        List<KnifeDef> out = new ArrayList<>();
        for (KnifeDef k : Knives.ALL) {
            if (category != null && k.category() != category) continue;
            if (favOnly && !cfg.favorites.contains(k.id())) continue;
            if (!search.isEmpty() && !k.name().toLowerCase(Locale.ROOT).contains(search)) continue;
            out.add(k);
        }
        return out;
    }

    private void rebuildDynamic() {
        for (AbstractWidget w : dynamic) removeWidget(w);
        dynamic.clear();

        // knife list with paging
        List<KnifeDef> list = filtered();
        int pages = Math.max(1, (list.size() + rowsKnife - 1) / rowsKnife);
        knifePage = Math.max(0, Math.min(pages - 1, knifePage));
        for (int i = 0; i < rowsKnife; i++) {
            int idx = knifePage * rowsKnife + i;
            if (idx >= list.size()) break;
            KnifeDef k = list.get(idx);
            String label = (cfg.favorites.contains(k.id()) ? "* " : "") + k.name();
            Component text = Component.literal(label);
            if (k.id().equals(selKnife)) text = Component.literal(label).withStyle(ChatFormatting.AQUA);
            else if (k.id().equals(cfg.knife)) text = Component.literal(label).withStyle(ChatFormatting.GREEN);
            dynamic.add(addRenderableWidget(Button.builder(text, b -> {
                selKnife = k.id();
                onSelectionChanged(true);
            }).bounds(MARGIN, listY + i * 15, LW, 14).build()));
        }
        int py = listY + rowsKnife * 15 + 2;
        dynamic.add(addRenderableWidget(Button.builder(Component.literal("<"), b -> { knifePage--; rebuildDynamic(); })
                .bounds(MARGIN, py, 22, 14).build()));
        dynamic.add(addRenderableWidget(Button.builder(Component.literal(">"), b -> { knifePage++; rebuildDynamic(); })
                .bounds(MARGIN + LW - 22, py, 22, 14).build()));

        // recently used
        int rw = (LW - 8) / 3;
        for (int i = 0; i < Math.min(3, cfg.recent.size()); i++) {
            String id = cfg.recent.get(i);
            String shortName = font.plainSubstrByWidth(Knives.get(id).name(), rw - 4);
            dynamic.add(addRenderableWidget(Button.builder(Component.literal(shortName), b -> {
                selKnife = id;
                onSelectionChanged(true);
            }).bounds(MARGIN + i * (rw + 4), barY - 26, rw, 14).build()));
        }

        // skins with paging
        int spages = Math.max(1, (Skins.ALL.size() + rowsSkin - 1) / rowsSkin);
        skinPage = Math.max(0, Math.min(spages - 1, skinPage));
        for (int i = 0; i < rowsSkin; i++) {
            int idx = skinPage * rowsSkin + i;
            if (idx >= Skins.ALL.size()) break;
            SkinDef s = Skins.ALL.get(idx);
            Component text = Component.literal(s.name());
            if (s.id().equals(selSkin)) text = Component.literal(s.name()).withStyle(ChatFormatting.AQUA);
            else if (s.id().equals(cfg.skin)) text = Component.literal(s.name()).withStyle(ChatFormatting.GREEN);
            dynamic.add(addRenderableWidget(Button.builder(text, b -> {
                selSkin = s.id();
                selVariant = 0;
                rebuildDynamic();
            }).bounds(rightX, skinY + i * 14, RW, 13).build()));
        }
        int sy = skinY + rowsSkin * 14 + 2;
        dynamic.add(addRenderableWidget(Button.builder(Component.literal("<"), b -> { skinPage--; rebuildDynamic(); })
                .bounds(rightX, sy, 22, 14).build()));
        dynamic.add(addRenderableWidget(Button.builder(Component.literal(">"), b -> { skinPage++; rebuildDynamic(); })
                .bounds(rightX + RW - 22, sy, 22, 14).build()));

        // variant selector (cycles through the finish variants)
        SkinDef sd = Skins.get(selSkin);
        selVariant = Math.floorMod(selVariant, sd.variantCount());
        dynamic.add(addRenderableWidget(Button.builder(
                        Component.literal("Variant: " + sd.variant(selVariant).name() + " (" + (selVariant + 1) + "/" + sd.variantCount() + ")"),
                        b -> { selVariant = (selVariant + 1) % sd.variantCount(); rebuildDynamic(); })
                .bounds(rightX, barY - 26, RW, 16).build()));
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updatePreview();
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void updatePreview() {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        dtAcc += dt;
        if (autoRotate) yaw = (yaw + 30f * dt) % 360f;
        if (now - lastRenderNs < 33_000_000L) return;      // cap the software preview at ~30 fps
        lastRenderNs = now;
        Pose pose = anim.update(dtAcc, 0f, true, true, true, cfg.animSpeed);
        dtAcc = 0f;
        Mesh mesh = Knives.mesh(selKnife);
        rasterizer.render(mesh, pose, Skins.pixels(selSkin, selVariant), yaw, pitch, zoom, cfg.previewQuality, pixels);
        McCompat.upload(texture, pixels);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, EDGE);
        g.fill(x, y + h - 1, x + w, y + h, EDGE);
        g.fill(x, y, x + 1, y + h, EDGE);
        g.fill(x + w - 1, y, x + w, y + h, EDGE);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, BG, 0xFF0C1C2C);
        g.fill(0, 0, width, 26, 0xFF060C16);
        g.fill(0, 26, width, 27, AQUA);
        g.drawString(font, "RIPTIDE", MARGIN, 9, AQUA);
        g.drawString(font, "KNIFE LAB", MARGIN + font.width("RIPTIDE ") + 2, 9, SILVER);

        Knives.get(cfg.knife);
        String eq = "Equipped: " + Knives.get(cfg.knife).name() + " | " + Skins.get(cfg.skin).name()
                + " (" + Skins.get(cfg.skin).variant(cfg.variant).name() + ")";
        g.drawString(font, eq, width - MARGIN - font.width(eq), 9, SILVER);

        panel(g, MARGIN - 2, TOP, LW + 4, barY - TOP - 4);
        panel(g, rightX - 2, TOP, RW + 4, barY - TOP - 4);
        int cx0 = MARGIN + LW + MARGIN;
        panel(g, cx0 - 2, TOP, rightX - MARGIN - cx0 + 4, barY - TOP - 4);

        // left labels
        List<KnifeDef> list = filtered();
        int pages = Math.max(1, (list.size() + rowsKnife - 1) / rowsKnife);
        g.drawString(font, "Page " + (knifePage + 1) + "/" + pages + "  (" + list.size() + ")",
                MARGIN + 28, listY + rowsKnife * 15 + 5, DIM);
        g.drawString(font, "Recently used", MARGIN, barY - 38, DIM);
        if (list.isEmpty()) g.drawString(font, "No knives match", MARGIN + 4, listY + 4, DIM);

        // centre: preview image
        g.fill(pvX - 1, pvY - 1, pvX + pvS + 1, pvY + pvS + 1, EDGE);
        g.fill(pvX, pvY, pvX + pvS, pvY + pvS, 0xFF0A1626);
        if (texture != null) McCompat.blit(g, texture, pvX, pvY, pvS, pvS);
        KnifeDef k = Knives.get(selKnife);
        String title = k.name() + (selKnife.equals(cfg.knife) && selSkin.equals(cfg.skin) && selVariant == cfg.variant ? "  [EQUIPPED]" : "");
        g.drawString(font, title, cx0 + 2, TOP + 4, WHITE);

        // right: info
        g.drawString(font, k.name(), rightX, TOP + 4, AQUA);
        g.drawString(font, k.category().label + (cfg.favorites.contains(k.id()) ? "  *favorite" : ""), rightX, TOP + 15, DIM);
        List<FormattedCharSequence> lines = font.split(Component.literal(k.description()), RW);
        for (int i = 0; i < Math.min(5, lines.size()); i++) g.drawString(font, lines.get(i), rightX, TOP + 28 + i * 10, SILVER);
        SkinDef sd = Skins.get(selSkin);
        g.drawString(font, "Finishes - " + sd.name(), rightX, skinY - 11, AQUA);
        int spages = Math.max(1, (Skins.ALL.size() + rowsSkin - 1) / rowsSkin);
        g.drawString(font, "Page " + (skinPage + 1) + "/" + spages, rightX + 28, skinY + rowsSkin * 14 + 5, DIM);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= pvX && mx <= pvX + pvS && my >= pvY && my <= pvY + pvS) {
            zoom = (float) Math.max(0.5, Math.min(2.5, zoom + sy * 0.1));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
