package io.linksquared.api

import io.linksquared.model.AppDetails
import io.linksquared.model.AuthenticationResponse
import io.linksquared.model.DeeplinkDetails
import io.linksquared.model.Event
import io.linksquared.model.GenerateLinkRequest
import io.linksquared.model.GenerateLinkResponse
import io.linksquared.model.GetDeviceResponse
import io.linksquared.model.UpdateAttributesRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface LinksquaredApi {

    @POST("data_for_device")
    suspend fun payloadFor(@Body request: AppDetails): Response<DeeplinkDetails>

    @POST("data_for_device_and_url")
    suspend fun payloadWithLinkFor(@Body request: AppDetails): Response<DeeplinkDetails>

    @POST("authenticate")
    suspend fun authenticate(@Body request: AppDetails): Response<AuthenticationResponse>

    @POST("create_link")
    suspend fun generateLink(@Body request: GenerateLinkRequest): Response<GenerateLinkResponse>

    @POST("event")
    suspend fun addEvent(@Body request: Event): Response<Unit>

    @POST("visitor_attributes")
    suspend fun updateAttributes(@Body request: UpdateAttributesRequest): Response<Unit>

    @GET("device_for_vendor_id")
    suspend fun getDeviceFor(@Query("vendor_id") page: String): Response<GetDeviceResponse>

}