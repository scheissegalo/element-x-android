#! /bin/bash

# Copyright (c) 2025 Element Creations Ltd.
# Copyright 2023-2024 New Vector Ltd.
#
# SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
# Please see LICENSE files in the repository root for full details.

# Format is:
# https://call.north-industries.com/*
# For instance
# https://call.north-industries.com/TestElementCall

adb shell am start -a android.intent.action.VIEW -d https://call.north-industries.com/TestElementCall
