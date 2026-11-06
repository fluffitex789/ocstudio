package dev.fluffitex.ocstudio.theme;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Datenmodell für das komplette Design der OneConfig-UI.
 * Farben sind ARGB-Ints (0xAARRGGBB).
 */
public final class Theme {

    public enum ColorSlot {
        ACCENT("Akzent", 0xFF4C8BF5),
        BACKGROUND("Hintergrund", 0xE61B1D24),
        SURFACE("Flächen", 0xFF262A33),
        TEXT("Text", 0xFFF2F4F8),
        TEXT_MUTED("Text gedämpft", 0xFF9AA3B2),
        BORDER("Rahmen", 0xFF3A404C);

        public final String label;
        public final int defaultArgb;

        ColorSlot(String label, int defaultArgb) {
            this.label = label;
            this.defaultArgb = defaultArgb;
        }
    }

    // Wertebereiche (auch vom Editor für die Slider genutzt)
    public static final float RADIUS_MIN = 0f, RADIUS_MAX = 24f;
    public static final float PADDING_MIN = 0f, PADDING_MAX = 24f;
    public static final float UI_SCALE_MIN = 0.7f, UI_SCALE_MAX = 1.5f;
    public static final float SIDEBAR_MIN = 100f, SIDEBAR_MAX = 260f;
    public static final float FONT_SCALE_MIN = 0.8f, FONT_SCALE_MAX = 1.4f;
    public static final float ICON_SCALE_MIN = 0.7f, ICON_SCALE_MAX = 1.5f;
    public static final float ANIM_SPEED_MIN = 0f, ANIM_SPEED_MAX = 3f;
    public static final float UNIT_MIN = 0f, UNIT_MAX = 1f;

    private final Map<ColorSlot, Integer> colors = new EnumMap<>(ColorSlot.class);

    // Layout & Größen
    public float cornerRadius = 8f;
    public float padding = 8f;
    public float uiScale = 1f;
    public float sidebarWidth = 180f;

    // Schrift & Icons
    public float fontScale = 1f;
    public boolean fontShadow = false;
    public float iconScale = 1f;

    // Animationen & Effekte
    public float animationSpeed = 1f;
    public boolean transitions = true;
    public float blurStrength = 0f;
    public float hoverGlow = 0.4f;

    public Theme() {
        resetToDefaults();
    }

    public int getColor(ColorSlot slot) {
        return colors.get(slot);
    }

    public void setColor(ColorSlot slot, int argb) {
        colors.put(slot, argb);
    }

    public void resetToDefaults() {
        for (ColorSlot s : ColorSlot.values()) {
            colors.put(s, s.defaultArgb);
        }
        cornerRadius = 8f;
        padding = 8f;
        uiScale = 1f;
        sidebarWidth = 180f;
        fontScale = 1f;
        fontShadow = false;
        iconScale = 1f;
        animationSpeed = 1f;
        transitions = true;
        blurStrength = 0f;
        hoverGlow = 0.4f;
    }

    public void copyFrom(Theme o) {
        for (ColorSlot s : ColorSlot.values()) {
            colors.put(s, o.colors.get(s));
        }
        cornerRadius = o.cornerRadius;
        padding = o.padding;
        uiScale = o.uiScale;
        sidebarWidth = o.sidebarWidth;
        fontScale = o.fontScale;
        fontShadow = o.fontShadow;
        iconScale = o.iconScale;
        animationSpeed = o.animationSpeed;
        transitions = o.transitions;
        blurStrength = o.blurStrength;
        hoverGlow = o.hoverGlow;
    }

    public Theme copy() {
        Theme t = new Theme();
        t.copyFrom(this);
        return t;
    }

    /** Erzwingt die Wertebereiche, z. B. nach dem Laden einer von Hand bearbeiteten Datei. */
    public void clamp() {
        cornerRadius = clamp(cornerRadius, RADIUS_MIN, RADIUS_MAX);
        padding = clamp(padding, PADDING_MIN, PADDING_MAX);
        uiScale = clamp(uiScale, UI_SCALE_MIN, UI_SCALE_MAX);
        sidebarWidth = clamp(sidebarWidth, SIDEBAR_MIN, SIDEBAR_MAX);
        fontScale = clamp(fontScale, FONT_SCALE_MIN, FONT_SCALE_MAX);
        iconScale = clamp(iconScale, ICON_SCALE_MIN, ICON_SCALE_MAX);
        animationSpeed = clamp(animationSpeed, ANIM_SPEED_MIN, ANIM_SPEED_MAX);
        blurStrength = clamp(blurStrength, UNIT_MIN, UNIT_MAX);
        hoverGlow = clamp(hoverGlow, UNIT_MIN, UNIT_MAX);
    }

    private static float clamp(float v, float lo, float hi) {
        if (Float.isNaN(v)) {
            return lo;
        }
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Theme)) {
            return false;
        }
        Theme t = (Theme) o;
        return colors.equals(t.colors)
                && Float.compare(cornerRadius, t.cornerRadius) == 0
                && Float.compare(padding, t.padding) == 0
                && Float.compare(uiScale, t.uiScale) == 0
                && Float.compare(sidebarWidth, t.sidebarWidth) == 0
                && Float.compare(fontScale, t.fontScale) == 0
                && fontShadow == t.fontShadow
                && Float.compare(iconScale, t.iconScale) == 0
                && Float.compare(animationSpeed, t.animationSpeed) == 0
                && transitions == t.transitions
                && Float.compare(blurStrength, t.blurStrength) == 0
                && Float.compare(hoverGlow, t.hoverGlow) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colors, cornerRadius, padding, uiScale, sidebarWidth, fontScale, fontShadow,
                iconScale, animationSpeed, transitions, blurStrength, hoverGlow);
    }
}
