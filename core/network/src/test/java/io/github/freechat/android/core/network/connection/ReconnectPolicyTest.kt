package io.github.freechat.android.core.network.connection

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReconnectPolicyTest {
    @Test
    fun `backoff doubles and caps`() {
        val policy = ReconnectPolicy(
            initialDelayMillis = 1_000,
            maxDelayMillis = 5_000,
            multiplier = 2.0,
        )

        assertThat(policy.delayForAttempt(0)).isEqualTo(1_000)
        assertThat(policy.delayForAttempt(1)).isEqualTo(2_000)
        assertThat(policy.delayForAttempt(2)).isEqualTo(4_000)
        assertThat(policy.delayForAttempt(3)).isEqualTo(5_000)
        assertThat(policy.delayForAttempt(10)).isEqualTo(5_000)
    }
}
