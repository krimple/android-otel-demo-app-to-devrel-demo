/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Modified from original at:
 * https://github.com/open-telemetry/opentelemetry-android/blob/main/demo-app/src/main/java/io/opentelemetry/android/demo/theme/Type.kt
 * 
 * Changes: Modified to use Honeycomb SDK and add additional telemetry and features.
 */

package io.opentelemetry.android.demo.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
     */
    )
