package dev.riptide.knifelab;

import dev.riptide.knifelab.config.KnifeConfig;
import dev.riptide.knifelab.gui.KnifeLabScreen;
import dev.riptide.knifelab.input.KnifeKeys;
import dev.riptide.knifelab.render.KnifeRuntime;
import dev.riptide.knifelab.render.SkinTextures;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class KnifeLabClient implements ClientModInitializer {
    public static final String MOD_ID = "riptide_knife_lab";

    @Override
    public void onInitializeClient() {
        KnifeConfig.load();
        KnifeKeys.register();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // Each key is consumed independently; one press can never trigger both actions.
            while (KnifeKeys.open.consumeClick()) {
                if (mc.screen == null && mc.player != null) mc.setScreen(new KnifeLabScreen(null));
            }
            while (KnifeKeys.inspect.consumeClick()) {
                KnifeRuntime.requestInspect(mc);
            }
            KnifeRuntime.tick(mc);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            KnifeConfig.save();
            SkinTextures.releaseAll();
        });
    }
}
