package io.github.freechat.android.core.network.client

import build.buf.gen.freechat.v1.ChatChannel
import build.buf.gen.freechat.v1.Envelope
import build.buf.gen.freechat.v1.PlayerInfo
import build.buf.gen.freechat.v1.PlayerListSnapshot
import build.buf.gen.freechat.v1.PlayerListStatus
import build.buf.gen.freechat.v1.SendResult
import build.buf.gen.freechat.v1.SendStatus
import build.buf.gen.freechat.v1.ServerInfo
import build.buf.gen.freechat.v1.SessionEnded
import build.buf.gen.freechat.v1.Subscribed
import build.buf.gen.freechat.v1.Welcome
import com.google.common.truth.Truth.assertThat
import io.github.freechat.android.core.model.ChatConversation
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.transport.EnvelopeTransport
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class ChatSessionTest {
    @Test
    fun `subscribe sends request and waits for subscribed`() = runBlocking {
        val transport = FakeTransport()
        val session = ChatSession(connection(transport))
        transport.onSend = { envelope ->
            if (envelope.hasSubscribe()) {
                transport.emit(
                    envelope(envelope.requestId) {
                        subscribed = Subscribed.newBuilder()
                            .setOldestSequence(1)
                            .setLatestSequence(9)
                            .build()
                    },
                )
            }
        }

        val subscribed = withTimeout(5_000) { session.subscribe() }

        assertThat(subscribed.oldestSequence).isEqualTo(1)
        assertThat(transport.sent.count { it.hasSubscribe() }).isEqualTo(1)
        session.close()
    }

    @Test
    fun `send treats submitted as terminal and ignores queued`() = runBlocking {
        val transport = FakeTransport()
        val session = ChatSession(connection(transport))
        transport.onSend = { envelope ->
            if (envelope.hasSendRequest()) {
                transport.emit(
                    envelope(envelope.requestId) {
                        sendResult = SendResult.newBuilder()
                            .setStatus(SendStatus.SEND_STATUS_QUEUED)
                            .build()
                    },
                )
                transport.emit(
                    envelope(envelope.requestId) {
                        sendResult = SendResult.newBuilder()
                            .setStatus(SendStatus.SEND_STATUS_SUBMITTED)
                            .setPartCount(1)
                            .build()
                    },
                )
            }
        }

        val result = withTimeout(5_000) {
            session.send(conversation(), "hello")
        }

        assertThat(result.status).isEqualTo(SendStatus.SEND_STATUS_SUBMITTED)
        assertThat(transport.sent.count { it.hasSendRequest() }).isEqualTo(1)
        session.close()
    }

    @Test
    fun `friend snapshots are aggregated across pages`() = runBlocking {
        val transport = FakeTransport()
        val session = ChatSession(connection(transport))
        val pageCalls = AtomicInteger()
        transport.onSend = { envelope ->
            if (envelope.hasPlayerListRequest()) {
                pageCalls.getAndIncrement()
                listOf(0, 1).forEach { page ->
                    transport.emit(
                        envelope(envelope.requestId) {
                            playerListSnapshot = PlayerListSnapshot.newBuilder()
                                .setStatus(PlayerListStatus.PLAYER_LIST_STATUS_SUCCESS)
                                .setSnapshotId("snap-1")
                                .setPageIndex(page)
                                .setPageCount(2)
                                .addPlayers(
                                    PlayerInfo.newBuilder()
                                        .setContentId(if (page == 0) 1L else 2L)
                                        .setName("Friend $page")
                                        .setHomeWorldName("Typhon")
                                        .build(),
                                )
                                .build()
                        },
                    )
                }
            }
        }

        val snapshot = withTimeout(5_000) { session.loadFriends() }

        assertThat(snapshot.players).hasSize(2)
        assertThat(snapshot.players.map { it.name })
            .containsExactly("Friend 0", "Friend 1")
        session.close()
    }

    @Test
    fun `session end completes closed signal`() = runBlocking {
        val transport = FakeTransport()
        val session = ChatSession(connection(transport))

        transport.emit(
            Envelope.newBuilder()
                .setSessionEnded(SessionEnded.newBuilder().setReason("logout").build())
                .build(),
        )
        withTimeout(5_000) { session.closed.await() }

        assertThat(session.closed.isCompleted).isTrue()
        session.close()
    }

    private fun connection(transport: EnvelopeTransport): FreeChatConnection {
        val profile = ServerProfile(
            serverId = "srv",
            host = "127.0.0.1",
            port = 14777,
            fingerprintHex = "ab".repeat(32),
        )
        val welcome = Welcome.newBuilder()
            .setServer(
                ServerInfo.newBuilder()
                    .setServerId("srv")
                    .setServerName("Test")
                    .setProtocolVersion(1)
                    .build(),
            )
            .setSessionId("sess")
            .build()
        return FreeChatConnection(profile, transport, welcome)
    }

    private fun conversation() = ChatConversation(
        id = "srv:1:public:PARTY",
        profileId = "srv:1",
        serverId = "srv",
        type = io.github.freechat.android.core.model.ConversationType.PUBLIC_CHANNEL,
        channel = ChatChannel.CHAT_CHANNEL_PARTY.name,
        peerName = null,
        peerWorld = null,
        displayName = "PARTY",
        lastMessageText = "",
        lastMessageAt = 0,
        unreadCount = 0,
    )

    private class FakeTransport : EnvelopeTransport {
        private val channel = Channel<Envelope>(Channel.UNLIMITED)
        override val incoming: Flow<Envelope> = channel.receiveAsFlow()
        val sent = mutableListOf<Envelope>()
        var onSend: suspend (Envelope) -> Unit = {}

        override suspend fun open() = Unit

        override suspend fun send(envelope: Envelope) {
            sent += envelope
            onSend(envelope)
        }

        override suspend fun close(code: Int, reason: String?) {
            channel.close()
        }

        fun emit(envelope: Envelope) {
            channel.trySend(envelope)
        }
    }

    private fun envelope(requestId: String, block: Envelope.Builder.() -> Unit): Envelope =
        Envelope.newBuilder().setRequestId(requestId).apply(block).build()
}
