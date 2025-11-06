/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Modified from original at:
 * https://github.com/open-telemetry/opentelemetry-android/blob/main/demo-app/src/main/java/io/opentelemetry/android/demo/MainOtelButton.kt
 * 
 * Changes: Modified to use Honeycomb SDK and add additional telemetry and features.
 */

package io.opentelemetry.android.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.SpanKind

@Composable
fun MainOtelButton(icon: Painter,
                   clickCounter: LongCounter? = OtelDemoApplication.counter("logo.clicks")) {
    Row {
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick = { generateClickEvent(clickCounter) },
            modifier = Modifier.padding(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            content = {
                Image(
                    painter = icon,
                    contentDescription = null,
                    Modifier
                        .width(150.dp)
                        .padding(30.dp),
                )
            },
        )
    }
}

fun generateClickEvent(counter: LongCounter?) {
    val tracer = OtelDemoApplication.getTracer()
    counter?.add(1)
    val span = tracer?.spanBuilder("main_otel_button.logo.clicked")
        ?.setSpanKind(SpanKind.INTERNAL)
        ?.setAttribute("app.ui.button.name", "otel_demo_button")
        ?.setAttribute("app.interaction.type", "tap")
        ?.setAttribute("app.operation.type", "ui_interaction")
        ?.startSpan()
    span?.end()
}
