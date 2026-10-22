package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query(
        "SELECT * FROM conversations " +
            "WHERE profileId = :profileId AND type = :type AND hidden = 0 " +
            "ORDER BY lastMessageAt DESC",
    )
    fun observeByType(profileId: String, type: String): Flow<List<ConversationEntity>>

    @Query(
        "SELECT * FROM conversations " +
            "WHERE profileId = :profileId AND hidden = 0 " +
            "ORDER BY lastMessageAt DESC",
    )
    fun observeAll(profileId: String): Flow<List<ConversationEntity>>

    @Query(
        "SELECT * FROM conversations WHERE profileId = :profileId " +
            "AND (channel LIKE 'LINKSHELL_%' OR channel LIKE 'CROSS_LINKSHELL_%')",
    )
    fun observeLinkshellConversations(profileId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query(
        """
        UPDATE conversations
        SET type = :type,
            lastMessageText = :preview,
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
        type: String,
        preview: String,
        at: Long,
        increment: Boolean,
        unhide: Boolean,
        displayName: String?,
    )

    @Query(
        "UPDATE conversations SET type = 'LINKSHELL' " +
            "WHERE profileId = :profileId AND type = 'PUBLIC_CHANNEL' " +
            "AND (channel LIKE 'LINKSHELL_%' OR channel LIKE 'CROSS_LINKSHELL_%')",
    )
    suspend fun reclassifyLinkshells(profileId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE conversations SET hidden = 1, unreadCount = 0 WHERE id = :id")
    suspend fun hide(id: String)

    @Query("UPDATE conversations SET hidden = 0 WHERE id = :id")
    suspend fun unhide(id: String)

    @Query(
        "SELECT COALESCE(SUM(unreadCount), 0) FROM conversations " +
            "WHERE profileId = :profileId",
    )
    fun observeTotalUnread(profileId: String): Flow<Int>

    @Query("DELETE FROM conversations WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
