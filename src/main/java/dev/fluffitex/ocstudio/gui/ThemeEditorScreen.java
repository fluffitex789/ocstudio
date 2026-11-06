package dev.fluffitex.ocstudio.gui;

import dev.fluffitex.ocstudio.OCStudioMod;
import dev.fluffitex.ocstudio.theme.Theme;
import dev.fluffitex.ocstudio.theme.ThemeHooks;
import dev.fluffitex.ocstudio.theme.ThemeManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Locale;

/**
 * Ingame-Editor mit Live-Vorschau. Änderungen wirken sofort auf das aktive Theme (ThemeHooks),
 * gespeichert wird erst mit "Speichern & schließen". Esc verwirft ungespeicherte Änderungen.
 */
public final class ThemeEditorScreen extends GuiScreen {

    private enum Tab {
        COLORS("Farben"), LAYOUT("Layout"), FONT("Schrift & Icons"), ANIM("Animationen");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int ID_SAVE = 1;
    private static final int ID_RESET = 2;
    private static final int ID_LOCK = 3;
    private static final int ID_TAB = 10;
    private static final int ID_SLOT = 20;
    private static final int ID_TOGGLE_A = 50;
    private static final int ID_TOGGLE_B = 51;
    private static final int ID_SLIDER_BASE = 100;

    private final Theme live = ThemeHooks.theme();
    private final Theme snapshot = live.copy();

    private Tab tab = Tab.COLORS;
    private Theme.ColorSlot slot = Theme.ColorSlot.ACCENT;
    private boolean keepChanges;
    private String status = "";

    private int ctlX;
    private int ctlW;
    private int hexLabelY;
    private int nextSliderId;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        nextSliderId = ID_SLIDER_BASE;

        ctlX = 20;
        ctlW = Math.max(160, Math.min(230, width / 2 - 30));

        Tab[] tabs = Tab.values();
        int tabW = Math.max(70, Math.min(110, (width - 40 - 12) / tabs.length));
        for (int i = 0; i < tabs.length; i++) {
            GuiButton b = new GuiButton(ID_TAB + i, ctlX + i * (tabW + 4), 10, tabW, 20, tabs[i].label);
            b.enabled = tabs[i] != tab;
            buttonList.add(b);
        }

        int y = 44;
        switch (tab) {
            case COLORS:
                buildColors(y);
                break;
            case LAYOUT:
                y = slider(y, "Eckenradius", Theme.RADIUS_MIN, Theme.RADIUS_MAX, live.cornerRadius, 0, v -> live.cornerRadius = v);
                y = slider(y, "Abstand", Theme.PADDING_MIN, Theme.PADDING_MAX, live.padding, 0, v -> live.padding = v);
                y = slider(y, "UI-Skalierung", Theme.UI_SCALE_MIN, Theme.UI_SCALE_MAX, live.uiScale, 2, v -> live.uiScale = v);
                slider(y, "Sidebar-Breite", Theme.SIDEBAR_MIN, Theme.SIDEBAR_MAX, live.sidebarWidth, 0, v -> live.sidebarWidth = v);
                break;
            case FONT:
                y = slider(y, "Schriftgröße", Theme.FONT_SCALE_MIN, Theme.FONT_SCALE_MAX, live.fontScale, 2, v -> live.fontScale = v);
                y = slider(y, "Icon-Größe", Theme.ICON_SCALE_MIN, Theme.ICON_SCALE_MAX, live.iconScale, 2, v -> live.iconScale = v);
                buttonList.add(new GuiButton(ID_TOGGLE_A, ctlX, y, ctlW, 20, shadowLabel()));
                break;
            case ANIM:
                y = slider(y, "Geschwindigkeit", Theme.ANIM_SPEED_MIN, Theme.ANIM_SPEED_MAX, live.animationSpeed, 2, v -> live.animationSpeed = v);
                y = slider(y, "Blur", Theme.UNIT_MIN, Theme.UNIT_MAX, live.blurStrength, 2, v -> live.blurStrength = v);
                y = slider(y, "Hover-Glow", Theme.UNIT_MIN, Theme.UNIT_MAX, live.hoverGlow, 2, v -> live.hoverGlow = v);
                buttonList.add(new GuiButton(ID_TOGGLE_B, ctlX, y, ctlW, 20, transitionsLabel()));
                break;
            default:
                break;
        }

        int footerY = height - 28;
        int fw = Math.max(80, (width - 40 - 8) / 3);
        buttonList.add(new GuiButton(ID_SAVE, 20, footerY, fw, 20, "Speichern & schließen"));
        buttonList.add(new GuiButton(ID_RESET, 24 + fw, footerY, fw, 20, "Standard"));
        buttonList.add(new GuiButton(ID_LOCK, 28 + 2 * fw, footerY, fw, 20, "Sperren"));

