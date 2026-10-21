package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.ChatChannel
import build.buf.gen.freechat.v1.ChatEvent
import io.github.freechat.android.core.data.conversation.ConversationIds
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType

data class MappedChatEvent(
    val message: ChatMessage,
    val conversation: ChatConversation,
)

object ChatEventMapper {
    fun map(
        event: ChatEvent,
        serverId: String,
        sessionId: String,
        now: Long = System.currentTimeMillis(),
    ): MappedChatEvent {
        val channel = event.channel.toChannelName()
        val timestamp = event.timestamp.toMillis(now)
        val sender = event.sender
        val senderName = sender.name.ifBlank { "System" }
        val senderWorld = sender.worldName.takeIf { it.isNotBlank() }
        val text = event.text
        val type = event.channel.toConversationType()

        val peer = if (event.hasTellPeer() && event.tellPeer.name.isNotBlank()) {
            event.tellPeer
        } else {
            sender
        }
        val peerName = peer.name
        val peerWorld = peer.worldName.takeIf { it.isNotBlank() }

        val conversationId = when (type) {
            ConversationType.PRIVATE ->
                ConversationIds.privateChat(serverId, peerName, peerWorld)
            ConversationType.SYSTEM ->
                ConversationIds.system(serverId, channel)
            ConversationType.PUBLIC_CHANNEL ->
                ConversationIds.publicChannel(serverId, channel)
        }

        val displayName = when (type) {
            ConversationType.PRIVATE -> if (peerWorld != null) "$peerName@$peerWorld" else peerName
            ConversationType.SYSTEM -> channel
            ConversationType.PUBLIC_CHANNEL -> event.channelName
                .takeIf { it.isNotBlank() }
                ?: channel
        }

        val messageId = event.messageId.ifBlank { "$serverId:$sessionId:${event.sequence}" }
        val message = ChatMessage(
            messageId = messageId,
            conversationId = conversationId,
            serverId = serverId,
            sessionId = sessionId,
            sequence = event.sequence,
            timestamp = timestamp,
            channel = channel,
            logKind = event.logKind.toLong(),
            senderName = senderName,
            senderWorld = senderWorld,
            text = text,
            outgoing = event.outgoing,
            dedupeKey = "$timestamp:${event.logKind}:${sender.contentId}:$text",
            rawSender = event.rawSender.takeIf { it.size() > 0 }?.toByteArray(),
            rawMessage = event.rawMessage.takeIf { it.size() > 0 }?.toByteArray(),
        )
        val conversation = ChatConversation(
            id = conversationId,
            serverId = serverId,
            type = type,
            channel = channel,
            peerName = peerName.takeIf { it.isNotBlank() },
            peerWorld = peerWorld,
            displayName = displayName,
            lastMessageText = "",
            lastMessageAt = timestamp,
            unreadCount = 0,
        )
        return MappedChatEvent(message, conversation)
    }

    private fun ChatChannel.toChannelName(): String = name.removePrefix("CHAT_CHANNEL_")

    private fun ChatChannel.toConversationType(): ConversationType = when (this) {
        ChatChannel.CHAT_CHANNEL_TELL -> ConversationType.PRIVATE
        ChatChannel.CHAT_CHANNEL_SYSTEM,
        ChatChannel.CHAT_CHANNEL_ERROR,
        ChatChannel.CHAT_CHANNEL_NPC,
        ChatChannel.CHAT_CHANNEL_BATTLE,
        ChatChannel.CHAT_CHANNEL_LOOT,
        ChatChannel.CHAT_CHANNEL_PROGRESS,
        ChatChannel.CHAT_CHANNEL_ECHO,
        -> ConversationType.SYSTEM

        else -> ConversationType.PUBLIC_CHANNEL
    }

    private fun com.google.protobuf.Timestamp.toMillis(now: Long): Long {
        if (seconds == 0L && nanos == 0) return now
        return seconds * 1_000 + nanos / 1_000_000
    }

}
