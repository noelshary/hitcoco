package dev.hitcolor;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Kleiner Test-Bot im Menü: zeigt die Trefferfarbe live und kann per "Hit" geschlagen werden. */
public class PreviewBot {
    private static final int U = 2; // Pixel pro Einheit

    private static final class Spark {
        float x, y, vx, vy;
        long born;
    }

    private long flashUntil = 0;
    private long envStart = 0;
    private long hitStart = -100000;
    private int sparkX, sparkY;
    private final List<Spark> sparks = new ArrayList<>();

    /** Kurzes Aufleuchten ohne Schlag (z. B. wenn die Farbe geändert wird). */
    public void flash(long now) {
        envStart = now;
        flashUntil = now + 1000;
    }

    /** Voller Treffer: Rückstoß, Aufleuchten, Funken. */
    public void hit(HitColorConfig cfg, long now) {
        hitStart = now;
        envStart = now;
        flashUntil = now + 600;
        if (cfg.particles) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 18; i++) {
                Spark s = new Spark();
                s.x = sparkX;
                s.y = sparkY;
                s.vx = r.nextFloat() * 100f - 15f;
                s.vy = -r.nextFloat() * 80f - 10f;
                s.born = now;
                sparks.add(s);
            }
        }
        HitEffects.playSound(cfg);
    }

    private static void part(DrawContext ctx, int x1, int y1, int x2, int y2, int base, boolean active, int argb) {
        int c = active ? HitEffects.mixOverlay(base, argb) : base;
        ctx.fill(x1, y1, x2, y2, 0xFF000000 | c);
    }

    public void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, HitColorConfig cfg, long now) {
        boolean active = now < flashUntil;
        int argb = active ? (cfg.enabled ? HitEffects.argbAt(cfg, now, envStart) : HitEffects.VANILLA_ARGB) : 0;
        int hitRgb = (cfg.enabled ? HitEffects.argbAt(cfg, now, envStart) : HitEffects.VANILLA_ARGB) & 0xFFFFFF;

        // Bühne
        int cx = x + w / 2;
        int bottom = y + h - 12;
        int top = bottom - 32 * U;
        int border = active ? (0xFF000000 | hitRgb) : 0xFF2C2C3C;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        ctx.fill(x, y, x + w, y + h, 0xFF0B0B12);
        ctx.fill(x, bottom + 2, x + w, y + h, 0xFF14141D);
        ctx.fill(cx - 16, bottom, cx + 16, bottom + 3, 0x66000000);

        // Rückstoß-Animation
        float ht = (now - hitStart) / 1000f;
        int dx = 0, dy = 0;
        if (ht >= 0f && ht < 0.45f) {
            float k = ht / 0.45f;
            dx = Math.round((float) Math.sin(Math.PI * k) * 10f + (float) Math.sin(ht * 70f) * (1f - k) * 2.5f);
            dy = Math.round(-(float) Math.sin(Math.PI * k) * 3f);
        }
        int ox = cx + dx;
        int oy = dy;
        sparkX = ox;
        sparkY = top + 20 * U / 2 + 14;

        // Körperteile (Beine, Torso, Arme, Kopf, Augen, Antenne)
        part(ctx, ox - 4 * U, top + 20 * U + oy, ox, top + 32 * U + oy, 0x4A5560, active, argb);
        part(ctx, ox, top + 20 * U + oy, ox + 4 * U, top + 32 * U + oy, 0x4A5560, active, argb);
        part(ctx, ox - 4 * U, top + 8 * U + oy, ox + 4 * U, top + 20 * U + oy, 0x6B7A8A, active, argb);
        part(ctx, ox - 8 * U, top + 8 * U + oy, ox - 4 * U, top + 20 * U + oy, 0x8794A1, active, argb);
        part(ctx, ox + 4 * U, top + 8 * U + oy, ox + 8 * U, top + 20 * U + oy, 0x8794A1, active, argb);
        part(ctx, ox - 3, top + 8 * U + 6 + oy, ox + 3, top + 8 * U + 12 + oy, 0x4DD0E1, active, argb);
        part(ctx, ox - 4 * U, top + oy, ox + 4 * U, top + 8 * U + oy, 0x9AA5B1, active, argb);
        part(ctx, ox - 6, top + 6 + oy, ox - 2, top + 10 + oy, 0x1B1F24, active, argb);
        part(ctx, ox + 2, top + 6 + oy, ox + 6, top + 10 + oy, 0x1B1F24, active, argb);
        part(ctx, ox - 1, top - 6 + oy, ox + 1, top + oy, 0x6B7A8A, active, argb);
        part(ctx, ox - 3, top - 10 + oy, ox + 3, top - 4 + oy, 0xE57373, active, argb);

        // Funken
        Iterator<Spark> it = sparks.iterator();
        while (it.hasNext()) {
            Spark s = it.next();
            float t = (now - s.born) / 1000f;
            if (t > 0.8f) {
                it.remove();
                continue;
            }
            int px = (int) (s.x + s.vx * t);
            int py = (int) (s.y + s.vy * t + 0.5f * 160f * t * t);
            int alpha = (int) (255 * (1f - t / 0.8f));
            ctx.fill(px, py, px + 2, py + 2, (alpha << 24) | hitRgb);
        }

        // Schadenszahl
        if (ht >= 0f && ht < 0.8f) {
            int alpha = (int) (255 * (1f - ht / 0.8f));
            if (alpha > 8) {
                int ty = (int) (top - ht * 22f);
                ctx.drawCenteredTextWithShadow(tr, HitText.lit("-4"), cx + 28, ty, (alpha << 24) | hitRgb);
            }
        }
    }
}