        previewX = ctlX + ctlW + 20;
        previewY = 44;
        previewW = Math.max(40, width - previewX - 20);
        previewH = Math.max(40, height - previewY - 40);
    }

    private void buildColors(int y) {
        Theme.ColorSlot[] slots = Theme.ColorSlot.values();
        int cols = 3;
        int cellW = (ctlW - (cols - 1) * 4) / cols;
        for (int i = 0; i < slots.length; i++) {
            int cx = ctlX + (i % cols) * (cellW + 4);
            int cy = y + (i / cols) * 22;
            GuiButton b = new GuiButton(ID_SLOT + i, cx, cy, cellW, 20, shorten(slots[i].label));
            b.enabled = slots[i] != slot;
            buttonList.add(b);
        }
        y += ((slots.length + cols - 1) / cols) * 22 + 6;
        hexLabelY = y;
        y += 14;
        y = channel(y, "Rot", 16);
        y = channel(y, "Grün", 8);
        y = channel(y, "Blau", 0);
        channel(y, "Deckkraft", 24);
    }

    private String shorten(String s) {
        return s.length() > 9 ? s.substring(0, 8) + "." : s;
    }

    private int channel(int y, String name, int shift) {
        int c = live.getColor(slot);
        return slider(y, name, 0, 255, (c >>> shift) & 0xFF, 0, v -> {
            int cur = live.getColor(slot);
            int channelValue = Math.round(v) & 0xFF;
            live.setColor(slot, (cur & ~(0xFF << shift)) | (channelValue << shift));
        });
    }

    private int slider(int y, String label, float min, float max, float value, int decimals, ThemeSlider.Setter setter) {
        buttonList.add(new ThemeSlider(nextSliderId++, ctlX, y, ctlW, 20, label, min, max, value, decimals, setter));
        return y + 24;
    }

    private String shadowLabel() {
        return "Textschatten: " + (live.fontShadow ? "An" : "Aus");
    }

