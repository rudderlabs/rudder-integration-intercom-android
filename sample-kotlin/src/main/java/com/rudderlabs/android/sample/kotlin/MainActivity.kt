package com.rudderlabs.android.sample.kotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rudderstack.android.sdk.core.RudderProperty
import com.rudderstack.android.sdk.core.RudderTraits

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendEvents()
    }

    private fun sendEvents() {
        MainApplication.instance.rudderClient?.let {
            it.track("simple_track_event_android")
            it.track(
                "simple_track_props_android", RudderProperty().putValue(
                    mapOf(
                        "key1" to "val1",
                        "key2" to "val2"
                    )
                )
            )

            it.identify("sample_user_id_android", RudderTraits()
                .putCompany(
                    RudderTraits.Company()
                        .putId("company_id")
                        .putIndustry("Software Industry")
                        .putName("RudderStack")
                )
                .putName("Test Name")
                .putEmail("test@rudderstack.com")
                .putCreatedAt("2020-09-09T09:00:00.000Z")
                .putPhone("9876543210")
                .putDescription("Test User"),
                null
            )
        }
    }
}
