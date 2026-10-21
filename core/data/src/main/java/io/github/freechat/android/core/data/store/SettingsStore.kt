package io.github.freechat.android.core.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val subscribeBattleLog: Boolean = false,
    val notifyPrivate: Boolean = true,
    val notifyParty: Boolean = true,
    val lockScreenHideContent: Boolean = true,
    val developerRawCommands: Boolean = false,
    val developerSplitToast: Boolean = false,
)

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            preferences[SUBSCRIBE_BATTLE_LOG] = updated.subscribeBattleLog
            preferences[NOTIFY_PRIVATE] = updated.notifyPrivate
            preferences[NOTIFY_PARTY] = updated.notifyParty
            preferences[LOCK_SCREEN_HIDE_CONTENT] = updated.lockScreenHideContent
            preferences[DEVELOPER_RAW_COMMANDS] = updated.developerRawCommands
            preferences[DEVELOPER_SPLIT_TOAST] = updated.developerSplitToast
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        subscribeBattleLog = this[SUBSCRIBE_BATTLE_LOG] ?: false,
        notifyPrivate = this[NOTIFY_PRIVATE] ?: true,
        notifyParty = this[NOTIFY_PARTY] ?: true,
        lockScreenHideContent = this[LOCK_SCREEN_HIDE_CONTENT] ?: true,
        developerRawCommands = this[DEVELOPER_RAW_COMMANDS] ?: false,
        developerSplitToast = this[DEVELOPER_SPLIT_TOAST] ?: false,
    )

    private companion object {
        val SUBSCRIBE_BATTLE_LOG = booleanPreferencesKey("subscribe_battle_log")
        val NOTIFY_PRIVATE = booleanPreferencesKey("notify_private")
        val NOTIFY_PARTY = booleanPreferencesKey("notify_party")
        val LOCK_SCREEN_HIDE_CONTENT = booleanPreferencesKey("lock_screen_hide_content")
        val DEVELOPER_RAW_COMMANDS = booleanPreferencesKey("developer_raw_commands")
        val DEVELOPER_SPLIT_TOAST = booleanPreferencesKey("developer_split_toast")
    }
}
