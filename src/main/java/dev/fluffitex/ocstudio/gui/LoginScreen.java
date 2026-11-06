package dev.fluffitex.ocstudio.gui;

import dev.fluffitex.ocstudio.auth.AuthManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Anmeldung am Design-Editor. Beim ersten Start legt man hier Benutzername und Passwort fest. */
public final class LoginScreen extends GuiScreen {

    private static final int ID_SUBMIT = 1;
    private static final int ID_CANCEL = 2;

    private final AuthManager auth;
    private final Runnable onSuccess;
    private final boolean setupMode;

    private GuiTextField userField;
    private SecretField pwField;
    private SecretField pw2Field;
    private int focus;
    private String message = "";

    public LoginScreen(AuthManager auth, Runnable onSuccess) {
        this.auth = auth;
        this.onSuccess = onSuccess;
        this.setupMode = !auth.isSetUp();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int fw = 190;
        int x = width / 2 - fw / 2;
        int y = height / 2 - (setupMode ? 62 : 44);

        userField = new GuiTextField(0, fontRendererObj, x, y, fw, 20);
        userField.setMaxStringLength(AuthManager.MAX_USER);
        pwField = new SecretField(1, fontRendererObj, x, y + 38, fw, 20);
        pw2Field = setupMode ? new SecretField(2, fontRendererObj, x, y + 76, fw, 20) : null;

        int by = y + (setupMode ? 112 : 74);
        buttonList.add(new GuiButton(ID_SUBMIT, x, by, fw, 20, setupMode ? "Zugang einrichten" : "Anmelden"));
        buttonList.add(new GuiButton(ID_CANCEL, x, by + 24, fw, 20, "Abbrechen"));
        setFocus(0);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (pwField != null) {
            pwField.clear();
        }
        if (pw2Field != null) {
            pw2Field.clear();
        }
    }

    @Override
    public void updateScreen() {
        userField.updateCursorCounter();
        pwField.box.updateCursorCounter();
        if (pw2Field != null) {
            pw2Field.box.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, setupMode ? "Design-Editor einrichten" : "Design-Editor",
                width / 2, userField.yPosition - 34, 0xFFFFFF);
        if (setupMode) {
            drawCenteredString(fontRendererObj, "Wird nur als Hash gespeichert, mindestens "
                    + AuthManager.MIN_PASSWORD + " Zeichen.", width / 2, userField.yPosition - 22, 0xFFAAAAAA);
        }
        label("Benutzername", userField);
        label("Passwort", pwField.box);
        if (pw2Field != null) {
            label("Passwort wiederholen", pw2Field.box);
        }
        userField.drawTextBox();
        pwField.box.drawTextBox();
        if (pw2Field != null) {
            pw2Field.box.drawTextBox();
        }
        if (!message.isEmpty()) {
            GuiButton last = buttonList.get(buttonList.size() - 1);
            drawCenteredString(fontRendererObj, message, width / 2, last.yPosition + 28, 0xFFFF5555);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void label(String text, GuiTextField field) {
        drawString(fontRendererObj, text, field.xPosition, field.yPosition - 10, 0xFFAAAAAA);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (hit(userField, mouseX, mouseY)) {
            setFocus(0);
        } else if (hit(pwField.box, mouseX, mouseY)) {
            setFocus(1);
        } else if (pw2Field != null && hit(pw2Field.box, mouseX, mouseY)) {
            setFocus(2);
        }
    }

    private static boolean hit(GuiTextField f, int mx, int my) {
        return mx >= f.xPosition && mx < f.xPosition + f.width && my >= f.yPosition && my < f.yPosition + f.height;
    }

    private int fieldCount() {
        return pw2Field != null ? 3 : 2;
    }

    private void setFocus(int index) {
        focus = index;
        userField.setFocused(index == 0);
        pwField.box.setFocused(index == 1);
        if (pw2Field != null) {
            pw2Field.box.setFocused(index == 2);
        }
    }

    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            setFocus((focus + 1) % fieldCount());
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            submit();
            return;
        }
        if (focus == 0) {
            userField.textboxKeyTyped(typed, keyCode);
        } else if (focus == 1) {
            pwField.type(typed, keyCode);
        } else if (pw2Field != null) {
            pw2Field.type(typed, keyCode);
        }
        message = "";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_SUBMIT) {
            submit();
        } else if (button.id == ID_CANCEL) {
            mc.displayGuiScreen(null);
        }
    }

    private void submit() {
        String user = userField.getText();
        if (setupMode) {
            if (!pwField.sameAs(pw2Field)) {
                message = "Die Passwörter stimmen nicht überein.";
                return;
            }
            switch (auth.setup(user, pwField.chars())) {
                case OK:
                    onSuccess.run();
                    break;
                case BAD_USERNAME:
                    message = "Benutzername: " + AuthManager.MIN_USER + " bis " + AuthManager.MAX_USER + " Zeichen.";
                    break;
                case WEAK_PASSWORD:
                    message = "Passwort: mindestens " + AuthManager.MIN_PASSWORD + " Zeichen.";
                    break;
                case ALREADY_SET_UP:
                    message = "Es existiert bereits ein Zugang.";
                    break;
                default:
                    message = "Zugang konnte nicht gespeichert werden.";
                    break;
            }
            return;
        }
        switch (auth.login(user, pwField.chars())) {
            case OK:
                onSuccess.run();
                break;
            case LOCKED:
                message = "Zu viele Versuche. Bitte " + auth.lockRemainingSeconds() + " s warten.";
                pwField.clear();
                break;
            case NOT_SET_UP:
                message = "Noch kein Zugang eingerichtet.";
                break;
            default:
                message = "Benutzername oder Passwort falsch.";
                pwField.clear();
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
