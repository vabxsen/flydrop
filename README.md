# FlyDrop

A native Android file-sharing UI built with Kotlin, Jetpack Compose and Material 3,
reproducing the FlyDrop design reference: **Home**, **Nearby** and **File Transfer**.

The design is the specification. Where a stock Material component would drift from
it, the component is custom-drawn instead — the floating navigation, the discovery
radar, the transfer arc, the switch, the cards and the icon set are all bespoke.

## Screens

| Screen | What it contains |
| --- | --- |
| **Home** | Two-tone layout: a pale aqua hero (profile card, split Send/Receive actions, Flydrop Web strip) with a full-bleed white sheet rising over it carrying user-selected Favourite Friends and the phone's Contacts. |
| **Nearby** | Discovery radar drawn on the background — concentric rings, a breathing centre bloom, a slow scanning pulse and devices placed by polar coordinates — above a white Nearby Friends panel. |
| **File Transfer** | Animated circular progress with both participants, a three-column stats strip, and per-file rows with their own progress rings. |
| **Sign in / Profile** | Not in the reference; built from the same vocabulary. Google sign-in, the account with its editable FlyDrop ID and profile photo, and a way back out of it. |
| **About** | Reached from Profile. Two tabs over one sheet — **Version** (version name, build number, package, build type) and **Credits** (licences and what the app is built from). |

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

The app ships no image assets. Avatars are generated as vector portraits from a
deterministic seed (`ui/components/Avatar.kt`), so the same account always draws
the same face, nothing is fetched, and Compose previews render fully offline.
Peers are always drawn this way.

The signed-in user can replace their own with a photo, from the avatar's camera
badge on Profile. It comes from Android's system photo picker, which returns one
image and needs **no storage permission**, so the app never asks for the gallery.
`data/profile/AvatarStore.kt` copies the pick into the app's private files
directory rather than holding the picker's `content://` URI, which is temporary
and would leave the avatar working until the next reboot and then vanishing. On
the way in the image is oriented from its EXIF tag, centre-cropped square and
downscaled to 512px, so every screen draws a small, already-correct bitmap and a
40-megapixel camera photo cannot exhaust memory.

Photos are filed per account (guest mode included), so signing into a different
account on the same device does not show the previous user's face. They are
**device-local**: nothing is uploaded, so a photo does not follow the account to
another phone and peers do not see it. Making it travel would mean Firebase
Storage — an upload path, storage rules and a URL on the Firestore profile.

## Stack

Kotlin 2.3 · AGP 8.13 · Compose BOM 2026.01 · Material 3 · Navigation Compose ·
ViewModel + StateFlow · Firebase Auth + Firestore + Credential Manager · minSdk 26 · compileSdk 36

Poppins is bundled under the SIL Open Font License; see `licenses/`.

## Building

```bash
./gradlew :app:assembleDebug
```

## Testing

Run the local model tests and the Android UI flow on a connected emulator:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

The UI test covers guest entry, contacts permission, the initially empty favourites
state, notifications, Send, Receive, scan, discovery, adding a nearby friend,
bottom navigation, profile/account actions, the profile photo sheet and the About
tabs. Unit tests cover favourite selection, transfer progress and FlyDrop ID
validation.

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

## FlyDrop IDs

Every signed-in account gets a FlyDrop ID derived from its Firebase uid, and can
change it to something of its own **once**. Two accounts can never hold the same
id.

Both rules live in Firestore rather than in the client:

- `flyIds/{handle}` is a reservation document whose **id is the handle**, so
  uniqueness is structural — two accounts cannot create one document. Handles are
  stored lowercase, so case cannot be used to claim an id twice. Reservations are
  never updated or deleted: a retired id keeps pointing at whoever held it rather
  than being handed to a stranger.
- `users/{uid}.handleChanged` starts false and may only ever move to true, which
  is the once-only limit.

`ProfileRepository` writes both documents in a single transaction, so a race
between two devices resolves to one winner. That is the cooperative half;
`firestore.rules` is the authoritative half, since a modified client can skip the
repository entirely. Deploy it alongside the app:

```bash
firebase deploy --only firestore:rules
```

Without Firebase configured the app still runs: Profile shows the derived id and
simply does not offer to change it. Handle shape — length, allowed characters,
reserved names — is `data/profile/FlyIdRules.kt`, checked in the client for fast
feedback and again in the rules on create.

## Contacts

Home requests Android's `READ_CONTACTS` permission and reads only contact IDs and
display names. Phone numbers are not queried. Contacts can be added to or removed
from Favourite Friends with the star action; the chosen IDs are stored locally in
app preferences and the section is empty until the user chooses someone.

## Status

The three reference screens, sign-in, profile and about are implemented; nearby devices
and transfers still use mock data (`data/MockData.kt`), while Home contacts come
from Android's contacts provider. Actual peer-to-peer transfer — Nearby Connections or
Wi-Fi Direct — is not wired up; the models and ViewModels are shaped so it can be
added without reworking the UI.
