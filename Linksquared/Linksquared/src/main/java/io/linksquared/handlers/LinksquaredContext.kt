package io.linksquared.handlers

import android.content.Context
import io.linksquared.settings.LinksquaredSettings
import io.linksquared.utils.AppDetailsHelper
import io.linksquared.utils.WebViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant

class LinksquaredContext {
    @OptIn(ExperimentalCoroutinesApi::class)
    val serialDispatcher = Dispatchers.IO.limitedParallelism(1)
    val settings = LinksquaredSettings()
    var linksquaredId: String? = null
    var identifier: String? = null
    var attributes: Map<String, Any>? = null
    var lastSeen: Instant? = null

    fun getAppDetails(context: Context): AppDetailsHelper = AppDetailsHelper(context)
    fun getUserAgent(context: Context): String = WebViewUtils.getUserAgent(context)
}