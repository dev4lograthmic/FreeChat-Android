package io.github.freechat.android.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["profileId", "conversationId", "timestamp"]),
        Index(value = ["profileId", "serverId", "timestamp"]),
        Index(value = ["dedupeKey"]),
    ],
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val profileId: String,
    val serverId: String,
    val sessionId: String,
    val sequence: Long,
    val timestamp: Long,
    val channel: String,
    val logKind: Long,
    val senderName: String,
    val senderWorld: String?,
    val text: String,
    val outgoing: Boolean,
    val dedupeKey: String?,
    val rawSender: ByteArray?,
    val rawMessage: ByteArray?,
)
