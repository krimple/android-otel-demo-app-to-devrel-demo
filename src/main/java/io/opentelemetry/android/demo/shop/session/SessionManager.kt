package io.opentelemetry.android.demo.shop.session

import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.api.trace.StatusCode
import java.util.UUID

/**
 * Manages session identifiers for the shopping cart functionality.
 * Generates UUID-based session IDs that persist for the duration of a shopping session
 * and resets after successful checkout completion.
 */
class SessionManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null
        
        fun getInstance(): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager().also { INSTANCE = it }
            }
        }
    }
    
    @Volatile
    private var _currentSessionId: String = generateNewUUID()
    
    val currentSessionId: String
        get() = _currentSessionId
    
    /**
     * Generates a new session ID and tracks the event with telemetry.
     * This should be called after successful checkout completion.
     */
    fun resetSession() {
        val tracer = OtelDemoApplication.getTracer()
        val span = tracer?.spanBuilder("SessionManager.resetSession")
            ?.setAttribute("app.prev.session.id", _currentSessionId)
            ?.setAttribute("app.operation.type", "reset_session")
            ?.startSpan()
        
        try {
            val newSessionId = generateNewUUID()
            _currentSessionId = newSessionId
            
            span?.setAttribute("app.temp.session.id", newSessionId)
        } catch (e: Exception) {
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(e)
            throw e
        } finally {
            span?.end()
        }
    }
    
    private fun generateNewUUID(): String {
        return UUID.randomUUID().toString()
    }
}