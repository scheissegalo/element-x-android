/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.compound.colors

import io.element.android.compound.annotations.CoreColorToken
import io.element.android.compound.tokens.generated.SemanticColors
import io.element.android.compound.tokens.generated.compoundColorsDark
import io.element.android.compound.tokens.generated.compoundColorsLight
import io.element.android.compound.tokens.generated.internal.DarkColorTokens
import io.element.android.compound.tokens.generated.internal.LightColorTokens

/**
 * N-App brand colors.
 *
 * The Compound semantic tokens map the "accent" slots to the Element green scale. We keep the
 * generated tokens untouched and only swap the accent/brand slots for the blue scale used by the
 * N-App artwork. Success colors intentionally stay green.
 */
@OptIn(CoreColorToken::class)
object NappBrandColors {
    val light: SemanticColors = compoundColorsLight.copy(
        bgAccentHovered = LightColorTokens.colorBlue1000,
        bgAccentPressed = LightColorTokens.colorBlue1100,
        bgAccentRest = LightColorTokens.colorBlue900,
        bgAccentSelected = LightColorTokens.colorAlphaBlue300,
        bgBadgeAccent = LightColorTokens.colorBlue400,
        borderAccentPrimary = LightColorTokens.colorBlue900,
        borderAccentSubtle = LightColorTokens.colorBlue700,
        gradientActionStop1 = LightColorTokens.colorBlue500,
        gradientActionStop2 = LightColorTokens.colorBlue700,
        gradientActionStop3 = LightColorTokens.colorBlue900,
        gradientActionStop4 = LightColorTokens.colorBlue1100,
        iconAccentPrimary = LightColorTokens.colorBlue900,
        iconAccentTertiary = LightColorTokens.colorBlue800,
        textActionAccent = LightColorTokens.colorBlue900,
        textBadgeAccent = LightColorTokens.colorBlue1100,
    )

    val dark: SemanticColors = compoundColorsDark.copy(
        bgAccentHovered = DarkColorTokens.colorBlue1000,
        bgAccentPressed = DarkColorTokens.colorBlue1100,
        bgAccentRest = DarkColorTokens.colorBlue900,
        bgAccentSelected = DarkColorTokens.colorAlphaBlue300,
        bgBadgeAccent = DarkColorTokens.colorBlue400,
        borderAccentPrimary = DarkColorTokens.colorBlue900,
        borderAccentSubtle = DarkColorTokens.colorBlue700,
        gradientActionStop1 = DarkColorTokens.colorBlue1100,
        gradientActionStop2 = DarkColorTokens.colorBlue900,
        gradientActionStop3 = DarkColorTokens.colorBlue700,
        gradientActionStop4 = DarkColorTokens.colorBlue500,
        iconAccentPrimary = DarkColorTokens.colorBlue900,
        iconAccentTertiary = DarkColorTokens.colorBlue800,
        textActionAccent = DarkColorTokens.colorBlue900,
        textBadgeAccent = DarkColorTokens.colorBlue1100,
    )
}
