package io.github.freechat.android.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["serverId", "type", "lastMessageAt"]),
    ],
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val serverId: String,
    val type: String,
    val channel: String?,
    val peerName: String?,
    val peerWorld: String?,
    val displayName: String,
    val lastMessageText: String,
    val lastMessageAt: Long,
    val unreadCount: Int,
    val hidden: Boolean = false,
)
