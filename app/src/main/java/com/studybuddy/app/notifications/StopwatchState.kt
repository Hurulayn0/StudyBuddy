package com.studybuddy.app.notifications

// A monotonic clock keeps pauses and wall-clock changes out of the measured duration.
data class StopwatchState(val phase: String = "idle", val accumulated: Long = 0, val anchor: Long = 0) {
    fun elapsed(now: Long): Long = accumulated + if (phase == "running") (now - anchor).coerceAtLeast(0) else 0
    fun start(now: Long) = StopwatchState("running", 0, now)
    fun pause(now: Long) = if (phase == "running") StopwatchState("paused", elapsed(now)) else this
    fun resume(now: Long) = if (phase == "paused") copy(phase = "running", anchor = now) else this
    fun finish(now: Long) = if (phase == "running" || phase == "paused") StopwatchState("finished", elapsed(now)) else this
}
