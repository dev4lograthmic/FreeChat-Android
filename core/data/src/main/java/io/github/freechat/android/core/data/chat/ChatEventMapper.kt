package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.ChatChannel
import build.buf.gen.freechat.v1.ChatEvent
import io.github.freechat.android.core.data.conversation.ConversationIds
import io.github.freechat.android.core.data.conversation.ConversationLabels
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.model.MessageContentSource
import java.util.Base64

data class MappedChatEvent(
    val message: ChatMessage,
    val conversation: ChatConversation,
)

object ChatEventMapper {
    fun map(
        event: ChatEvent,
        profileId: String,
        serverId: String,
        sessionId: String,
        now: Long = System.currentTimeMillis(),
        contentSource: MessageContentSource = MessageContentSource.TEXT,
    ): MappedChatEvent {
        val channel = event.channel.toChannelName()
        val timestamp = event.timestamp.toMillis(now)
        val sender = event.sender
        val senderName = sender.name.ifBlank { "System" }
        val senderWorld = sender.worldName.takeIf { it.isNotBlank() }
        val text = when (contentSource) {
            MessageContentSource.TEXT -> event.text
            MessageContentSource.ORIGINAL_TEXT ->
                if (event.hasOriginalText()) event.originalText else event.text

            MessageContentSource.RAW_MESSAGE ->
                rawText(event.rawMessage) ?: event.text

            MessageContentSource.ORIGINAL_RAW_MESSAGE ->
                rawText(
                    if (event.hasOriginalRawMessage()) event.originalRawMessage else event.rawMessage,
                ) ?: event.text
        }
        val type = event.channel.toConversationType()

        val peer = if (event.hasTellPeer() && event.tellPeer.name.isNotBlank()) {
            event.tellPeer
        } else {
            sender
        }
        val peerName = peer.name
        val peerWorld = peer.worldName.takeIf { it.isNotBlank() }

        val systemKey = event.channel.toSystemKey()
        val conversationChannel = if (type == ConversationType.SYSTEM) {
            systemKey ?: channel
        } else {
            channel
        }
        val conversationId = when (type) {
            ConversationType.PRIVATE ->
                ConversationIds.privateChat(profileId, peerName, peerWorld)
            ConversationType.SYSTEM ->
                ConversationIds.system(profileId, conversationChannel)
            ConversationType.PUBLIC_CHANNEL,
            ConversationType.LINKSHELL,
            ->
                ConversationIds.publicChannel(profileId, channel)
        }

        val displayName = when (type) {
            ConversationType.PRIVATE -> if (peerWorld != null) "$peerName@$peerWorld" else peerName
            ConversationType.SYSTEM -> ConversationLabels.systemLabel(conversationChannel)
            ConversationType.PUBLIC_CHANNEL -> event.channelName
                .takeIf { it.isNotBlank() }
                ?: ConversationLabels.publicLabel(channel)
            // Linkshells are only shown once the game reports their name.
            ConversationType.LINKSHELL -> event.channelName.trim()
        }

        val serverMessageId = event.messageId.ifBlank {
            "$serverId:$sessionId:${event.sequence}"
        }
        val messageId = "$profileId:$serverMessageId"
        val message = ChatMessage(
            messageId = messageId,
            conversationId = conversationId,
            profileId = profileId,
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
            dedupeKey = "$profileId:$timestamp:${event.logKind}:${sender.contentId}:$text",
            rawSender = event.rawSender.takeIf { it.size() > 0 }?.toByteArray(),
            rawMessage = event.rawMessage.takeIf { it.size() > 0 }?.toByteArray(),
        )
        val conversation = ChatConversation(
            id = conversationId,
            profileId = profileId,
            serverId = serverId,
            type = type,
            channel = conversationChannel,
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

    private fun ChatChannel.toSystemKey(): String? =
        ConversationLabels.systemKey(toChannelName())

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

        else -> if (ConversationLabels.isLinkshell(toChannelName())) {
            ConversationType.LINKSHELL
        } else {
            ConversationType.PUBLIC_CHANNEL
        }
    }

    private fun rawText(value: com.google.protobuf.ByteString): String? {
        if (value.isEmpty) return null
        return "[raw] " + Base64.getEncoder().encodeToString(value.toByteArray())
    }

    private fun com.google.protobuf.Timestamp.toMillis(now: Long): Long {
        if (seconds == 0L && nanos == 0) return now
        return seconds * 1_000 + nanos / 1_000_000
    }

}
