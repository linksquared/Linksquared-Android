package io.linksquared.handlers

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.linksquared.LinksquaredNotificationsListener
import io.linksquared.fragments.AutoDisplayedNotificationFragment
import io.linksquared.fragments.NotificationsMainFragment
import io.linksquared.model.exceptions.LinksquaredErrorCode
import io.linksquared.model.exceptions.LinksquaredException
import io.linksquared.model.notifications.Notification
import io.linksquared.service.LinksquaredService
import io.linksquared.utils.LSResult
import kotlinx.coroutines.launch
import java.time.Instant

interface ActivityProvider {
    fun requireActivity(): Activity?
    fun requireNotificationsListener(): LinksquaredNotificationsListener?
}

class NotificationsManager(val context: Context, val linksquaredContext: LinksquaredContext, apiKey: String, val activityProvider: ActivityProvider) {
    private val linksquaredService = LinksquaredService(context = context, apiKey = apiKey, linksquaredContext = linksquaredContext)

    fun displayAutomaticNotificationsIfNeeded() {
        val activity = activityProvider.requireActivity() as? FragmentActivity
        activity?.lifecycleScope?.launch {
            val result = linksquaredService.notificationsToDisplayAutomatically()
            when (result) {
                is LSResult.Success -> {
                    for (notification in result.data.notifications) {
                        displayAutomaticNotificationFor(notification = notification)
                    }
                }
                is LSResult.Error -> {}
            }
        }

//        val activity = activityProvider.requireActivity() as? FragmentActivity
//        activity?.lifecycleScope?.launch {
//            val notification = Notification(
//                123,
//                "Test not",
//                Instant.now(),
//                "Test sub",
//                autoDisplay = true,
//                "https:google.ro",
//                read = false
//            )
//            displayAutomaticNotificationFor(notification)
//
//            val notification2 = Notification(
//                1234,
//                "Test not",
//                Instant.now(),
//                "Test sub",
//                autoDisplay = true,
//                "https:google.ro",
//                read = false
//            )
//            displayAutomaticNotificationFor(notification2)
//        }
    }

    fun displayNotificationsViewController(onDismissed: (()->Unit)?): Boolean {
        val activity = activityProvider.requireActivity() as? FragmentActivity
        activity?.let { activity ->
            val count = activity.supportFragmentManager.fragments.filterIsInstance<NotificationsMainFragment>().count { it.isVisible }
            if (count != 0) {
                return true
            }

            val dialogFragment = NotificationsMainFragment(linksquaredService = linksquaredService)
            dialogFragment.onDialogDismissed = onDismissed
            dialogFragment.show(activity.supportFragmentManager, "NotificationsMainFragment")
            activity.supportFragmentManager.executePendingTransactions()

            return true
        } ?: run {
            return false
        }
    }

    suspend fun numberOfUnreadNotifications(): Int? {
        val result = linksquaredService.numberOfUnreadNotifications()
        when (result) {
            is LSResult.Success -> {
                return result.data.numberOfUnreadNotifications
            }
            is LSResult.Error -> {
                return null
            }
        }
    }

    private fun displayAutomaticNotificationFor(notification: Notification) {
        val activity = activityProvider.requireActivity() as? FragmentActivity
        activity?.let { activity ->
            val alreadyShownFragment = activity.supportFragmentManager.findFragmentByTag(notification.id.toString())
            if (alreadyShownFragment == null) {
                val dialogFragment = AutoDisplayedNotificationFragment.newInstance(notification = notification, linksquaredService = linksquaredService)
                dialogFragment.onDialogDismissed = {
                    val count = activity.supportFragmentManager.fragments.filterIsInstance<AutoDisplayedNotificationFragment>().count { it.isVisible }
                    activityProvider.requireNotificationsListener()?.onAutomaticNotificationClosed(count == 0)
                }
                dialogFragment.show(activity.supportFragmentManager, notification.id.toString())
                activity.supportFragmentManager.executePendingTransactions()
            }
        }
    }

}