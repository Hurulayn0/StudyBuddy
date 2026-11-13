package com.studybuddy.app.notifications

import org.junit.Assert.*
import org.junit.Test

class StopwatchStateTest {
    @Test fun pausesAreExcludedAndResumeAccumulates() {
        val paused = StopwatchState().start(1000).pause(6000)
        assertEquals(5000L, paused.elapsed(20000))
        val finished = paused.resume(20000).finish(23000)
        assertEquals(8000L, finished.elapsed(90000))
        assertEquals("finished", finished.phase)
    }
    @Test fun duplicateActionsDoNotResetTime() {
        val paused = StopwatchState().start(100).pause(1100)
        assertEquals(paused, paused.pause(5000))
        val running = paused.resume(6000)
        assertEquals(running, running.resume(9000))
        val finished = running.finish(10000)
        assertEquals(finished, finished.finish(14000))
    }
    @Test fun finishingWhilePausedAndStartingFresh() {
        val finished = StopwatchState().start(1000).pause(3000).finish(9000)
        assertEquals(2000L, finished.elapsed(10000))
        assertEquals(0L, finished.start(20000).elapsed(20000))
    }
}
