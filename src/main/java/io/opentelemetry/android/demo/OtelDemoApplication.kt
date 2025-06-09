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
        
        val apiKey = otelProperties.getProperty("HONEYCOMB_API_KEY") 
            ?: throw IllegalStateException("HONEYCOMB_API_KEY must be set in otel.properties")
        val serviceName = otelProperties.getProperty("SERVICE_NAME") 
            ?: throw IllegalStateException("SERVICE_NAME must be set in otel.properties")
        val endpoint = otelProperties.getProperty("TELEMETRY_ENDPOINT") 
            ?: throw IllegalStateException("TELEMETRY_ENDPOINT must be set in otel.properties")

        apiEndpoint = otelProperties.getProperty("API_ENDPOINT")
            ?: throw IllegalStateException("API_ENDPOINT must be set in otel.properties")
        
        val options = HoneycombOptions.builder(this)
            .setApiKey(apiKey)
            .setApiEndpoint(endpoint)
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
        var apiEndpoint: String = "https://www.zurelia.honeydemo.io/api"

        fun tracer(name: String): Tracer? {
            val tracer = rum?.openTelemetry?.getTracer(name, "1.0.0")
            Log.d(TAG, "Getting tracer for scope: $name, tracer: $tracer")
            return tracer
        }

        fun counter(name: String): LongCounter? {
            return rum?.openTelemetry?.getMeter("otel.demo.app")?.counterBuilder(name)?.build()
        }

        fun eventBuilder(scopeName: String, eventName: String): LogRecordBuilder? {
            return rum?.openTelemetry?.getLogsBridge()?.get(scopeName)?.logRecordBuilder()?.setBody(eventName)
        }
    }
}
