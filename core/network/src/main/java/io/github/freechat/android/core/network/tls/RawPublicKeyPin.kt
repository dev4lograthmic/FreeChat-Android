package io.github.freechat.android.core.network.tls

import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey

/**
 * The protocol pins SHA-256 over the raw X9.63 uncompressed EC point:
 * `0x04 || X(32) || Y(32)`.
 *
 * This is intentionally not the certificate fingerprint and not the
 * SubjectPublicKeyInfo (SPKI) fingerprint used by OkHttp's CertificatePinner.
 */
object RawPublicKeyPin {
    private const val P256_FIELD_SIZE = 256
    private const val COORDINATE_BYTES = 32

    fun fingerprint(certificate: X509Certificate): ByteArray {
        val key = certificate.publicKey as? ECPublicKey
            ?: throw IllegalArgumentException("Server certificate does not contain an EC public key")
        return fingerprint(key)
    }

    fun fingerprint(publicKey: ECPublicKey): ByteArray = sha256(encodeUncompressed(publicKey))

    /**
     * Encodes the EC public key as the 65-byte X9.63 uncompressed point:
     * 0x04 || X(32) || Y(32).
     */
    fun encodeUncompressed(publicKey: ECPublicKey): ByteArray {
        if (publicKey.params.curve.field.fieldSize != P256_FIELD_SIZE) {
            throw IllegalArgumentException("Expected a P-256 public key")
        }
        val point = ByteArray(1 + COORDINATE_BYTES * 2)
        point[0] = 0x04
        toFixed32(publicKey.w.affineX).copyInto(point, 1)
        toFixed32(publicKey.w.affineY).copyInto(point, 1 + COORDINATE_BYTES)
        return point
    }

    fun matches(certificate: X509Certificate, expectedFingerprint: ByteArray): Boolean {
        if (expectedFingerprint.size != 32) return false
        return MessageDigest.isEqual(fingerprint(certificate), expectedFingerprint)
    }

    fun toHex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(HEX[value ushr 4])
            append(HEX[value and 0x0f])
        }
    }

    fun fromHex(hex: String): ByteArray {
        require(hex.length == 64) { "Fingerprint must be 64 hex characters" }
        val result = ByteArray(32)
        for (index in result.indices) {
            val high = Character.digit(hex[index * 2], 16)
            val low = Character.digit(hex[index * 2 + 1], 16)
            require(high >= 0 && low >= 0) { "Fingerprint contains non-hex characters" }
            result[index] = ((high shl 4) or low).toByte()
        }
        return result
    }

    private fun toFixed32(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        val result = ByteArray(COORDINATE_BYTES)
        when {
            bytes.size == COORDINATE_BYTES -> bytes.copyInto(result)
            bytes.size == COORDINATE_BYTES + 1 && bytes[0] == 0.toByte() ->
                bytes.copyInto(result, destinationOffset = 0, startIndex = 1, endIndex = bytes.size)
            bytes.size < COORDINATE_BYTES ->
                bytes.copyInto(result, destinationOffset = COORDINATE_BYTES - bytes.size)
            else -> throw IllegalArgumentException("EC coordinate does not fit in 32 bytes")
        }
        return result
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    private val HEX = "0123456789abcdef".toCharArray()
}
