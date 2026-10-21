package io.github.freechat.android.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["serverId"])],
)
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val serverId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val lastSequence: Long,
)
