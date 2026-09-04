package com.n8nmonitor.app

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class N8nApiTest {
    @Test
    fun parsesDocumentedWorkflowEnvelope() {
        val workflows = parseWorkflows(
            """{"data":[{"id":"7","name":"Daily sync","active":true,"updatedAt":"2026-09-04T10:00:00Z"}],"nextCursor":null}""",
        )

        assertEquals(1, workflows.size)
        assertEquals("Daily sync", workflows.single().name)
        assertEquals(true, workflows.single().active)
    }

    @Test
    fun parsesDocumentedExecutionFields() {
        val executions = parseExecutions(
            """{"data":[{"id":"42","workflowId":"7","status":"error","startedAt":"2026-09-04T10:00:00Z","stoppedAt":null}]}""",
        )

        assertEquals("error", executions.single().status)
        assertEquals("7", executions.single().workflowId)
        assertNull(executions.single().stoppedAt)
    }

    @Test
    fun rejectsTheOldIncorrectEnvelope() {
        assertThrows(JSONException::class.java) {
            parseWorkflows("""{"results":[]}""")
        }
    }

    @Test
    fun requiresHttpsAndAnApiKey() {
        assertEquals("Enter an n8n API key.", validateMonitorSettings("https://n8n.example.com", ""))
        assertEquals("The n8n URL must use HTTPS.", validateMonitorSettings("http://n8n.example.com", "key"))
        assertEquals(
            "Use the n8n root URL without a query or fragment.",
            validateMonitorSettings("https://n8n.example.com?debug=true", "key"),
        )
        assertNull(validateMonitorSettings("https://n8n.example.com", "key"))
    }
}
