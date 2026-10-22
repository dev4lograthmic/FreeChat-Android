package io.github.freechat.android.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.freechat.android.core.data.db.entity.SendJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SendJobDao {
    @Query("SELECT * FROM send_jobs WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeByProfile(profileId: String): Flow<List<SendJobEntity>>

    @Query("SELECT * FROM send_jobs WHERE requestId = :requestId")
    suspend fun get(requestId: String): SendJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: SendJobEntity)

    @Query("DELETE FROM send_jobs WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    @Query("DELETE FROM send_jobs")
    suspend fun deleteAll()
}
