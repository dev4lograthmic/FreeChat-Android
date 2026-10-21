package io.github.freechat.android.core.network.tls

import com.google.common.truth.Truth.assertThat
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertThrows
import org.junit.Test

class RawPublicKeyPinTest {
    @Test
    fun `encodes a 65 byte uncompressed point`() {
        val keyPair = generateKeyPair()
        val point = RawPublicKeyPin.encodeUncompressed(keyPair.public as ECPublicKey)

        assertThat(point.size).isEqualTo(65)
        assertThat(point[0]).isEqualTo(0x04.toByte())
    }

    @Test
    fun `fingerprint is stable for the same key`() {
        val keyPair = generateKeyPair()
        val first = RawPublicKeyPin.fingerprint(keyPair.public as ECPublicKey)
        val second = RawPublicKeyPin.fingerprint(keyPair.public as ECPublicKey)

        assertThat(first.size).isEqualTo(32)
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `different keys produce different fingerprints`() {
        val first = RawPublicKeyPin.fingerprint(generateKeyPair().public as ECPublicKey)
        val second = RawPublicKeyPin.fingerprint(generateKeyPair().public as ECPublicKey)

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `hex round trip`() {
        val value = ByteArray(32) { (it * 7).toByte() }
        val hex = RawPublicKeyPin.toHex(value)

        assertThat(hex).hasLength(64)
        assertThat(RawPublicKeyPin.fromHex(hex)).isEqualTo(value)
    }

    @Test
    fun `rejects malformed hex`() {
        assertThrows(IllegalArgumentException::class.java) {
            RawPublicKeyPin.fromHex("abcd")
        }
    }

    private fun generateKeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
}
