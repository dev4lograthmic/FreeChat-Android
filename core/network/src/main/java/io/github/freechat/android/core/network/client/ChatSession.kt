package io.github.freechat.android.core.network.client

import build.buf.gen.freechat.v1.CharacterRef
import build.buf.gen.freechat.v1.ChatChannel
import build.buf.gen.freechat.v1.ChatEvent
import build.buf.gen.freechat.v1.ChatFilter
import build.buf.gen.freechat.v1.ChatSend
import build.buf.gen.freechat.v1.ChatSource
import build.buf.gen.freechat.v1.ContentMode
import build.buf.gen.freechat.v1.Envelope
import build.buf.gen.freechat.v1.ErrorCode
import build.buf.gen.freechat.v1.PlayerInfo
import build.buf.gen.freechat.v1.PlayerListRequest
import build.buf.gen.freechat.v1.PlayerListSnapshot
import build.buf.gen.freechat.v1.PlayerListStatus
import build.buf.gen.freechat.v1.PlayerListType
import build.buf.gen.freechat.v1.SendRequest
import build.buf.gen.freechat.v1.SendResult
import build.buf.gen.freechat.v1.SendStatus
import build.buf.gen.freechat.v1.Subscribe
import build.buf.gen.freechat.v1.Subscribed
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.FriendInfo
import io.github.freechat.android.core.model.FriendListSnapshot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

sealed class SendException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Rejected(val code: ErrorCode, val detail: String) : SendException("Send rejected: $detail")
    class Timeout : SendException("Send timed out")
    class ConnectionClosed : SendException("Connection closed")
}

sealed class SubscriptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class Timeout : SubscriptionException("Subscription timed out")
    class Rejected(val code: ErrorCode, val detail: String) :
        SubscriptionException("Subscription rejected: $detail")
}

sealed class FriendListException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class Rejected(val status: PlayerListStatus, val detail: String) :
        FriendListException("Friend list rejected: $detail")

    class Timeout : FriendListException("Friend list timed out")
    class ConnectionClosed : FriendListException("Connection closed")
}

/**
 * Owns the single incoming-message collector after authentication. Routes
 * chat events, send results and friend snapshots to their consumers.
 */
