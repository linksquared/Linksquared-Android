package io.linksquared.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

class AuthenticationResponse(
    @SerializedName("linksquared")
    val linksquaredId: String,
    @SerializedName("uri_scheme")
    val uriScheme: String,
    @SerializedName("sdk_identifier")
    val sdkIdentifier: String?,
    @SerializedName("sdk_attributes")
    val sdkAttributes: Map<String, Object>?
)  {
}