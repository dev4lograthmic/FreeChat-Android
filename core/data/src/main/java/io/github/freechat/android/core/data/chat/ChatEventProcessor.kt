package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.ChatEvent
import io.github.freechat.android.core.data.repository.ChatRepository

class ChatEventProcessor(
    private val repository: ChatRepository,
    private val serverId: String,
    private val sessionId: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun process(event: ChatEvent) {
        val mapped = ChatEventMapper.map(
            event = event,
            serverId = serverId,
            sessionId = sessionId,
            now = now(),
        )
        repository.recordIncoming(mapped.message, mapped.conversation)
    }
}
