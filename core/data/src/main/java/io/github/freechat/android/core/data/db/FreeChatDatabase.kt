package io.github.freechat.android.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.freechat.android.core.data.db.dao.ConversationDao
import io.github.freechat.android.core.data.db.dao.DraftDao
import io.github.freechat.android.core.data.db.dao.MessageDao
import io.github.freechat.android.core.data.db.dao.SendJobDao
import io.github.freechat.android.core.data.db.dao.ServerDao
import io.github.freechat.android.core.data.db.dao.SessionDao
import io.github.freechat.android.core.data.db.entity.ConversationEntity
import io.github.freechat.android.core.data.db.entity.DraftEntity
import io.github.freechat.android.core.data.db.entity.MessageEntity
import io.github.freechat.android.core.data.db.entity.MessageFtsEntity
import io.github.freechat.android.core.data.db.entity.SendJobEntity
import io.github.freechat.android.core.data.db.entity.ServerEntity
import io.github.freechat.android.core.data.db.entity.SessionEntity

@Database(
    entities = [
        ServerEntity::class,
        SessionEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        SendJobEntity::class,
        DraftEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FreeChatDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun sessionDao(): SessionDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun sendJobDao(): SendJobDao
    abstract fun draftDao(): DraftDao

    companion object {
        const val NAME = "freechat.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
