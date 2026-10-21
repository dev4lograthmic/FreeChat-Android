package io.github.freechat.android.core.network.connection

import kotlin.math.min
import kotlin.math.pow

/**
 * Exponential backoff for reconnect attempts. The caller owns the actual
 * reconnect loop and decides when a failure is terminal (for example an auth
 * rejection).
 */
class ReconnectPolicy(
    private val initialDelayMillis: Long = 1_000,
    private val maxDelayMillis: Long = 30_000,
    private val multiplier: Double = 2.0,
) {
    init {
        require(initialDelayMillis > 0)
        require(maxDelayMillis >= initialDelayMillis)
        require(multiplier >= 1.0)
    }

    fun delayForAttempt(attempt: Int): Long {
        require(attempt >= 0)
        val raw = initialDelayMillis * multiplier.pow(attempt.toDouble())
        return min(raw.toLong(), maxDelayMillis)
    }
}
