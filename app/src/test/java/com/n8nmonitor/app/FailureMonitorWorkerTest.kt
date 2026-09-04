package com.n8nmonitor.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FailureMonitorWorkerTest {
    @Test
    fun returnsOnlyExecutionsThatHaveNotBeenSeen() {
        val executions = listOf(
            Execution("new", "1", "error", null, null),
            Execution("old", "1", "error", null, null),
        )

        assertEquals(listOf("new"), unseenExecutions(executions, setOf("old")).map(Execution::id))
    }
}
