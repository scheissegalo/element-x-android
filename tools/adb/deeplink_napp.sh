#! /bin/bash

# Copyright (c) 2026 Element Creations Ltd.
#Fix
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

adb shell am start -a android.intent.action.VIEW \
   -d "nappx://room/%23napp-x-android%3Achat.north-industries.com"
