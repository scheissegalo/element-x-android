# N-App X — server & infrastructure setup

Everything the **homeserver / website / push infrastructure** needs so the rebranded app
(`com.karich.design.nappx`) works end to end. The app-side values these notes refer to live in
`appconfig/`, `plugins/src/main/kotlin/config/BuildTimeConfig.kt`, `app/build.gradle.kts` and
`libraries/pushproviders/firebase/src/main/kotlin/.../FirebaseConfig.kt`.

> Status: this document is a checklist for later. Nothing here runs in the Android build.

---

## 0. Values the app currently uses

| Setting | Current value | Where |
| --- | --- | --- |
| Default homeserver | `https://chat.north-industries.com` | `appconfig/.../AuthenticationConfig.kt` |
| Android application id | `com.karich.design.nappx` (`.debug`, `.nightly`) | `plugins/.../BuildTimeConfig.kt` |
| OAuth redirect URI | `com.karich.design.nappx:/` (debug: `…nappx.debug:/`, nightly: `…nappx.nightly:/`) | `resValue("login_redirect_scheme")` in `app/build.gradle.kts` |
| Custom scheme | `nappx://` | `app/src/main/AndroidManifest.xml` |
| Login link | `https://mobile.north-industries.com/napp/?account_provider=…&login_hint=…` | manifest + `DefaultLoginIntentResolver` |
| Push gateway | `https://push.klabausterbeere.xyz/_matrix/push/v1/notify` | `libraries/pushproviders/firebase/.../FirebaseConfig.kt` |
| Push `app_id` | `com.karich.design.nappx` (new app; old app uses `kb.chat`) | `appconfig/.../PushConfig.kt` |
| UnifiedPush gateway | `https://push.klabausterbeere.xyz/_matrix/push/v1/notify` | `libraries/pushproviders/unifiedpush/.../UnifiedPushConfig.kt` |
| OAuth client metadata | client `https://north-industries.com`, logo `https://north-industries.com/mobile-icon.png`, TOS `.../acceptable-use-policy-terms`, policy `.../privacy` | `libraries/matrix/api/build.gradle.kts` |

### Existing infrastructure (from the active N-App Classic app)

| Item | Value |
| --- | --- |
| Firebase project | `klabausterbeerechat` (project number `210317184062`) |
| Old app id | `kb.chat` (`kb.chat.debug` for debug), pusher `app_id` = `kb.chat` |
| Push gateway (Sygnal) | `https://push.klabausterbeere.xyz/_matrix/push/v1/notify` |
| Firebase Android clients | `chat.klabauster.beere`, `kb.chat`, `kb.chat.debug`, `com.karich.design.nappx` |
| **Still missing in Firebase** | `com.karich.design.nappx.debug`, `com.karich.design.nappx.nightly` |

The old app can stay as is. The new app registers its own pusher with
`app_id = com.karich.design.nappx`; add a second app entry in Sygnal using the same FCM service
account. Both apps can be installed and logged in at the same time — each registers a pusher with
its own FCM token, so both receive notifications (the same account will get one notification per app).

### Signing fingerprints

FCM does **not** need fingerprints. They are needed for Firebase Auth / Google Sign-In, Play
Integrity/App Check, and Android App Links (`assetlinks.json`).

- Debug (standard Android debug key, same in both projects):
  - SHA-1: `41:BD:63:B3:B6:12:A1:5D:9B:A3:6A:52:45:C3:93:F2:A9:B9:92:D1`
  - SHA-256: `B0:B0:51:DC:56:5C:81:2F:E1:7F:6F:3E:94:5B:4D:79:04:71:23:AB:0D:A6:12:86:76:9E:B2:94:91:97:13:0E`
- Release / Play: get the **App signing key certificate** from Play Console → Test and release →
  Setup → App integrity. This is the certificate devices actually see (Play re-signs your upload).
  If you sign locally, use `keytool -list -v -keystore <keystore> -alias <alias>`.

### Release/upload key (generated locally)

- Keystore: `~/.android/napp-release.jks`, alias `napp`, RSA 4096, valid until 2054.
- Configured via `local.properties` (`signing.napp.release.storePath|storePassword|keyId|keyPassword`);
  environment variables `NAPP_ANDROID_RELEASE_*` are also supported. `local.properties` is git-ignored.
- Upload-key SHA-256:
  `3C:13:E5:5F:99:BF:4A:A8:67:C0:3D:A7:5B:06:72:FD:14:FA:11:96:D5:72:BD:F4:01:3D:31:EA:6B:B1:DF:ED`
- **Back up the keystore and its password** — if lost, you must request an upload-key reset from Play.
- The release build falls back to the debug keystore with a warning if this key is not configured.

---

## 1. DNS and TLS