class ChatSession(
    private val connection: FreeChatConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventChannel = Channel<ChatEvent>(Channel.UNLIMITED)
    private val sendMutex = Mutex()
    private val pendingSends = ConcurrentHashMap<String, CompletableDeferred<SendResult>>()
    private val closedDeferred = CompletableDeferred<Unit>()
    private val pendingFriends = ConcurrentHashMap<String, FriendAccumulator>()

    @Volatile
    private var subscribeDeferred: CompletableDeferred<Subscribed>? = null

    val events: Flow<ChatEvent> = eventChannel.receiveAsFlow()
    val closed: Deferred<Unit> = closedDeferred

    init {
        scope.launch {
            try {
                connection.incoming.collect { envelope -> route(envelope) }
            } finally {
                closedDeferred.complete(Unit)
                eventChannel.close()
            }
        }
    }

    suspend fun subscribe(
        fromSequence: Long = 0,
        includeBattle: Boolean = false,
    ): Subscribed {
        val deferred = CompletableDeferred<Subscribed>()
        subscribeDeferred = deferred
        val envelope = Envelope.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setSubscribe(
                Subscribe.newBuilder()
                    .setFilter(
                        ChatFilter.newBuilder()
                            .setIncludeBattle(includeBattle)
                            .setIncludeOwn(true)
                            .build(),
                    )
                    .addSources(ChatSource.CHAT_SOURCE_POST_HANDLE)
                    .apply {
                        if (includeBattle) addSources(ChatSource.CHAT_SOURCE_STRUCTURED)
                    }
                    .setContentMode(ContentMode.CONTENT_MODE_CURRENT)
                    .setFromSequence(fromSequence)
                    .build(),
            )
            .build()

        return try {
            connection.send(envelope)
            withTimeout(10_000) { deferred.await() }
        } catch (timeout: TimeoutCancellationException) {
            throw SubscriptionException.Timeout()
        } finally {
            if (subscribeDeferred === deferred) subscribeDeferred = null
        }
    }

    suspend fun unsubscribe() {
        connection.send(
            Envelope.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setUnsubscribe(build.buf.gen.freechat.v1.Unsubscribe.getDefaultInstance())
                .build(),
        )
    }

    suspend fun send(
        conversation: ChatConversation,
        text: String,
        raw: Boolean = false,
        timeoutMillis: Long = 15_000,
    ): SendResult = sendMutex.withLock {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<SendResult>()
        pendingSends[requestId] = deferred

        val request = if (raw) {
            SendRequest.newBuilder().setRawText(text).build()
        } else {
            val channel = chatChannel(conversation.channel)
            val chat = ChatSend.newBuilder()
                .setChannel(channel)
                .setText(text)
            if (channel == ChatChannel.CHAT_CHANNEL_TELL) {
                chat.setTarget(
                    CharacterRef.newBuilder()
                        .setName(conversation.peerName.orEmpty())
                        .setWorldName(conversation.peerWorld.orEmpty())
                        .build(),
                )
            }
            SendRequest.newBuilder().setChat(chat.build()).build()
        }

        try {
            connection.send(
                Envelope.newBuilder()
                    .setRequestId(requestId)
                    .setSendRequest(request)
                    .build(),
            )
            withTimeout(timeoutMillis) { deferred.await() }
        } catch (timeout: TimeoutCancellationException) {
            throw SendException.Timeout()
        } finally {
            pendingSends.remove(requestId)
        }
    }

    suspend fun loadFriends(pageSize: Int = 32): FriendListSnapshot {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<FriendListSnapshot>()
        pendingFriends[requestId] = FriendAccumulator(deferred)

        return try {
            connection.send(
                Envelope.newBuilder()
                    .setRequestId(requestId)
                    .setPlayerListRequest(
                        PlayerListRequest.newBuilder()
                            .setType(PlayerListType.PLAYER_LIST_TYPE_FRIEND)
                            .setPageSize(pageSize)
                            .build(),
                    )
                    .build(),
            )
            withTimeout(20_000) { deferred.await() }
        } catch (timeout: TimeoutCancellationException) {
            throw FriendListException.Timeout()
        } finally {
            pendingFriends.remove(requestId)
        }
    }

    suspend fun close() {
        closedDeferred.complete(Unit)
        scope.cancel()
        eventChannel.close()
        connection.close()
    }

    private suspend fun route(envelope: Envelope) {
        when {
            envelope.hasChatEvent() -> eventChannel.trySend(envelope.chatEvent)
            envelope.hasSubscribed() -> subscribeDeferred?.complete(envelope.subscribed)
            envelope.hasSendResult() -> {
                val result = envelope.sendResult
                if (result.status.isTerminal()) {
                    pendingSends[envelope.requestId]?.complete(result)
                }
            }

            envelope.hasPlayerListSnapshot() -> {
                pendingFriends[envelope.requestId]?.add(envelope.playerListSnapshot)
            }

            envelope.hasError() -> {
                val error = SendException.Rejected(envelope.error.code, envelope.error.message)
                pendingSends[envelope.requestId]?.completeExceptionally(error)
                pendingFriends[envelope.requestId]?.deferred?.completeExceptionally(
                    FriendListException.Rejected(
                        PlayerListStatus.PLAYER_LIST_STATUS_FAILED,
                        envelope.error.message,
                    ),
                )
                subscribeDeferred?.completeExceptionally(error)
            }

            envelope.hasSessionEnded() || envelope.hasServerShutdown() -> {
                val sendError = SendException.ConnectionClosed()
                pendingSends.values.forEach { it.completeExceptionally(sendError) }
                pendingFriends.values.forEach {
                    it.deferred.completeExceptionally(FriendListException.ConnectionClosed())
                }
                closedDeferred.complete(Unit)
                eventChannel.close()
            }
        }
    }

    private fun SendStatus.isTerminal(): Boolean = when (this) {
        SendStatus.SEND_STATUS_SUBMITTED,
        SendStatus.SEND_STATUS_FAILED,
        SendStatus.SEND_STATUS_BLOCKED,
        SendStatus.SEND_STATUS_RATE_LIMITED,
        SendStatus.SEND_STATUS_QUEUE_FULL,
        SendStatus.SEND_STATUS_CANCELLED -> true

        else -> false
    }

    private fun chatChannel(name: String?): ChatChannel {
        if (name.isNullOrBlank()) return ChatChannel.CHAT_CHANNEL_UNSPECIFIED
        return ChatChannel.values().firstOrNull { channel ->
            channel.name == name || channel.name == "CHAT_CHANNEL_$name"
        } ?: ChatChannel.CHAT_CHANNEL_UNSPECIFIED
    }

    private class FriendAccumulator(
        val deferred: CompletableDeferred<FriendListSnapshot>,
    ) {
        private val pages = mutableMapOf<Int, PlayerListSnapshot>()
        private var expectedPages = 1
        private var snapshotId: String? = null

        fun add(snapshot: PlayerListSnapshot) {
            if (snapshot.status != PlayerListStatus.PLAYER_LIST_STATUS_SUCCESS) {
                deferred.completeExceptionally(
                    FriendListException.Rejected(snapshot.status, snapshot.message),
                )
                return
            }
            if (snapshotId == null) snapshotId = snapshot.snapshotId
            expectedPages = snapshot.pageCount.coerceAtLeast(1)
            pages[snapshot.pageIndex] = snapshot
            if (pages.size >= expectedPages) {
                val ordered = pages.toSortedMap().values
                val players = ordered.flatMap { page -> page.playersList.map { it.toDomain() } }
                val first = ordered.first()
                val capturedAt = if (first.hasCapturedAt()) {
                    first.capturedAt.seconds * 1_000 + first.capturedAt.nanos / 1_000_000
                } else {
                    0L
                }
                deferred.complete(
                    FriendListSnapshot(
                        players = players,
                        capturedAt = capturedAt,
                        ownerName = first.owner.name.takeIf { it.isNotBlank() },
                        ownerWorld = first.owner.worldName.takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }
}

private fun PlayerInfo.toDomain(): FriendInfo = FriendInfo(
    contentId = contentId,
    name = name,
    homeWorldName = homeWorldName.takeIf { it.isNotBlank() },
    currentWorldName = currentWorldName.takeIf { it.isNotBlank() },
    freeCompanyTag = freeCompanyTag.takeIf { it.isNotBlank() },
    jobName = jobName.takeIf { it.isNotBlank() },
    territoryName = territoryName.takeIf { it.isNotBlank() },
    statusBits = statusBits,
    identityUnavailable = identityUnavailable,
)
