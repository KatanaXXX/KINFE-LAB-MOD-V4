package dev.riptide.knifelab.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riptide.knifelab.render.McCompat;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Both keybinds use Minecraft's own KeyMapping system, so they appear in Options > Controls. */
public final class KnifeKeys {
    private KnifeKeys() {}

    public static KeyMapping open;
    public static KeyMapping inspect;

    public static void register() {
        // UNVERIFIED: KeyMapping.Category.register(Identifier) as introduced in 1.21.9.
        KeyMapping.Category category = KeyMapping.Category.register(McCompat.id("main"));
        open = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.riptide_knife_lab.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        inspect = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.riptide_knife_lab.inspect", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
    }
}
