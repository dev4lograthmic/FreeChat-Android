package io.github.freechat.android.core.data.mapper

import io.github.freechat.android.core.data.db.entity.ConversationEntity
import io.github.freechat.android.core.data.db.entity.MessageEntity
import io.github.freechat.android.core.data.db.entity.ServerEntity
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.model.ServerProfile

fun ServerEntity.toDomain(): ServerProfile = ServerProfile(
    serverId = serverId,
    host = host,
    port = port,
    fingerprintHex = fingerprintHex,
    serverName = serverName,
    protocolVersion = protocolVersion,
    characterId = characterId,
    characterName = characterName,
    worldName = worldName,
)

fun ServerProfile.toEntity(
    createdAt: Long,
    lastConnectedAt: Long?,
): ServerEntity = ServerEntity(
    profileId = profileId,
    serverId = serverId,
    host = host,
    port = port,
    fingerprintHex = fingerprintHex,
    serverName = serverName,
    protocolVersion = protocolVersion,
    characterId = characterId,
    characterName = characterName,
    worldName = worldName,
    createdAt = createdAt,
    lastConnectedAt = lastConnectedAt,
)

fun ConversationEntity.toDomain(): ChatConversation = ChatConversation(
    id = id,
    profileId = profileId,
    serverId = serverId,
    type = runCatching { ConversationType.valueOf(type) }
        .getOrDefault(ConversationType.PUBLIC_CHANNEL),
    channel = channel,
    peerName = peerName,
    peerWorld = peerWorld,
    displayName = displayName,
    lastMessageText = lastMessageText,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
)

fun ChatConversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    profileId = profileId,
    serverId = serverId,
    type = type.name,
    channel = channel,
    peerName = peerName,
    peerWorld = peerWorld,
    displayName = displayName,
    lastMessageText = lastMessageText,
    lastMessageAt = lastMessageAt,
    unreadCount = unreadCount,
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    messageId = messageId,
    conversationId = conversationId,
    profileId = profileId,
    serverId = serverId,
    sessionId = sessionId,
    sequence = sequence,
    timestamp = timestamp,
    channel = channel,
    logKind = logKind,
    senderName = senderName,
    senderWorld = senderWorld,
    text = text,
    outgoing = outgoing,
    dedupeKey = dedupeKey,
    rawSender = rawSender,
    rawMessage = rawMessage,
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
    messageId = messageId,
    conversationId = conversationId,
    profileId = profileId,
    serverId = serverId,
    sessionId = sessionId,
    sequence = sequence,
    timestamp = timestamp,
    channel = channel,
    logKind = logKind,
    senderName = senderName,
    senderWorld = senderWorld,
    text = text,
    outgoing = outgoing,
    dedupeKey = dedupeKey,
    rawSender = rawSender,
    rawMessage = rawMessage,
)
