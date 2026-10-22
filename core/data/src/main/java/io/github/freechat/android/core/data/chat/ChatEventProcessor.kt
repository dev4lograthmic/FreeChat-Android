package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.ChatEvent
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.MessageContentSource

class ChatEventProcessor(
    private val repository: ChatRepository,
    private val serverId: String,
    private val sessionId: String,
    private val contentSource: MessageContentSource = MessageContentSource.TEXT,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun process(event: ChatEvent): ChatMessage? {
        val mapped = ChatEventMapper.map(
            event = event,
            serverId = serverId,
            sessionId = sessionId,
            now = now(),
            contentSource = contentSource,
        )
        return if (repository.recordIncoming(mapped.message, mapped.conversation)) {
            mapped.message
        } else {
            null
        }
    }
}
