package io.github.freechat.android.core.network.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingLinkParserTest {
    @Test
    fun `parses a complete pairing link`() {
        val link = PairingLinkParser.parse(
            "freechat://pair?v=1&host=192.168.1.10&port=14777&sid=srv-1&fp=${"ab".repeat(32)}&code=482913",
        )

        assertThat(link.host).isEqualTo("192.168.1.10")
        assertThat(link.port).isEqualTo(14777)
        assertThat(link.serverId).isEqualTo("srv-1")
        assertThat(link.fingerprintHex).isEqualTo("ab".repeat(32))
        assertThat(link.code).isEqualTo("482913")
    }

    @Test
    fun `allows a link without a pairing code`() {
        val link = PairingLinkParser.parse(
            "freechat://pair?v=1&host=localhost&port=14777&sid=srv-1&fp=${"cd".repeat(32)}",
        )

        assertThat(link.code).isNull()
    }

    @Test
    fun `rejects wrong scheme`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingLinkParser.parse(
                "https://pair?v=1&host=localhost&port=14777&sid=srv-1&fp=${"ab".repeat(32)}",
            )
        }
    }

    @Test
    fun `rejects invalid fingerprint`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingLinkParser.parse(
                "freechat://pair?v=1&host=localhost&port=14777&sid=srv-1&fp=not-a-fingerprint",
            )
        }
    }
}
