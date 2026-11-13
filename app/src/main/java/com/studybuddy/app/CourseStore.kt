package com.studybuddy.app

import android.content.Context

object CourseStore {
    fun all(context: Context): List<String> = (listOf("Mobil Programlama", "Veritabanı Sistemleri", "Matematik", "Algoritmalar", "İngilizce") +
        context.getSharedPreferences("courses", Context.MODE_PRIVATE).getStringSet("names", emptySet()).orEmpty() +
        TaskStore.load(context).map { it.course }.filter { it.isNotBlank() }).distinct()
    fun add(context: Context, name: String) {
        val p = context.getSharedPreferences("courses", Context.MODE_PRIVATE)
        p.edit().putStringSet("names", p.getStringSet("names", emptySet()).orEmpty() + name.trim()).commit()
    }
}
