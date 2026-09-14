package com.si13.forgetty

import android.content.Context
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

data class TaskTagDefinition(
    val id: String,
    val name: String,
    val color: String
)

/** Lightweight local metadata for tag names and colors; task membership remains in Task. */
class TaskTagStore private constructor(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getTags(): List<TaskTagDefinition> = preferences.getString(KEY_TAGS, null)?.let { encoded ->
        if (encoded.isBlank()) emptyList() else encoded.split(RECORD_SEPARATOR).mapNotNull(::decode)
    } ?: DEFAULT_TAGS

    fun create(name: String, color: String = nextColor()): TaskTagDefinition {
        val tag = TaskTagDefinition(UUID.randomUUID().toString(), uniqueName(name), color)
        save(getTags() + tag)
        return tag
    }

    fun update(id: String, name: String, color: String): TaskTagChange? {
        val current = getTags()
        val old = current.firstOrNull { it.id == id } ?: return null
        val updated = old.copy(name = uniqueName(name, id), color = color)
        save(current.map { if (it.id == id) updated else it })
        return TaskTagChange(old.name, updated)
    }

    fun delete(id: String): TaskTagDefinition? {
        val current = getTags()
        val target = current.firstOrNull { it.id == id } ?: return null
        save(current.filterNot { it.id == id })
        return target
    }

    fun ensureNames(names: Collection<String>) {
        val existing = getTags()
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val missing = names.map(String::trim).filter(String::isNotEmpty)
            .distinctBy(String::lowercase).filter { it.lowercase() !in existingNames }
        if (missing.isEmpty()) return
        save(existing + missing.mapIndexed { index, name ->
            TaskTagDefinition(UUID.randomUUID().toString(), name.take(MAX_TAG_NAME_LENGTH), COLORS[(existing.size + index) % COLORS.size])
        })
    }

    private fun uniqueName(raw: String, excludingId: String? = null): String {
        val base = raw.trim().take(MAX_TAG_NAME_LENGTH).ifBlank { "Tag" }
        val names = getTags().filterNot { it.id == excludingId }.map { it.name.lowercase() }.toSet()
        if (base.lowercase() !in names) return base
        var suffix = 2
        while (true) {
            val suffixText = " $suffix"
            val candidate = base.take(MAX_TAG_NAME_LENGTH - suffixText.length) + suffixText
            if (candidate.lowercase() !in names) return candidate
            suffix++
        }
    }

    private fun nextColor(): String = COLORS[getTags().size % COLORS.size]
    private fun save(values: List<TaskTagDefinition>) = preferences.edit()
        .putString(KEY_TAGS, values.joinToString(RECORD_SEPARATOR, transform = ::encode)).apply()
    private fun encode(value: TaskTagDefinition) = listOf(value.id, value.name, value.color)
        .joinToString(FIELD_SEPARATOR) { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
    private fun decode(value: String): TaskTagDefinition? = value.split(FIELD_SEPARATOR)
        .takeIf { it.size == 3 }?.map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
        ?.let { TaskTagDefinition(it[0], it[1], it[2]) }

    companion object {
        private const val NAME = "forgetty_task_tags"
        private const val KEY_TAGS = "tags"
        private const val RECORD_SEPARATOR = "|"
        private const val FIELD_SEPARATOR = ","
        private const val MAX_TAG_NAME_LENGTH = 32
        val COLORS = listOf("#0E9BED", "#00A884", "#7B61FF", "#F59E0B", "#D32F2F")
        val DEFAULT_TAGS = listOf(
            TaskTagDefinition("qa", "QA", COLORS[0]),
            TaskTagDefinition("release", "Release", COLORS[1]),
            TaskTagDefinition("android", "Android", COLORS[2]),
            TaskTagDefinition("personal", "Personal", COLORS[3])
        )

        fun create(context: Context) = TaskTagStore(context)
    }
}

data class TaskTagChange(val oldName: String, val updated: TaskTagDefinition)
