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
| Push gateway | `https://matrix.org/_matrix/push/v1/notify` (**needs changing**) | `libraries/pushproviders/firebase/.../FirebaseConfig.kt` |
| Push `app_id` | `im.vector.app.android` (**needs changing**) | `appconfig/.../PushConfig.kt` |
| OAuth client metadata | client `https://north-industries.com`, logo `https://north-industries.com/mobile-icon.png`, TOS `…/acceptable-use-policy-terms`, policy `…/privacy` | `libraries/matrix/api/build.gradle.kts` |

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

1. Create a Firebase Android app for each package id:
   - `com.karich.design.nappx` (release)
   - `com.karich.design.nappx.debug`
   - `com.karich.design.nappx.nightly`
2. Download `google-services.json` and place it in `app/` (it is git-ignored).
3. Enable the Google services Gradle plugin in `app/build.gradle.kts` (currently commented out):
   `alias(libs.plugins.gms.google.services)`.
   In CI, inject the file as a secret before building the `gplay` flavor.
4. The `fdroid` flavor does not bundle Firebase; push there only works through UnifiedPush.

### 5b. Push gateway (Sygnal)

1. Deploy Sygnal at `https://push.north-industries.com` with the FCM service-account credentials.
2. Configure an app entry whose `app_id` matches `PushConfig.PUSHER_APP_ID`
   (change it to e.g. `com.karich.design.nappx`; max 64 chars).
3. Make sure `POST https://push.north-industries.com/_matrix/push/v1/notify` is reachable from the
   homeserver (it is the URL the client registered).
4. Update the app defaults:
   - `libraries/pushproviders/firebase/.../FirebaseConfig.kt` → `PUSHER_HTTP_URL`
   - or set it through the enterprise `firebasePushGateway()` if you keep enterprise config.
5. Synapse side: no global gateway setting is required (clients register the URL), but ensure
   outbound HTTPS from the homeserver workers to the gateway and that `enable_push`/pusher workers
   are healthy.

### 5c. UnifiedPush (optional, F-Droid)

Default gateway is `https://matrix.gateway.unifiedpush.org/_matrix/push/v1/notify`. Override with
`EnterpriseService.unifiedPushDefaultPushGateway()` if you run your own.

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
