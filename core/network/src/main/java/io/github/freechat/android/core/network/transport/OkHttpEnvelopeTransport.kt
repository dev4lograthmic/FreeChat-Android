package io.github.freechat.android.core.network.transport

import build.buf.gen.freechat.v1.Envelope
import io.github.freechat.android.core.model.ServerProfile
import io.github.freechat.android.core.network.tls.RawPublicKeyPin
import io.github.freechat.android.core.protocol.FreeChatProtocolCodec
import java.io.IOException
import java.security.cert.X509Certificate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * OkHttp-backed transport. TLS is pinned in PinnedOkHttpClientFactory and is
 * checked once more before the first application frame is sent.
 */
class OkHttpEnvelopeTransport(
    private val client: OkHttpClient,
    private val profile: ServerProfile,
) : EnvelopeTransport {
    private val incomingChannel = Channel<Envelope>(Channel.UNLIMITED)
    private val opened = CompletableDeferred<Unit>()
    private var webSocket: WebSocket? = null

    override val incoming: Flow<Envelope> = incomingChannel.receiveAsFlow()

    override suspend fun open() {
        val request = Request.Builder()
            .url(profile.webSocketUrl)
            .header("Sec-WebSocket-Protocol", FreeChatProtocolCodec.SUBPROTOCOL)
            .build()

        webSocket = client.newWebSocket(request, Listener())
        opened.await()
    }

    override suspend fun send(envelope: Envelope) {
        val socket = webSocket ?: throw IOException("WebSocket is not connected")
        val bytes = FreeChatProtocolCodec.encode(envelope)
        val payload = bytes.toByteString(0, bytes.size)
        if (!socket.send(payload)) {
            throw IOException("Failed to enqueue WebSocket frame")
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        webSocket?.close(code, reason)
        incomingChannel.close()
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val expected = runCatching { RawPublicKeyPin.fromHex(profile.fingerprintHex) }.getOrNull()
            if (expected == null) {
                val error = SecurityException("Invalid pinned fingerprint")
                webSocket.close(4004, "TLS pin mismatch")
                opened.completeExceptionally(error)
                incomingChannel.close(error)
                return
            }

            val leaf = response.handshake?.peerCertificates?.firstOrNull() as? X509Certificate
            if (leaf != null && !RawPublicKeyPin.matches(leaf, expected)) {
                // The TrustManager already checked during the handshake. This
                // second check catches a platform/session mismatch, but must
                // not fail when handshake metadata is unavailable.
                val actual = runCatching { RawPublicKeyPin.toHex(RawPublicKeyPin.fingerprint(leaf)) }
                    .getOrDefault("<unavailable>")
                webSocket.close(4004, "TLS pin mismatch")
                val error = SecurityException(
                    "Server public key pin mismatch (expected ${profile.fingerprintHex}, actual $actual)",
                )
                opened.completeExceptionally(error)
                incomingChannel.close(error)
                return
            }
            opened.complete(Unit)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            runCatching { FreeChatProtocolCodec.decode(bytes.toByteArray()) }
                .onSuccess { incomingChannel.trySend(it) }
                .onFailure {
                    incomingChannel.close(it)
                    webSocket.close(4004, "Invalid protobuf frame")
                }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // JSON debug transport is intentionally limited to debug builds.
            incomingChannel.close(IllegalStateException("Unexpected text frame"))
            webSocket.close(1003, "Binary protobuf required")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
            incomingChannel.close()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            incomingChannel.close()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!opened.isCompleted) opened.completeExceptionally(t)
            incomingChannel.close(t)
        }
    }
}
