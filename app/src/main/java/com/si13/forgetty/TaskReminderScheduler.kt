package com.si13.forgetty

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface TaskMutationObserver {
    fun onTaskChanged(task: Task)
    fun onTaskDeleted(taskId: String)
}

class AndroidTaskMutationObserver(private val context: Context) : TaskMutationObserver {
    override fun onTaskChanged(task: Task) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = reminderPendingIntent(context, task.id)
        alarm.cancel(pending)
        val trigger = task.reminderAt
        if (!task.completed && trigger != null && trigger > System.currentTimeMillis()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
        ForgettyWidgetUpdater.updateAll(context)
    }

    override fun onTaskDeleted(taskId: String) {
        context.getSystemService(AlarmManager::class.java).cancel(reminderPendingIntent(context, taskId))
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
        ForgettyWidgetUpdater.updateAll(context)
    }

    private fun reminderPendingIntent(context: Context, taskId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            Intent(context, TaskReminderReceiver::class.java).putExtra(EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val task = TaskRepository.create(context).getTasks().firstOrNull { it.id == taskId } ?: return@runCatching
                if (task.completed) return@runCatching
                createReminderChannel(context)
                val open = PendingIntent.getActivity(
                    context, task.id.hashCode(),
                    Intent(context, MainActivity::class.java).putExtra(EXTRA_TASK_ID, task.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(context.getString(R.string.reminder_notification_title, task.text))
                    .setContentText(task.notificationSummary(context))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(0, context.getString(R.string.open_app), open)
                    .build()
                if (context.canPostNotifications()) {
                    NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
                }
            }
            pending.finish()
        }
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val repository = TaskRepository.create(context)
                val task = repository.getTasks().firstOrNull { it.id == taskId } ?: return@runCatching
                when (intent.action) {
                    ACTION_COMPLETE -> repository.setTaskCompleted(task, true)
                    ACTION_SNOOZE -> repository.updateTask(task.copy(reminderAt = System.currentTimeMillis() + 10 * 60_000L))
                }
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
            }
            pending.finish()
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                TaskRepository.create(context).getTasks().forEach(AndroidTaskMutationObserver(context)::onTaskChanged)
                DailyNotificationScheduler.schedule(context)
            }
            pending.finish()
        }
    }
}

object DailyNotificationScheduler {
    private const val REQUEST_CODE = 9071
    fun schedule(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DailyNotificationReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = ZonedDateTime.now()
        var next = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending)
    }
}

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                createDailyChannel(context)
                val preferences = ForgettyPreferences.create(context).notificationPreferences
                val today = LocalDate.now()
                val activeTasks = TaskRepository.create(context).getTasks().filterNot(Task::completed)
                val dueToday = activeTasks.filter { it.dueDate == today.toString() }
                if (preferences.overdueReminders && dueToday.isNotEmpty()) {
                    val task = dueToday.first()
                    postDailyNotification(
                        context,
                        DUE_TODAY_NOTIFICATION_ID,
                        context.getString(R.string.due_today_notification_title),
                        listOf(task.text, task.listName).filter(String::isNotBlank).joinToString(" · ")
                    )
                }
                if (preferences.dailySummary && dueToday.isNotEmpty()) {
                    val high = dueToday.count { it.priority == TaskPriority.HIGH }
                    val later = activeTasks.count { task ->
                        runCatching { task.dueDate?.let(LocalDate::parse)?.isAfter(today) == true }.getOrDefault(false)
                    }
                    val parts = buildList {
                        if (high > 0) add(context.resources.getQuantityString(R.plurals.today_digest_high_priority, high, high))
                        if (later > 0) add(context.resources.getQuantityString(R.plurals.today_digest_due_later, later, later))
                    }
                    postDailyNotification(
                        context,
                        TODAY_DIGEST_NOTIFICATION_ID,
                        context.getString(R.string.today_digest_notification_title, dueToday.size),
                        parts.ifEmpty { listOf(context.getString(R.string.today_digest_ready)) }.joinToString(" · ")
                    )
                }
            }
            DailyNotificationScheduler.schedule(context)
            pendingResult.finish()
        }
    }
}

private fun postDailyNotification(context: Context, id: Int, title: String, body: String) {
    if (!context.canPostNotifications()) return
    val open = PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_TODAY_TASKS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, DAILY_CHANNEL)
        .setSmallIcon(R.drawable.ic_notifications)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(open)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .addAction(0, context.getString(R.string.open_app), open)
        .build()
    NotificationManagerCompat.from(context).notify(id, notification)
}

private fun Task.notificationSummary(context: Context): String {
    val dateLabel = when (dueDate) {
        LocalDate.now().toString() -> context.getString(R.string.today)
        LocalDate.now().plusDays(1).toString() -> context.getString(R.string.tomorrow)
        null -> null
        else -> dueDate
    }
    val timeLabel = dueTimeMinutes?.let { "%02d:%02d".format(it / 60, it % 60) }
    return listOfNotNull(
        listOfNotNull(dateLabel, timeLabel).takeIf(List<String>::isNotEmpty)?.joinToString(" at "),
        listName.takeIf(String::isNotBlank)
    ).joinToString(" · ")
}

private fun Context.canPostNotifications() = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
    android.content.pm.PackageManager.PERMISSION_GRANTED

private fun createReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(REMINDER_CHANNEL, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
    )
}

private fun createDailyChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(DAILY_CHANNEL, context.getString(R.string.daily_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.daily_channel_description)
        }
    )
}

const val EXTRA_TASK_ID = "forgetty_task_id"
const val ACTION_COMPLETE = "com.si13.forgetty.COMPLETE_TASK"
const val ACTION_SNOOZE = "com.si13.forgetty.SNOOZE_TASK"
private const val REMINDER_CHANNEL = "forgetty_task_reminders"
private const val DAILY_CHANNEL = "forgetty_daily_planning"
private const val DUE_TODAY_NOTIFICATION_ID = 9072
private const val TODAY_DIGEST_NOTIFICATION_ID = 9073
