package dev.fluffitex.ocstudio.theme;

import dev.fluffitex.ocstudio.AtomicFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Lädt und speichert ein Theme als einfache key=value-Datei. */
public final class ThemeStore {

    private ThemeStore() {}

    public static Theme load(Path file) {
        Theme t = new Theme();
        if (!Files.isRegularFile(file)) {
            return t;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException | IllegalArgumentException e) {
            return t;
        }
        for (Theme.ColorSlot s : Theme.ColorSlot.values()) {
            t.setColor(s, parseColor(p.getProperty("color." + s.name()), s.defaultArgb));
        }
        t.cornerRadius = parseFloat(p, "layout.cornerRadius", t.cornerRadius);
        t.padding = parseFloat(p, "layout.padding", t.padding);
        t.uiScale = parseFloat(p, "layout.uiScale", t.uiScale);
        t.sidebarWidth = parseFloat(p, "layout.sidebarWidth", t.sidebarWidth);
        t.fontScale = parseFloat(p, "font.scale", t.fontScale);
        t.fontShadow = parseBool(p, "font.shadow", t.fontShadow);
        t.iconScale = parseFloat(p, "icon.scale", t.iconScale);
        t.animationSpeed = parseFloat(p, "anim.speed", t.animationSpeed);
        t.transitions = parseBool(p, "anim.transitions", t.transitions);
        t.blurStrength = parseFloat(p, "anim.blur", t.blurStrength);
        t.hoverGlow = parseFloat(p, "anim.hoverGlow", t.hoverGlow);
        t.clamp();
        return t;
    }

    public static void save(Path file, Theme t) throws IOException {
        Properties p = new Properties();
        for (Theme.ColorSlot s : Theme.ColorSlot.values()) {
            p.setProperty("color." + s.name(), String.format(Locale.ROOT, "#%08X", t.getColor(s)));
        }
        p.setProperty("layout.cornerRadius", Float.toString(t.cornerRadius));
        p.setProperty("layout.padding", Float.toString(t.padding));
        p.setProperty("layout.uiScale", Float.toString(t.uiScale));
        p.setProperty("layout.sidebarWidth", Float.toString(t.sidebarWidth));
        p.setProperty("font.scale", Float.toString(t.fontScale));
        p.setProperty("font.shadow", Boolean.toString(t.fontShadow));
        p.setProperty("icon.scale", Float.toString(t.iconScale));
        p.setProperty("anim.speed", Float.toString(t.animationSpeed));
        p.setProperty("anim.transitions", Boolean.toString(t.transitions));
        p.setProperty("anim.blur", Float.toString(t.blurStrength));
        p.setProperty("anim.hoverGlow", Float.toString(t.hoverGlow));
        AtomicFiles.writeProperties(file, p, "OneConfig Studio Theme (Farben als #AARRGGBB)");
    }

    static int parseColor(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            if (s.length() == 6) {
                return 0xFF000000 | (int) Long.parseLong(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {
            // fällt auf den Default zurück
        }
        return fallback;
    }

    private static float parseFloat(Properties p, String key, float fallback) {
        String v = p.getProperty(key);
        if (v == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBool(Properties p, String key, boolean fallback) {
        String v = p.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }
}
