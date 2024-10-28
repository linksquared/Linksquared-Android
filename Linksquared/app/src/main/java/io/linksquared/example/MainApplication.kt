package io.linksquared.example

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.linksquared.Linksquared
import io.linksquared.model.LogLevel

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // TODO: Replace with your own API Key
        val API_KEY = "testap_6d980b2dd52c8665c48b6be13d0dafb9672327ae9d3436df8a191e6acebe6096"
        Linksquared.configure(this, API_KEY)
        //Linksquared.useTestEnvironment = true

        //Optionally, you can adjust the debug level for logging:
        Linksquared.setDebug(LogLevel.INFO)

        Linksquared.identifier = "1234"
        Linksquared.attributes = mapOf("param1" to "value1", "param2" to 123, "param3" to true)
    }

}