package com.studybuddy.app.school

data class SchoolLesson(val id: Int, val name: String, val teacher: String, val room: String, val start: Long, val end: Long, val started: Boolean = false, val ended: Boolean = false) {
    fun status(now: Long): String = when { now < start -> "Başlayacak"; now < end -> "Devam ediyor"; else -> "Tamamlandı" }
    fun valid(now: Long) = name.isNotBlank() && teacher.isNotBlank() && start > now && end > start
}
