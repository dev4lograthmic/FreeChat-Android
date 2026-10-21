package io.github.freechat.android.core.data.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FtsQueryTest {
    @Test
    fun `quotes tokens and adds prefix match`() {
        assertThat(FtsQuery.build("party boss")).isEqualTo("\"party\"* AND \"boss\"*")
    }

    @Test
    fun `escapes quotes`() {
        assertThat(FtsQuery.build("\"quoted\"")).isEqualTo("\"\"\"quoted\"\"\"*")
    }

    @Test
    fun `returns null for blank input`() {
        assertThat(FtsQuery.build("   ")).isNull()
    }

    @Test
    fun `limits token count`() {
        val query = FtsQuery.build((1..20).joinToString(" ") { "t$it" })
        assertThat(query!!.split(" AND ")).hasSize(16)
    }
}
