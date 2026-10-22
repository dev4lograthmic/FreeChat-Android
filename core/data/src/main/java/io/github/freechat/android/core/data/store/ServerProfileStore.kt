package io.github.freechat.android.core.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    val activeProfileId: Flow<String?>
    val activeProfile: Flow<ServerProfile?>

    suspend fun get(profileId: String): ServerProfile?
    suspend fun save(profile: ServerProfile, makeActive: Boolean = false)
    suspend fun setActive(profileId: String)
    suspend fun delete(profileId: String)
}

class DataStoreServerProfileStore(
    private val dataStore: DataStore<Preferences>,
) : ServerProfileStore {
    override val profiles: Flow<List<ServerProfile>> = dataStore.data.map { preferences ->
        preferences[PROFILE_IDS]
            .orEmpty()
            .mapNotNull { profileId -> readProfile(preferences, profileId) }
            .sortedWith(
                compareBy<ServerProfile>(
                    { it.serverName ?: it.serverId },
                    { it.characterName.orEmpty() },
                    { it.characterId },
                ),
            )
    }

    override val activeProfileId: Flow<String?> = dataStore.data.map {
        it[ACTIVE_PROFILE_ID]
    }

    override val activeProfile: Flow<ServerProfile?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_PROFILE_ID]?.let { readProfile(preferences, it) }
    }

    override suspend fun get(profileId: String): ServerProfile? =
        readProfile(dataStore.data.first(), profileId)

    override suspend fun save(profile: ServerProfile, makeActive: Boolean) {
        val profileId = profile.profileId
        dataStore.edit { preferences ->
            preferences[PROFILE_IDS] = preferences[PROFILE_IDS].orEmpty() + profileId
            preferences[serverIdKey(profileId)] = profile.serverId
            preferences[hostKey(profileId)] = profile.host
            preferences[portKey(profileId)] = profile.port
            preferences[fingerprintKey(profileId)] = profile.fingerprintHex
            preferences[protocolKey(profileId)] = profile.protocolVersion
            preferences[characterIdKey(profileId)] = profile.characterId
            profile.serverName?.let { preferences[nameKey(profileId)] = it }
            profile.characterName?.let { preferences[characterNameKey(profileId)] = it }
            profile.worldName?.let { preferences[worldNameKey(profileId)] = it }
            if (makeActive) preferences[ACTIVE_PROFILE_ID] = profileId
        }
    }

    override suspend fun setActive(profileId: String) {
        dataStore.edit { it[ACTIVE_PROFILE_ID] = profileId }
    }

    override suspend fun delete(profileId: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_IDS] = preferences[PROFILE_IDS].orEmpty() - profileId
            preferences.remove(serverIdKey(profileId))
            preferences.remove(hostKey(profileId))
            preferences.remove(portKey(profileId))
            preferences.remove(fingerprintKey(profileId))
            preferences.remove(protocolKey(profileId))
            preferences.remove(nameKey(profileId))
            preferences.remove(characterIdKey(profileId))
            preferences.remove(characterNameKey(profileId))
            preferences.remove(worldNameKey(profileId))
            if (preferences[ACTIVE_PROFILE_ID] == profileId) {
                preferences.remove(ACTIVE_PROFILE_ID)
            }
        }
    }

    private fun readProfile(preferences: Preferences, profileId: String): ServerProfile? {
        val serverId = preferences[serverIdKey(profileId)] ?: return null
        val host = preferences[hostKey(profileId)] ?: return null
        val port = preferences[portKey(profileId)] ?: return null
        val fingerprint = preferences[fingerprintKey(profileId)] ?: return null
        return ServerProfile(
            serverId = serverId,
            host = host,
            port = port,
            fingerprintHex = fingerprint,
            serverName = preferences[nameKey(profileId)],
            protocolVersion = preferences[protocolKey(profileId)] ?: 1,
            characterId = preferences[characterIdKey(profileId)] ?: 0,
            characterName = preferences[characterNameKey(profileId)],
            worldName = preferences[worldNameKey(profileId)],
        )
    }

    private fun serverIdKey(profileId: String) =
        stringPreferencesKey("profile.$profileId.server_id")

    private fun hostKey(profileId: String) =
        stringPreferencesKey("profile.$profileId.host")

    private fun portKey(profileId: String) =
        intPreferencesKey("profile.$profileId.port")

    private fun fingerprintKey(profileId: String) =
        stringPreferencesKey("profile.$profileId.fingerprint")

    private fun protocolKey(profileId: String) =
        intPreferencesKey("profile.$profileId.protocol")

    private fun nameKey(profileId: String) =
        stringPreferencesKey("profile.$profileId.name")

    private fun characterIdKey(profileId: String) =
        longPreferencesKey("profile.$profileId.character_id")

    private fun characterNameKey(profileId: String) =
        stringPreferencesKey("profile.$profileId.character_name")

    private fun worldNameKey(profileId: String) =
        stringPreferencesKey("profile.$profileId.world_name")

    private companion object {
        val PROFILE_IDS = stringSetPreferencesKey("profile_ids")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }
}
