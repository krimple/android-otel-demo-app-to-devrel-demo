/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Modified from original at:
 * https://github.com/open-telemetry/opentelemetry-android/blob/main/demo-app/src/main/java/io/opentelemetry/android/demo/OtelDemoApplication.kt
 * 
 * Changes: Modified to use Honeycomb SDK and add additional telemetry and features.
 */

package io.opentelemetry.android.demo

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import java.io.IOException
import java.util.Properties
import io.honeycomb.opentelemetry.android.Honeycomb
import io.honeycomb.opentelemetry.android.HoneycombOptions
import io.honeycomb.opentelemetry.android.OtlpProtocol
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME
import okhttp3.OkHttpClient
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant.now
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds;

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {
    override fun onCreate() {

        val timeoutDuration = 0.seconds

        super.onCreate()
        INSTANCE = this

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
        val honeycombEndpoint = otelProperties.getProperty("HONEYCOMB_ENDPOINT")
            ?: throw IllegalStateException("HONEYCOMB_ENDPOINT must be set in otel.properties")

        apiEndpoint = otelProperties.getProperty("API_ENDPOINT")
            ?: throw IllegalStateException("API_ENDPOINT must be set in otel.properties")
        
        val options = HoneycombOptions.builder(this)
            .setApiKey(apiKey)
            .setApiEndpoint(honeycombEndpoint)
            .setProtocol(OtlpProtocol.HTTP_PROTOBUF)
            .setServiceName(serviceName)
            .setServiceVersion("1.0.3")
            .setTimeout(timeoutDuration)
            .setDebug(true)
            .build()


        try {
            rum = Honeycomb.configure(this, options)

            // Initialize OkHttpClient after telemetry is configured
            // make sure to add some timeout time, as the defaults are too low
            // and we're getting CANCELs
            httpClient = OkHttpClient
                .Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .callTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .build()
            Log.d(TAG, "OkHttpClient singleton initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Honeycomb!", e)
        }
    }


    companion object {

        var rum: OpenTelemetryRum? = null
        private lateinit var INSTANCE: OtelDemoApplication

        // for REST API endpoint requests
        private var httpClient: OkHttpClient? = null
        
        fun getInstance(): OtelDemoApplication = INSTANCE

        // TODO - extract this!
        var apiEndpoint: String = "https://www.zurelia.honeydemo.io/api"

        fun getTracer(): Tracer? {
            return rum?.openTelemetry?.getTracer("otel.demo.app", "1.0.1")
        }

        fun counter(name: String): LongCounter? {
            return rum?.openTelemetry?.getMeter("otel.demo.app")?.counterBuilder(name)?.build()
        }
    }
}
