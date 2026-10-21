package io.github.freechat.android.core.network.tls

import io.github.freechat.android.core.model.ServerProfile
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Protocol

/**
 * Creates one OkHttp client per server profile. Clients are not shared across
 * profiles because each profile has its own TLS pin and connection semantics.
 */
class PinnedOkHttpClientFactory(
    private val debugLogging: Boolean = false,
) {
    fun create(profile: ServerProfile): OkHttpClient {
        val expected = RawPublicKeyPin.fromHex(profile.fingerprintHex)
        val trustManager = PinnedTrustManager { expected }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(PinnedHostnameVerifier { expected })
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .protocols(listOf(Protocol.HTTP_1_1))
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)

        if (debugLogging) {
            // Logging is intentionally omitted here; debug builds add it in P1.5.
        }

        return builder.build()
    }
}
