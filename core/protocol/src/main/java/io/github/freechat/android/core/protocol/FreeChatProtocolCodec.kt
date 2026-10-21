package io.github.freechat.android.core.protocol

import build.buf.gen.freechat.v1.Envelope

/**
 * The wire format for freechat.v1. Binary frames always contain exactly one
 * Envelope; JSON debug frames are handled separately by debug builds.
 */
object FreeChatProtocolCodec {
    const val SUBPROTOCOL = "freechat.v1.protobuf"

    fun encode(envelope: Envelope): ByteArray = envelope.toByteArray()

    fun decode(bytes: ByteArray): Envelope = Envelope.parseFrom(bytes)
}
