package io.github.freechat.android.core.network.security

import com.google.common.truth.Truth.assertThat
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import io.github.freechat.android.core.network.tls.RawPublicKeyPin
import org.junit.Test

class ChallengeSignerTest {
    @Test
    fun `builds the exact protocol input`() {
        val input = ChallengeSigner.buildInput(
            serverId = "srv-1",
            deviceId = "dev-1",
            nonce = byteArrayOf(1, 2, 3),
        )

        val expected = "FreeChat/v1 challenge".toByteArray(StandardCharsets.UTF_8) +
            byteArrayOf(0) +
            "srv-1".toByteArray(StandardCharsets.UTF_8) +
            byteArrayOf(0) +
            "dev-1".toByteArray(StandardCharsets.UTF_8) +
            byteArrayOf(0) +
            byteArrayOf(1, 2, 3)

        assertThat(input).isEqualTo(expected)
    }

    @Test
    fun `signature is converted to 64 byte P1363`() {
        val key = TestDeviceKey()
        val signature = ChallengeSigner.sign(
            deviceKey = key,
            serverId = "srv-1",
            deviceId = "dev-1",
            nonce = ByteArray(32) { it.toByte() },
        )

        assertThat(signature.size).isEqualTo(64)
    }

    private class TestDeviceKey : DeviceKey {
        private val pair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        override fun publicKeyBytes(): ByteArray =
            RawPublicKeyPin.encodeUncompressed(pair.public as ECPublicKey)

        override fun sign(data: ByteArray): ByteArray {
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(pair.private)
            signer.update(data)
            return DerSignature.toP1363(signer.sign())
        }
    }
}
