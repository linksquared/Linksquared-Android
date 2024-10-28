package io.linksquared.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.get
import io.linksquared.R
import io.linksquared.adapters.NotificationsListItem
import io.linksquared.databinding.FragmentAutoDisplayedNotificationBinding
import io.linksquared.databinding.FragmentNotificationsMainBinding
import io.linksquared.model.notifications.Notification
import io.linksquared.service.LinksquaredService
import io.linksquared.viewmodels.NotificationsMainViewModel

class NotificationsMainFragment(val linksquaredService: LinksquaredService) : DialogFragment() {
    private lateinit var binding: FragmentNotificationsMainBinding
    private val viewModel: NotificationsMainViewModel by viewModels()

    var onDialogDismissed: (()->Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.LinksquaredFullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNotificationsMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setup()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        onDialogDismissed?.invoke()
    }

    @SuppressLint("RestrictedApi")
    private fun setup() {
        viewModel.linksquaredService = linksquaredService

        binding.backButton.setOnClickListener {
            val navHost = childFragmentManager.findFragmentById(R.id.notificationsHostFragment) as NavHostFragment
            navHost.navController.navigateUp()
        }
        binding.closeButton.setOnClickListener {
            dismiss()
            onDialogDismissed?.invoke()
        }

        val navHost = childFragmentManager.findFragmentById(R.id.notificationsHostFragment) as NavHostFragment
        navHost.navController.addOnDestinationChangedListener { controller, destination, _ ->
            val hasBackEntries = navHost.navController.currentBackStack.value.size != 1
            if (hasBackEntries && (destination == navHost.navController.graph[R.id.notificationDetailsFragment])) {
                binding.backButton.visibility = View.VISIBLE
            } else {
                binding.backButton.visibility = View.GONE
            }
        }
    }

}