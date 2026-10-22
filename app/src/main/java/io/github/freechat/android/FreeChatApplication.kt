package io.github.freechat.android

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.freechat.android.core.ui.locale.LocaleController
import io.github.freechat.android.notify.NotificationDispatcher
import javax.inject.Inject

@HiltAndroidApp
class FreeChatApplication : Application() {
    @Inject lateinit var notifications: NotificationDispatcher

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleController.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannels()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        notifications.ensureChannels()
    }
}
