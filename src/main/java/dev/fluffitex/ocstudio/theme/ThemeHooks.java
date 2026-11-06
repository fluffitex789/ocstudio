package dev.fluffitex.ocstudio.theme;

/**
 * Schmale, statische Schnittstelle, die die Mixins in die OneConfig-UI aufrufen.
 * Jede Methode bekommt den Originalwert und gibt entweder diesen oder den Theme-Wert zurück.
 */
public final class ThemeHooks {

    private static volatile Theme current = new Theme();
    private static volatile boolean enabled = true;

    private ThemeHooks() {}

    public static Theme theme() {
        return current;
    }

    public static void set(Theme theme) {
        current = theme;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static int color(Theme.ColorSlot slot, int original) {
        return enabled ? current.getColor(slot) : original;
    }

    public static float cornerRadius(float original) {
        return enabled ? current.cornerRadius : original;
    }

    public static float padding(float original) {
        return enabled ? current.padding : original;
    }

    public static float uiScale(float original) {
        return enabled ? original * current.uiScale : original;
    }

    public static float sidebarWidth(float original) {
        return enabled ? current.sidebarWidth : original;
    }

    public static float fontScale(float original) {
        return enabled ? original * current.fontScale : original;
    }

    public static boolean fontShadow(boolean original) {
        return enabled ? current.fontShadow : original;
    }

    public static float iconScale(float original) {
        return enabled ? original * current.iconScale : original;
    }

    /** Dauer einer Animation in ms; 0 wenn Übergänge aus sind. */
    public static long animationDuration(long originalMs) {
        if (!enabled) {
            return originalMs;
        }
        if (!current.transitions || current.animationSpeed <= 0f) {
            return 0L;
        }
        return Math.round(originalMs / current.animationSpeed);
    }

    public static float blurStrength(float original) {
        return enabled ? current.blurStrength : original;
    }

    public static float hoverGlow(float original) {
        return enabled ? current.hoverGlow : original;
    }
}
