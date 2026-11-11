package dev.hitcolor.mixin;

import dev.hitcolor.ClientHubConfig;
import dev.hitcolor.ClientHubHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla crosshair only when the custom reticle toggle is enabled. */
@Mixin(InGameHud.class)
public abstract class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hitcolor$renderCustomCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ClientHubConfig.get().customCrosshair) {
            ci.cancel();
            ClientHubHud.drawCustomCrosshair(context);
        }
    }
}
