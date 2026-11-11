package dev.hitcolor;

import dev.hitcolor.mixin.OverlayTextureAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.concurrent.ThreadLocalRandom;

/** Berechnet die Trefferfarbe, schreibt sie in die Overlay-Textur und spielt Partikel/Sounds. */
public final class HitEffects {
    /** Vanilla: rot mit Alpha 178 (ARGB). */
    public static final int VANILLA_ARGB = (178 << 24) | 0xFF0000;

    /** Zeitpunkt unseres letzten Angriffs (für die FADE-Animation). */
    private static volatile long lastAttackMs = 0;
    private static int lastArgb = 0;

    private HitEffects() {}

    public static void invalidate() {
        lastArgb = 0;
    }

    public static void tick(MinecraftClient client) {
        if (client.gameRenderer == null) return;
        HitColorConfig cfg = HitColorConfig.get();
        int argb = cfg.enabled ? argbAt(cfg, System.currentTimeMillis()) : VANILLA_ARGB;
        if (argb == lastArgb) return;
        apply(client, argb);
    }

    private static void apply(MinecraftClient client, int argb) {
        NativeImageBackedTexture tex =
            ((OverlayTextureAccessor) client.gameRenderer.getOverlayTexture()).hitcolor$getTexture();
        NativeImage img = tex.getImage();
        if (img == null) return;
        // Zeilen 0-7 der 16x16 Overlay-Textur sind der "Treffer"-Bereich.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                img.setColorArgb(x, y, argb);
            }
        }
        tex.upload();
        lastArgb = argb;
    }

    public static int argbAt(HitColorConfig cfg, long millis) {
        return argbAt(cfg, millis, lastAttackMs);
    }

    /**
     * ARGB-Wert für den Shader: Der Shader rechnet mix(overlay.rgb, entity.rgb, overlay.a).
     * Kleines Alpha = starke Farbe, großes Alpha = durchsichtig.
     *
     * @param hitMs Zeitpunkt des Treffers (für FADE)
     */
    public static int argbAt(HitColorConfig cfg, long millis, long hitMs) {
        float sp = cfg.speed <= 0f ? 1f : cfg.speed;
        float t = millis / 1000f * sp;
        float since = (millis - hitMs) / 1000f;
        float s = cfg.strength;
        int rgb = cfg.color;

        switch (cfg.animation) {
            case PULSE -> s *= 0.55f + 0.45f * (0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2 * 1.2));
            case FADE -> {
                // Nach einem Treffer innerhalb von 0,5 s vollständig ausblenden.
                // Vor dem ersten Treffer bleibt die Overlay-Farbe unsichtbar.
                float elapsed = Math.max(0f, since) * sp;
                s *= elapsed < 0.5f ? 1f - elapsed / 0.5f : 0f;
            }
            case HEARTBEAT -> {
                float ph = t % 1f;
                float b1 = (float) Math.exp(-Math.pow(ph * 9f, 2));
                float b2 = 0.75f * (float) Math.exp(-Math.pow((ph - 0.28f) * 9f, 2));
                s *= 0.35f + 0.65f * Math.max(b1, b2);
            }
            case RAINBOW -> rgb = hsvToRgb((t * 0.5f) % 1f, 0.85f, 1f);
            case SHIMMER -> {
                float[] hsv = rgbToHsv(rgb);
                rgb = hsvToRgb(hsv[0] + 0.08f * (float) Math.sin(t * Math.PI * 2 * 0.8), hsv[1], hsv[2]);
            }
            case FIRE -> {
                float n = 0.5f + 0.25f * (float) Math.sin(t * 13f) + 0.25f * (float) Math.sin(t * 23.7f);
                rgb = hsvToRgb(0.01f + 0.10f * n, 1f, 1f);
                s *= 0.7f + 0.3f * n;
            }
            case STROBE -> s *= (((int) (t * 5)) % 2 == 0) ? 1f : 0.25f;
            default -> { }
        }

        s = Math.max(0f, Math.min(1f, s));
        int alpha = Math.round(255f * (1f - s));
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int rgbAt(HitColorConfig cfg, long millis) {
        return argbAt(cfg, millis) & 0xFFFFFF;
    }

    /** Simuliert den Shader: mix(overlay.rgb, base.rgb, overlay.a). */
    public static int mixOverlay(int baseRgb, int argb) {
        float a = ((argb >>> 24) & 255) / 255f;
        int r = Math.round(((argb >> 16) & 255) * (1 - a) + ((baseRgb >> 16) & 255) * a);
        int g = Math.round(((argb >> 8) & 255) * (1 - a) + ((baseRgb >> 8) & 255) * a);
        int b = Math.round((argb & 255) * (1 - a) + (baseRgb & 255) * a);
        return (r << 16) | (g << 8) | b;
    }

    // ---------- Partikel & Sounds ----------

    public static void onHit(Entity target) {
        HitColorConfig cfg = HitColorConfig.get();
        if (!cfg.enabled) return;
        lastAttackMs = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;

        if (cfg.particles) {
            int rgb = rgbAt(cfg, System.currentTimeMillis());
            ThreadLocalRandom r = ThreadLocalRandom.current();
            double x = target.getX();
            double y = target.getBodyY(0.5);
            double z = target.getZ();
            for (int i = 0; i < 14; i++) {
                world.addParticleClient(
                    new DustParticleEffect(rgb, 1.2f),
                    x + (r.nextDouble() - 0.5) * 0.6,
                    y + (r.nextDouble() - 0.5) * 0.8,
                    z + (r.nextDouble() - 0.5) * 0.6,
                    (r.nextDouble() - 0.5) * 0.2,
                    r.nextDouble() * 0.15,
                    (r.nextDouble() - 0.5) * 0.2
                );
            }
        }
        playSound(cfg);
    }

    public static void playSound(HitColorConfig cfg) {
        SoundEvent ev = switch (cfg.sound) {
            case CHIME -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case PING -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case ORB -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> null;
        };
        if (ev == null || cfg.volume <= 0f) return;
        float pitch = 0.9f + ThreadLocalRandom.current().nextFloat() * 0.4f;
        MinecraftClient.getInstance().getSoundManager()
            .play(PositionedSoundInstance.master(ev, pitch, cfg.volume));
    }

    // ---------- Farb-Helfer ----------

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    public static int hsvToRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        switch ((int) (h * 6)) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
