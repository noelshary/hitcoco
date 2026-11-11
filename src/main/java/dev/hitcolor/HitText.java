package dev.hitcolor;

/**
 * Kleine Helfer-Klasse für Texte. Der volle Klassenname wird bewusst ausgeschrieben,
 * damit kein anderer Typ namens "Text" (z. B. aus einer Oberklasse) dazwischenfunkt.
 */
public final class HitText {
    private HitText() {}

    public static net.minecraft.text.Text lit(String s) {
        return net.minecraft.text.Text.literal(s);
    }

    public static net.minecraft.text.Text tr(String key, Object... args) {
        return net.minecraft.text.Text.translatable(key, args);
    }
}
