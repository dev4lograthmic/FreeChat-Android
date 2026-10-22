package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE type = :type AND hidden = 0 ORDER BY lastMessageAt DESC")
    fun observeByType(type: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE hidden = 0 ORDER BY lastMessageAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query(
        """
        UPDATE conversations
        SET lastMessageText = :preview,
            lastMessageAt = :at,
            displayName = CASE
                WHEN :displayName IS NULL OR :displayName = '' THEN displayName
                ELSE :displayName
            END,
            hidden = CASE WHEN :unhide THEN 0 ELSE hidden END,
            unreadCount = unreadCount + CASE WHEN :increment THEN 1 ELSE 0 END
        WHERE id = :id
        """,
    )
    suspend fun updateAfterMessage(
        id: String,
        preview: String,
        at: Long,
        increment: Boolean,
        unhide: Boolean,
        displayName: String?,
    )

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE conversations SET hidden = 1, unreadCount = 0 WHERE id = :id")
    suspend fun hide(id: String)

    @Query("UPDATE conversations SET hidden = 0 WHERE id = :id")
    suspend fun unhide(id: String)

    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM conversations")
    fun observeTotalUnread(): Flow<Int>

    @Query("DELETE FROM conversations WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
