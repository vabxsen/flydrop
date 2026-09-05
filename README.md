# FlyDrop

A native Android file-sharing UI built with Kotlin, Jetpack Compose and Material 3,
reproducing the FlyDrop design reference: **Home**, **Nearby** and **File Transfer**.

The design is the specification. Where a stock Material component would drift from
it, the component is custom-drawn instead — the floating navigation, the discovery
radar, the transfer arc, the switch, the cards and the icon set are all bespoke.

## Screens

| Screen | What it contains |
| --- | --- |
| **Home** | Two-tone layout: a pale aqua hero (profile card, split Send/Receive actions, Flydrop Web strip) with a full-bleed white sheet rising over it carrying Favourite Friends and Latest Activities. |
| **Nearby** | Discovery radar drawn on the background — concentric rings, a breathing centre bloom, a slow scanning pulse and devices placed by polar coordinates — above a white Nearby Friends panel. |
| **File Transfer** | Animated circular progress with both participants, a three-column stats strip, and per-file rows with their own progress rings. |
| **Sign in / Profile** | Not in the reference; built from the same vocabulary. Google sign-in, and the account with a way back out of it. |

## Design tokens

Colours, type sizes and spacing were measured out of the reference image rather
than estimated — dominant-colour sampling for fills, darkest-pixel sampling for
glyphs, and edge scans for geometry.

| Role | Value |
| --- | --- |
| Primary violet | `#5C33FD` |
| Secondary teal | `#17C7CD` |
| Text primary | `#0A0B2E` |
| Home hero | `#E3FBFD` |
| Nearby background | `#F5F8FE` |

The reference renders a phone 355px wide, which maps 1:1 onto a ~360dp Android
screen, so measured pixels are used directly as dp. Type sizes come from
cap-height measurements (Poppins cap height is 0.70em).

Everything lives in `ui/theme/` — `Color.kt`, `Type.kt`, `Shape.kt`, `Dimens.kt` —
so the whole layout can be retuned from one place.

## Avatars

There are no image assets. Avatars are generated as vector portraits from a
deterministic seed (`ui/components/Avatar.kt`), so the same account always draws
the same face, nothing is fetched, and Compose previews render fully offline.
Swap the body of that one composable for an image loader to use real pictures.

## Stack

Kotlin 2.4 · AGP 8.13 · Compose BOM 2026.01 · Material 3 · Navigation Compose ·
ViewModel + StateFlow · Firebase Auth + Credential Manager · minSdk 26 · compileSdk 36

Poppins is bundled under the SIL Open Font License; see `licenses/`.

## Building

```bash
./gradlew :app:assembleDebug
```

`local.properties` is not committed — point `sdk.dir` at your Android SDK, or set
`ANDROID_HOME`.

## Google Sign-In (optional)

`app/google-services.json` is **not** committed. The Google Services plugin is
applied conditionally in `app/build.gradle.kts`, so **the project builds and runs
without it** — sign-in simply reports that it is not configured and offers a way
through to the rest of the app.

To enable it:

1. Create a Firebase project and add an Android app with package `com.flydrop.app`.
2. **Authentication → Sign-in method → Google → Enable.** This creates the web
   OAuth client that becomes `default_web_client_id`.
3. Add your signing SHA-1 under **Project settings → Your apps → Add fingerprint**.
   For a debug build:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android
   ```
4. Download `google-services.json` into `app/` and rebuild.

A correct file contains both `"client_type": 1` (Android, with your certificate
hash) and `"client_type": 3` (web). If `oauth_client` is empty, step 2 was skipped.

Sign-in uses Credential Manager with `GetGoogleIdOption` — authorised accounts
first for a one-tap sheet, falling back to the full picker. It needs Google Play
Services **and a Google account on the device**, so an emulator must use a
`google_apis_playstore` system image; a plain `google_apis` image cannot sign in.

## Status

The three reference screens, sign-in and profile are implemented against mock
data (`data/MockData.kt`). Actual peer-to-peer transfer — Nearby Connections or
Wi-Fi Direct — is not wired up; the models and ViewModels are shaped so it can be
added without reworking the UI.
