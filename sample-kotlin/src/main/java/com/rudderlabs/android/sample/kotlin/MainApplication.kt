package com.rudderlabs.android.sample.kotlin

import android.app.Application
import com.rudderlabs.android.integration.intercom.IntercomIntegrationFactory
import com.rudderstack.android.sdk.core.RudderClient
import com.rudderstack.android.sdk.core.RudderConfig
import com.rudderstack.android.sdk.core.RudderLogger

class MainApplication : Application() {
    companion object {
        lateinit var instance: MainApplication
        const val WRITE_KEY = "1celVBhCvQjHF1ATFyqHNxJyirW"
        const val DATA_PLANE_URL = "https://hosted.rudderlabs.com"
    }

    var rudderClient: RudderClient? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        rudderClient = RudderClient.getInstance(
            this,
            WRITE_KEY,
            RudderConfig.Builder()
                .withDataPlaneUrl(DATA_PLANE_URL)
                .withLogLevel(RudderLogger.RudderLogLevel.DEBUG)
                .withFactory(IntercomIntegrationFactory.FACTORY)
                .build()
        )
    }
}