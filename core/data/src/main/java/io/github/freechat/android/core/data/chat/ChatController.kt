package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.PairingRequired
import build.buf.gen.freechat.v1.SendResult
import build.buf.gen.freechat.v1.Subscribed
import build.buf.gen.freechat.v1.Welcome
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.FriendListSnapshot
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.client.ChatSession
import io.github.freechat.android.core.network.client.FreeChatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatController(
    private val client: FreeChatClient,
    private val repository: ChatRepository,
) {
    suspend fun connect(
        profile: ServerProfile,
        fromSequence: Long = 0,
        includeBattle: Boolean = false,
        pairingCodeProvider: suspend (PairingRequired) -> String? = { null },
        onPairingRequired: suspend (PairingRequired) -> Unit = {},
    ): ActiveChat {
        val connection = client.connect(
            profile = profile,
            pairingCodeProvider = pairingCodeProvider,
            onPairingRequired = onPairingRequired,
        )
        val session = ChatSession(connection)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val processor = ChatEventProcessor(
            repository = repository,
            serverId = profile.serverId,
            sessionId = connection.welcome.sessionId,
        )
        val processorJob: Job = scope.launch {
            session.events.collect { event -> processor.process(event) }
        }

        val subscribed = try {
            session.subscribe(fromSequence = fromSequence, includeBattle = includeBattle)
        } catch (error: Throwable) {
            processorJob.cancel()
            scope.cancel()
            session.close()
            throw error
        }

        return ActiveChat(
            profile = profile,
            welcome = connection.welcome,
            subscribed = subscribed,
            session = session,
            scope = scope,
            processorJob = processorJob,
        )
    }
}

class ActiveChat internal constructor(
    val profile: ServerProfile,
    val welcome: Welcome,
    val subscribed: Subscribed,
    private val session: ChatSession,
    private val scope: CoroutineScope,
    private val processorJob: Job,
) {
    suspend fun send(
        conversation: ChatConversation,
        text: String,
        raw: Boolean = false,
    ): SendResult = session.send(conversation, text, raw)

    suspend fun loadFriends(pageSize: Int = 32): FriendListSnapshot =
        session.loadFriends(pageSize)

    suspend fun close() {
        processorJob.cancel()
        scope.cancel()
        session.close()
    }
}
