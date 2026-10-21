package io.github.freechat.android.core.data.search

/**
 * Turns user input into a conservative FTS4 query. Each token is quoted and
 * suffix-matched, so input such as "party:" or quotes cannot break the query.
 */
object FtsQuery {
    fun build(input: String): String? {
        val tokens = input.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(16)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { token ->
            "\"" + token.replace("\"", "\"\"") + "\"*"
        }
    }
}
