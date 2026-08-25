package com.focuspath.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.focuspath.app.data.local.AppDatabase
import com.focuspath.app.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = try {
            val dao = AppDatabase.getDatabase(context).taskDaoProvider()
            dao.getAllTasksOnce()
        } catch (e: Exception) {
            emptyList<TaskEntity>()
        }

        provideContent {
            WidgetContent(tasks)
        }
    }

    @Composable
    private fun WidgetContent(tasks: List<TaskEntity>) {
        val terminalGreen = Color(0xFF00FF41)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp)
                .appWidgetBackground()
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> FOCUS TASKS",
                    style = TextStyle(
                        color = ColorProvider(terminalGreen),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(GlanceModifier.height(4.dp))
            
            if (tasks.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No tasks found", style = TextStyle(color = ColorProvider(Color.Gray)))
                }
            } else {
                tasks.take(3).forEach { task ->
                    TaskItem(task)
                }
            }
        }
    }

    @Composable
    private fun TaskItem(task: TaskEntity) {
        val textColor = if (task.isCompleted) Color.Gray else Color.White
        
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(TaskIdKey to task.id)
                )),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val status = if (task.isCompleted) "[X]" else "[ ]"
            Text(
                text = "$status ${task.title}",
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 12.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1
            )
        }
    }
}

val TaskIdKey = ActionParameters.Key<Long>("taskId")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        
        withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).taskDaoProvider()
                val tasks = dao.getAllTasksOnce()
                val task = tasks.find { it.id == taskId }
                if (task != null) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    dao.updateTask(updatedTask)
                    
                    // Refresh widget
                    TaskWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                android.util.Log.e("TaskWidget", "Error toggling task", e)
            }
        }
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}
