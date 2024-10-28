package io.linksquared.service

import android.content.Context
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.linksquared.BuildConfig
import io.linksquared.api.LinksquaredApi
import io.linksquared.handlers.LinksquaredContext
import io.linksquared.model.AppDetails
import io.linksquared.model.AuthenticationResponse
import io.linksquared.model.DebugLogger
import io.linksquared.model.DeeplinkDetails
import io.linksquared.model.ErrorMessage
import io.linksquared.model.Event
import io.linksquared.model.GenerateLinkRequest
import io.linksquared.model.GenerateLinkResponse
import io.linksquared.model.GetDeviceResponse
import io.linksquared.model.LogLevel
import io.linksquared.model.UpdateAttributesRequest
import io.linksquared.model.notifications.MarkNotificationAsReadRequest
import io.linksquared.model.notifications.NotificationsRequest
import io.linksquared.model.notifications.NotificationsResponse
import io.linksquared.model.notifications.NumberOfUnreadNotificationsResponse
import io.linksquared.settings.LinksquaredSettings
import io.linksquared.utils.LSJsonDateTypeAdapterFactory
import io.linksquared.utils.LSResult
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import java.io.Serializable
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

val nullOnEmptyConverterFactory = object : Converter.Factory() {
    fun converterFactory() = this
    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit) = object :
        Converter<ResponseBody, Any?> {
        val nextResponseBodyConverter = retrofit.nextResponseBodyConverter<Any?>(converterFactory(), type, annotations)
        override fun convert(value: ResponseBody) = if (value.contentLength() != 0L) nextResponseBodyConverter.convert(value) else null
    }
}

// Custom Interceptor to add headers to every request
class HeaderInterceptor(private val headers: ()->Map<String, String>) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val requestBuilder: Request.Builder = originalRequest.newBuilder()

        // Add each custom header to the request
        for ((key, value) in headers.invoke()) {
            requestBuilder.addHeader(key, value)
        }

        val request: Request = requestBuilder.build()
        return chain.proceed(request)
    }
}

class LinksquaredService(val context: Context, val apiKey: String, val linksquaredContext: LinksquaredContext) {
    private val linksquaredApi: LinksquaredApi
    private val appDetails = linksquaredContext.getAppDetails(context = context)
    private val userAgent = linksquaredContext.getUserAgent(context = context)
    private val gson = GsonBuilder().setLenient().registerTypeAdapterFactory(
        LSJsonDateTypeAdapterFactory()
    ).create()
    private val accessKey: String
        get() {
            if (linksquaredContext.settings.useTestEnvironment) {
                return "test_$apiKey"
            }

            return apiKey
        }

    companion object {
        val EAGER_RETRY_COUNT: Long = 15
        val EAGER_RETRY_FALLBACK_TIME: Long = 5000
        val RETRY_FALLBACK_TIME: Long = 60000
    }

    init {
        linksquaredApi = getRetrofit().create(LinksquaredApi::class.java)
    }

