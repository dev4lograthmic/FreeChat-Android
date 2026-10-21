package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.MessageEntity
import io.github.freechat.android.core.data.db.entity.MessageFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(message: MessageFtsEntity): Long

    @Query("DELETE FROM messages_fts WHERE messageId = :messageId")
    suspend fun deleteFts(messageId: String)

    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    fun observeByConversation(conversationId: String, limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun get(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE serverId = :serverId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getByServer(serverId: String, limit: Int): List<MessageEntity>

    @Query(
        """
        SELECT m.* FROM messages m
        JOIN messages_fts f ON m.messageId = f.messageId
        WHERE messages_fts MATCH :match
          AND (:conversationId IS NULL OR m.conversationId = :conversationId)
        ORDER BY m.timestamp DESC
        LIMIT :limit
        """,
    )
    suspend fun search(match: String, conversationId: String?, limit: Int): List<MessageEntity>

    @Query(
        """
        DELETE FROM messages_fts
        WHERE messageId IN (SELECT messageId FROM messages WHERE timestamp < :cutoff)
        """,
    )
    suspend fun deleteFtsOlderThan(cutoff: Long)

    @Query("DELETE FROM messages WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query(
        """
        DELETE FROM messages_fts
        WHERE messageId IN (SELECT messageId FROM messages WHERE serverId = :serverId)
        """,
    )
    suspend fun deleteFtsByServer(serverId: String)

    @Query("DELETE FROM messages WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: String): Int

    @Query("DELETE FROM messages_fts")
    suspend fun deleteAllFts()

    @Query("DELETE FROM messages")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Long
}
