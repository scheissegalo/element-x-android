/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.api.store

import androidx.compose.runtime.Immutable

/**
 * The user's choice of notification sound for a notification channel.
 */
@Immutable
sealed interface NotificationSound {
    /** Use the system's default tone for this channel (e.g. notification or ringtone). */
    data object SystemDefault : NotificationSound

    /**
     * Use the bundled in-app sound. Semantically meaningful only for the noisy message channel,
     * where it resolves to `R.raw.message`; the ringing-call channel has no bundled tone and
     * treats this the same as [SystemDefault].
     */
    data object NAppDefault : NotificationSound

    /**
     * Use the bundled in-app fade sound. Like [NAppDefault] this is meaningful only for the
     * noisy message channel (`R.raw.napp_fade`); the ringing-call channel has no bundled tone
     * and treats this the same as [SystemDefault].
     */
    data object NAppFade : NotificationSound

    /** Produce no sound. */
    data object Silent : NotificationSound

    /** Use the ringtone at [uri]. */
    data class Custom(val uri: String) : NotificationSound

    companion object {
        // String? round-trip used by [AppPreferencesStore]:
        //   null -> SystemDefault, "silent" -> Silent, "napp_default" -> NAppDefault,
        //   "napp_fade" -> NAppFade, else -> Custom(uri).
        private const val STORED_SILENT = "silent"
        private const val STORED_NAPP_DEFAULT = "napp_default"
        private const val STORED_NAPP_FADE = "napp_fade"

        fun fromStored(value: String?): NotificationSound = when (value) {
            null -> SystemDefault
            STORED_SILENT -> Silent
            STORED_NAPP_DEFAULT -> NAppDefault
            STORED_NAPP_FADE -> NAppFade
            else -> Custom(value)
        }

        fun NotificationSound.toStored(): String? = when (this) {
            SystemDefault -> null
            Silent -> STORED_SILENT
            NAppDefault -> STORED_NAPP_DEFAULT
            NAppFade -> STORED_NAPP_FADE
            is Custom -> uri
        }
    }
}

/**
 * Snapshot of the persisted notification-sound state, returned by
 * [AppPreferencesStore.getNotificationSoundChannelConfig] in a single read.
 */
data class NotificationSoundChannelConfig(
    val messageSound: NotificationSound,
    val messageSoundVersion: Int,
    val messageSoundDisplayName: String?,
    val callRingtone: NotificationSound,
    val callRingtoneVersion: Int,
    val callRingtoneDisplayName: String?,
)