    suspend fun payloadFor(@Body request: AppDetails): LSResult<DeeplinkDetails> {
        DebugLogger.instance.log(LogLevel.INFO, "Fetching payload for device")

        var retryCount = 0
        while (true) {
            try {
                val response = linksquaredApi.payloadFor(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Fetching payload for device - Received payload")
                        return LSResult.Success(it)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Fetching payload - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed to fetch the payload. Reason: $response"))
                }
            } catch (e: Exception) {}

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun payloadWithLinkFor(@Body request: AppDetails): LSResult<DeeplinkDetails> {
        DebugLogger.instance.log(LogLevel.INFO, "Fetching payload for device")

        var retryCount = 0
        while (true) {
            try {
                val response = linksquaredApi.payloadWithLinkFor(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Fetching payload for device - Received payload")
                        return LSResult.Success(it)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Fetching payload - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed to fetch the payload. ${error.error}"))
                }
            } catch (e: Exception) {}

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    /// Authenticates the app.
    ///
    /// - Parameters:
    ///   - appDetails: Details of the app.
    suspend fun authenticate(appDetails: AppDetails): LSResult<AuthenticationResponse> {
        DebugLogger.instance.log(LogLevel.INFO, "Authenticate")

        var retryCount = 0
        while (true) {
            try {
                val response = linksquaredApi.authenticate(appDetails)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Authenticate - Success")
                        return LSResult.Success(it)
                    }
                }


                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Authenticate - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed to authenticate. ${error.error}"))
                }
            } catch (e: Exception) {}

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun generateLink(title: String?, subtitle: String?, imageURL: String?, data: Map<String, Serializable>?, tags: List<String>?): LSResult<GenerateLinkResponse> {
        try {
            val stringData = gson.toJson(data)
            val stringTags = gson.toJson(tags)
            val request = GenerateLinkRequest(title = title,
                subtitle = subtitle,
                imageUrl =  imageURL,
                data = stringData,
                tags = stringTags)
            val response = linksquaredApi.generateLink(request)
            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    return LSResult.Success(it)
                }
            }

            val error = gson.fromJson(response.errorBody()!!.string(), ErrorMessage::class.java)

            DebugLogger.instance.log(LogLevel.INFO, "Generate link - Failed. ${error.error}")

            return LSResult.Error(java.io.IOException("Failed to generate link. ${error.error}"))
        } catch (e: Exception) {
            return LSResult.Error(e)
        }
    }

    /// Adds an event.
    ///
    /// - Parameters:
    ///   - event: The event to add.
    ///   - completion: A closure indicating the success or failure of the operation.
    suspend fun addEvent(event: Event): LSResult<Boolean> {
        try {
            DebugLogger.instance.log(LogLevel.INFO, "Add event - $event")
            val response = linksquaredApi.addEvent(event)
            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    DebugLogger.instance.log(LogLevel.INFO, "Add event - Successful - $event")

                    return LSResult.Success(true)
                }
            }

            val error = gson.fromJson(response.errorBody()!!.string(), ErrorMessage::class.java)

            DebugLogger.instance.log(LogLevel.INFO, "Add event - Failed - $event ${error.error}")

            return LSResult.Error(java.io.IOException("Failed to log the event. ${error.error}"))
        } catch (e: Exception) {
            return LSResult.Error(e)
        }
    }

    suspend fun updateAttributes(identifier: String? = null, attributes: Map<String, Any>? = null, pushToken: String? = null): LSResult<Boolean> {
        DebugLogger.instance.log(LogLevel.INFO, "Set attributes - $identifier $attributes push token: $pushToken")

        var retryCount = 0
        while (true) {
            try {
                val request = UpdateAttributesRequest( sdkIdentifier = identifier,
                    sdkAttributes = attributes,
                    pushToken = pushToken)
                val response = linksquaredApi.updateAttributes(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Set attributes - Successful - $identifier $attributes")

                        return LSResult.Success(true)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Set attributes - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed to set attributes. ${error.error}"))
                }
            } catch (e: Exception) { }

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun getDeviceFor(vendorId: String): LSResult<GetDeviceResponse> {
        DebugLogger.instance.log(LogLevel.INFO, "Getting device last seen")

        var retryCount = 0
        while (true) {
            try {
                val response = linksquaredApi.getDeviceFor(vendorId)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Getting device last seen - Successful")

                        return LSResult.Success(it)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Getting device last seen - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed to get device last seen. ${error.error}"))
                }
            } catch (e: Exception) { }

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun notifications(page: Int): LSResult<NotificationsResponse> {
        DebugLogger.instance.log(LogLevel.INFO, "Getting all the notifications")

        var retryCount = 0
        while (true) {
            try {
                val request = NotificationsRequest(page = page)
                val response = linksquaredApi.notifications(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Getting all the notifications - Successful")

                        return LSResult.Success(it)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Getting all the notifications - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed getting all the notifications. ${error.error}"))
                }
            } catch (e: Exception) { }

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun numberOfUnreadNotifications(): LSResult<NumberOfUnreadNotificationsResponse> {
        DebugLogger.instance.log(LogLevel.INFO, "Get unread messages")

        var retryCount = 0
        while (true) {
            try {
                val response = linksquaredApi.numberOfUnreadNotifications()
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Get unread messages - Successful")

                        return LSResult.Success(it)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Get unread messages - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed get unread messages. ${error.error}"))
                }
            } catch (e: Exception) { }

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun markNotificationAsRead(notificationId: Int): LSResult<Boolean> {
        DebugLogger.instance.log(LogLevel.INFO, "Mark notification as read")

        var retryCount = 0
        while (true) {
            try {
                val request = MarkNotificationAsReadRequest(notificationId = notificationId)
                val response = linksquaredApi.markNotificationAsRead(request = request)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Mark notification as read - Successful")

                        return LSResult.Success(true)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Mark notification as read - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed to mark notification as read. ${error.error}"))
                }
            } catch (e: Exception) { }

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    suspend fun notificationsToDisplayAutomatically(): LSResult<NotificationsResponse> {
        DebugLogger.instance.log(LogLevel.INFO, "Notifications to display automatically")

        var retryCount = 0
        while (true) {
            try {
                val response = linksquaredApi.notificationsToDisplayAutomatically()
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        DebugLogger.instance.log(LogLevel.INFO, "Getting notifications to display automatically - Successful")

                        return LSResult.Success(it)
                    }
                }

                response.errorBody()?.string()?.let { responseString ->
                    val error = gson.fromJson(responseString, ErrorMessage::class.java)
                    DebugLogger.instance.log(LogLevel.INFO, "Getting notifications to display automatically - Failed. ${error.error}")

                    return LSResult.Error(java.io.IOException("Failed getting notifications to display automatically. ${error.error}"))
                }
            } catch (e: Exception) { }

            delay(if (retryCount < EAGER_RETRY_COUNT) EAGER_RETRY_FALLBACK_TIME else RETRY_FALLBACK_TIME)
            retryCount++
        }
    }

    private fun getRetrofit(): Retrofit {
        val gson = GsonBuilder().setLenient().registerTypeAdapterFactory(
            LSJsonDateTypeAdapterFactory()
        ).create()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL)
            .addConverterFactory(nullOnEmptyConverterFactory)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(getOkhttpClient())
            .build()
    }

    private fun getOkhttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(40, TimeUnit.SECONDS)
        builder.readTimeout(40, TimeUnit.SECONDS)
        builder.writeTimeout(40, TimeUnit.SECONDS)
        builder.addInterceptor(HeaderInterceptor { headers() })

        if (BuildConfig.DEBUG) {
            /** add logging interceptor at last Interceptor*/
            builder.addInterceptor(httpLoggingInterceptor.apply {
                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            })
        }

        return builder.build()
    }

    private fun headers(): Map<String, String> {
        val customHeaders = mutableMapOf(
            "PROJECT-KEY" to accessKey,
            "IDENTIFIER" to appDetails.applicationId,
            "PLATFORM" to "android",
            "User-Agent" to userAgent,
        )

        linksquaredContext.linksquaredId?.let {
            customHeaders["LINKSQUARED"] = it
        }

        return customHeaders
    }

}