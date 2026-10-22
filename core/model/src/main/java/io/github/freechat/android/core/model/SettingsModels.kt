package io.github.freechat.android.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    CHINESE("zh-CN"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank() || tag.equals("system", ignoreCase = true)) {
                return SYSTEM
            }
            return entries.firstOrNull {
                it.tag.isNotBlank() && it.tag.equals(tag, ignoreCase = true)
            } ?: SYSTEM
        }
    }
}
