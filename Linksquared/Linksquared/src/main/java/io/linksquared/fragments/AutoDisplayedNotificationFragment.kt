package io.linksquared.fragments

import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import io.linksquared.LinksquaredNotificationsListener
import io.linksquared.R
import io.linksquared.databinding.FragmentAutoDisplayedNotificationBinding
import io.linksquared.model.notifications.Notification
import io.linksquared.service.LinksquaredService
import io.linksquared.viewmodels.AutoDisplayedNotificationViewModel
import io.linksquared.viewmodels.NotificationsMainViewModel

const val ARG_NOTIFICATION = "notification"

class AutoDisplayedNotificationFragment(val linksquaredService: LinksquaredService) : DialogFragment() {
    private lateinit var binding: FragmentAutoDisplayedNotificationBinding
    private val viewModel: AutoDisplayedNotificationViewModel by viewModels()

    private var notification: Notification? = null

    var onDialogDismissed: (()->Unit)? = null

    companion object {

        @JvmStatic
        fun newInstance(notification: Notification, linksquaredService: LinksquaredService) =
            AutoDisplayedNotificationFragment(linksquaredService).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTIFICATION, notification)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_NOTIFICATION, Notification::class.java)
            } else {
                it.getParcelable(ARG_NOTIFICATION)
            }
        }

        setStyle(STYLE_NO_TITLE, R.style.LinksquaredFullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAutoDisplayedNotificationBinding.inflate(inflater, container, false)

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

    private fun setup() {
        viewModel.linksquaredService = linksquaredService

        binding.closeButton.setOnClickListener {
            dismiss()
            onDialogDismissed?.invoke()
        }

        // Configure the WebView settings
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (notification?.read == false) {
                    notification?.accessURL?.let {
                        if (url == "https://$it") {
                            viewModel.markAsRead(notification!!)
                        }
                    }
                }
            }
        }
        val webSettings: WebSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true  // Enable JavaScript if needed

        binding.webView.setBackgroundColor(Color.TRANSPARENT)

        // Load a URL in the WebView
        notification?.accessURL?.let {
            binding.webView.loadUrl("https://$it")
        }
    }

}