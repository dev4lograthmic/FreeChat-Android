package io.github.freechat.android.core.network.security

/**
 * Converts the DER-encoded ECDSA signature returned by JCA into the IEEE
 * P1363 form required by freechat.v1: 64 bytes, r || s.
 */
internal object DerSignature {
    fun toP1363(der: ByteArray): ByteArray {
        var offset = 0
        require(der.size > 8 && der[offset++] == 0x30.toByte()) { "Not a DER sequence" }
        val sequenceLength = readLength(der, offset)
        offset += sequenceLength.bytes
        require(sequenceLength.value == der.size - offset) { "Invalid DER sequence length" }

        require(der[offset++] == 0x02.toByte()) { "Missing r INTEGER" }
        val rLength = readLength(der, offset)
        offset += rLength.bytes
        val r = der.copyOfRange(offset, offset + rLength.value)
        offset += rLength.value

        require(der[offset++] == 0x02.toByte()) { "Missing s INTEGER" }
        val sLength = readLength(der, offset)
        offset += sLength.bytes
        val s = der.copyOfRange(offset, offset + sLength.value)
        offset += sLength.value

        require(offset == der.size) { "Trailing bytes in DER signature" }
        return ByteArray(64).also { result ->
            copyFixed(r, result, 0)
            copyFixed(s, result, 32)
        }
    }

    private fun copyFixed(value: ByteArray, destination: ByteArray, offset: Int) {
        var start = 0
        while (start < value.size - 1 && value[start] == 0.toByte()) start++
        val length = value.size - start
        require(length <= 32) { "ECDSA integer is too large" }
        value.copyInto(destination, offset + (32 - length), start)
    }

    private fun readLength(bytes: ByteArray, offset: Int): Length {
        val first = bytes[offset].toInt() and 0xff
        if (first and 0x80 == 0) return Length(first, 1)
        val count = first and 0x7f
        require(count in 1..2) { "Unsupported DER length" }
        var value = 0
        for (index in 0 until count) {
            value = (value shl 8) or (bytes[offset + 1 + index].toInt() and 0xff)
        }
        return Length(value, 1 + count)
    }

    private data class Length(val value: Int, val bytes: Int)
}
