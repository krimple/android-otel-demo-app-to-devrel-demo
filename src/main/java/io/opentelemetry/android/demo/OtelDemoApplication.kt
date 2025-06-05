/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.app.Application
import android.util.Log
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
// import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing Honeycomb OpenTelemetry Android SDK")
        
        val options = HoneycombOptions.builder(this)
            .setApiKey(getString(R.string.rum_access_token))
            .setServiceName("android-otel-demo")
            .setServiceVersion("1.0")
            .setDebug(true)
            .build()

        try {
            rum = Honeycomb.configure(this, options)
            Log.d(TAG, "RUM session started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Honeycomb!", e)
        }
    }


    companion object {
        var rum: Any? = null

        fun tracer(name: String): Tracer? {
            // TODO: Access tracing through Honeycomb SDK
            return null
        }

        fun counter(name: String): LongCounter? {
            // TODO: Access metrics through Honeycomb SDK
            return null
        }

        fun eventBuilder(scopeName: String, eventName: String): LogRecordBuilder? {
            // TODO: Access logging through Honeycomb SDK
            return null
        }
    }
}
