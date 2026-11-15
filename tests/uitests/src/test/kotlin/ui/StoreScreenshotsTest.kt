/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package ui

import base.ComposablePreviewProvider
import base.PaparazziPreviewRule
import base.ScreenshotTest
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * Renders a curated set of day-mode previews with mock data, used to produce the Play Store screenshots.
 *
 * Run with:
 *   ./gradlew :tests:uitests:recordPaparazziDebug --tests 'ui.StoreScreenshotsTest'
 */
@RunWith(TestParameterInjector::class)
class StoreScreenshotsTest(
    @TestParameter(valuesProvider = StoreScreenshotPreviewProvider::class)
    val preview: ComposablePreview<AndroidPreviewInfo>,
) {
    @get:Rule(order = 0)
    val layoutLibErrorFilterStatement = LayoutLibErrorFilterStatement()

    @get:Rule(order = 1)
    val paparazziRule = PaparazziPreviewRule.createFor(preview, locale = "en")

    @Test
    fun snapshot() {
        ScreenshotTest.runTest(paparazzi = paparazziRule, preview = preview, localeStr = "en")
    }
}

object StoreScreenshotPreviewProvider : TestParameterValuesProvider() {
    private val classNames = setOf(
        "HomeView",
        "MessagesView",
        "OnBoardingView",
        "PreferencesRootView",
        "CallScreenView",
        "NotificationSettingsView",
        "RoomDetailsView",
    )

    override fun provideValues(context: Context): List<ComposablePreview<AndroidPreviewInfo>> =
        ComposablePreviewProvider.values
            .map { it.value }
            .filter { it.previewInfo.name == "Day" }
            .filter { it.methodName.removeSuffix("Preview") in classNames }
}
