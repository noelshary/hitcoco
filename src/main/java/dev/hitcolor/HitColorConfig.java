package dev.hitcolor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HitColorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hitcolor.json");
    private static HitColorConfig instance = new HitColorConfig();

    public enum Animation { STATIC, PULSE, FADE, HEARTBEAT, RAINBOW, SHIMMER, FIRE, STROBE }
    public enum HitSound { OFF, CHIME, PING, ORB }

    public boolean enabled = true;
    public int color = 0x00E5FF;
    /** 0 = unsichtbar/durchsichtig, 1 = voll deckend */
    public float strength = 0.6f;
    public Animation animation = Animation.PULSE;
    /** Tempo der Animation (0.25 - 2.0) */
    public float speed = 1.0f;
    public boolean particles = true;
    public HitSound sound = HitSound.CHIME;
    public float volume = 0.5f;

    public static HitColorConfig get() {
        return instance;
    }

    /** Setzt die Werte in-place zurück, damit offene Screens dasselbe Objekt behalten. */
    public static void reset() {
        HitColorConfig d = new HitColorConfig();
        HitColorConfig c = instance;
        c.enabled = d.enabled;
        c.color = d.color;
        c.strength = d.strength;
        c.animation = d.animation;
        c.speed = d.speed;
        c.particles = d.particles;
        c.sound = d.sound;
        c.volume = d.volume;
        save();
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                HitColorConfig loaded = GSON.fromJson(Files.readString(FILE), HitColorConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            }
        } catch (Exception e) {
            System.err.println("[HitColor] Config konnte nicht geladen werden: " + e);
        }
        if (instance.animation == null) instance.animation = Animation.PULSE;
        if (instance.sound == null) instance.sound = HitSound.CHIME;
        if (!Float.isFinite(instance.strength)) instance.strength = 0.6f;
        if (!Float.isFinite(instance.speed) || instance.speed <= 0f) instance.speed = 1.0f;
        if (!Float.isFinite(instance.volume)) instance.volume = 0.5f;
        instance.strength = Math.max(0f, Math.min(1f, instance.strength));
        instance.speed = Math.max(0.25f, Math.min(2.0f, instance.speed));
        instance.volume = Math.max(0f, Math.min(1f, instance.volume));
        instance.color &= 0xFFFFFF;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[HitColor] Config konnte nicht gespeichert werden: " + e);
        }
    }
}
