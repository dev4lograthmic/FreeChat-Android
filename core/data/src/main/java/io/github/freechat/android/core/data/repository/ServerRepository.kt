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
    val activeProfileId: Flow<String?> = profileStore.activeProfileId

    suspend fun get(profileId: String): ServerProfile? = profileStore.get(profileId)

    suspend fun save(profile: ServerProfile, makeActive: Boolean = false) {
        profileStore.save(profile, makeActive)
        val existing = database.serverDao().get(profile.profileId)
        database.serverDao().upsert(
            profile.toEntity(
                createdAt = existing?.createdAt ?: clock(),
                lastConnectedAt = existing?.lastConnectedAt,
            ),
        )
    }

    suspend fun setActive(profileId: String) {
        profileStore.setActive(profileId)
    }

    suspend fun markConnected(profileId: String) {
        database.serverDao().markConnected(profileId, clock())
    }

    suspend fun delete(profileId: String) {
        database.withTransaction {
            database.draftDao().deleteByProfile(profileId)
            database.sendJobDao().deleteByProfile(profileId)
            database.sessionDao().deleteByProfile(profileId)
            database.messageDao().deleteFtsByProfile(profileId)
            database.messageDao().deleteByProfile(profileId)
            database.conversationDao().deleteByProfile(profileId)
            database.serverDao().delete(profileId)
        }
        profileStore.delete(profileId)
    }
}
