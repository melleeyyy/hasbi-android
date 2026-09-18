# Hasbi — Music Player 🎵 (Android)

**Hasbi** as a real, installable Android app — with every feature of the [web version](https://github.com/melleeyyy/hasbi-music-player):

- 🎶 Instant library — add any audio files from your device (MP3, M4A, WAV, OGG, FLAC, AAC, OPUS)
- 💾 Persistent library — songs, playlists, likes, sorting and the last played position are saved on the device
- 🖼️ Full-screen Now Playing — circular disc with glowing progress ring, balanced layout
- 👆 Gestures — tap the left / right half of the disc to jump 10 seconds; swipe left / right to change songs (slide animation); swipe down to dismiss; swipe on the home page to switch tabs
- ❤️ Likes with an auto **Liked Songs** playlist
- ▶️ Mini player with like / previous / play / next
- 🔀 Shuffle + three repeat modes
- 🔃 Sorting (recently added, A–Z, Z–A, shortest, longest — remembered)
- 🔍 Search across songs and playlists
- ⏱️ Automatic duration detection — times always show
- 📋 Queue viewer with a ⋮ menu on every row
- 📱 Lock-screen / notification controls (play, pause, next, previous, seek)
- 🎨 Dark glassmorphism design — blue / black / green / white only
- 📴 Works offline after first launch
- 🔒 100% private — nothing leaves the device

## Install

1. Download the APK from the [v1.0.0 release](https://github.com/melleeyyy/hasbi-android/releases/tag/v1.0.0)
2. Open it on your Android phone and allow "install from unknown sources" if asked
3. Open **Hasbi** from your app drawer

Requirements: Android 5.0+ (Lollipop) and Chrome installed (the app uses Chrome as its engine — Trusted Web Activity).

Your existing library from the web app carries over automatically if you used Chrome in the same profile (both use the same site storage on `melleeyyy.github.io`).

## What's in this repository

```
hasbi-android/
├── index.html            # Complete web app (all features)
├── css/style.css         # Styles
├── js/app.js            # Player logic
├── sw.js                 # Service worker (offline)
├── manifest.webmanifest # PWA manifest
├── icons/                # App icons
├── twa-manifest.json     # Trusted Web Activity configuration
├── .well-known/assetlinks.json
├── LICENSE
└── README.md
```

The web app is deployed at **https://melleeyyy.github.io/hasbi-android/** (GitHub Pages), and the Android app (TWA) loads that address — so it always runs the latest version and works offline thanks to the service worker.

## Rebuilding the APK

The APK was generated with [Bubblewrap](https://github.com/GoogleChromeLabs/bubblewrap) (TWA):

1. Install `@bubblewrap/cli` (needs JDK 17 and the Android SDK)
2. Put the signing keystore (`hasbi.keystore`, provided separately) next to `twa-manifest.json`
3. Run:

```bash
bubblewrap update   # regenerates the Android project
bubblewrap build    # builds and signs the APK (asks for the keystore password)
```

The APK must be signed with the same key to install as an update (Android rejects updates signed with a different key), so **keep the keystore safe**.

## Digital asset links

`https://melleeyyy.github.io/.well-known/assetlinks.json` (in the [melleeyyy.github.io](https://github.com/melleeyyy/melleeyyy.github.io) repo) verifies the app's signature so Hasbi opens full-screen without a URL bar.

## License

[MIT](LICENSE)
