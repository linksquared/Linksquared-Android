package io.linksquared.integration_example

import android.app.Application
import io.linksquared.Linksquared

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        val API_LINKSQUARED_KEY = "emotii_57b6850cca700b709175bbe039c615418b195efb69943a6d19480570a313bce0"
        Linksquared.configure(this, API_LINKSQUARED_KEY)
    }

}