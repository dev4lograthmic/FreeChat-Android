package io.github.freechat.android.core.data.export

import com.google.common.truth.Truth.assertThat
import io.github.freechat.android.core.model.ChatMessage
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import org.junit.Test

class MessageExporterTest {
    private val message = ChatMessage(
        messageId = "srv:sess:1",
        conversationId = "srv:public:PARTY",
        profileId = "srv:100",
        serverId = "srv",
        sessionId = "sess",
        sequence = 1,
        timestamp = 1_700_000_000_000,
        channel = "PARTY",
        logKind = 10,
        senderName = "Tester",
        senderWorld = "Typhon",
        text = "hello \"world\"",
        outgoing = false,
        dedupeKey = "d1",
    )

    @Test
    fun `exports txt`() {
        val output = ByteArrayOutputStream()
        MessageExporter(ZoneOffset.UTC).write(listOf(message), output, ExportFormat.TXT, false)

        val text = output.toString(Charsets.UTF_8.name())
        assertThat(text).contains("PARTY Tester: hello \"world\"")
    }

    @Test
    fun `exports valid json shape`() {
        val output = ByteArrayOutputStream()
        MessageExporter(ZoneOffset.UTC).write(listOf(message), output, ExportFormat.JSON, true)

        val text = output.toString(Charsets.UTF_8.name())
        assertThat(text).startsWith("[")
        assertThat(text).endsWith("]\n")
        assertThat(text).contains("\"messageId\": \"srv:sess:1\"")
        assertThat(text).contains("\"text\": \"hello \\\"world\\\"\"")
    }
}
