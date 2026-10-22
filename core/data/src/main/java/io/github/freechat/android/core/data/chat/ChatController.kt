package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.PairingRequired
import build.buf.gen.freechat.v1.SendResult
import build.buf.gen.freechat.v1.SendStatus
import build.buf.gen.freechat.v1.Welcome
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ChatMessage
import io.github.freechat.android.core.model.ChatSourceOption
import io.github.freechat.android.core.model.MessageContentSource
import io.github.freechat.android.core.model.FriendListSnapshot
import io.github.freechat.android.core.model.SendOutcome
import io.github.freechat.android.core.model.SendOutcomeStatus
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.model.SessionInfo
import io.github.freechat.android.core.network.client.ChatSession
import io.github.freechat.android.core.network.client.FreeChatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
        sources: Set<ChatSourceOption> = setOf(ChatSourceOption.POST_HANDLE),
        contentSource: MessageContentSource = MessageContentSource.TEXT,
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
            contentSource = contentSource,
        )
        val eventFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 128)
        val processorJob: Job = scope.launch {
            session.events.collect { event ->
                processor.process(event)?.let { eventFlow.emit(it) }
            }
        }

        try {
            session.subscribe(
                fromSequence = fromSequence,
                includeBattle = includeBattle,
                sources = sources,
                includeOriginal = contentSource.needsOriginal,
            )
            repository.ensureDefaultConversations(
                serverId = profile.serverId,
                supportedChannels = connection.welcome.server.capabilities.supportedChannelsList
                    .map { it.name },
            )
        } catch (error: Throwable) {
            processorJob.cancel()
            scope.cancel()
            session.close()
            throw error
        }

        return ActiveChat(
            profile = profile,
            sessionInfo = connection.welcome.toSessionInfo(profile),
            events = eventFlow.asSharedFlow(),
            closed = session.closed,
            session = session,
            scope = scope,
            processorJob = processorJob,
        )
    }
}

class ActiveChat internal constructor(
    val profile: ServerProfile,
    val sessionInfo: SessionInfo,
    val events: SharedFlow<ChatMessage>,
    val closed: Deferred<Unit>,
    private val session: ChatSession,
    private val scope: CoroutineScope,
    private val processorJob: Job,
) {
    suspend fun send(
        conversation: ChatConversation,
        text: String,
        raw: Boolean = false,
    ): SendOutcome = session.send(conversation, text, raw).toOutcome()

    suspend fun loadFriends(pageSize: Int = 32): FriendListSnapshot =
        session.loadFriends(pageSize)

    suspend fun close() {
        processorJob.cancel()
        scope.cancel()
        session.close()
    }
}

private fun Welcome.toSessionInfo(profile: ServerProfile): SessionInfo = SessionInfo(
    serverId = server.serverId,
    serverName = server.serverName.ifBlank { profile.serverName.orEmpty() },
    characterName = server.character.name,
    worldName = server.character.worldName,
    sessionId = sessionId,
    enabledFeatures = enabledFeaturesList,
)

private fun SendResult.toOutcome(): SendOutcome {
    val outcomeStatus = when (status) {
        SendStatus.SEND_STATUS_SUBMITTED -> SendOutcomeStatus.SUBMITTED
        SendStatus.SEND_STATUS_FAILED -> SendOutcomeStatus.FAILED
        SendStatus.SEND_STATUS_BLOCKED -> SendOutcomeStatus.BLOCKED
        SendStatus.SEND_STATUS_RATE_LIMITED -> SendOutcomeStatus.RATE_LIMITED
        SendStatus.SEND_STATUS_QUEUE_FULL -> SendOutcomeStatus.QUEUE_FULL
        SendStatus.SEND_STATUS_CANCELLED -> SendOutcomeStatus.CANCELLED
        else -> SendOutcomeStatus.UNKNOWN
    }
    return SendOutcome(
        status = outcomeStatus,
        errorCode = error.takeIf { it.name != "ERROR_CODE_UNSPECIFIED" }?.name,
        message = message,
        partIndex = partIndex,
        partCount = partCount,
        originMessageId = originMessageId.takeIf { it.isNotBlank() },
    )
}
