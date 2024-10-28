package io.linksquared

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.linksquared.handlers.ActivityProvider
import io.linksquared.handlers.LinksquaredContext
import io.linksquared.handlers.LinksquaredManager
import io.linksquared.handlers.NotificationsManager
import io.linksquared.model.DebugLogger
import io.linksquared.model.DeeplinkDetails
import io.linksquared.model.LogLevel
import io.linksquared.model.exceptions.LinksquaredErrorCode
import io.linksquared.model.exceptions.LinksquaredException
import io.linksquared.service.LinksquaredService
import io.linksquared.settings.LinksquaredSettings
import io.linksquared.utils.FlowObservable
import io.linksquared.utils.LSResult
import io.linksquared.utils.flowDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executors

fun interface LinksquaredDeeplinkListener {
    fun onDeeplinkReceived(link:String, data:Map<String, Object>?)
}

fun interface LinksquaredLinkGenerationListener {
    fun onLinkGenerated(link:String?, error: LinksquaredException?)
}

fun interface LinksquaredNotificationsListener {
    fun onAutomaticNotificationClosed(isLast:Boolean)
}

public class Linksquared: ActivityProvider {

    companion object {
        private val instance = Linksquared()

        /// Indicates if the test environment should be used
        var useTestEnvironment: Boolean
            get() = instance.linksquaredContext.settings.useTestEnvironment
            set(value) {
                instance.linksquaredContext.settings.useTestEnvironment = value
                instance.apiKey?.let {
                    checkConfiguration()
                }
            }

        /// Flow to listen for link and data from which the app was opened from.
        /// The value of this param is null if the app was not opened from a link.
        /// The data provided is same as the one from setOnDeeplinkReceivedListener. This is just for convenience when using kotlin coroutines api.
        @FlowObservable
        @get:FlowObservable
        val openedLinkDetails: DeeplinkDetails?
            get() = instance.openedLinkDetails

        /// The identifier for the current user, normally a userID. This will be visible in the linksquared dashboard.
        var identifier: String?
            get() = instance.identifier
            set(value) {
                instance.identifier = value
            }

        /// The push token for the user. This property allows getting and setting the push notification token.
        var pushToken: String?
            get() = instance.pushToken
            set(value) {
                instance.pushToken = value
            }

        /// The attributes for the current user. This will be visible in the linksquared dashboard.
        var attributes: Map<String, Any>?
            get() = instance.attributes
            set(value) {
                instance.attributes = value
            }

        /// Configures Linksquared with the API key from the web console
        fun configure(application: Application, apiKey: String) {
            instance.configure(application, apiKey)
        }

        /// Disables the Linksquared SDK.
        /// - Parameter enabled: The log level to set.
        /// Default is true.
        fun setSDK(enabled: Boolean) {
            instance.setSDK(enabled)
        }

        /// Sets the debug level for the SDK log messages.
        fun setDebug(level: LogLevel) {
            instance.setDebug(level)
        }

        /// Generates a link using kotlin coroutine style.
        ///
        /// - Parameters:
        ///   - title: The title of the link.
        ///   - subtitle: The subtitle of the link.
        ///   - imageURL: The URL of the image associated with the link.
        ///   - data: Additional data for the link.
        ///   - tags: Tags for the link.
        suspend fun generateLink(title: String? = null,
                                 subtitle: String? = null,
                                 imageURL: String? = null,
                                 data: Map<String, Serializable>? = null,
                                 tags: List<String>? = null): String {
            return instance.generateLink(title, subtitle, imageURL, data, tags)
        }

        /// Generates a link.
        ///
        /// - Parameters:
        ///   - title: The title of the link.
        ///   - subtitle: The subtitle of the link.
        ///   - imageURL: The URL of the image associated with the link.
        ///   - data: Additional data for the link.
        ///   - tags: Tags for the link.
        ///   - completion: A closure to be executed after generating the link.
        fun generateLink(title: String? = null,
                         subtitle: String? = null,
                         imageURL: String? = null,
                         data: Map<String, Serializable>? = null,
                         tags: List<String>? = null,
                         lifecycleOwner: LifecycleOwner? = null,
                         listener: LinksquaredLinkGenerationListener) {
            instance.generateLink(title, subtitle, imageURL, data, tags, lifecycleOwner, listener)
        }

        /// This needs to be called on the launcher activity onStart() to allow the SDK to handle incoming links
        fun onStart() {
            instance.onStart()
        }

        /// This needs to be called on the launcher activity onNewIntent() to allow the SDK to handle incoming links
        fun onNewIntent(intent: Intent?) {
            instance.onNewIntent(intent)
        }

        /// Register a listener to receive the link and data from which the app was opened.
        ///
        /// - Parameters:
        ///   - launcherActivity: The launcher activity.
        ///   - listener: A listener to receive the link and data from which the app was opened.
        fun setOnDeeplinkReceivedListener(launcherActivity: Activity, listener: LinksquaredDeeplinkListener) {
            instance.setOnDeeplinkReceivedListener(launcherActivity, listener)
        }

        /// Checks the configuration validity.
        private fun checkConfiguration() {
            instance.checkConfiguration()
        }

        fun setOnAutomaticNotificationsListener(listener: LinksquaredNotificationsListener) {
            instance.setOnAutomaticNotificationsListener(listener = listener)
        }


        fun displayMessagesFragment(onDismissed: (()->Unit)?) {
            instance.displayMessagesFragment(onDismissed)
        }

        suspend fun numberOfUnreadMessages(): Int? {
            return instance.numberOfUnreadMessages()
        }
    }

