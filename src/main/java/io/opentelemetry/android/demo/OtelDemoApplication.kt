/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.app.Application
import android.util.Log
import java.io.IOException
import java.util.Properties
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing Honeycomb OpenTelemetry Android SDK")
        
        val otelProperties = Properties()
        try {
            val inputStream = assets.open("otel.properties")
            otelProperties.load(inputStream)
            inputStream.close()
        } catch (e: IOException) {
            Log.w(TAG, "No otel.properties file found in assets, using defaults")
        }
        
        val apiKey = otelProperties.getProperty("HONEYCOMB_API_KEY") ?: getString(R.string.rum_access_token)
        val serviceName = otelProperties.getProperty("SERVICE_NAME") ?: "android-otel-demo"
        
        val options = HoneycombOptions.builder(this)
            .setApiKey(apiKey)
            .setServiceName(serviceName)
            .setServiceVersion("1.0")
            .setDebug(true)
            .build()

        try {
            rum = Honeycomb.configure(this, options)
            Log.d(TAG, "RUM session started with service: $serviceName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Honeycomb!", e)
        }
    }


    companion object {
        var rum: OpenTelemetryRum? = null

        fun tracer(name: String): Tracer? {
            return rum?.openTelemetry?.getTracer(name)
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
