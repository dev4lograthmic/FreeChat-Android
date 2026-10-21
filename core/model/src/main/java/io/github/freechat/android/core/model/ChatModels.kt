package io.github.freechat.android.core.model

enum class ConversationType {
    SYSTEM,
    PUBLIC_CHANNEL,
    PRIVATE,
}

data class ChatConversation(
    val id: String,
    val serverId: String,
    val type: ConversationType,
    val channel: String?,
    val peerName: String?,
    val peerWorld: String?,
    val displayName: String,
    val lastMessageText: String,
    val lastMessageAt: Long,
    val unreadCount: Int,
)

data class ChatMessage(
    val messageId: String,
    val conversationId: String,
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
    val rawSender: ByteArray? = null,
    val rawMessage: ByteArray? = null,
)

data class SendJob(
    val requestId: String,
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
