# OpenTelemetry Context Propagation with OkHttpClient

## Research Notes on Singleton OkHttpClient vs Per-Request Client Creation

### Current Implementation Analysis

The current `FetchHelpers.kt` creates a new `OkHttpClient()` for every HTTP request:

```kotlin
val client = OkHttpClient()
client.newCall(tracedRequest).enqueue(object : Callback { ... })
```

This is inefficient but may be working around context propagation concerns.

### The Core Question

**Does OkHttpClient auto-instrumentation work properly with singleton clients?**

### How OpenTelemetry Android Auto-Instrumentation Works

The OpenTelemetry Android SDK uses **ByteBuddy** to instrument OkHttpClient at runtime:

1. **Runtime Method Wrapping**: Wraps `Call.execute()` and `Call.enqueue()` methods
2. **Context Capture Timing**: Captures current OpenTelemetry context when the call is made (not when client is created)
3. **Automatic Span Creation**: Creates HTTP spans as children of the current span
4. **Header Injection**: Automatically injects trace headers into requests
5. **Response Handling**: Handles response/error cases with proper span status

### Singleton Client Should Work (In Theory)

If auto-instrumentation is working correctly, this should work:

```kotlin
// Created once at app startup
private val client = OkHttpClient()

// Later in request coroutine
span?.makeCurrent().use {
    // Auto-instrumentation should capture THIS context, not startup context
    client.newCall(request).enqueue(callback)
}
```

### Why Current Code Creates New Clients

Possible reasons for the current per-request client pattern:

1. **Defensive Programming**: Ensuring each request gets fresh instrumentation
2. **Uncertainty**: About auto-instrumentation timing behavior
3. **Legacy Patterns**: From before auto-instrumentation was reliable
4. **Actual Issues**: Real problems with singleton + auto-instrumentation

### Solution 1: Manual Context Propagation (Fallback)

If auto-instrumentation doesn't work with singletons, manually inject context:

```kotlin
class FetchHelpers {
    companion object {
        // Singleton client
        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder().build()
        }
        
        suspend fun executeRequestWithBaggage(
            request: Request, 
            baggageHeaders: Map<String, String>
        ): String = suspendCancellableCoroutine<String> { cont ->
            
            val tracer = OtelDemoApplication.getTracer()
            val span = tracer?.spanBuilder("executeRequestWithBaggage")
                ?.setSpanKind(SpanKind.CLIENT)
                ?.startSpan()
            
            try {
                span?.makeCurrent().use { scope ->
                    val requestBuilder = request.newBuilder()
                    baggageHeaders.forEach { (key, value) ->
                        requestBuilder.addHeader(key, value)
                    }
                    
                    // CRITICAL: Manually inject trace context
                    val currentContext = Context.current()
                    val injectedRequest = injectTraceContext(requestBuilder, currentContext).build()
                    
                    client.newCall(injectedRequest).enqueue(object : Callback {
                        // ... callback implementation
                    })
                }
            } catch (e: Exception) {
                span?.setStatus(StatusCode.ERROR)
                span?.recordException(e)
                span?.end()
                throw e
            }
        }
        
        private fun injectTraceContext(
            requestBuilder: Request.Builder, 
            context: Context
        ): Request.Builder {
            val propagator = GlobalOpenTelemetry.getPropagators().textMapPropagator
            val setter = TextMapSetter<Request.Builder> { carrier, key, value ->
                carrier?.addHeader(key, value)
            }
            propagator.inject(context, requestBuilder, setter)
            return requestBuilder
        }
    }
}
```

**Required Imports:**
```kotlin
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
```

### Solution 2: OkHttp Interceptor Approach

Alternative using an interceptor for automatic context injection:

```kotlin
class TracingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentContext = Context.current()
        
        val requestBuilder = originalRequest.newBuilder()
        val propagator = GlobalOpenTelemetry.getPropagators().textMapPropagator
        
        val setter = TextMapSetter<Request.Builder> { carrier, key, value ->
            carrier?.addHeader(key, value)
        }
        
        propagator.inject(currentContext, requestBuilder, setter)
        
        return chain.proceed(requestBuilder.build())
    }
}

// Add to singleton client
private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor(TracingInterceptor())
        .build()
}
```

### Expected Trace Hierarchy

With proper implementation, traces should show:

```
ProductAPIService.fetchProducts (manual span)
├── executeRequestWithBaggage (manual span) 
    └── GET /products (OkHttp auto-instrumentation span)
```

### Research Questions to Investigate

1. **Auto-Instrumentation Compatibility**: Does ByteBuddy instrumentation work reliably with singleton OkHttpClient?
2. **Context Timing**: When exactly does auto-instrumentation capture OpenTelemetry context?
3. **Configuration Requirements**: Are there specific OkHttpClient settings needed for auto-instrumentation?
4. **Performance Impact**: How much overhead does per-request client creation add?
5. **Trace Quality**: Which approach produces cleaner, more useful traces?

### Potential Issues with Current Refactoring

The recent refactoring to use `executeRequestWithBaggage` everywhere might have broken trace linkage because:

1. **Span Naming Changes**: All spans now named `executeRequestWithBaggage` instead of mix
2. **Baggage Addition**: All requests now carry session baggage (server might not expect this)
3. **Context Changes**: New parent-child relationships might confuse existing trace queries

### Recommended Investigation Steps

1. **Test Singleton Approach**: Create singleton OkHttpClient and verify auto-instrumentation works
2. **Compare Trace Quality**: Examine Honeycomb traces for both approaches
3. **Performance Testing**: Measure overhead of per-request vs singleton client
4. **Server-Side Impact**: Ensure servers handle baggage headers on all endpoints
5. **Gradual Rollout**: Consider feature flags for testing different approaches

### Key Takeaway

The current per-request client creation might be masking an auto-instrumentation issue or might be unnecessary defensive programming. The ideal solution would be a singleton client with reliable auto-instrumentation, falling back to manual context injection if needed.