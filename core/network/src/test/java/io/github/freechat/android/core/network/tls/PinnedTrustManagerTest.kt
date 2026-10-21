package io.github.freechat.android.core.network.tls

import com.google.common.truth.Truth.assertThat
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertThrows
import org.junit.Test

class PinnedTrustManagerTest {
    @Test
    fun `accepts the pinned self signed certificate`() {
        val certificate = selfSignedCertificate()
        val expected = RawPublicKeyPin.fingerprint(certificate)
        val trustManager = PinnedTrustManager { expected }

        trustManager.checkServerTrusted(arrayOf(certificate), "ECDHE_ECDSA")
    }

    @Test
    fun `rejects a different public key`() {
        val certificate = selfSignedCertificate()
        val otherCertificate = selfSignedCertificate()
        val trustManager = PinnedTrustManager {
            RawPublicKeyPin.fingerprint(otherCertificate)
        }

        assertThrows(CertificateException::class.java) {
            trustManager.checkServerTrusted(arrayOf(certificate), "ECDHE_ECDSA")
        }
    }

    @Test
    fun `rejects empty chain`() {
        val trustManager = PinnedTrustManager { ByteArray(32) }

        assertThrows(CertificateException::class.java) {
            trustManager.checkServerTrusted(emptyArray<X509Certificate>(), "ECDHE_ECDSA")
        }
    }

    @Test
    fun `fingerprint of certificate equals fingerprint of its key`() {
        val certificate = selfSignedCertificate()
        val expected = RawPublicKeyPin.fingerprint(certificate)
        val fromKey = RawPublicKeyPin.fingerprint(certificate.publicKey as java.security.interfaces.ECPublicKey)

        assertThat(expected).isEqualTo(fromKey)
    }

    private fun selfSignedCertificate(): X509Certificate {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val name = X500Name("CN=FreeChat Test")
        val now = Date()
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.ONE,
            now,
            Date(now.time + 86_400_000L),
            name,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }
}
