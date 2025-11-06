/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Modified from original at:
 * https://github.com/open-telemetry/opentelemetry-android/blob/main/demo-app/src/main/java/io/opentelemetry/android/demo/DemoViewModel.kt
 * 
 * Changes: Modified to use Honeycomb SDK and add additional telemetry and features.
 */

package io.opentelemetry.android.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DemoViewModel : ViewModel() {
    val sessionIdState = MutableStateFlow("? unknown ?")
    private val tracer = OtelDemoApplication.getTracer()

    init {
        // Update session ID on initialization
        updateSession()
        
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5000)
                // TODO: Do some work here maybe
            }
        }
    }

    fun updateSession() {
        // Hardcoded session ID since there's currently no way to access it from the SDK
        sessionIdState.value = "demo-session-12345"
    }

    private fun sendTrace(
        type: String,
        value: Float,
    ) {
        // A metric should be a better fit, but for now we're using spans.
        tracer?.spanBuilder(type)?.setAttribute("value", value.toDouble())?.startSpan()?.end()
    }
}
