# OneConfig Studio (Forge 1.8.9)

Ingame-Editor für das Design der OneConfig-UI. Nur wer sich mit Benutzername und Passwort anmeldet, kann das Design ändern.

## Bedienung

- **F8** öffnet den Editor (Taste in den Minecraft-Einstellungen änderbar).
- Beim ersten Start legst du Benutzername und Passwort fest. Gespeichert werden nur Hashes in `config/ocstudio/auth.properties`.
- Vier Tabs: **Farben**, **Layout**, **Schrift & Icons**, **Animationen**. Änderungen sind sofort in der Vorschau sichtbar.
- **Speichern & schließen** schreibt `config/ocstudio/theme.properties`. **Esc** verwirft ungespeicherte Änderungen. **Sperren** meldet dich ab.
- Die Sitzung läuft nach 10 Minuten ab. Nach 5 falschen Versuchen gibt es eine Sperre (30 s, danach verdoppelt sich die Zeit).

## Sicherheit, ehrlich

Alles läuft im Client. Der Login hält andere Spieler vom Editor fern, schützt aber nicht gegen jemanden, der die Mod decompiliert oder `auth.properties` löscht (dann kann ein neuer Zugang eingerichtet werden). Wenn andere dein Design nur sehen, aber nie ändern sollen, verteile ihnen eine Build ohne Editor oder entferne die Tastenbindung.

## Stand

| Teil | Status |
|---|---|
| Theme-Modell, Speichern/Laden, Hooks (`theme/`) | getestet (plain Java) |
| Login mit PBKDF2, Sperre, Sitzung (`auth/`) | getestet (plain Java) |
| Editor, Login-Screen, Mod-Klasse (`gui/`, `OCStudioMod`) | geschrieben, **nicht** gegen Minecraft kompiliert |
| `build.gradle` | Referenz, nicht getestet |
| **Mixins in die OneConfig-UI** (`mixin/`) | **noch offen** – siehe unten |

## Offen: Hooks in die OneConfig-UI

Der Editor verändert ein Theme, das über `ThemeHooks` abrufbar ist. Damit OneConfig es auch verwendet, müssen Mixins an die Stellen, an denen die UI Farben, Radien, Abstände, Schriften und Animationszeiten liest. Dafür braucht es die echten Klassen- und Methodennamen der OneConfig-Version, die du verwendest.

Klassenliste holen:

```
jar tf <oneconfig-1.8.9-jar> | grep -iE "gui|ui/|theme|color|palette"
```

Jeder Mixin ruft dann nur eine `ThemeHooks`-Methode auf, z. B. `ThemeHooks.color(Theme.ColorSlot.ACCENT, originalColor)` oder `ThemeHooks.cornerRadius(originalRadius)`. Neue Mixins in `mixins.ocstudio.json` unter `"client"` eintragen.
