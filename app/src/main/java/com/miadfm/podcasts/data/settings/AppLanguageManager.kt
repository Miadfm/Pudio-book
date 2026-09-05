package com.miadfm.podcasts.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLanguageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentLanguage = MutableStateFlow(loadPersistedLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private fun loadPersistedLanguage(): AppLanguage {
        val savedCode = prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ENGLISH.code)
        return AppLanguage.fromCode(savedCode)
    }

    fun getLanguage(): AppLanguage {
        return _currentLanguage.value
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit()
            .putString(KEY_SELECTED_LANGUAGE, language.code)
            .apply()
        _currentLanguage.value = language
    }

    companion object {
        private const val PREFS_NAME = "app_language_persistent_settings"
        private const val KEY_SELECTED_LANGUAGE = "selected_app_language_code"
    }
}
