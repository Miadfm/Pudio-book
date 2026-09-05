package com.miadfm.podcasts.data.podcast

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class PodcastHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _historyMap = MutableStateFlow<Map<String, Long>>(loadHistory())
    val historyMap: StateFlow<Map<String, Long>> = _historyMap.asStateFlow()

    private fun loadHistory(): Map<String, Long> {
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyMap()
        return try {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getLong(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun recordPlayback(episodeId: String, timestamp: Long = System.currentTimeMillis()) {
        val current = _historyMap.value.toMutableMap()
        current[episodeId] = timestamp
        _historyMap.value = current

        try {
            val json = JSONObject()
            for ((k, v) in current) {
                json.put(k, v)
            }
            prefs.edit().putString(KEY_HISTORY, json.toString()).apply()
        } catch (e: Exception) {
            // Silently handle any persistence edge case
        }
    }

    fun clearHistory() {
        _historyMap.value = emptyMap()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "podcast_playback_history_prefs"
        private const val KEY_HISTORY = "podcast_history_timestamps_json"
    }
}
