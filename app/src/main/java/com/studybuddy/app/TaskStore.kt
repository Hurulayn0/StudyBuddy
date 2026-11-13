package com.studybuddy.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StudyTask(val id: Int, val name: String, val course: String, val done: Boolean = false)

object TaskStore {
    fun preferences(context: Context) = context.getSharedPreferences("study_tasks", Context.MODE_PRIVATE)
    fun load(context: Context): List<StudyTask> {
        val raw = preferences(context).getString("tasks", null) ?: return listOf(
            StudyTask(1, "Finish Kotlin homework", "Mobile Programming"),
            StudyTask(2, "Review database notes", "Database Systems"),
            StudyTask(3, "Prepare project sketches", "Mobile Programming")
        )
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i -> array.getJSONObject(i).let { StudyTask(it.getInt("id"), it.getString("name"), it.getString("course"), it.getBoolean("done")) } }
        }.getOrDefault(emptyList())
    }
    fun save(context: Context, tasks: List<StudyTask>): Boolean {
        val array = JSONArray()
        tasks.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("course", it.course).put("done", it.done)) }
        return preferences(context).edit().putString("tasks", array.toString()).commit()
    }
}