    private String transitionsLabel() {
        return "Übergänge: " + (live.transitions ? "An" : "Aus");
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= ID_TAB && b.id < ID_TAB + Tab.values().length) {
            tab = Tab.values()[b.id - ID_TAB];
            initGui();
            return;
        }
        if (b.id >= ID_SLOT && b.id < ID_SLOT + Theme.ColorSlot.values().length) {
            slot = Theme.ColorSlot.values()[b.id - ID_SLOT];
            initGui();
            return;
        }
        switch (b.id) {
            case ID_SAVE:
                try {
                    ThemeManager.save();
                    keepChanges = true;
                    mc.displayGuiScreen(null);
                } catch (IOException e) {
                    status = "Speichern fehlgeschlagen: " + e.getMessage();
                }
                break;
            case ID_RESET:
                live.resetToDefaults();
                status = "Auf Standard gesetzt (noch nicht gespeichert).";
                initGui();
                break;
            case ID_LOCK:
                OCStudioMod.auth().lock();
                mc.displayGuiScreen(null);
                break;
            case ID_TOGGLE_A:
                live.fontShadow = !live.fontShadow;
                b.displayString = shadowLabel();
                break;
            case ID_TOGGLE_B:
                live.transitions = !live.transitions;
                b.displayString = transitionsLabel();
                break;
            default:
                break;
        }
    }

    @Override
    public void updateScreen() {
        // Sitzung abgelaufen -> Editor schließen (onGuiClosed verwirft Ungespeichertes)
        if (!OCStudioMod.auth().isUnlocked()) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (!keepChanges) {
            live.copyFrom(snapshot);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawString(fontRendererObj, "Vorschau", previewX, previewY - 12, 0xFFAAAAAA);
        drawPreview(mouseX, mouseY);

        if (tab == Tab.COLORS) {
            drawString(fontRendererObj, slot.label + "  " + String.format(Locale.ROOT, "#%08X", live.getColor(slot)),
                    ctlX, hexLabelY, 0xFFDDDDDD);
        }
        int footerY = height - 28;
        if (!status.isEmpty()) {
            drawString(fontRendererObj, status, ctlX, footerY - 12, 0xFFFF8855);
        } else if (!live.equals(snapshot)) {
            drawString(fontRendererObj, "Ungespeicherte Änderungen – Esc verwirft sie.", ctlX, footerY - 12, 0xFFFFCC55);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ---- Vorschau -------------------------------------------------------------------------

    private int c(Theme.ColorSlot s) {
        return live.getColor(s);
    }

    private void drawPreview(int mouseX, int mouseY) {
        int x1 = previewX;
        int y1 = previewY;
        int x2 = previewX + previewW;
        int y2 = previewY + previewH;
        drawRect(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF000000);
        drawRect(x1, y1, x2, y2, 0xFF59697A);

        int wx1 = x1 + 10;
        int wy1 = y1 + 10;
        int wx2 = x2 - 10;
        int wy2 = y2 - 10;
        int r = Math.round(live.cornerRadius);
        int pad = Math.round(live.padding);

        fillRounded(wx1 - 1, wy1 - 1, wx2 + 1, wy2 + 1, r + 1, c(Theme.ColorSlot.BORDER));
        fillRounded(wx1, wy1, wx2, wy2, r, c(Theme.ColorSlot.BACKGROUND));

        // Sidebar
        int sbW = Math.min(Math.round(live.sidebarWidth * live.uiScale * 0.45f), (wx2 - wx1) / 2);
        int sbX1 = wx1 + pad;
        int sbY1 = wy1 + pad;
        int sbX2 = sbX1 + sbW;
        int sbY2 = wy2 - pad;
        fillRounded(sbX1, sbY1, sbX2, sbY2, r, c(Theme.ColorSlot.SURFACE));
        String[] items = {"Allgemein", "HUD", "Mods", "Profile"};
        int rowH = Math.round(18 * live.uiScale);
        for (int i = 0; i < items.length; i++) {
            int ry = sbY1 + pad + i * (rowH + 2);
            if (ry + rowH > sbY2) {
                break;
            }
            if (i == 0) {
                fillRounded(sbX1 + 3, ry, sbX2 - 3, ry + rowH, Math.max(0, r - 2), c(Theme.ColorSlot.ACCENT));
            }
            int iconSize = Math.max(3, Math.round(6 * live.iconScale));
            drawRect(sbX1 + 8, ry + (rowH - iconSize) / 2, sbX1 + 8 + iconSize, ry + (rowH + iconSize) / 2,
                    c(Theme.ColorSlot.TEXT));
            text(items[i], sbX1 + 12 + iconSize, ry + (rowH - Math.round(8 * live.fontScale)) / 2,
                    c(Theme.ColorSlot.TEXT));
        }

        // Inhalt
        int cx1 = sbX2 + pad;
        int cx2 = wx2 - pad;
        int cardH = Math.round(44 * live.uiScale);
        int cardY = wy1 + pad;
        for (int i = 0; i < 2; i++) {
            int top = cardY + i * (cardH + pad);
            if (top + cardH > wy2 - pad) {
                break;
            }
            fillRounded(cx1 - 1, top - 1, cx2 + 1, top + cardH + 1, r + 1, c(Theme.ColorSlot.BORDER));
            fillRounded(cx1, top, cx2, top + cardH, r, c(Theme.ColorSlot.SURFACE));
            text(i == 0 ? "Beispiel-Einstellung" : "Weitere Einstellung", cx1 + pad, top + pad,
                    c(Theme.ColorSlot.TEXT));
            text("Gedämpfter Beschreibungstext", cx1 + pad, top + pad + Math.round(11 * live.fontScale),
                    c(Theme.ColorSlot.TEXT_MUTED));
            if (i == 0) {
                int bw = Math.round(46 * live.uiScale);
                int bh = Math.round(16 * live.uiScale);
                int bx2 = cx2 - pad;
                int bx1 = bx2 - bw;
                int by1 = top + (cardH - bh) / 2;
                boolean hover = mouseX >= bx1 && mouseX < bx2 && mouseY >= by1 && mouseY < by1 + bh;
                if (hover && live.hoverGlow > 0f) {
                    int alpha = Math.round(live.hoverGlow * 0x77);
                    int glow = (alpha << 24) | (c(Theme.ColorSlot.ACCENT) & 0x00FFFFFF);
                    fillRounded(bx1 - 3, by1 - 3, bx2 + 3, by1 + bh + 3, r + 3, glow);
                }
                fillRounded(bx1, by1, bx2, by1 + bh, Math.min(r, bh / 2), c(Theme.ColorSlot.ACCENT));
                text("An", bx1 + bw / 2 - Math.round(4 * live.fontScale), by1 + (bh - Math.round(8 * live.fontScale)) / 2,
                        c(Theme.ColorSlot.TEXT));
            }
        }
        int infoY = cardY + 2 * (cardH + pad);
        if (infoY + 10 < wy2 - pad) {
            text(String.format(Locale.ROOT, "Blur %d %%  ·  Animation %.2fx%s",
                            Math.round(live.blurStrength * 100), live.animationSpeed,
                            live.transitions ? "" : " (aus)"),
                    cx1, infoY, c(Theme.ColorSlot.TEXT_MUTED));
        }
    }

    private void text(String s, int x, int y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(live.fontScale, live.fontScale, 1f);
        fontRendererObj.drawString(s, 0, 0, color, live.fontShadow);
        GlStateManager.popMatrix();
    }

    /** Rechteck mit Rundung, angenähert aus Pixelzeilen (reicht für die Vorschau). */
    private static void fillRounded(int x1, int y1, int x2, int y2, int radius, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            drawRect(x1, y1, x2, y2, color);
            return;
        }
        drawRect(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            drawRect(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            drawRect(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }
}
