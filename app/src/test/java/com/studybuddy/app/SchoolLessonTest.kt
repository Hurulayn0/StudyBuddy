package com.studybuddy.app.school

import org.junit.Assert.*
import org.junit.Test

class SchoolLessonTest {
    private val lesson = SchoolLesson(1, "Mobil Programlama", "Öğretmen", "B101", 1000, 2000)
    @Test fun lessonBoundaryStates() {
        assertEquals("Başlayacak", lesson.status(999))
        assertEquals("Devam ediyor", lesson.status(1000))
        assertEquals("Devam ediyor", lesson.status(1999))
        assertEquals("Tamamlandı", lesson.status(2000))
    }
    @Test fun rejectsMissingInformationAndInvalidTimes() {
        assertTrue(lesson.valid(999))
        assertFalse(lesson.valid(1000))
        assertFalse(lesson.copy(name = " ").valid(0))
        assertFalse(lesson.copy(teacher = "").valid(0))
        assertFalse(lesson.copy(end = 1000).valid(0))
        assertFalse(lesson.copy(end = 999).valid(0))
    }
}
