package io.github.freechat.android.core.data.repository

import androidx.room.withTransaction
import io.github.freechat.android.core.data.db.FreeChatDatabase
import io.github.freechat.android.core.data.mapper.toEntity
import io.github.freechat.android.core.data.store.ServerProfileStore
import io.github.freechat.android.core.model.ServerProfile
import kotlinx.coroutines.flow.Flow

class ServerRepository(
    private val database: FreeChatDatabase,
    private val profileStore: ServerProfileStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val profiles: Flow<List<ServerProfile>> = profileStore.profiles
    val activeProfile: Flow<ServerProfile?> = profileStore.activeProfile
    val activeServerId: Flow<String?> = profileStore.activeServerId

    suspend fun get(serverId: String): ServerProfile? = profileStore.get(serverId)

    suspend fun save(profile: ServerProfile, makeActive: Boolean = false) {
        profileStore.save(profile, makeActive)
        val existing = database.serverDao().get(profile.serverId)
        database.serverDao().upsert(
            profile.toEntity(
                createdAt = existing?.createdAt ?: clock(),
                lastConnectedAt = existing?.lastConnectedAt,
            ),
        )
    }

    suspend fun setActive(serverId: String) {
        profileStore.setActive(serverId)
    }

    suspend fun markConnected(serverId: String) {
        database.serverDao().markConnected(serverId, clock())
    }

    suspend fun delete(serverId: String) {
        database.withTransaction {
            database.draftDao().deleteByServer(serverId)
            database.sendJobDao().deleteByServer(serverId)
            database.sessionDao().deleteByServer(serverId)
            database.messageDao().deleteFtsByServer(serverId)
            database.messageDao().deleteByServer(serverId)
            database.conversationDao().deleteByServer(serverId)
            database.serverDao().delete(serverId)
        }
        profileStore.delete(serverId)
    }
}
