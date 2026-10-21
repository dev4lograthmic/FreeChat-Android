package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE serverId = :serverId ORDER BY startedAt DESC LIMIT 1")
    fun observeLatest(serverId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun get(sessionId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("UPDATE sessions SET lastSequence = :sequence WHERE sessionId = :sessionId")
    suspend fun updateLastSequence(sessionId: String, sequence: Long)

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE sessionId = :sessionId")
    suspend fun markEnded(sessionId: String, endedAt: Long)

    @Query("DELETE FROM sessions WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
