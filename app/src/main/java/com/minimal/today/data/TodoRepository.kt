package com.minimal.today.data

import android.content.Context
import com.minimal.today.model.TodoItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

class TodoRepository(private val context: Context) {

    private val fileName = "todos.txt"
    private val prefsName = "today_local_storage"
    private val prefsKey = "cached_todos_data"
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    // Regex to match: [x] 09:00 AM - 11:30 AM - Task OR [x] 09:00 AM - Task OR [x] Task
    private val linePattern = Pattern.compile("^\\[([ xX])\\]\\s*(?:(\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?)(?:\\s*-\\s*(\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?))?\\s*[-|•]\\s*)?(.*)$")

    private fun getInternalFile(): File {
        return File(context.filesDir, fileName)
    }

    private fun getExternalFile(): File? {
        return try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) File(externalDir, fileName) else null
        } catch (_: Exception) {
            null
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getFilePath(): String {
        val ext = getExternalFile()
        return ext?.absolutePath ?: getInternalFile().absolutePath
    }

    /**
     * Reads all todos from local storage or parses from txt file
     */
    @Synchronized
    fun readAllFromTxt(): Map<String, List<TodoItem>> {
        // Try reading from local JSON storage first
        val cachedJson = prefs.getString(prefsKey, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val result = mutableMapOf<String, MutableList<TodoItem>>()
                val rootObj = JSONObject(cachedJson)
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val dateKey = keys.next()
                    val array = rootObj.getJSONArray(dateKey)
                    val items = mutableListOf<TodoItem>()
                    for (i in 0 until array.length()) {
                        val itemObj = array.getJSONObject(i)
                        items.add(
                            TodoItem(
                                id = itemObj.optString("id", UUID.randomUUID().toString()),
                                text = itemObj.getString("text"),
                                notes = if (itemObj.has("notes") && !itemObj.isNull("notes")) itemObj.getString("notes") else null,
                                startTime = if (itemObj.has("startTime") && !itemObj.isNull("startTime")) itemObj.getString("startTime") else null,
                                endTime = if (itemObj.has("endTime") && !itemObj.isNull("endTime")) itemObj.getString("endTime") else null,
                                isDone = itemObj.optBoolean("isDone", false),
                                date = itemObj.optString("date", dateKey)
                            )
                        )
                    }
                    result[dateKey] = items
                }
                return result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: Read from txt file
        val file = getInternalFile()
        if (!file.exists()) {
            return emptyMap()
        }

        val result = mutableMapOf<String, MutableList<TodoItem>>()
        var currentDate = ""
        var lastItem: TodoItem? = null

        try {
            file.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("===") && line.endsWith("===")) {
                    currentDate = line.replace("=", "").trim()
                    if (currentDate.isNotEmpty()) {
                        result.putIfAbsent(currentDate, mutableListOf())
                    }
                    lastItem = null
                } else if (line.startsWith(">") && lastItem != null && currentDate.isNotEmpty()) {
                    val noteText = line.removePrefix(">").trim()
                    val list = result[currentDate]
                    if (!list.isNullOrEmpty()) {
                        val updated = list.last().copy(notes = noteText)
                        list[list.size - 1] = updated
                    }
                } else if (line.isNotEmpty() && currentDate.isNotEmpty()) {
                    val matcher = linePattern.matcher(line)
                    if (matcher.matches()) {
                        val mark = matcher.group(1) ?: " "
                        val isDone = mark.equals("x", ignoreCase = true)
                        val start = matcher.group(2)?.trim()?.takeIf { it.isNotEmpty() }
                        val end = matcher.group(3)?.trim()?.takeIf { it.isNotEmpty() }
                        val text = matcher.group(4)?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            val newItem = TodoItem(
                                id = UUID.randomUUID().toString(),
                                text = text,
                                notes = null,
                                startTime = start,
                                endTime = end,
                                isDone = isDone,
                                date = currentDate
                            )
                            result[currentDate]?.add(newItem)
                            lastItem = newItem
                        }
                    } else {
                        val newItem = TodoItem(
                            id = UUID.randomUUID().toString(),
                            text = line,
                            notes = null,
                            startTime = null,
                            endTime = null,
                            isDone = false,
                            date = currentDate
                        )
                        result[currentDate]?.add(newItem)
                        lastItem = newItem
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    /**
     * Gets only today's todos.
     */
    fun getTodayTodos(): List<TodoItem> {
        val today = getTodayDateString()
        val allData = readAllFromTxt()
        return allData[today] ?: emptyList()
    }

    /**
     * Updates today's todos and rewrites the single .txt file keeping past history intact.
     */
    @Synchronized
    fun saveTodayTodos(todayItems: List<TodoItem>) {
        val today = getTodayDateString()
        val allData = readAllFromTxt().toMutableMap()
        allData[today] = todayItems.map { it.copy(date = today) }

        // 1. Save to Local SharedPreferences Cache
        try {
            val rootObj = JSONObject()
            for ((dateKey, items) in allData) {
                val array = JSONArray()
                for (item in items) {
                    val itemObj = JSONObject().apply {
                        put("id", item.id)
                        put("text", item.text)
                        put("notes", item.notes)
                        put("startTime", item.startTime)
                        put("endTime", item.endTime)
                        put("isDone", item.isDone)
                        put("date", item.date)
                    }
                    array.put(itemObj)
                }
                rootObj.put(dateKey, array)
            }
            prefs.edit().putString(prefsKey, rootObj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Format to .txt File
        val sb = StringBuilder()

        // Put today first
        if (allData.containsKey(today)) {
            sb.append("=== ").append(today).append(" ===\n")
            val items = allData[today].orEmpty()
            for (item in items) {
                val mark = if (item.isDone) "[x]" else "[ ]"
                val timeStr = item.formattedTime
                if (!timeStr.isNullOrBlank()) {
                    sb.append(mark).append(" ").append(timeStr).append(" - ").append(item.text).append("\n")
                } else {
                    sb.append(mark).append(" ").append(item.text).append("\n")
                }
                if (!item.notes.isNullOrBlank()) {
                    sb.append("  > ").append(item.notes.replace("\n", " ")).append("\n")
                }
            }
            sb.append("\n")
        }

        // Put previous dates in descending order
        val otherDates = allData.keys.filter { it != today }.sortedDescending()
        for (date in otherDates) {
            val items = allData[date].orEmpty()
            if (items.isNotEmpty()) {
                sb.append("=== ").append(date).append(" ===\n")
                for (item in items) {
                    val mark = if (item.isDone) "[x]" else "[ ]"
                    val timeStr = item.formattedTime
                    if (!timeStr.isNullOrBlank()) {
                        sb.append(mark).append(" ").append(timeStr).append(" - ").append(item.text).append("\n")
                    } else {
                        sb.append(mark).append(" ").append(item.text).append("\n")
                    }
                    if (!item.notes.isNullOrBlank()) {
                        sb.append("  > ").append(item.notes.replace("\n", " ")).append("\n")
                    }
                }
                sb.append("\n")
            }
        }

        val content = sb.toString().trimEnd() + "\n"

        // Write to internal file
        try {
            getInternalFile().writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Mirror to external files directory
        try {
            getExternalFile()?.writeText(content)
        } catch (_: Exception) {}
    }

    /**
     * Gets the full text content of the .txt file.
     */
    fun getRawFileContent(): String {
        val file = getInternalFile()
        return if (file.exists()) file.readText() else "No logs recorded yet."
    }
}
