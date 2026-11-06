package dev.fluffitex.ocstudio.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/**
 * Passwortfeld: Die echten Zeichen liegen in {@code raw}, das GuiTextField zeigt nur Sternchen.
 * (Vanilla 1.8.9 hat kein maskiertes Textfeld.)
 */
final class SecretField {

    static final int MAX_LENGTH = 64;

    final GuiTextField box;
    private final StringBuilder raw = new StringBuilder();

    SecretField(int id, FontRenderer font, int x, int y, int w, int h) {
        box = new GuiTextField(id, font, x, y, w, h);
        box.setMaxStringLength(MAX_LENGTH);
    }

    void type(char c, int keyCode) {
        if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            paste(GuiScreen.getClipboardString());
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (raw.length() > 0) {
                raw.deleteCharAt(raw.length() - 1);
            }
        } else if (c >= 32 && c != 127 && raw.length() < MAX_LENGTH) {
            raw.append(c);
        }
        sync();
    }

    private void paste(String text) {
        if (text == null) {
            return;
        }
        for (int i = 0; i < text.length() && raw.length() < MAX_LENGTH; i++) {
            char c = text.charAt(i);
            if (c >= 32 && c != 127) {
                raw.append(c);
            }
        }
        sync();
    }

    boolean sameAs(SecretField other) {
        return raw.toString().contentEquals(other.raw);
    }

    char[] chars() {
        char[] out = new char[raw.length()];
        raw.getChars(0, raw.length(), out, 0);
        return out;
    }

    void clear() {
        for (int i = 0; i < raw.length(); i++) {
            raw.setCharAt(i, '\0');
        }
        raw.setLength(0);
        sync();
    }

    private void sync() {
        StringBuilder stars = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            stars.append('*');
        }
        box.setText(stars.toString());
    }
}
