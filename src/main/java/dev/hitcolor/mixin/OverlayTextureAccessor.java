package dev.hitcolor.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gibt Zugriff auf die Overlay-Textur, in der das rote Treffer-Flackern gespeichert ist. */
@Mixin(OverlayTexture.class)
public interface OverlayTextureAccessor {
    @Accessor("texture")
    NativeImageBackedTexture hitcolor$getTexture();
}
