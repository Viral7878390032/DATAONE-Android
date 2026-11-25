# DATAONE Android

DATAONE is an Android app shell with a native Telegram client powered by TDLib. Telegram sign-in, chats, messaging, shared media, and on-demand media loading are implemented in the Android module.

## Build the Android APK

The project is built in GitHub Actions; Android Studio is not required.

1. Open the **Actions** tab in this repository.
2. Select **Build DATAONE Android APK**.
3. Run the workflow on the `main` branch (or push a commit).
4. Open the successful workflow run and download the `app-debug.apk` artifact.
5. Install the APK on an Android device.

## Telegram API credentials

TDLib requires Telegram API credentials from Telegram's official developer portal. Add these repository Actions secrets:

- `TELEGRAM_API_ID`
- `TELEGRAM_API_HASH`

Keep these values private. Do not commit them into source files. The workflow passes them to the Android build.

## Current scope

- Telegram authentication using phone number, login code, and optional two-step verification.
- Chat list, contacts, private and group chats.
- Text messaging and supported media/document/voice sending.
- Message actions and shared media browsing.
- Photos, videos, GIFs, stickers, audio, voice messages, and documents.
- Media is requested on demand rather than automatically downloaded in the background.

## Important testing note

A successful GitHub Actions build verifies that the APK compiles. Telegram sign-in, account-specific permissions, message delivery, playback, and device behavior still need to be tested on a real Android device with a Telegram account.

## Repository layout

- `android/` — native Android application.
- `src/`, `index.html`, `vite.config.ts` — separate web preview project.
