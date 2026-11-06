package dev.fluffitex.ocstudio;

import dev.fluffitex.ocstudio.auth.AuthManager;
import dev.fluffitex.ocstudio.gui.LoginScreen;
import dev.fluffitex.ocstudio.gui.ThemeEditorScreen;
import dev.fluffitex.ocstudio.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(modid = OCStudioMod.MODID, name = "OneConfig Studio", version = OCStudioMod.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class OCStudioMod {

    public static final String MODID = "ocstudio";
    public static final String VERSION = "0.1.0";

    private static AuthManager auth;
    private static Logger log;
    private KeyBinding openKey;

    public static AuthManager auth() {
        return auth;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        Path dir = event.getModConfigurationDirectory().toPath().resolve(MODID);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("Konnte Config-Ordner nicht anlegen: " + dir, e);
        }
        ThemeManager.init(dir.resolve("theme.properties"));
        auth = new AuthManager(dir.resolve("auth.properties"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // F8 ist in Vanilla 1.8.9 frei. OneConfig selbst nutzt standardmäßig Rechts-Shift.
        openKey = new KeyBinding("Design-Editor öffnen", Keyboard.KEY_F8, "OneConfig Studio");
        ClientRegistry.registerKeyBinding(openKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openKey != null && openKey.isPressed()) {
            openEditor();
        }
    }

    private void openEditor() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            return;
        }
        if (auth.isUnlocked()) {
            mc.displayGuiScreen(new ThemeEditorScreen());
        } else {
            mc.displayGuiScreen(new LoginScreen(auth, new Runnable() {
                @Override
                public void run() {
                    mc.displayGuiScreen(new ThemeEditorScreen());
                }
            }));
        }
    }
}
