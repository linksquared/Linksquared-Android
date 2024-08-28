package io.linksquared.example

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.linksquared.Linksquared
import io.linksquared.LinksquaredDeeplinkListener
import io.linksquared.example.ui.theme.LinksquaredTestAppTheme
import io.linksquared.utils.flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinksquaredTestAppTheme {
                CenteredTextViewAndButton(viewModel)
            }
        }

        Linksquared.setOnDeeplinkReceivedListener(this) { link, payload ->
            val message = "Got link from listener: $link payload: $payload"
            Log.d("MainActivity", message)
            viewModel.updateState(message)
        }

        lifecycleScope.launchWhenStarted {
            Linksquared.Companion::openedLinkDetails.flow.collect { deeplinkDetails ->
                val message = "Got link from flow: ${deeplinkDetails?.link} payload: ${deeplinkDetails?.data}"
                Log.d("MainActivity", message)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Linksquared.onStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        Linksquared.onNewIntent(intent)
    }
}

@Composable
fun CenteredTextViewAndButton(viewModel: MainViewModel) {
    // State for the text content
    val generatedLinkState = remember { mutableStateOf("") }
    val incomingLinkState by viewModel::incomingLinkState
    val context = LocalContext.current
    val activity = context as? MainActivity
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = generatedLinkState.value,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(onClick = {
//                Linksquared.generateLink(title = "Test title",
//                    subtitle = "Test subtitle",
//                    imageURL = null,
//                    data = mapOf("param1" to "Test value"),
//                    tags = null,
//                    lifecycleOwner = activity,
//                    listener = { link, error ->
//                        link?.let { link ->
//                            generatedLinkState.value = link
//                        }
//                        error?.let { error ->
//                            generatedLinkState.value = error.toString()
//                        }
//                    })

                coroutineScope.launch {
                    val link = Linksquared.generateLink(title = "Test title",
                        subtitle = "Test subtitle",
                        imageURL = null,
                        data = mapOf("param1" to "Test value"),
                        tags = null)
                    generatedLinkState.value = link
                }
            }) {
                Text(text = "Generate link")
            }

            Text(
                text = incomingLinkState,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LinksquaredTestAppTheme {
        CenteredTextViewAndButton(viewModel = MainViewModel())
    }
}