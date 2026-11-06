package dev.fluffitex.ocstudio.gui;

import dev.fluffitex.ocstudio.theme.Theme;
import dev.fluffitex.ocstudio.theme.ThemeHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;

import java.util.Locale;

/** Einfacher Slider ohne Textur-Abhängigkeiten. */
final class ThemeSlider extends GuiButton {

    interface Setter {
        void set(float value);
    }

    private final String label;
    private final float min;
    private final float max;
    private final int decimals;
    private final Setter setter;
    private float norm;
    private boolean dragging;

    ThemeSlider(int id, int x, int y, int w, int h, String label,
                float min, float max, float value, int decimals, Setter setter) {
        super(id, x, y, w, h, "");
        this.label = label;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.setter = setter;
        this.norm = max > min ? clamp01((value - min) / (max - min)) : 0f;
        updateLabel();
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private float value() {
        float v = min + norm * (max - min);
        return decimals == 0 ? Math.round(v) : v;
    }

    private void updateLabel() {
        displayString = label + ": " + String.format(Locale.ROOT, "%." + decimals + "f", value());
    }

    private void applyMouse(int mouseX) {
        norm = clamp01((mouseX - xPosition) / (float) width);
        setter.set(value());
        updateLabel();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            dragging = true;
            applyMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }

    @Override
    public void playPressSound(SoundHandler handler) {
        // Slider bleiben leise
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        if (dragging) {
            applyMouse(mouseX);
        }
        int fillX = xPosition + Math.round(norm * width);
        int accent = 0xFF000000 | (ThemeHooks.theme().getColor(Theme.ColorSlot.ACCENT) & 0x00FFFFFF);
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, 0xCC11131A);
        drawRect(xPosition, yPosition, fillX, yPosition + height, accent);
        drawRect(fillX - 1, yPosition - 1, fillX + 1, yPosition + height + 1, 0xFFFFFFFF);
        drawCenteredString(mc.fontRendererObj, displayString,
                xPosition + width / 2, yPosition + (height - 8) / 2, 0xFFFFFF);
    }
}