    var openedLinkDetails: DeeplinkDetails? by flowDelegate(null)

    /// The identifier for the current user, normally a userID. This will be visible in the linksquared dashboard.
    private var identifier: String?
        get() = linksquaredManager?.identifier
        set(value) {
            linksquaredManager?.identifier = value
        }

    /// The push token for the user. This property allows getting and setting the push notification token.
    var pushToken: String?
        get() = linksquaredManager?.pushToken
        set(value) {
            linksquaredManager?.pushToken = value
        }

    /// The attributes for the current user. This will be visible in the linksquared dashboard.
    private var attributes: Map<String, Any>?
        get() = linksquaredManager?.attributes
        set(value) {
            linksquaredManager?.attributes = value
        }

    private var linksquaredManager: LinksquaredManager? = null
    private var notificationsManager: NotificationsManager? = null

    // This is used for linking the SDK to your account
    private var apiKey: String? = null

    private var application: Application? = null

    private var deeplinkListener: LinksquaredDeeplinkListener? = null
    private var linksquaredNotificationsListener: LinksquaredNotificationsListener? = null

    private var launcherActivityReference: WeakReference<Activity>? = null
    private var currentActivityReference: WeakReference<Activity>? = null
        set(value) {
            field = value
            if ((field != null) && (linksquaredManager?.authenticated == true)) {
                notificationsManager?.displayAutomaticNotificationsIfNeeded()
            }
        }

    private var linksquaredContext = LinksquaredContext()

    private var authenticationJob: Job? = null

    private val applicationLifecycleObserver: Application.ActivityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        private var numStarted = 0

