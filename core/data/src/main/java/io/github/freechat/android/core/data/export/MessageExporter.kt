package io.github.freechat.android.core.data.export

import io.github.freechat.android.core.model.ChatMessage
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

enum class ExportFormat {
    TXT,
    JSON,
}

class MessageExporter(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun write(
        messages: List<ChatMessage>,
        output: OutputStream,
        format: ExportFormat,
        includeRaw: Boolean,
    ) {
        val writer = output.bufferedWriter(StandardCharsets.UTF_8)
        when (format) {
            ExportFormat.TXT -> writeTxt(messages, writer, includeRaw)
            ExportFormat.JSON -> writeJson(messages, writer, includeRaw)
        }
        writer.flush()
    }

    private fun writeTxt(messages: List<ChatMessage>, writer: Appendable, includeRaw: Boolean) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(zoneId)
        messages.asReversed().forEach { message ->
            val time = formatter.format(Instant.ofEpochMilli(message.timestamp))
            writer.appendLine("[$time] ${message.channel} ${message.senderName}: ${message.text}")
            if (includeRaw) {
                message.rawMessage?.let {
                    writer.appendLine("  raw: ${Base64.getEncoder().encodeToString(it)}")
                }
            }
        }
    }

    private fun writeJson(messages: List<ChatMessage>, writer: Appendable, includeRaw: Boolean) {
        writer.appendLine("[")
        messages.asReversed().forEachIndexed { index, message ->
            writer.appendLine("  {")
            writer.appendLine("    \"messageId\": ${json(message.messageId)},")
            writer.appendLine("    \"conversationId\": ${json(message.conversationId)},")
            writer.appendLine("    \"serverId\": ${json(message.serverId)},")
            writer.appendLine("    \"sessionId\": ${json(message.sessionId)},")
            writer.appendLine("    \"sequence\": ${message.sequence},")
            writer.appendLine("    \"timestamp\": ${message.timestamp},")
            writer.appendLine("    \"channel\": ${json(message.channel)},")
            writer.appendLine("    \"logKind\": ${message.logKind},")
            writer.appendLine("    \"senderName\": ${json(message.senderName)},")
            writer.appendLine("    \"senderWorld\": ${json(message.senderWorld)},")
            writer.appendLine("    \"text\": ${json(message.text)},")
            writer.appendLine("    \"outgoing\": ${message.outgoing},")
            writer.appendLine(
                "    \"dedupeKey\": ${json(message.dedupeKey)}" + if (includeRaw) "," else "",
            )
            if (includeRaw) {
                writer.appendLine(
                    "    \"rawMessage\": ${json(message.rawMessage?.let { Base64.getEncoder().encodeToString(it) })}",
                )
            }
            writer.append("  }")
            if (index != messages.lastIndex) writer.append(',')
            writer.appendLine()
        }
        writer.appendLine("]")
    }

    private fun json(value: String?): String {
        if (value == null) return "null"
        return buildString(value.length + 2) {
            append('"')
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (character < ' ') {
                        append("\\u%04x".format(character.code))
                    } else {
                        append(character)
                    }
                }
            }
            append('"')
        }
    }
}
