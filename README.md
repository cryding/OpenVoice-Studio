# OpenVoice Studio

Offline-first Android text-to-speech starter. Kotlin + Jetpack Compose; no account, cloud backend, API key, or paid service.

## Current scope
- Text input and installed Android TTS preview
- Speaking-rate control
- WAV file synthesis interface
- GitHub Actions debug APK build artifact

## Important limitations
This starter does **not** bundle an open-source neural TTS model. Offline support and voice choice depend on the Android TTS engine and voice data installed on the device. Model integration, language-by-language validation, and model licensing checks remain future work. Do not describe this build as a multilingual neural voice generator yet.

## Build
- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.9.1 + Gradle 8.11.1
- Open in Android Studio or run `gradle assembleDebug` in a configured Android environment.

GitHub Actions builds a debug APK artifact on push. A production-signed release APK has not been published yet.

## Privacy
The app has no network permission and does not upload text. The installed system TTS engine is a separate component with its own behaviour.

## Licensing
Starter app code: MIT. No third-party model weights are bundled. Verify each model/runtime licence and preserve notices before distributing them.