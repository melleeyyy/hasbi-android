# Hasbi — Music Player 🎵 (Native Android, Kotlin)

**Hasbi** is a fully native Android music player written in **Kotlin** — a real Android app (not a WebView wrapper), with the same design and features as the web version.

## Features

- Add any audio files from your device (MP3, M4A, WAV, OGG, FLAC, AAC, OPUS) — files are copied into the app's private storage
- Songs, playlists (create, delete, add/remove songs), Liked Songs auto-playlist
- Sorting (recently added, A–Z, Z–A, shortest, longest — remembered) + search
- Now Playing full screen with rotating disc + glowing progress ring
- Tap the disc left / right half to jump 10 seconds back / forward (with badge)
- Swipe left / right to change songs (with slide animation), swipe down to close
- Mini player with progress bar, like, prev / play / next
- Shuffle + three repeat modes
- Queue viewer with a ⋮ menu on every row
- Lock-screen / notification media controls (seek, next, previous)
- Background playback via a foreground service (keeps playing when the app is closed)
- Automatic song-duration detection
- Resumes the last song at its saved position
- Dark glassmorphism design — blue / black / green / white only, 100% English UI
- 100% private — nothing leaves the device, no permissions needed

## Install

Download the APK from the [releases page](https://github.com/melleeyyy/hasbi-android/releases) (v2.0.0 or later is the native app), open it on your Android phone, allow "install from unknown apps" if asked. Requires Android 8.0+. It installs as an update over v1.x (same signing key).

## Project structure

```
app/src/main/java/com/melleeyyy/hasbi/
├── MainActivity.kt      # Full UI (lists, playlists, Now Playing, sheets, gestures)
├── PlayerService.kt     # Foreground service: MediaPlayer, MediaSession, notification
├── Db.kt                # SQLite storage (tracks, playlists, likes)
└── Ui.kt                # Theme colors, custom DiscView + EqView
app/src/main/res/        # Icons, theme, adaptive launcher icon
```

Build with Android Studio (or Gradle 8.11 + AGP 8.9 + Kotlin 2.0). The old v1.x app was a Trusted Web Activity; v2.0.0+ is the native Kotlin app. The web app source lives at https://github.com/melleeyyy/hasbi-music-player.

## License

MIT (see LICENSE).