- `chat.north-industries.com` → Synapse (A/AAAA + valid TLS certificate).
- `app.north-industries.com`, `mobile.north-industries.com` → the web app / redirector (see §7).
- `push.north-industries.com` → Sygnal (push gateway, §5).
- `call.north-industries.com`, `livekit.north-industries.com`, `turn.north-industries.com` → calls (§6).
- If you want MXIDs of the form `@user:north-industries.com` instead of `@user:chat.north-industries.com`,
  serve `https://north-industries.com/.well-known/matrix/server`:
  ```json
  { "m.server": "chat.north-industries.com:443" }
  ```

## 2. Matrix homeserver + discovery

Run Synapse (and MAS if you want OIDC) behind a reverse proxy. The app requires **Sliding Sync**;
either a Synapse version with native sliding sync enabled, or a `sliding-sync` proxy.

Serve `https://chat.north-industries.com/.well-known/matrix/client` (with
`Content-Type: application/json` and `Access-Control-Allow-Origin: *`):

```json
{
  "m.homeserver": { "base_url": "https://chat.north-industries.com" },
  "m.identity_server": { "base_url": "https://matrix.org" },
  "org.matrix.msc3575.proxy": { "url": "https://sync.north-industries.com" },
  "org.matrix.msc4143.rtc_foci": [
    { "type": "livekit", "livekit_service_url": "https://livekit-jwt.call.north-industries.com" }
  ]
}
```

- `org.matrix.msc3575.proxy` only if you run the external sliding-sync proxy (drop it for native support).
- `org.matrix.msc4143.rtc_foci` only if calls are enabled (§6). Verify the exact shape against the
  current Element Call self-hosting documentation; the key is read by the Rust SDK, not the app.
- Make sure `/_matrix/client/versions` is reachable and advertises the features the app needs.

## 3. Account creation ("Create account")

The app fetches the **Element well-known** from the homeserver and only offers the temporary web
registration flow when `registration_helper_url` is present:

`https://chat.north-industries.com/.well-known/element/element.json`

```json
{
  "registration_helper_url": "https://app.north-industries.com/register",
  "enforce_element_pro": false,
  "custom_recovery_passphrase": { "min_character_count": 8 }
}
```

How the app uses it (`features/login/impl/.../web/WebClientUrlForAuthenticationRetriever.kt`):

1. It appends `?hs_url=<homeserver>` to the URL and opens it in a Custom Tab.
2. That page must read `hs_url`, perform registration against that homeserver
   (`/_matrix/client/v3/register`, UIA flows, optional reCAPTCHA/terms/email) and then deep-link the
   user back.
3. The app only enables this flow for the **default homeserver**
   (`AuthenticationConfig.DEFAULT_HOMESERVER_URL`), i.e. `chat.north-industries.com`.

Server prerequisites:
- Registration enabled (or MAS registration), with a working UIA flow.
- SMTP configured for e-mail verification / password resets.
- Terms of service / privacy policy URLs configured in `/_matrix/client/v3/register` if required.

If you do **not** want in-app account creation, set
`OnBoardingConfig.CAN_CREATE_ACCOUNT = false` (`appconfig/.../OnBoardingConfig.kt`) and skip the
helper URL.

## 4. OIDC / Matrix Authentication Service (MAS)

If Synapse is fronted by MAS:

- Register the app as an OAuth client. Accepted redirect URIs (exact strings):
  - `com.karich.design.nappx:/`
  - `com.karich.design.nappx.debug:/`
  - `com.karich.design.nappx.nightly:/`
- Client metadata the app sends: name `N-App X`, `client_uri` `https://north-industries.com`,
  logo `https://north-industries.com/mobile-icon.png`, TOS `https://north-industries.com/acceptable-use-policy-terms`,
  policy `https://north-industries.com/privacy`.
- Either allow dynamic client registration in MAS, or pin a static client id/secret and put it in
  `OAuthConfig.STATIC_REGISTRATIONS` (`libraries/matrix/api/.../auth/OAuthConfig.kt`); it is empty today.
- Expose the OIDC discovery document through `.well-known` (`org.matrix.msc2965.authentication`).

## 5. Push notifications

The app registers an HTTP pusher against the homeserver with:

- `app_id` = `PushConfig.PUSHER_APP_ID`
- `url` = `FirebaseGatewayProvider` value (currently `FirebaseConfig.PUSHER_HTTP_URL`)

The homeserver then posts to that gateway URL, and the gateway talks to FCM. So you need **both** a
Firebase project and a Matrix push gateway.

### 5a. Firebase

Done:

- Firebase project `klabausterbeerechat` contains all package ids:
  `com.karich.design.nappx`, `.debug`, `.nightly` (plus the old `kb.chat` apps).
- `app/google-services.json` is **not tracked** (gitignored). Place it locally or inject it as a CI
  secret before building the `gplay` flavor.
- `com.google.gms.google-services` is enabled in `app/build.gradle.kts`.
- Verified: debug/nightly/release resolve to distinct Firebase app ids.
- The `gplay` flavor bundles Firebase; `fdroid` uses UnifiedPush only.

