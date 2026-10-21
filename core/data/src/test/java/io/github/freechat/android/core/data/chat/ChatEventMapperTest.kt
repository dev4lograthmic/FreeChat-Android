package io.github.freechat.android.core.data.chat

import build.buf.gen.freechat.v1.CharacterRef
import build.buf.gen.freechat.v1.ChatChannel
import build.buf.gen.freechat.v1.ChatEvent
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Timestamp
import io.github.freechat.android.core.model.ConversationType
import org.junit.Test

class ChatEventMapperTest {
    @Test
    fun `maps a public channel message`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_PARTY, "hello"),
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.id).isEqualTo("srv:public:PARTY")
        assertThat(mapped.conversation.type).isEqualTo(ConversationType.PUBLIC_CHANNEL)
        assertThat(mapped.message.text).isEqualTo("hello")
        assertThat(mapped.message.senderName).isEqualTo("Alice")
    }

    @Test
    fun `maps a private tell to a peer conversation`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_TELL, "psst"),
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.id).isEqualTo("srv:tell:Typhon:Alice")
        assertThat(mapped.conversation.type).isEqualTo(ConversationType.PRIVATE)
        assertThat(mapped.conversation.displayName).isEqualTo("Alice@Typhon")
    }

    @Test
    fun `maps battle to system conversation`() {
        val mapped = ChatEventMapper.map(
            event = event(ChatChannel.CHAT_CHANNEL_BATTLE, "hit"),
            serverId = "srv",
            sessionId = "sess",
        )

        assertThat(mapped.conversation.type).isEqualTo(ConversationType.SYSTEM)
        assertThat(mapped.conversation.id).isEqualTo("srv:system:BATTLE")
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
