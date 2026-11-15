# N-App X Android deep links

<!--- TOC -->

* [Introduction](#introduction)
  * [Asset Links](#asset-links)
  * [Supported links](#supported-links)
* [Developer tools](#developer-tools)

<!--- END -->

## Introduction

N-App X Android supports deep linking to specific screens in the application. This document
explains how to use deep links.

### Asset Links

The asset links file is available at:
`https://app.north-industries.com/.well-known/assetlinks.json`

The same file must be served from `develop.north-industries.com`, `staging.north-industries.com`
and `mobile.north-industries.com`.

### Supported links

Element Call link:
> `https://call.north-industries.com/Example`

Link to a user:
> `https://app.north-industries.com/#/user/@alice:chat.north-industries.com`

Link to a room by id or alias:
> `https://app.north-industries.com/#/room/!roomid:chat.north-industries.com`
> `https://app.north-industries.com/#/room/#napp-x-android:chat.north-industries.com`

Link to a room with a specific event:
> `https://app.north-industries.com/#/room/!roomid:chat.north-industries.com/$eventid`

Note that it also works with the other declared web hosts:
> `https://mobile.north-industries.com`
> `https://develop.north-industries.com`
> `https://staging.north-industries.com`

Login link (handled by `DefaultLoginIntentResolver`):
> `https://mobile.north-industries.com/napp/?account_provider=chat.north-industries.com&login_hint=mxid:@alice:chat.north-industries.com`

Custom scheme:
> `nappx://open/{sessionId}` (session)
> `nappx://open/{sessionId}/{roomId}` (room)
> `nappx://open/{sessionId}/{roomId}/{eventId}` (thread)
> `nappx://room/{roomId}` / `nappx://user/{userId}`

## Developer tools

Using an Android 12 or higher emulator, ensure link verification is enabled:

```bash
adb shell am compat enable 175408749 com.karich.design.nappx.debug
```

Reset link verifications for the given package id:

```bash
adb shell pm set-app-links --package com.karich.design.nappx.debug 0 all
```

Force the package id links to be verified:

```bash
adb shell pm verify-app-links --re-verify com.karich.design.nappx.debug
```

Print the link verification of the package id:

```bash
adb shell pm get-app-links com.karich.design.nappx.debug
```

The output should show the domains verified with your signing certificate SHA-256 (the debug
certificate fingerprint is documented in `docs/north-industries-server-setup.md`).
