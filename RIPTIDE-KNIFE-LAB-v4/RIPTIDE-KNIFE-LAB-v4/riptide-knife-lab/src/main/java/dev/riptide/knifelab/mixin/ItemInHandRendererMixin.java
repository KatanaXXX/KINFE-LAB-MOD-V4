package dev.riptide.knifelab.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riptide.knifelab.render.FirstPersonKnife;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the first-person drawing of a held sword. No Fabric API hook exists for replacing the
 * first-person item model, so this mixin is the supported-by-necessity route. It only cancels the
 * visual render call: nothing about the item, damage, reach or networking is touched.
 *
 * UNVERIFIED: the exact parameter list of renderArmWithItem in 1.21.11 (see KNOWN_LIMITATIONS.md).
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void riptide$replaceSword(AbstractClientPlayer player, float partialTick, float pitch,
                                      InteractionHand hand, float swingProgress, ItemStack stack,
                                      float equipProgress, PoseStack poseStack,
                                      SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!FirstPersonKnife.shouldReplace(player, hand, stack)) return;
        boolean left = player.getMainArm() == HumanoidArm.LEFT;
        FirstPersonKnife.render(poseStack, collector, light, swingProgress, left);
        ci.cancel();
    }
}
