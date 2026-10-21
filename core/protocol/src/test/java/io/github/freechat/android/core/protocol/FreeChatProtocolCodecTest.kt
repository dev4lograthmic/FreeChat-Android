package io.github.freechat.android.core.protocol

import build.buf.gen.freechat.v1.ClientHello
import build.buf.gen.freechat.v1.Envelope
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FreeChatProtocolCodecTest {
    @Test
    fun `binary round trip preserves envelope`() {
        val envelope = Envelope.newBuilder()
            .setRequestId("req-1")
            .setClientHello(
                ClientHello.newBuilder()
                    .setProtocolVersion(1)
                    .setClientName("FreeChat Android")
                    .setDeviceId("device-1")
                    .build(),
            )
            .build()

        val decoded = FreeChatProtocolCodec.decode(FreeChatProtocolCodec.encode(envelope))

        assertThat(decoded.requestId).isEqualTo("req-1")
        assertThat(decoded.clientHello.protocolVersion).isEqualTo(1)
        assertThat(decoded.clientHello.deviceId).isEqualTo("device-1")
    }
}
