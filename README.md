# Protest Mode

Android app for the digital safety of people taking part in a protest. In a few seconds, it helps make the phone discreet: radios off, location off, background scanning stopped.

A banner tells whether the phone is **exposed**, **partially protected** or **discreet**, then what it is secured against: access to device content, location tracking, fingerprinting/identity, activity history. Each setting (radios, screen lock, encryption, USB debugging, device name…) carries a risk level: critical, important or advised.

Android almost never lets an app change a setting by itself. The app therefore opens the matching system screen, then reads the actual state again on return. A setting is only shown as "Off" after that check.

## Privacy

- No `INTERNET` permission, no third-party SDK, no telemetry.
- Normal permissions only: `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `BLUETOOTH` (API ≤ 30), `BLUETOOTH_ADMIN` (API ≤ 32), `NFC`, `USE_FINGERPRINT` (API ≤ 28), `USE_BIOMETRIC`, `QUERY_ADVANCED_PROTECTION_MODE`.
- The only thing stored, on the phone, is the SHA-256 fingerprint of the device name you checked. Cloud backup is disabled.

## Languages

English (default), French, Spanish and Italian, following the language of the phone.

## Compatibility

Android 6.0 (API 23) and later.

## Development

Requirements: macOS, Android SDK (`~/Library/Android/sdk`, platform 37), JDK 17 or 21 (the one bundled with Android Studio is detected automatically).

Scripts that act on a device never pick one implicitly: it must be named (`ANDROID_SERIAL=<serial number> scripts/test.sh`, serial number given by `adb devices`). They refuse any device other than a phone or a tablet, and refuse to start when several devices are connected.

```sh
scripts/build.sh       # ktlint, lint, unit tests, debug APK
scripts/test.sh        # all of the above; + instrumented tests if ANDROID_SERIAL names a phone
scripts/install.sh     # installs and launches the debug build (ANDROID_SERIAL required)
scripts/update-deps.sh # after a dependency update: regenerates and compares the checksums
scripts/audit.sh       # checks the release APK (permissions, components, network code)
scripts/icons.sh       # regenerates the icons of each language from art/logo.svg (local BentonSans font required)
```

## Verifying an APK

Release APKs are signed with this certificate (SHA-256):

```
BE:5A:AE:DD:D0:2A:EE:76:CB:0A:2F:87:B2:07:11:17:57:D0:99:51:DB:C9:D8:F0:75:D0:41:2C:B8:0E:2C:A6
```

```sh
apksigner verify --print-certs protest-mode-<version>.apk
```

APKs installed from F-Droid are built and signed by F-Droid with its own key.

## Credits

Logo (`art/logo.svg`): original work of the project. The text of the English, Spanish and Italian variants is set in BentonSans Comp Bold, a commercial font that is not distributed: only the rendered icons are versioned.

Store feature graphic (`fastlane/`): public domain photo (CC0, rawpixel no. 3336870), dithered, cropped and captioned.

## License

GNU GPL v3 or later. See [LICENSE](LICENSE).
