package io.opentelemetry.android.demo.shop.session

import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class SessionManagerTest {

    @Test
    fun getInstance_returnsConsistentSingleton() {
        val instance1 = SessionManager.getInstance()
        val instance2 = SessionManager.getInstance()
        
        assertSame("SessionManager should be a singleton", instance1, instance2)
    }

    @Test
    fun currentSessionId_isValidUUID() {
        val sessionManager = SessionManager.getInstance()
        val sessionId = sessionManager.currentSessionId
        
        assertNotNull("Session ID should not be null", sessionId)
        assertTrue("Session ID should not be empty", sessionId.isNotEmpty())
        
        // Verify it's a valid UUID format
        try {
            UUID.fromString(sessionId)
        } catch (e: IllegalArgumentException) {
            fail("Session ID should be valid UUID format: $sessionId")
        }
    }

    @Test
    fun resetSession_generatesNewSessionId() {
        val sessionManager = SessionManager.getInstance()
        val originalSessionId = sessionManager.currentSessionId
        
        sessionManager.resetSession()
        val newSessionId = sessionManager.currentSessionId
        
        assertNotEquals("Reset should generate a new session ID", originalSessionId, newSessionId)
        assertTrue("New session ID should not be empty", newSessionId.isNotEmpty())
        
        // Verify new ID is also a valid UUID
        try {
            UUID.fromString(newSessionId)
        } catch (e: IllegalArgumentException) {
            fail("New session ID should be valid UUID format: $newSessionId")
        }
    }

    @Test
    fun resetSession_maintainsSingletonBehavior() {
        val sessionManager1 = SessionManager.getInstance()
        val originalSessionId = sessionManager1.currentSessionId
        
        sessionManager1.resetSession()
        
        val sessionManager2 = SessionManager.getInstance()
        val newSessionId = sessionManager2.currentSessionId
        
        assertSame("Should still be the same singleton instance", sessionManager1, sessionManager2)
        assertNotEquals("Session ID should have changed", originalSessionId, newSessionId)
        assertEquals("Both references should return the same new session ID", 
            sessionManager1.currentSessionId, sessionManager2.currentSessionId)
    }
}