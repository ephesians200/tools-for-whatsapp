# 100+ Tools For WhatsApp

A toolbox for WhatsApp users and small businesses, built with Kotlin and Jetpack Compose. It helps you format messages, clean up media, and organise chats, with most features working on the device.

[![Get it on Google Play](https://img.shields.io/badge/Google%20Play-Download-3DDC84?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.prince.whatstoolbox)

## Features
- **Text tools:** reverse and flip text, repeat a message, text to emoji art, invisible characters, stylised fonts
- **Business message builders:** order forms, product catalogs, announcements, meeting and event messages, channel polls, a bill splitter
- **Media tools:** split long videos into shorter clips, convert voice notes (including to MP3), image tools
- **Voice note transcriber:** offline speech-to-text with Vosk when the model is installed (see below)
- **Chat tools:** work with exported chat files, and a "Wrapped" style summary
- **Vault:** private items protected with biometrics
- **Share target:** send text, images or audio to the app directly from WhatsApp's share sheet
- Free version with ads and a one-time upgrade through Google Play Billing

## How it is built
- **Language and UI:** Kotlin, Jetpack Compose, Material 3, Navigation Compose
- **Architecture:** a `data`, `domain` and `presentation` split with Hilt for dependency injection
- **Storage and background work:** Room, WorkManager and a foreground service for long media jobs
- **Media:** AndroidX Media3 Transformer and Effect, TAndroidLame (MP3 encoding), ZXing and Google code scanner
- **Speech:** Vosk offline speech recognition
- **Monetisation:** AdMob with Google's consent flow (UMP) and Google Play Billing
- **Tests:** unit tests for the text and chat tools

```
app/src/main/java/com/prince/whatstoolbox
├── App.kt, MainActivity.kt
├── data/           storage and billing
├── domain/         repositories and use cases (tool specs, media)
├── presentation/   view models and share inbox
├── ui/             screens (dashboard, text, chat, business, media, image, vault, settings, paywall), components, theme
└── util/           chat parser, media jobs, transcriber, video splitter, font tables
```

## Optional setup: offline transcription
The speech model is not included in the repository because of its size.
1. Download `vosk-model-small-en-us-0.15` from the Vosk models page.
2. Unzip it and put its contents in `app/src/main/assets/model-en/` (the folder must contain `am/`, `conf/` and so on).

## Build it yourself
1. Open the folder in Android Studio (JDK 17), let Gradle sync, and run on a device or emulator with Android 8.0 (API 26) or newer.
2. Before a Play upload that targets Android 15 or newer, check that the native libraries (Vosk, TAndroidLame) are 16 KB aligned.

## Author
**Prince Oppong**, software and web developer and tech educator.
[Portfolio](https://princeoppong.safeoddsfootballtips.com/) · [LinkedIn](https://www.linkedin.com/in/ohenebaoppong/)
