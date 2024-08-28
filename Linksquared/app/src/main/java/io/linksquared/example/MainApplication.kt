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
        val API_KEY = "demos_10476ea9e132ef17e3971be01e23195ac4f3eb4d30f58bff0a9794945c7e02d7"
        Linksquared.configure(this, API_KEY)
        Linksquared.useTestEnvironment = true

        //Optionally, you can adjust the debug level for logging:
        Linksquared.setDebug(LogLevel.INFO)

        Linksquared.identifier = "1234"
        Linksquared.attributes = mapOf("param1" to "value1", "param2" to 123, "param3" to true)
    }

}