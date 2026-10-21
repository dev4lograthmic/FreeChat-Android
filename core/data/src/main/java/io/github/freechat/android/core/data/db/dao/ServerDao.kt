package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY lastConnectedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE serverId = :serverId")
    suspend fun get(serverId: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: ServerEntity)

    @Query("UPDATE servers SET lastConnectedAt = :at WHERE serverId = :serverId")
    suspend fun markConnected(serverId: String, at: Long)

    @Query("DELETE FROM servers WHERE serverId = :serverId")
    suspend fun delete(serverId: String)
}