        override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {
            currentActivityReference = WeakReference(activity)

            if (numStarted == 0) {
                // App is in foreground
                onAppForegrounded()
            }
            numStarted++
        }
        override fun onActivityResumed(activity: Activity) {
            currentActivityReference = WeakReference(activity)
        }
        override fun onActivityPaused(activity: Activity) {
            if (currentActivityReference?.get() == activity) currentActivityReference = null
        }
        override fun onActivityStopped(activity: Activity) {
            if (currentActivityReference?.get() == activity) currentActivityReference = null

            numStarted--
            if (numStarted == 0) {
                // App is in background
                onAppBackgrounded()
            }
        }
        override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivityReference?.get() == activity) currentActivityReference = null
        }

        private fun onAppForegrounded() {
            // App moved to the foreground
            DebugLogger.instance.log(LogLevel.INFO, "App is in the foreground")

            GlobalScope.launch(linksquaredContext.serialDispatcher) {
                authenticationJob?.join()
                linksquaredManager?.onAppForegrounded()
            }
        }

        private fun onAppBackgrounded() {
            // App moved to the background
            DebugLogger.instance.log(LogLevel.INFO, "App is in the background")
            linksquaredManager?.onAppBackgrounded()
        }
    }

    fun configure(application: Application, apiKey: String) {
        this.apiKey = apiKey
        this.application = application

        linksquaredManager = LinksquaredManager(context = application.applicationContext,
            application = application,
            linksquaredContext = linksquaredContext,
            apiKey = apiKey)

        notificationsManager = NotificationsManager(context = application.applicationContext,
            linksquaredContext = linksquaredContext,
            apiKey = apiKey,
            activityProvider = this)

        checkConfiguration()
        application.registerActivityLifecycleCallbacks(applicationLifecycleObserver)
    }

    fun setSDK(enabled: Boolean) {
        linksquaredContext.settings.sdkEnabled = enabled
        linksquaredManager?.setEnabled(enabled)
    }

    fun setDebug(level: LogLevel) {
        linksquaredContext.settings.debugLevel = level
    }

    suspend fun generateLink(title: String? = null,
                     subtitle: String? = null,
                     imageURL: String? = null,
                     data: Map<String, Serializable>? = null,
                     tags: List<String>? = null): String {
        var link: String? = null
        linksquaredManager?.let { manager ->
            withContext(linksquaredContext.serialDispatcher) {
                authenticationJob?.join()
                val result = manager.generateLink(
                    title = title,
                    subtitle = subtitle,
                    imageURL = imageURL,
                    data = data,
                    tags = tags
                )

                withContext(Dispatchers.Main) {
                    when (result) {
                        is LSResult.Success -> {
                            link = result.data.link
                        }
                        is LSResult.Error -> {
                            throw LinksquaredException(result.exception.message, LinksquaredErrorCode.LINK_GENERATION_ERROR)
                        }
                    }
                }
            }
        } ?: run {
            DebugLogger.instance.log(LogLevel.ERROR,"The SDK is not properly configured. Call Linksquared.configure(application: Application, apiKey: String) first.")
            throw LinksquaredException("The sdk is not initialized. Initialize the sdk before generating links.", LinksquaredErrorCode.SDK_NOT_INITIALIZED)
        }

        link?.let { link ->
            return link
        } ?: run {
            throw LinksquaredException("Failed to generate the link.", LinksquaredErrorCode.LINK_GENERATION_ERROR)
        }
    }

    fun generateLink(title: String? = null,
                     subtitle: String? = null,
                     imageURL: String? = null,
                     data: Map<String, Serializable>? = null,
                     tags: List<String>? = null,
                     lifecycleOwner: LifecycleOwner? = null,
                     listener: LinksquaredLinkGenerationListener) {
        linksquaredManager?.let { manager ->
            if (lifecycleOwner == null) {
                DebugLogger.instance.log(LogLevel.INFO,"LifecycleScope not provided, will use global scope.")
            }

            val scope = (lifecycleOwner?.lifecycleScope ?: GlobalScope)
            scope.launch(linksquaredContext.serialDispatcher) {
                authenticationJob?.join()
                val result = manager.generateLink(
                    title = title,
                    subtitle = subtitle,
                    imageURL = imageURL,
                    data = data,
                    tags = tags
                )

                withContext(Dispatchers.Main) {
                    when (result) {
                        is LSResult.Success -> {
                            listener.onLinkGenerated(result.data.link, null)
                        }
                        is LSResult.Error -> {
                            listener.onLinkGenerated(null, LinksquaredException(result.exception.message, LinksquaredErrorCode.LINK_GENERATION_ERROR))
                        }
                    }
                }
            }
        } ?: run {
            DebugLogger.instance.log(LogLevel.ERROR,"The SDK is not properly configured. Call Linksquared.configure(application: Application, apiKey: String) first.")
        }
    }

    fun onStart() {
        handleIntent(launcherActivityReference?.get()?.intent)
    }

    fun onNewIntent(intent: Intent?) {
        handleIntent(intent)
    }

    fun setOnDeeplinkReceivedListener(launcherActivity: Activity, listener: LinksquaredDeeplinkListener) {
        launcherActivityReference = WeakReference(launcherActivity)
        deeplinkListener = listener
    }

    fun setOnAutomaticNotificationsListener(listener: LinksquaredNotificationsListener) {
        linksquaredNotificationsListener = listener
    }

    fun displayMessagesFragment(onDismissed: (()->Unit)?): Boolean {
        notificationsManager?.let { notificationsManager ->
            return notificationsManager.displayNotificationsViewController(onDismissed = onDismissed)
        } ?: run {
            return false
        }
    }

    suspend fun numberOfUnreadMessages(): Int? {
        val maxRetry = 20
        var count = 0
        while (linksquaredManager?.authenticated != true && count < maxRetry) {
            delay(200L * count)
            count += 1
        }

        return notificationsManager?.numberOfUnreadNotifications()
    }

    private fun checkConfiguration() {
        instance.apiKey?.let { apiKey ->
            linksquaredManager?.let { manager ->
                authenticationJob = GlobalScope.launch(linksquaredContext.serialDispatcher) {
                    val response = manager.authenticate()
                    if (response) {
                        manager.start()
                        notificationsManager?.displayAutomaticNotificationsIfNeeded()
                    }
                }
            } ?: run {
                DebugLogger.instance.log(LogLevel.ERROR,"The SDK is not properly configured. Call Linksquared.configure(application: Application, apiKey: String) first.")
            }
        } ?: run {
            DebugLogger.instance.log(LogLevel.ERROR,"API Key is invalid. Make sure you've used the right value from the Web interface.")
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let { intent ->
            linksquaredManager?.let { linksquaredManager ->
                (launcherActivityReference?.get() as? LifecycleOwner)?.let { lifecycleOwner ->
                    lifecycleOwner.lifecycleScope.launch(linksquaredContext.serialDispatcher) {
                        authenticationJob?.join()
                            val result = linksquaredManager.handleIntent(intent)
                            result?.let { deeplinkDetails ->
                                deeplinkDetails.link?.let { link ->
                                    withContext(Dispatchers.Main) {
                                        openedLinkDetails = deeplinkDetails
                                        deeplinkListener?.onDeeplinkReceived(link, deeplinkDetails.data)
                                    }
                                } ?: run {
                                    DebugLogger.instance.log(LogLevel.INFO,"App NOT opened from deeplink.")
                                }
                            }
                    }
                } ?: run {
                    DebugLogger.instance.log(LogLevel.ERROR,"The SDK is not properly configured. Call Linksquared.configure(application: Application, apiKey: String) first.")
                }
            } ?: run {
                DebugLogger.instance.log(LogLevel.ERROR,"The SDK is not properly configured. Call Linksquared.configure(application: Application, apiKey: String) first.")
            }
        }
    }

    override fun requireActivity(): Activity? {
        return currentActivityReference?.get()
    }

    override fun requireNotificationsListener(): LinksquaredNotificationsListener? {
        return linksquaredNotificationsListener
    }

}