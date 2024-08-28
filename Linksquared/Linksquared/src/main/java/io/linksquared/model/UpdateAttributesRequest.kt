package io.linksquared.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


class UpdateAttributesRequest(
    @SerializedName("sdk_identifier")
    val sdkIdentifier: String?,
    @SerializedName("sdk_attributes")
    val sdkAttributes: Map<String, Any>?
)  {
}
