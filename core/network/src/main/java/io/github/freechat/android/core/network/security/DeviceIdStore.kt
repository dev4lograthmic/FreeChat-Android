package io.github.freechat.android.core.network.security

import android.content.Context
import java.util.UUID

interface ServerDeviceIdStore {
    fun getOrCreate(serverId: String): String
    fun save(serverId: String, deviceId: String)
}

/**
 * Server-assigned device ids are stored per server, because a single install
 * may be paired with multiple Dalamud servers.
 */
class SharedPreferencesServerDeviceIdStore(context: Context) : ServerDeviceIdStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getOrCreate(serverId: String): String {
        val key = key(serverId)
        val existing = preferences.getString(key, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        preferences.edit().putString(key, generated).apply()
        return generated
    }

    override fun save(serverId: String, deviceId: String) {
        preferences.edit().putString(key(serverId), deviceId).apply()
    }

    private fun key(serverId: String): String = "device_id_$serverId"

    private companion object {
        const val PREFERENCES_NAME = "freechat_identity"
    }
}
