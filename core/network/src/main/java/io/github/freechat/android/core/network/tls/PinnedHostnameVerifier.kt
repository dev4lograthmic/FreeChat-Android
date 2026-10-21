package io.github.freechat.android.core.network.tls

import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/**
 * Hostnames are not the identity in this protocol. The pinned public key is.
 * Return true only when the peer certificate still matches the expected pin.
 */
class PinnedHostnameVerifier(
    private val expectedFingerprint: () -> ByteArray?,
) : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        val leaf = session?.peerCertificates?.firstOrNull() as? X509Certificate ?: return false
        val expected = expectedFingerprint() ?: return false
        return RawPublicKeyPin.matches(leaf, expected)
    }
}
