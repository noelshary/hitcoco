package dev.hitcolor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HitColorClient implements ClientModInitializer {
    public static final String MOD_ID = "hitcolor";
    private static KeyBinding openKey;
    private static KeyBinding hubKey;

    @Override
    public void onInitializeClient() {
        HitColorConfig.load();
        ClientHubConfig.load();
        ClientHubHud.register();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hitcolor.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))
        ));
        hubKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hitcolor.hub",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyBinding.Category.create(Identifier.of(MOD_ID, "hub"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hubKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClientHubScreen(null));
            }
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HitColorScreen(null));
            }
            ClientHubHud.tick(client);
            HitEffects.tick(client);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> HitEffects.invalidate());

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()
                && entity instanceof LivingEntity
                && player == MinecraftClient.getInstance().player) {
                HitEffects.onHit(entity);
            }
            return ActionResult.PASS;
        });
    }
}
