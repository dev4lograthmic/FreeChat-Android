package io.github.freechat.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.freechat.android.core.data.chat.ChatController
import io.github.freechat.android.core.data.db.FreeChatDatabase
import io.github.freechat.android.core.data.repository.ChatRepository
import io.github.freechat.android.core.data.repository.ServerRepository
import io.github.freechat.android.core.data.store.DataStoreServerProfileStore
import io.github.freechat.android.core.data.store.DataStoreSettingsStore
import io.github.freechat.android.core.data.store.ServerProfileStore
import io.github.freechat.android.core.data.store.SettingsStore
import io.github.freechat.android.core.data.store.freeChatDataStore
import io.github.freechat.android.core.model.ClientInfo
import io.github.freechat.android.core.network.client.FreeChatClient
import io.github.freechat.android.core.network.security.AndroidDeviceKeyStore
import io.github.freechat.android.core.network.security.DeviceIdentityProvider
import io.github.freechat.android.core.network.security.DeviceKey
import io.github.freechat.android.core.network.security.ServerDeviceIdStore
import io.github.freechat.android.core.network.security.SharedPreferencesServerDeviceIdStore
import io.github.freechat.android.core.network.tls.PinnedOkHttpClientFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.freeChatDataStore

    @Provides
    @Singleton
    fun provideServerProfileStore(dataStore: DataStore<Preferences>): ServerProfileStore =
        DataStoreServerProfileStore(dataStore)

    @Provides
    @Singleton
    fun provideSettingsStore(dataStore: DataStore<Preferences>): SettingsStore =
        DataStoreSettingsStore(dataStore)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FreeChatDatabase =
        Room.databaseBuilder(context, FreeChatDatabase::class.java, FreeChatDatabase.NAME)
            .addMigrations(FreeChatDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideChatRepository(database: FreeChatDatabase): ChatRepository =
        ChatRepository(database)

    @Provides
    @Singleton
    fun provideServerRepository(
        database: FreeChatDatabase,
        serverProfileStore: ServerProfileStore,
    ): ServerRepository = ServerRepository(database, serverProfileStore)

    @Provides
    @Singleton
    fun provideDeviceKey(@ApplicationContext context: Context): DeviceKey =
        AndroidDeviceKeyStore(context)

    @Provides
    @Singleton
    fun provideDeviceIdStore(@ApplicationContext context: Context): ServerDeviceIdStore =
        SharedPreferencesServerDeviceIdStore(context)

    @Provides
    @Singleton
    fun provideIdentityProvider(
        deviceKey: DeviceKey,
        deviceIdStore: ServerDeviceIdStore,
    ): DeviceIdentityProvider = DeviceIdentityProvider(deviceKey, deviceIdStore)

    @Provides
    @Singleton
    fun provideClientFactory(): PinnedOkHttpClientFactory = PinnedOkHttpClientFactory()

    @Provides
    @Singleton
    fun provideClientInfo(): ClientInfo = ClientInfo(clientVersion = BuildConfig.VERSION_NAME)

    @Provides
    @Singleton
    fun provideFreeChatClient(
        clientFactory: PinnedOkHttpClientFactory,
        deviceKey: DeviceKey,
        identityProvider: DeviceIdentityProvider,
        clientInfo: ClientInfo,
    ): FreeChatClient = FreeChatClient(
        clientFactory = clientFactory,
        deviceKey = deviceKey,
        identityProvider = identityProvider,
        clientInfo = clientInfo,
    )

    @Provides
    @Singleton
    fun provideChatController(
        client: FreeChatClient,
        chatRepository: ChatRepository,
    ): ChatController = ChatController(client, chatRepository)
}
