package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.CharacterRef
import build.buf.gen.freechat.v1.ChatChannel
import build.buf.gen.freechat.v1.ChatEvent
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Timestamp
import io.github.freechat.android.core.model.ConversationType
import io.github.freechat.android.core.model.MessageContentSource
import org.junit.Test

class ChatEventMapperTest {
    @Test
    fun `maps a public channel message`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_PARTY, "hello"),
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.id).isEqualTo("srv:100:public:PARTY")
        assertThat(mapped.conversation.type).isEqualTo(ConversationType.PUBLIC_CHANNEL)
        assertThat(mapped.message.text).isEqualTo("hello")
        assertThat(mapped.message.senderName).isEqualTo("Alice")
    }

    @Test
    fun `maps a named linkshell to its own conversation`() {
        val event = event(ChatChannel.CHAT_CHANNEL_LINKSHELL_1, "hi")
            .toBuilder()
            .setChannelName("Crystal Guild")
            .build()
        val mapped = ChatEventMapper.map(
            event = event,
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.type).isEqualTo(ConversationType.LINKSHELL)
        assertThat(mapped.conversation.id).isEqualTo("srv:100:public:LINKSHELL_1")
        assertThat(mapped.conversation.displayName).isEqualTo("Crystal Guild")
    }

    @Test
    fun `leaves an unnamed linkshell without a display name`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_CROSS_LINKSHELL_2, "hi"),
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.type).isEqualTo(ConversationType.LINKSHELL)
        assertThat(mapped.conversation.displayName).isEmpty()
    }

    @Test
    fun `maps a private tell to a peer conversation`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_TELL, "psst"),
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.id).isEqualTo("srv:100:tell:Typhon:Alice")
        assertThat(mapped.conversation.type).isEqualTo(ConversationType.PRIVATE)
        assertThat(mapped.conversation.displayName).isEqualTo("Alice@Typhon")
    }

    @Test
    fun `maps battle to system conversation`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_BATTLE, "hit"),
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.type).isEqualTo(ConversationType.SYSTEM)
        assertThat(mapped.conversation.id).isEqualTo("srv:100:system:BATTLE_LOG")
    }

    @Test
    fun `can select original text for debugging`() {
        val event = event(ChatChannel.CHAT_CHANNEL_SAY, "current")
            .toBuilder()
            .setOriginalText("original")
            .build()
        val mapped = ChatEventMapper.map(
            event = event,
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
            contentSource = MessageContentSource.ORIGINAL_TEXT,
        )

        assertThat(mapped.message.text).isEqualTo("original")
    }

    @Test
    fun `can select raw message for debugging`() {
        val event = event(ChatChannel.CHAT_CHANNEL_SAY, "current")
            .toBuilder()
            .setRawMessage(com.google.protobuf.ByteString.copyFrom(byteArrayOf(1, 2, 3)))
            .build()
        val mapped = ChatEventMapper.map(
            event = event,
            profileId = "srv:100",
            serverId = "srv",
            sessionId = "sess",
            contentSource = MessageContentSource.RAW_MESSAGE,
        )

        assertThat(mapped.message.text).startsWith("[raw] ")
    }

    private fun event(channel: ChatChannel, text: String): ChatEvent =
        ChatEvent.newBuilder()
            .setMessageId("srv:sess:42")
            .setSequence(42)
            .setTimestamp(
                Timestamp.newBuilder()
                    .setSeconds(1_700_000_000)
                    .setNanos(123_000_000)
                    .build(),
            )
            .setChannel(channel)
            .setLogKind(10)
            .setSender(
                CharacterRef.newBuilder()
                    .setName("Alice")
                    .setWorldName("Typhon")
                    .setContentId(123)
                    .build(),
            )
            .setText(text)
            .build()
}
