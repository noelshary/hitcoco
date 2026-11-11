package dev.hitcolor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent settings shared by the Right Shift client hub and its HUD. */
public final class ClientHubConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hitcolor-client.json");
    private static ClientHubConfig instance = new ClientHubConfig();

    public boolean hudEnabled = true;
    public boolean showFps = true;
    public boolean showCps = true;
    public boolean showPing = true;
    public boolean showTps = false;
    public boolean showClock = true;
    public boolean showPlaytime = true;

    public float themeHue = 0.53f;
    public float themeSaturation = 0.72f;
    public float themeBrightness = 0.92f;

    public boolean customCrosshair = false;
    public String crosshairShape = "CROSS";
    public int crosshairColor = 0xF4F7FF;
    public float crosshairSize = 8.0f;
    public float crosshairGap = 3.5f;
    public float crosshairThickness = 2.0f;

    /** Accumulated time while this client mod has been active. */
    public long playtimeMillis = 0L;

    private ClientHubConfig() {}

    public static ClientHubConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ClientHubConfig loaded = GSON.fromJson(Files.readString(FILE), ClientHubConfig.class);
                if (loaded != null) instance = loaded;
            }
        } catch (Exception e) {
            System.err.println("[HitColor] Could not load client settings: " + e.getMessage());
        }
        instance.normalise();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[HitColor] Could not save client settings: " + e.getMessage());
        }
    }

    private void normalise() {
        themeHue = finiteClamp(themeHue, 0.53f, 0f, 1f);
        themeSaturation = finiteClamp(themeSaturation, 0.72f, 0f, 1f);
        themeBrightness = finiteClamp(themeBrightness, 0.92f, 0.1f, 1f);
        crosshairSize = finiteClamp(crosshairSize, 8f, 2f, 24f);
        crosshairGap = finiteClamp(crosshairGap, 3.5f, 0f, 16f);
        crosshairThickness = finiteClamp(crosshairThickness, 2f, 1f, 8f);
        crosshairColor &= 0xFFFFFF;
        if (!"DOT".equals(crosshairShape) && !"RING".equals(crosshairShape)) crosshairShape = "CROSS";
        playtimeMillis = Math.max(0L, playtimeMillis);
    }

    private static float finiteClamp(float value, float fallback, float min, float max) {
        if (!Float.isFinite(value)) value = fallback;
        return Math.max(min, Math.min(max, value));
    }

    public static int accent() {
        return hsvToRgb(instance.themeHue, instance.themeSaturation, instance.themeBrightness);
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float s = Math.max(0f, Math.min(1f, saturation));
        float v = Math.max(0f, Math.min(1f, value));
        int sector = (int) h;
        float f = h - sector;
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);
        float r, g, b;
        switch (sector % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f;
        float g = ((rgb >> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0f;
        if (delta > 0f) {
            if (max == r) h = ((g - b) / delta) % 6f;
            else if (max == g) h = ((b - r) / delta) + 2f;
            else h = ((r - g) / delta) + 4f;
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        float s = max <= 0f ? 0f : delta / max;
        return new float[]{h, s, max};
    }
}
