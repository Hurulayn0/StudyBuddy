
package com.studybuddy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

data class StudyTask(
    val id: Int,
    val title: String,
    val isCompleted: Boolean = false
)

@Composable
fun HomeScreen() {

    val tasks = remember {
        mutableStateListOf<StudyTask>()
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var newTaskTitle by remember {
        mutableStateOf("")
    }

    var nextTaskId by remember {
        mutableIntStateOf(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        // Application title
        Text(
            text = "StudyBuddy",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Welcome to your study planner!",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Task section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Upcoming Tasks",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    newTaskTitle = ""
                    showAddDialog = true
                }
            ) {
                Text("Add Task")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Empty list message
        if (tasks.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No tasks yet. Add your first task!",
                    modifier = Modifier.padding(16.dp)
                )
            }

        } else {

            // Task list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = tasks,
                    key = { task -> task.id }
                ) { task ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { checked ->

                                    val index = tasks.indexOfFirst {
                                        it.id == task.id
                                    }

                                    if (index != -1) {
                                        tasks[index] = task.copy(
                                            isCompleted = checked
                                        )
                                    }
                                }
                            )

                            Text(
                                text = task.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (task.isCompleted) {
                                    TextDecoration.LineThrough
                                } else {
                                    TextDecoration.None
                                }
                            )

                            TextButton(
                                onClick = {
                                    tasks.removeAll {
                                        it.id == task.id
                                    }
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add task dialog
    if (showAddDialog) {

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
            },

            title = {
                Text("Add New Task")
            },

            text = {

                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = {
                        newTaskTitle = it
                    },
                    label = {
                        Text("Task Name")
                    },
                    placeholder = {
                        Text("e.g. Complete Kotlin homework")
                    },
                    singleLine = true
                )
            },

            confirmButton = {

                Button(
                    enabled = newTaskTitle.isNotBlank(),
                    onClick = {

                        tasks.add(
                            StudyTask(
                                id = nextTaskId,
                                title = newTaskTitle.trim()
                            )
                        )

                        nextTaskId++

                        newTaskTitle = ""
                        showAddDialog = false
                    }
                ) {
                    Text("Save")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showAddDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
