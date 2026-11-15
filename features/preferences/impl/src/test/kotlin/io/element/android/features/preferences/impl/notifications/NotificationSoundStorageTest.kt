/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.notifications

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.preferences.api.store.NotificationSound
import io.element.android.libraries.preferences.api.store.NotificationSound.Companion.toStored
import org.junit.Test

class NotificationSoundStorageTest {
    @Test
    fun `fromStored - null maps to SystemDefault`() {
        assertThat(NotificationSound.fromStored(null)).isEqualTo(NotificationSound.SystemDefault)
    }

    @Test
    fun `fromStored - silent sentinel maps to Silent`() {
        assertThat(NotificationSound.fromStored("silent")).isEqualTo(NotificationSound.Silent)
    }

    @Test
    fun `fromStored - napp_default sentinel maps to NAppDefault`() {
        assertThat(NotificationSound.fromStored("napp_default")).isEqualTo(NotificationSound.NAppDefault)
    }

    @Test
    fun `fromStored - napp_fade sentinel maps to NAppFade`() {
        assertThat(NotificationSound.fromStored("napp_fade")).isEqualTo(NotificationSound.NAppFade)
    }

    @Test
    fun `fromStored - any other string is treated as a Custom URI`() {
        assertThat(NotificationSound.fromStored("content://media/42"))
            .isEqualTo(NotificationSound.Custom("content://media/42"))
    }

    @Test
    fun `toStored - SystemDefault encodes as null`() {
        assertThat(NotificationSound.SystemDefault.toStored()).isNull()
    }

    @Test
    fun `toStored - NAppDefault encodes as napp_default`() {
        assertThat(NotificationSound.NAppDefault.toStored()).isEqualTo("napp_default")
    }

    @Test
    fun `toStored - NAppFade encodes as napp_fade`() {
        assertThat(NotificationSound.NAppFade.toStored()).isEqualTo("napp_fade")
    }

    @Test
    fun `toStored - Silent encodes as silent`() {
        assertThat(NotificationSound.Silent.toStored()).isEqualTo("silent")
    }

    @Test
    fun `toStored - Custom encodes as the URI string`() {
        assertThat(NotificationSound.Custom("content://x").toStored()).isEqualTo("content://x")
    }

    @Test
    fun `round-trip preserves every variant`() {
        val variants = listOf(
            NotificationSound.SystemDefault,
            NotificationSound.NAppDefault,
            NotificationSound.NAppFade,
            NotificationSound.Silent,
            NotificationSound.Custom("content://media/42"),
        )
        for (sound in variants) {
            assertThat(NotificationSound.fromStored(sound.toStored())).isEqualTo(sound)
        }
    }
}
