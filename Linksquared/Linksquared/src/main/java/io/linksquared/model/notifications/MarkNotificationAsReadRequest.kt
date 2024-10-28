package io.linksquared.model.notifications

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class MarkNotificationAsReadRequest (
    @SerializedName("id")
    var notificationId: Int
) : Parcelable {
}