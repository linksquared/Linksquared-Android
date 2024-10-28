package io.linksquared.handlers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.gson.Gson
import io.linksquared.LinksquaredLinkGenerationListener
import io.linksquared.model.DebugLogger
import io.linksquared.model.DeeplinkDetails
import io.linksquared.model.Event
import io.linksquared.model.EventType
import io.linksquared.model.GenerateLinkRequest
import io.linksquared.model.GenerateLinkResponse
import io.linksquared.model.LogLevel
import io.linksquared.model.exceptions.LinksquaredErrorCode
import io.linksquared.model.exceptions.LinksquaredException
import io.linksquared.service.LinksquaredService
import io.linksquared.utils.AppDetailsHelper
import io.linksquared.utils.LSResult
import io.linksquared.utils.WebViewUtils
import io.linksquared.utils.hasURISchemesConfigured
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Serializable
import java.lang.ref.WeakReference
import java.net.URLDecoder
import java.time.Instant
import kotlin.coroutines.resumeWithException

class LinksquaredManager(val context: Context, val application: Application, val linksquaredContext: LinksquaredContext, apiKey: String) {
    private val linksquaredService = LinksquaredService(context = context, apiKey = apiKey, linksquaredContext = linksquaredContext)
    private val appDetails = linksquaredContext.getAppDetails(context = context)
    private val eventsManager = EventsManager(context = context, apiKey = apiKey, linksquaredContext = linksquaredContext)

    private var lastItentHandledReference: WeakReference<Intent>? = null
    /// Stores if attributes needs to be updated after auth
    private var shouldUpdateAttributes = false

    /// A flag indicating whether the user is authenticated with the Linksquared backend.
    var authenticated = false

    var identifier: String?
        get() = linksquaredContext.identifier
        set(value) {
            linksquaredContext.identifier = value
            updateAttributesIfNeeded()
        }

    var pushToken: String?
        get() = linksquaredContext.pushToken
        set(value) {
            linksquaredContext.pushToken = value
            updateAttributesIfNeeded()
        }


    var attributes: Map<String, Any>?
        get() = linksquaredContext.attributes
        set(value) {
            linksquaredContext.attributes = value
            updateAttributesIfNeeded()
        }

    suspend fun onAppForegrounded() {
        eventsManager.onAppForegrounded()
    }

    fun onAppBackgrounded() {
        eventsManager.onAppBackgrounded()
    }

    fun setEnabled(enabled: Boolean) {
        DebugLogger.instance.log(LogLevel.INFO, "SDK setEnabled to: $enabled")
    }

    private suspend fun getDataForDevice(link: String? = null): DeeplinkDetails? {
        eventsManager.setLinkToNewFutureActions(link)

        val appDetails = AppDetailsHelper(context).toAppDetails()
        appDetails.url = link
        val result = if (link == null) linksquaredService.payloadFor(appDetails) else linksquaredService.payloadWithLinkFor(appDetails)
        when (result) {
            is LSResult.Success -> {
                eventsManager.setLinkToNewFutureActions(result.data.link)
                return result.data
            }
            is LSResult.Error -> {
                DebugLogger.instance.log(LogLevel.ERROR, "Error occurred while trying to resolve the deeplink. ${result.exception.message}")
                return null
            }
        }
    }

    suspend fun authenticate(): Boolean {
        if (!context.hasURISchemesConfigured()) {
            DebugLogger.instance.log(LogLevel.INFO, "URI schemes are not configured. Deep linking won't work!")
            return false
        }

        val appDetails = appDetails.toAppDetails()

        val deviceResult = linksquaredService.getDeviceFor(appDetails.deviceID)
        when (deviceResult) {
            is LSResult.Success -> {
                linksquaredContext.lastSeen = deviceResult.data.lastSeen
            }
            is LSResult.Error -> {}
        }

        val result = linksquaredService.authenticate(appDetails = appDetails)
        when (result) {
            is LSResult.Success -> {
                authenticated = true
                linksquaredContext.linksquaredId = result.data.linksquaredId

                // Update context attributes if needed
                if (shouldUpdateAttributes) {
                    updateAttributesIfNeeded()
                } else {
                    linksquaredContext.identifier = result.data.sdkIdentifier
                    linksquaredContext.attributes = result.data.sdkAttributes
                }

                eventsManager.logAppLaunchEvents()

                return true
            }
            is LSResult.Error -> {
                authenticated = true
                DebugLogger.instance.log(LogLevel.ERROR, "Failed to authenticate the app.")

                return false
            }
        }
    }

