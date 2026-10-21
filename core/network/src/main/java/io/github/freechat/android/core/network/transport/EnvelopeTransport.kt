package io.github.freechat.android.core.network.transport

import build.buf.gen.freechat.v1.Envelope
import kotlinx.coroutines.flow.Flow

/**
 * One authenticated WebSocket transport. Implementations are single-use.
 */
interface EnvelopeTransport {
    val incoming: Flow<Envelope>

    suspend fun open()

    suspend fun send(envelope: Envelope)

    suspend fun close(code: Int = 1000, reason: String? = null)
}
