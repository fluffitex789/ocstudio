package dev.fluffitex.ocstudio.theme;

import java.io.IOException;
import java.nio.file.Path;

/** Verbindet das aktive Theme (ThemeHooks) mit der Datei auf der Platte. */
public final class ThemeManager {

    private static Path file;

    private ThemeManager() {}

    public static void init(Path themeFile) {
        file = themeFile;
        ThemeHooks.set(ThemeStore.load(themeFile));
    }

    public static void save() throws IOException {
        if (file == null) {
            throw new IOException("ThemeManager ist nicht initialisiert");
        }
        ThemeStore.save(file, ThemeHooks.theme());
    }
}
