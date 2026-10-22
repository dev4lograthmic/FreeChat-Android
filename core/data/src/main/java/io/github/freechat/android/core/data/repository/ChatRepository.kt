package io.github.freechat.android.core.data.repository

import androidx.room.withTransaction
import io.github.freechat.android.core.data.db.FreeChatDatabase
import io.github.freechat.android.core.data.db.entity.MessageFtsEntity
import io.github.freechat.android.core.data.conversation.ConversationIds
import io.github.freechat.android.core.data.conversation.ConversationLabels
import io.github.freechat.android.core.data.export.ExportFormat
import io.github.freechat.android.core.data.export.MessageExporter
import io.github.freechat.android.core.data.mapper.toDomain
import io.github.freechat.android.core.data.mapper.toEntity
import io.github.freechat.android.core.data.search.FtsQuery
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ConversationType
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val database: FreeChatDatabase,
    private val exporter: MessageExporter = MessageExporter(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val draftDao = database.draftDao()

    fun observeConversations(
        profileId: String,
        type: ConversationType,
    ): Flow<List<ChatConversation>> =
        conversationDao.observeByType(profileId, type.name)
            .map { rows -> rows.map { it.toDomain() } }

    fun observeAllConversations(profileId: String): Flow<List<ChatConversation>> =
        conversationDao.observeAll(profileId).map { rows -> rows.map { it.toDomain() } }

    fun observeMessages(conversationId: String, limit: Int = 200): Flow<List<ChatMessage>> =
        messageDao.observeByConversation(conversationId, limit).map { rows ->
            rows.asReversed().map { it.toDomain() }
        }

    fun observeTotalUnread(profileId: String): Flow<Int> =
        conversationDao.observeTotalUnread(profileId)

    suspend fun getConversation(conversationId: String): ChatConversation? =
        conversationDao.get(conversationId)?.toDomain()

    suspend fun ensureConversation(conversation: ChatConversation) {
        if (conversationDao.get(conversation.id) == null) {
            conversationDao.upsert(conversation.toEntity())
        }
    }

    suspend fun ensureDefaultConversations(
        profileId: String,
        serverId: String,
        supportedChannels: List<String>,
    ) {
        val supported = supportedChannels
            .map { it.removePrefix("CHAT_CHANNEL_") }
            .toSet()
        // Linkshells are intentionally absent: they only get a conversation
        // once the game reports a name, so unnamed slots never clutter the list.
        conversationDao.reclassifyLinkshells(profileId)
        val publicChannels = ConversationLabels.publicChannels.keys
            .let { channels -> if (supported.isEmpty()) channels else channels.filter { it in supported } }

        publicChannels.forEach { channel ->
            ensureConversation(
                ChatConversation(
                    id = ConversationIds.publicChannel(profileId, channel),
                    profileId = profileId,
                    serverId = serverId,
                    type = ConversationType.PUBLIC_CHANNEL,
                    channel = channel,
                    peerName = null,
                    peerWorld = null,
                    displayName = ConversationLabels.publicLabel(channel),
                    lastMessageText = "",
                    lastMessageAt = 0,
                    unreadCount = 0,
                ),
            )
        }

        ConversationLabels.systemChannels.keys.forEach { key ->
            ensureConversation(
                ChatConversation(
                    id = ConversationIds.system(profileId, key),
                    profileId = profileId,
                    serverId = serverId,
                    type = ConversationType.SYSTEM,
                    channel = key,
                    peerName = null,
                    peerWorld = null,
                    displayName = ConversationLabels.systemLabel(key),
                    lastMessageText = "",
                    lastMessageAt = 0,
                    unreadCount = 0,
                ),
            )
        }
    }

    suspend fun hideConversation(conversationId: String) {
        conversationDao.hide(conversationId)
    }

    suspend fun unhideConversation(conversationId: String) {
        conversationDao.unhide(conversationId)
    }

    suspend fun recordIncoming(message: ChatMessage, conversation: ChatConversation): Boolean =
        database.withTransaction {
            val inserted = messageDao.insert(message.toEntity())
            if (inserted == -1L) return@withTransaction false

            messageDao.insertFts(
                MessageFtsEntity(
                    messageId = message.messageId,
                    text = message.text,
                    senderName = message.senderName,
                    channel = message.channel,
                ),
            )

            val preview = preview(message.text)
            val existing = conversationDao.get(conversation.id)
            if (existing == null) {
                conversationDao.upsert(
                    conversation.copy(
                        lastMessageText = preview,
                        lastMessageAt = message.timestamp,
                        unreadCount = if (message.outgoing) 0 else 1,
                    ).toEntity(),
                )
            } else {
                conversationDao.updateAfterMessage(
                    id = conversation.id,
                    type = conversation.type.name,
                    preview = preview,
                    at = message.timestamp,
                    increment = !message.outgoing,
                    unhide = !message.outgoing,
                    displayName = conversation.displayName,
                )
            }
            true
        }

    suspend fun markRead(conversationId: String) {
        conversationDao.markRead(conversationId)
    }

    suspend fun search(
        query: String,
        profileId: String,
        conversationId: String? = null,
        limit: Int = 100,
    ): List<ChatMessage> {
        val match = FtsQuery.build(query) ?: return emptyList()
        return messageDao.search(match, profileId, conversationId, limit)
            .map { it.toDomain() }
    }

    suspend fun export(
        profileId: String,
        format: ExportFormat,
        includeRaw: Boolean,
        output: OutputStream,
        limit: Int = 100_000,
    ) {
        val messages = messageDao.getByProfile(profileId, limit).map { it.toDomain() }
        exporter.write(messages, output, format, includeRaw)
    }

    suspend fun deleteOlderThan(cutoff: Long) {
        database.withTransaction {
            messageDao.deleteFtsOlderThan(cutoff)
            messageDao.deleteOlderThan(cutoff)
        }
    }

    suspend fun clearAll() {
        database.withTransaction {
            messageDao.deleteAllFts()
            messageDao.deleteAll()
            conversationDao.deleteAll()
            draftDao.deleteAll()
            database.sendJobDao().deleteAll()
            database.sessionDao().deleteAll()
        }
    }

    suspend fun saveDraft(conversationId: String, text: String) {
        if (text.isBlank()) {
            draftDao.delete(conversationId)
        } else {
            draftDao.upsert(
                io.github.freechat.android.core.data.db.entity.DraftEntity(
                    conversationId = conversationId,
                    text = text,
                    updatedAt = clock(),
                ),
            )
        }
    }

    fun observeDraft(conversationId: String): Flow<String?> =
        draftDao.observe(conversationId).map { it?.text }

    private fun preview(text: String): String =
        text.replace('\n', ' ').replace('\r', ' ').take(160)
}