    suspend fun generateLink(title: String?, subtitle: String?, imageURL: String?, data: Map<String, Serializable>?, tags: List<String>?): LSResult<GenerateLinkResponse> {
        if (!linksquaredContext.settings.sdkEnabled) {
            DebugLogger.instance.log(LogLevel.ERROR, "The SDK is not enabled. Links cannot be generated.")
            return LSResult.Error(java.io.IOException("The SDK is not enabled. Links cannot be generated."))
        }
        if (!authenticated) {
            DebugLogger.instance.log(LogLevel.ERROR, "SDK is not ready for usage yet.")
            return LSResult.Error(java.io.IOException("SDK is not ready for usage yet."))
        }

        return linksquaredService.generateLink(title = title,
            subtitle = subtitle,
            imageURL = imageURL,
            data = data,
            tags = tags)
    }

    fun start() {
        // Implementation for starting the LinksquaredManager, if needed.
    }

    suspend fun handleIntent(intent: Intent): DeeplinkDetails? {
        if (!linksquaredContext.settings.sdkEnabled) {
            DebugLogger.instance.log(LogLevel.ERROR, "The SDK is not enabled. Links cannot be generated.")
            return null
        }
        if (!authenticated) {
            DebugLogger.instance.log(LogLevel.ERROR, "SDK is not ready for usage yet.")
            return null
        }

        // avoid handling same link multiple times
        if (intent === lastItentHandledReference?.get()) {
            DebugLogger.instance.log(LogLevel.INFO, "No link provided, trying to infer it.")
            return getDataForDevice(null)
        }
        lastItentHandledReference = WeakReference(intent)

        intent.data?.toString()?.let { link ->
            return getDataForDevice(intent.data?.toString())
        } ?: run {
            getInstallReferrer()?.let {
                val referrerData = decodeReferrer(it)

                return getDataForDevice(referrerData["linksquared_link"])
            }

            return getDataForDevice(null)
        }
    }

    suspend fun getInstallReferrer(): String? {
        return suspendCancellableCoroutine { continuation ->
            val referrerClient = InstallReferrerClient.newBuilder(context).build()

            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            try {
                                val referrerDetails = referrerClient.installReferrer
                                val referrerUrl = referrerDetails.installReferrer
                                continuation.resume(referrerUrl, null)
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            } finally {
                                referrerClient.endConnection()
                            }
                        }
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            continuation.resume(null, null)
                            referrerClient.endConnection()
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // Try to reconnect if needed, or handle the disconnection
                    // In this example, we simply end the connection and return null
                    if (continuation.isActive) {
                        continuation.resume(null, null)
                    }
                }
            })

            // Ensure that if the coroutine is cancelled, the connection is ended
            continuation.invokeOnCancellation {
                referrerClient.endConnection()
            }
        }
    }

    private fun decodeReferrer(referrer: String): Map<String, String> {
        return referrer
            .split("&")  // Split by '&' to separate the key-value pairs
            .map {
                val (key, value) = it.split("=")  // Split each pair by '=' to separate key and value
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")  // Decode both key and value
            }
            .toMap()  // Convert the list of pairs to a map
    }

    private fun updateAttributesIfNeeded() {
        if (!authenticated) {
            shouldUpdateAttributes = true
            return
        }

        GlobalScope.launch {
            val result = linksquaredService.updateAttributes(identifier = identifier, attributes = attributes, pushToken = pushToken)
            when (result) {
                is LSResult.Success -> {
                    shouldUpdateAttributes = false
                }
                is LSResult.Error -> {}
            }
        }
    }
}