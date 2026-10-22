package io.github.freechat.android.core.network.security

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.freechat.android.core.network.tls.RawPublicKeyPin

/**
 * P-256 signing key stored in Android Keystore. The private key never leaves
 * the keystore; only the uncompressed public point is exported.
 */
class AndroidDeviceKeyStore(
    context: Context,
    private val alias: String = DEFAULT_ALIAS,
) : DeviceKey {
    private val keyPair: KeyPair by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            val privateKey = keyStore.getKey(alias, null) as PrivateKey
            val publicKey = keyStore.getCertificate(alias).publicKey
            KeyPair(publicKey, privateKey)
        } else {
            generateKeyPair()
        }
    }

    override fun publicKeyBytes(): ByteArray {
        val publicKey = keyPair.public as? ECPublicKey
            ?: error("Android Keystore key is not an EC key")
        return RawPublicKeyPin.encodeUncompressed(publicKey)
    }

    override fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(keyPair.private)
        signature.update(data)
        return DerSignature.toP1363(signature.sign())
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()
        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val DEFAULT_ALIAS = "freechat_device_key_v1"
    }
}
