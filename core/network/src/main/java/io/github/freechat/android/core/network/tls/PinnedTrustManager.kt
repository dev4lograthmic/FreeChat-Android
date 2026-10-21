package io.github.freechat.android.core.network.tls

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Trust decision is pin-based: the leaf certificate must carry the expected
 * raw P-256 public key. Self-signed certificates are expected.
 */
class PinnedTrustManager(
    private val expectedFingerprint: () -> ByteArray?,
) : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client certificates are not supported")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull()
            ?: throw CertificateException("Server certificate chain is empty")
        val expected = expectedFingerprint()
            ?: throw CertificateException("No server fingerprint is configured")

        val actual = try {
            RawPublicKeyPin.fingerprint(leaf)
        } catch (error: IllegalArgumentException) {
            throw CertificateException("Invalid server public key", error)
        }

        if (!java.security.MessageDigest.isEqual(actual, expected)) {
            throw CertificateException("Server public key fingerprint mismatch")
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
