# Linksquared Android SDK

[Linksquared](https://linksquared.io) is a powerful SDK that enables deep linking and universal linking within your Android applications. This document serves as a guide to integrate and utilize Linksquared seamlessly within your project.

<br />

## Installation

### Gradle

Linksquared is available as a Gradle artifact, add the below dependency to your `build.gradle`

```
implementation("io.linksquared:Linksquared:1.0.3")
```

## Configuration

To configure the Linksquared SDK within your application, follow these steps:

1. Initialize the SDK with your API key (usually in your `Application` class):

```kotlin
override fun onCreate() {
    super.onCreate()

    Linksquared.configure(this, "your-api-key")
}
```

2. In your **launcher activity** add the code for handling incoming links:

```kotlin
override fun onStart() {
    super.onStart()

    Linksquared.onStart()
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)

    Linksquared.onNewIntent(intent)
}
```

3. Add intent filters to your **launcher activity** in the `AndroidManifest.xml` file to register your app for opening the linksquared links:

```xml
<intent-filter>
    <data android:scheme="your_app_scheme" android:host="open" />
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
</intent-filter>

<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="your_app_host" />
</intent-filter>

<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="your_app_test_host" />
</intent-filter>
```

### Usage

Once configured, you can utilize the various functionalities provided by Linksquared.

### Handling deeplinks

You can receive deep link events by registering a listener OR by using kotlin coroutines `flow`. Here's how you can implement it:

```kotlin
Linksquared.setOnDeeplinkReceivedListener(this) { link, payload ->
    val message = "Got link from listener: $link payload: $payload"
    Log.d("Linksquared", message)
}
```

```kotlin
Linksquared.Companion::openedLinkDetails.flow.collect { deeplinkDetails ->
    val message = "Got link from flow: ${deeplinkDetails?.link} payload: ${deeplinkDetails?.data}"
    Log.d("Linksquared", message)
}
```

### Generating Links

You can generate links using `generateLink` functions, below are some examples:

```kotlin
Linksquared.generateLink(title = "Title",
                        subtitle = "Subtitle",
                        imageURL = "url_to_some_image",
                        data = mapOf("param1" to "Value"),
                        tags = listOf("my_tag"),
                        lifecycleOwner = activity,
                        listener = { link, error ->
                        link?.let { link ->
                            Log.d("Linksquared", "Generated link: $link")
                        }
                        error?.let { error ->
                            Log.d("Linksquared", "Some error occurred: $error")
                        }
})
```

```kotlin
coroutineScope.launch {
    val link = Linksquared.generateLink(title = "Title",
                                        subtitle = "Subtitle",
                                        imageURL = "url_to_some_image",
                                        data = mapOf("param1" to "Value"),
                                        tags = listOf("my_tag"))
    Log.d("Linksquared", "Generated link: $link")
}
```

## Demo project

You can download and run a demo project [from here](https://github.com/linksquared/Linksquared-Android-example-app).

## Further Assistance

For further assistance and detailed documentation, refer to the Linksquared documentation available at https://linksquared.io/docs.

For technical support and inquiries, contact our support team at [support@linksquared.io](mailto:support@linksquared.io).

Thank you for choosing Linksquared! We're excited to see what you build with our SDK.

<br />
<br />
Copyright linksquared.
