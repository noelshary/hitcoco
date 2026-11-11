package dev.hitcolor;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class HitColorScreen extends Screen {
    private static final int PW = 440, PH = 272;
    private static final int SV = 96;      // Größe des Farbfelds
    private static final int HW = 12;      // Breite der Farbton-Leiste
    private static final int CELL = 4;     // Auflösung des Farbfelds

    private final Screen parent;
    private final HitColorConfig cfg = HitColorConfig.get();
    private final PreviewBot bot = new PreviewBot();
    private TextFieldWidget hexField;
    private int left, top;

    private float hue, sat, val;
    private int drag = 0;                  // 0 = nichts, 1 = Farbfeld, 2 = Farbton
    private boolean wasDown = false;
    private int lastSig = 0;
    private boolean firstFrame = true;

    public HitColorScreen(Screen parent) {
        super(HitText.tr("hitcolor.screen.title"));
        this.parent = parent;
    }

    // Positionen
    private int svX() { return left + 12; }
    private int svY() { return top + 24; }
    private int hueX() { return left + 12 + SV + 8; }

    @Override
    protected void init() {
        left = (width - PW) / 2;
        top = Math.max(2, (height - PH) / 2);
        int x1 = left + 12, x2 = left + 224, cw = 204;
        syncHsv();

        // Info-Spalte neben dem Farbfeld: HEX + Zufall
        int ic = left + 136;
        hexField = new TextFieldWidget(textRenderer, ic, top + 60, 70, 16, HitText.lit(""));
        hexField.setMaxLength(7);
        hexField.setText(hex(cfg.color));
        hexField.setChangedListener(this::onHex);
        addDrawableChild(hexField);

        addDrawableChild(ButtonWidget.builder(HitText.tr("hitcolor.option.random"), b -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            selectColor(HitEffects.hsvToRgb(r.nextFloat(), 0.7f + 0.3f * r.nextFloat(), 1f));
        }).dimensions(ic, top + 80, 70, 16).build());

        // Schnellwahl-Palette (20 x 2)
        int[] pal = ColorPalette.COLORS;
        for (int i = 0; i < pal.length; i++) {
            final int rgb = pal[i];
            addDrawableChild(new SwatchButton(
                x1 + (i % 20) * 10, top + 102 + (i / 20) * 10, 9, rgb,
                () -> selectColor(rgb), () -> cfg.color == rgb));
        }

        // Hit-Button unter dem Bot
        addDrawableChild(ButtonWidget.builder(HitText.tr("hitcolor.option.hit"),
            b -> bot.hit(cfg, System.currentTimeMillis()))
            .dimensions(x2 + 52, top + 124, 100, 16).build());

        // Einstellungen
        int r0 = top + 148, r1 = top + 170, r2 = top + 192, r3 = top + 214;

        addDrawableChild(ButtonWidget.builder(enabledText(), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(enabledText());
        }).dimensions(x1, r0, cw, 18).build());

        addDrawableChild(ButtonWidget.builder(animText(), b -> {
            HitColorConfig.Animation[] all = HitColorConfig.Animation.values();
            cfg.animation = all[(cfg.animation.ordinal() + 1) % all.length];
            b.setMessage(animText());
        }).dimensions(x2, r0, cw, 18).build());

        addDrawableChild(new ValueSlider(x1, r1, cw, 18, cfg.strength,
            v -> HitText.tr("hitcolor.option.strength", Math.round(v * 100)),
            v -> cfg.strength = v));

        float speedV = Math.max(0f, Math.min(1f, (cfg.speed - 0.25f) / 1.75f));
        addDrawableChild(new ValueSlider(x2, r1, cw, 18, speedV,
            v -> HitText.tr("hitcolor.option.speed", String.format(Locale.ROOT, "%.1f", 0.25f + v * 1.75f)),
            v -> cfg.speed = 0.25f + v * 1.75f));

        addDrawableChild(ButtonWidget.builder(soundText(), b -> {
            HitColorConfig.HitSound[] all = HitColorConfig.HitSound.values();
            cfg.sound = all[(cfg.sound.ordinal() + 1) % all.length];
            b.setMessage(soundText());
            HitEffects.playSound(cfg);
        }).dimensions(x1, r2, cw, 18).build());

        addDrawableChild(new ValueSlider(x2, r2, cw, 18, cfg.volume,
            v -> HitText.tr("hitcolor.option.volume", Math.round(v * 100)),
            v -> cfg.volume = v));

        addDrawableChild(ButtonWidget.builder(particleText(), b -> {
            cfg.particles = !cfg.particles;
            b.setMessage(particleText());
        }).dimensions(x1, r3, cw, 18).build());

        addDrawableChild(ButtonWidget.builder(HitText.tr("hitcolor.option.reset"), b -> {
            HitColorConfig.reset();
            HitEffects.invalidate();
            clearAndInit();
        }).dimensions(x2, r3, cw, 18).build());

        addDrawableChild(ButtonWidget.builder(HitText.tr("gui.done"), b -> close())
            .dimensions(x1, top + 244, PW - 24, 18).build());
    }

    // ---------- Farb-Logik ----------

    private void syncHsv() {
        float[] hsv = HitEffects.rgbToHsv(cfg.color);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
    }

    private void applyHsv() {
        int rgb = HitEffects.hsvToRgb(hue, sat, val);
        cfg.color = rgb;
        if (hexField != null) hexField.setText(hex(rgb));
    }

    private void selectColor(int rgb) {
        cfg.color = rgb;
        syncHsv();
        hexField.setText(hex(rgb));
    }

    private void onHex(String s) {
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return;
        try {
            int rgb = Integer.parseInt(s, 16) & 0xFFFFFF;
            if (rgb == cfg.color) return;
            cfg.color = rgb;
            syncHsv();
        } catch (NumberFormatException ignored) {
        }
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // ---------- Texte ----------

    private net.minecraft.text.Text onOff(boolean b) {
        return HitText.tr(b ? "options.on" : "options.off");
    }

    private net.minecraft.text.Text enabledText() {
        return HitText.tr("hitcolor.option.enabled", onOff(cfg.enabled));
    }

    private net.minecraft.text.Text particleText() {
        return HitText.tr("hitcolor.option.particles", onOff(cfg.particles));
    }

    private net.minecraft.text.Text animText() {
        return HitText.tr("hitcolor.option.animation",
            HitText.tr("hitcolor.animation." + cfg.animation.name().toLowerCase(Locale.ROOT)));
    }

    private net.minecraft.text.Text soundText() {
        return HitText.tr("hitcolor.option.sound",
            HitText.tr("hitcolor.sound." + cfg.sound.name().toLowerCase(Locale.ROOT)));
    }

    // ---------- Zeichnen ----------

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        long now = System.currentTimeMillis();
        int accent = HitEffects.rgbAt(cfg, now);

        // Panel mit leuchtendem Rand in der aktuellen Trefferfarbe
        ctx.fill(left - 1, top - 1, left + PW + 1, top + PH + 1, 0xFF000000 | accent);
        ctx.fill(left, top, left + PW, top + PH, 0xF0101018);

        // Farbfeld (Sättigung x Helligkeit)
        int sx = svX(), sy = svY();
        ctx.fill(sx - 1, sy - 1, sx + SV + 1, sy + SV + 1, 0xFF3A3A4A);
        for (int cy = 0; cy < SV; cy += CELL) {
            for (int cx = 0; cx < SV; cx += CELL) {
                float s = (cx + CELL / 2f) / SV;
                float v = 1f - (cy + CELL / 2f) / SV;
                ctx.fill(sx + cx, sy + cy, sx + cx + CELL, sy + cy + CELL,
                    0xFF000000 | HitEffects.hsvToRgb(hue, s, v));
            }
        }
        int mx = sx + Math.round(sat * (SV - 1));
        int my = sy + Math.round((1f - val) * (SV - 1));
        ctx.fill(mx - 4, my - 4, mx + 4, my + 4, 0xFFFFFFFF);
        ctx.fill(mx - 3, my - 3, mx + 3, my + 3, 0xFF000000 | cfg.color);

        // Farbton-Leiste
        int hx = hueX();
        ctx.fill(hx - 1, sy - 1, hx + HW + 1, sy + SV + 1, 0xFF3A3A4A);
        for (int cy = 0; cy < SV; cy += CELL) {
            ctx.fill(hx, sy + cy, hx + HW, sy + cy + CELL,
                0xFF000000 | HitEffects.hsvToRgb((cy + CELL / 2f) / SV, 1f, 1f));
        }
        int hy = sy + Math.round(hue * (SV - 1));
        ctx.fill(hx - 3, hy - 1, hx + HW + 3, hy + 2, 0xFFFFFFFF);

        // Aktuelle Farbe
        int ic = left + 136;
        ctx.fill(ic - 1, top + 23, ic + 71, top + 55, 0xFF3A3A4A);
        ctx.fill(ic, top + 24, ic + 70, top + 54, 0xFF000000 | cfg.color);

        // Bot-Bühne
        bot.render(ctx, textRenderer, left + 224, top + 24, 204, 98, cfg, now);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();

        // Maus auf Farbfeld / Farbton-Leiste (per Polling, damit kein Maus-Event-API nötig ist)
        long window = GLFW.glfwGetCurrentContext();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        int sx = svX(), sy = svY(), hx = hueX();
        if (down) {
            if (drag == 0 && !wasDown) {
                if (mouseX >= sx && mouseX < sx + SV && mouseY >= sy && mouseY < sy + SV) drag = 1;
                else if (mouseX >= hx - 2 && mouseX < hx + HW + 2 && mouseY >= sy && mouseY < sy + SV) drag = 2;
            }
            if (drag == 1) {
                sat = clamp01((mouseX - sx) / (float) (SV - 1));
                val = 1f - clamp01((mouseY - sy) / (float) (SV - 1));
                applyHsv();
            } else if (drag == 2) {
                hue = clamp01((mouseY - sy) / (float) (SV - 1));
                applyHsv();
            }
        } else {
            drag = 0;
        }
        wasDown = down;

        // Bot leuchtet kurz auf, sobald sich Farbe/Stärke/Animation ändert
        int sig = Objects.hash(cfg.color, cfg.strength, cfg.animation, cfg.speed, cfg.enabled);
        if (firstFrame || sig != lastSig) {
            bot.flash(now);
            lastSig = sig;
            firstFrame = false;
        }

        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 8, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        HitColorConfig.save();
    }

    // ---------- Slider ----------
    private interface Label { net.minecraft.text.Text make(float v); }
    private interface Setter { void set(float v); }

    private static class ValueSlider extends SliderWidget {
        private final Label label;
        private final Setter setter;

        ValueSlider(int x, int y, int w, int h, float value, Label label, Setter setter) {
            super(x, y, w, h, HitText.lit(""), value);
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(label.make((float) value));
        }

        @Override
        protected void applyValue() {
            setter.set((float) value);
        }
    }
}