If you add/rename packages later, update the JSON in `app/` (replace it wholesale) and rebuild.

### 5b. Push gateway (Sygnal)

Sygnal is already running at `https://push.klabausterbeere.xyz` and serves the old app's `kb.chat`
entry plus the new `com.karich.design.nappx` entry.

1. `PushConfig.PUSHER_APP_ID` is already set to `com.karich.design.nappx`; make sure the Sygnal entry
   matches exactly. (Alternatively keep `kb.chat` in both apps and reuse the existing entry.)
2. The gateway URL is already configured as
   `https://push.klabausterbeere.xyz/_matrix/push/v1/notify` in `FirebaseConfig` and
   `UnifiedPushConfig`.
3. Synapse side: no global gateway setting is required (clients register the URL), but ensure
   outbound HTTPS from the homeserver workers to the gateway and that pusher workers are healthy.
4. Both apps can run side by side: each registers a pusher with its own FCM token, so both receive
   notifications. If the same account is signed in on both apps on the same phone, expect one
   notification per app per event.

### 5c. UnifiedPush (optional, F-Droid)

The default gateway is set to the same Sygnal instance
(`https://push.klabausterbeere.xyz/_matrix/push/v1/notify`). Override with
`EnterpriseService.unifiedPushDefaultPushGateway()` if needed.

### 5d. Verification

Use the in-app notification troubleshooting screens (Settings → Notifications → Troubleshoot) and
test a push with `TestPush`. Check `GET /_matrix/client/v3/pushers` on the homeserver after login —
you should see `app_id` and the gateway URL.

## 6. Calls (Element Call / MatrixRTC)

Calls are a web app + LiveKit SFU, not part of Synapse:

1. Deploy Element Call at `https://call.north-industries.com`.
2. Deploy LiveKit SFU + its JWT service, plus TURN (`turn.north-industries.com`) for NAT traversal.
3. Advertise the focus in `.well-known/matrix/client` (`org.matrix.msc4143.rtc_foci`, §2).
4. Allow WebSocket/TLS traffic to LiveKit and TURN UDP/TCP ports.
5. If you do not want calls, hide the relevant UI (no server toggle); note that incoming-call
   notifications will not work without a focus.

## 7. App links, login links and the website

- **Android App Links** — publish `assetlinks.json` (with the release signing SHA-256 fingerprint)
  on every host the manifest claims:
  - `https://app.north-industries.com/.well-known/assetlinks.json`
  - `https://develop.north-industries.com/.well-known/assetlinks.json`
  - `https://staging.north-industries.com/.well-known/assetlinks.json`
  - `https://mobile.north-industries.com/.well-known/assetlinks.json`
  Include all signing certs you ship with (release/nightly/debug if you test App Links).
- **Login links** — `https://mobile.north-industries.com/napp/?account_provider=…&login_hint=…`
  must exist (page or redirect) and match the `/napp/` path in the manifest.
- **Legal / branding pages** used by the app:
  - `https://north-industries.com/privacy`
  - `https://north-industries.com/cookie-policy`
  - `https://north-industries.com/copyright`
  - `https://north-industries.com/acceptable-use-policy-terms`
  - `https://north-industries.com/mobile-icon.png` (OAuth client logo)
- The registration helper page from §3 (`https://app.north-industries.com/register`).

## 8. Location sharing (optional)

MapTiler key + map styles are build-time values, set in `local.properties` or the environment:

```properties
services.maptiler.apikey=…
services.maptiler.lightMapId=…
services.maptiler.darkMapId=…
```

or `ELEMENT_ANDROID_MAPTILER_API_KEY`, `ELEMENT_ANDROID_MAPTILER_LIGHT_MAP_ID`,
`ELEMENT_ANDROID_MAPTILER_DARK_MAP_ID`.

## 9. Analytics / crash reporting (currently disabled)

- PostHog has no endpoint in FOSS builds (`PosthogEndpointConfigProvider` returns `null`).
- Sentry is only enabled when `ELEMENT_ANDROID_SENTRY_DSN` / `services.analyticsproviders.sentry.dsn`
  is set. The Rust SDK DSN uses `ELEMENT_SDK_SENTRY_DSN`.
- If you later enable analytics, the opt-in screen returns automatically.

## 10. Quick verification checklist

```bash
curl -s https://chat.north-industries.com/_matrix/client/versions | jq .
curl -s https://chat.north-industries.com/.well-known/matrix/client | jq .
curl -s https://chat.north-industries.com/.well-known/element/element.json | jq .
curl -s https://mobile.north-industries.com/.well-known/assetlinks.json | jq .
curl -s -X POST https://push.north-industries.com/_matrix/push/v1/notify -d '{}' -H 'Content-Type: application/json'
```

Then in the app: sign in with the default server, send a message from another account and confirm
the push arrives, and try "Create account" once the registration helper is live.
