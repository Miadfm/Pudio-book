package com.miadfm.podcasts.data.settings

enum class AppLanguage(
    val code: String,
    val displayNameEn: String,
    val nativeName: String
) {
    ENGLISH("en", "English", "English"),
    PERSIAN("fa", "Persian", "فارسی");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code == null) return ENGLISH
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
