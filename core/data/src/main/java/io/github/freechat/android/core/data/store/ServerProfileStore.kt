package io.github.freechat.android.core.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.freechat.android.core.model.ServerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.freeChatDataStore: DataStore<Preferences> by preferencesDataStore(name = "freechat")

interface ServerProfileStore {
    val profiles: Flow<List<ServerProfile>>
    val activeServerId: Flow<String?>
    val activeProfile: Flow<ServerProfile?>

    suspend fun get(serverId: String): ServerProfile?
    suspend fun save(profile: ServerProfile, makeActive: Boolean = false)
    suspend fun setActive(serverId: String)
    suspend fun delete(serverId: String)
}

class DataStoreServerProfileStore(
    private val dataStore: DataStore<Preferences>,
) : ServerProfileStore {
    override val profiles: Flow<List<ServerProfile>> = dataStore.data.map { preferences ->
        preferences[SERVER_IDS]
            .orEmpty()
            .mapNotNull { serverId -> readProfile(preferences, serverId) }
            .sortedByDescending { it.serverId }
    }

    override val activeServerId: Flow<String?> = dataStore.data.map { it[ACTIVE_SERVER_ID] }

    override val activeProfile: Flow<ServerProfile?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_SERVER_ID]?.let { readProfile(preferences, it) }
    }

    override suspend fun get(serverId: String): ServerProfile? =
        readProfile(dataStore.data.first(), serverId)

    override suspend fun save(profile: ServerProfile, makeActive: Boolean) {
        dataStore.edit { preferences ->
            val ids = preferences[SERVER_IDS].orEmpty() + profile.serverId
            preferences[SERVER_IDS] = ids
            preferences[hostKey(profile.serverId)] = profile.host
            preferences[portKey(profile.serverId)] = profile.port
            preferences[fingerprintKey(profile.serverId)] = profile.fingerprintHex
            preferences[protocolKey(profile.serverId)] = profile.protocolVersion
            profile.serverName?.let { preferences[nameKey(profile.serverId)] = it }
            profile.characterName?.let { preferences[characterKey(profile.serverId)] = it }
            profile.worldName?.let { preferences[worldKey(profile.serverId)] = it }
            if (makeActive) preferences[ACTIVE_SERVER_ID] = profile.serverId
        }
    }

    override suspend fun setActive(serverId: String) {
        dataStore.edit { it[ACTIVE_SERVER_ID] = serverId }
    }

    override suspend fun delete(serverId: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_IDS] = preferences[SERVER_IDS].orEmpty() - serverId
            preferences.remove(hostKey(serverId))
            preferences.remove(portKey(serverId))
            preferences.remove(fingerprintKey(serverId))
            preferences.remove(protocolKey(serverId))
            preferences.remove(nameKey(serverId))
            preferences.remove(characterKey(serverId))
            preferences.remove(worldKey(serverId))
            if (preferences[ACTIVE_SERVER_ID] == serverId) {
                preferences.remove(ACTIVE_SERVER_ID)
            }
        }
    }

    private fun readProfile(preferences: Preferences, serverId: String): ServerProfile? {
        val host = preferences[hostKey(serverId)] ?: return null
        val port = preferences[portKey(serverId)] ?: return null
        val fingerprint = preferences[fingerprintKey(serverId)] ?: return null
        return ServerProfile(
            serverId = serverId,
            host = host,
            port = port,
            fingerprintHex = fingerprint,
            serverName = preferences[nameKey(serverId)],
            protocolVersion = preferences[protocolKey(serverId)] ?: 1,
            characterName = preferences[characterKey(serverId)],
            worldName = preferences[worldKey(serverId)],
        )
    }

    private fun hostKey(serverId: String) = stringPreferencesKey("server.$serverId.host")
    private fun portKey(serverId: String) = intPreferencesKey("server.$serverId.port")
    private fun fingerprintKey(serverId: String) = stringPreferencesKey("server.$serverId.fingerprint")
    private fun protocolKey(serverId: String) = intPreferencesKey("server.$serverId.protocol")
    private fun nameKey(serverId: String) = stringPreferencesKey("server.$serverId.name")
    private fun characterKey(serverId: String) = stringPreferencesKey("server.$serverId.character")
    private fun worldKey(serverId: String) = stringPreferencesKey("server.$serverId.world")

    private companion object {
        val SERVER_IDS = stringSetPreferencesKey("server_ids")
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
    }
}
