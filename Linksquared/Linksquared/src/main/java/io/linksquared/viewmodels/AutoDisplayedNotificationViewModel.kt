package io.linksquared.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.linksquared.model.notifications.Notification
import io.linksquared.service.LinksquaredService
import io.linksquared.utils.LSResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutoDisplayedNotificationViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var linksquaredService: LinksquaredService

    fun markAsRead(notification: Notification) {
        viewModelScope.launch {
            val result = linksquaredService.markNotificationAsRead(notificationId = notification.id)
        }
    }

}