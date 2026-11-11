package dev.hitcolor;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.BooleanSupplier;

/** Quadratischer Farb-Button für die Palette. */
public class SwatchButton extends ButtonWidget {
    private final int rgb;
    private final BooleanSupplier selected;

    public SwatchButton(int x, int y, int size, int rgb, Runnable onPick, BooleanSupplier selected) {
        super(x, y, size, size, HitText.lit(""), b -> onPick.run(), DEFAULT_NARRATION_SUPPLIER);
        this.rgb = rgb;
        this.selected = selected;
    }

    // renderWidget ist in 1.21.11 final, deshalb wird über drawIcon gezeichnet.
    @Override
    public void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int inset = w >= 14 ? 2 : 1;
        int border = selected.getAsBoolean() ? 0xFFFFFFFF : (isHovered() ? 0xFFDDDDDD : 0xFF23232E);
        ctx.fill(x, y, x + w, y + h, border);
        ctx.fill(x + inset, y + inset, x + w - inset, y + h - inset, 0xFF000000 | rgb);
    }
}
