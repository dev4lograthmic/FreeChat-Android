package io.github.freechat.android.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "send_jobs",
    indices = [
        Index(value = ["profileId", "updatedAt"]),
        Index(value = ["serverId"]),
    ],
)
data class SendJobEntity(
    @PrimaryKey val requestId: String,
    val profileId: String,
    val serverId: String,
    val conversationId: String,
    val channel: String,
    val targetName: String?,
    val targetWorld: String?,
    val text: String,
    val status: String,
    val partIndex: Int,
    val partCount: Int,
    val errorCode: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
