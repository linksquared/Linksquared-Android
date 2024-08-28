package io.linksquared.settings

import io.linksquared.Linksquared
import io.linksquared.model.DebugLogger
import io.linksquared.model.LogLevel

class LinksquaredSettings {
    var debugLevel: LogLevel = LogLevel.ERROR
        set(value) {
            field = value
            DebugLogger.instance.logLevel = debugLevel
        }
    var useTestEnvironment: Boolean = false
    var sdkEnabled: Boolean = true

}