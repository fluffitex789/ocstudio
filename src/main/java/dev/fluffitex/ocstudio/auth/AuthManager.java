package dev.fluffitex.ocstudio.auth;

import dev.fluffitex.ocstudio.AtomicFiles;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.function.LongSupplier;

/**
 * Benutzername + Passwort für den Design-Editor.
 * Gespeichert wird nur ein Salt, ein Hash des Benutzernamens und ein PBKDF2-Hash des Passworts.
 *
 * Hinweis: Das läuft komplett im Client. Es hält Gelegenheitsnutzer vom Editor fern,
 * ist aber kein Schutz gegen jemanden, der die Mod decompiliert oder die Datei ersetzt.
 */
public final class AuthManager {

    public enum LoginResult { OK, WRONG, LOCKED, NOT_SET_UP }

    public enum SetupResult { OK, ALREADY_SET_UP, BAD_USERNAME, WEAK_PASSWORD, IO_ERROR }

    public static final int MIN_USER = 3;
    public static final int MAX_USER = 32;
    public static final int MIN_PASSWORD = 8;

    private static final int ITERATIONS = 150_000;
    private static final int KEY_BITS = 256;
    private static final int MAX_FAILURES = 5;
    private static final long BASE_LOCK_MS = 30_000L;
    private static final long SESSION_MS = 10 * 60_000L;

    private final Path file;
    private final LongSupplier clock;
    private final SecureRandom random = new SecureRandom();

    private byte[] salt;
    private byte[] userHash;
    private byte[] passwordHash;
    private int iterations = ITERATIONS;

    private int failures;
    private long lockedUntil;
    private long unlockedUntil;

    public AuthManager(Path file) {
        this(file, System::currentTimeMillis);
    }

    public AuthManager(Path file, LongSupplier clock) {
        this.file = file;
        this.clock = clock;
        load();
    }

    public synchronized boolean isSetUp() {
        return salt != null && userHash != null && passwordHash != null;
    }

    public synchronized boolean isUnlocked() {
        return clock.getAsLong() < unlockedUntil;
    }

    public synchronized void lock() {
        unlockedUntil = 0L;
    }

    public synchronized long lockRemainingSeconds() {
        long left = lockedUntil - clock.getAsLong();
        return left <= 0 ? 0 : (left + 999) / 1000;
    }

    public synchronized SetupResult setup(String username, char[] password) {
        try {
            if (isSetUp()) {
                return SetupResult.ALREADY_SET_UP;
            }
            String user = normalize(username);
            if (user.length() < MIN_USER || user.length() > MAX_USER) {
                return SetupResult.BAD_USERNAME;
            }
            if (password == null || password.length < MIN_PASSWORD) {
                return SetupResult.WEAK_PASSWORD;
            }
            byte[] newSalt = new byte[16];
            random.nextBytes(newSalt);
            byte[] newUserHash = userDigest(user, newSalt);
            byte[] newPasswordHash = pbkdf2(password, newSalt, ITERATIONS);

            Properties p = new Properties();
            p.setProperty("iterations", Integer.toString(ITERATIONS));
            p.setProperty("salt", Base64.getEncoder().encodeToString(newSalt));
            p.setProperty("user", Base64.getEncoder().encodeToString(newUserHash));
            p.setProperty("password", Base64.getEncoder().encodeToString(newPasswordHash));
            try {
                AtomicFiles.writeProperties(file, p, "OneConfig Studio Zugang (nur Hashes)");
            } catch (IOException e) {
                return SetupResult.IO_ERROR;
            }

            salt = newSalt;
            userHash = newUserHash;
            passwordHash = newPasswordHash;
            iterations = ITERATIONS;
            failures = 0;
            unlockedUntil = clock.getAsLong() + SESSION_MS;
            return SetupResult.OK;
        } finally {
            wipe(password);
        }
    }

    public synchronized LoginResult login(String username, char[] password) {
        try {
            if (!isSetUp()) {
                return LoginResult.NOT_SET_UP;
            }
            long now = clock.getAsLong();
            if (now < lockedUntil) {
                return LoginResult.LOCKED;
            }
            byte[] u = userDigest(normalize(username), salt);
            byte[] h = pbkdf2(password == null ? new char[0] : password, salt, iterations);
            // Bewusst & statt &&, damit beide Vergleiche immer laufen.
            boolean ok = MessageDigest.isEqual(u, userHash) & MessageDigest.isEqual(h, passwordHash);
            if (ok) {
                failures = 0;
                unlockedUntil = now + SESSION_MS;
                return LoginResult.OK;
            }
            failures++;
            if (failures >= MAX_FAILURES) {
                int step = Math.min(failures - MAX_FAILURES, 6);
                lockedUntil = now + BASE_LOCK_MS * (1L << step);
            }
            return LoginResult.WRONG;
        } finally {
            wipe(password);
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
            byte[] s = Base64.getDecoder().decode(p.getProperty("salt", ""));
            byte[] u = Base64.getDecoder().decode(p.getProperty("user", ""));
            byte[] h = Base64.getDecoder().decode(p.getProperty("password", ""));
            int it = Integer.parseInt(p.getProperty("iterations", Integer.toString(ITERATIONS)));
            if (s.length == 0 || u.length == 0 || h.length == 0 || it < 1000) {
                return;
            }
            salt = s;
            userHash = u;
            passwordHash = h;
            iterations = it;
        } catch (IOException | IllegalArgumentException e) {
            // Defekte Datei: wie "nicht eingerichtet" behandeln
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] userDigest(String normalizedUser, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(normalizedUser.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }

    private static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }
}
