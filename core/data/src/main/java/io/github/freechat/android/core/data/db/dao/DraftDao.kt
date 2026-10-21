package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE conversationId = :conversationId")
    fun observe(conversationId: String): Flow<DraftEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE conversationId = :conversationId")
    suspend fun delete(conversationId: String)

    @Query("DELETE FROM drafts WHERE conversationId IN (SELECT id FROM conversations WHERE serverId = :serverId)")
    suspend fun deleteByServer(serverId: String)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()
}
