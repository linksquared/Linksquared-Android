package io.linksquared.handlers

import android.content.Context
import io.linksquared.model.DebugLogger
import io.linksquared.model.Event
import io.linksquared.model.EventType
import io.linksquared.model.LogLevel
import io.linksquared.service.LinksquaredService
import io.linksquared.storage.EventsStorage
import io.linksquared.storage.LocalCache
import io.linksquared.utils.LSResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant

class EventsManager(val context: Context, val linksquaredContext: LinksquaredContext, apiKey: String) {
    private val linksquaredService = LinksquaredService(context = context, apiKey = apiKey,
        linksquaredContext = linksquaredContext)
    private val eventsStorage = EventsStorage(context = context)
    private val localCache = LocalCache(context = context)
    private var linkForFutureActions: String? = null

    companion object {
        private const val FIRST_BATCH_EVENTS_SENDING_LEEWAY: Double = 30.0
        private const val NUMBER_OF_DAYS_FOR_REACTIVATION: Int = 7
    }

    suspend fun onAppForegrounded() {
        sendNormalEventsToBackend()
        val lastResignTimestamp = localCache.resignTimestamp
        lastResignTimestamp?.let {
            handleOldEvents(timestamp = lastResignTimestamp)
        } ?: run {
            val event = Event(event = EventType.TIME_SPENT, createdAt = Instant.now())
            eventsStorage.addEvent(event)
        }
    }

    fun onAppBackgrounded() {
        localCache.resignTimestamp = Instant.now()
    }

    suspend fun logAppLaunchEvents() {
        addInitialEvents()
        addOpenEvent()
    }

    /// Logs an event and sends it to the backend.
    /// - Parameter event: The event to log
    suspend fun log(event: Event) {
        val newEvent = event
        if (newEvent.link == null) {
            newEvent.link = linkForFutureActions
        }

        eventsStorage.addEvent(newEvent)
        sendNormalEventsToBackend()
    }

    /// Sets the link for future actions to associate with new events.
    /// - Parameter link: The link to set
    suspend fun setLinkToNewFutureActions(link: String?) {
        linkForFutureActions = link
        link?.let {
            addLinkToEvents(link)
        } ?: kotlin.run {
            sendNormalEventsToBackend()
        }
    }

    /// Adds initial events such as install or reactivation events.
    private suspend fun addInitialEvents() {
        addInstallIfNeeded()
        addReactivationIfNeeded()

        localCache.numberOfOpens += 1
    }

    /// Logs an install event if it's the first app launch.
    private suspend fun addInstallIfNeeded() {
        val numberOfOpens = localCache.numberOfOpens
        if (numberOfOpens == 0) {
            linksquaredContext.lastSeen?.let {
                val event = Event(event = EventType.REINSTALL, createdAt = Instant.now())
                eventsStorage.addEvent(event)
            } ?: run {
                val event = Event(event = EventType.INSTALL, createdAt = Instant.now())
                eventsStorage.addEvent(event)
            }
        }
    }

    /// Logs a reactivation event if the app was inactive for the specified number of days.
    private suspend fun addReactivationIfNeeded() {
        val lastResignTimestamp = localCache.lastStartTimestamp
        lastResignTimestamp?.let {
            val duration = Duration.between(it, Instant.now())
            val daysBetween = duration.toDays()

            if (daysBetween >= NUMBER_OF_DAYS_FOR_REACTIVATION) {
                val event = Event(EventType.REACTIVATION, Instant.now())
                eventsStorage.addEvent(event)
            }
        }

        localCache.lastStartTimestamp = Instant.now()
    }

    /// Logs an app open event.
    private suspend fun addOpenEvent() {
        val event = Event(event = EventType.APP_OPEN, createdAt = Instant.now())
        eventsStorage.addEvent(event)
    }

    /// Handles old events that occurred before the app resigned active.
    /// - Parameter timestamp: The timestamp of when the app last resigned active
    private suspend fun handleOldEvents(timestamp: Instant) {
        // Handle events that occurred before the app resigned active
        val event = Event(event = EventType.TIME_SPENT, createdAt = Instant.now())

        changeStorageEvents { oldEvent ->
            val newEvent = oldEvent
            if (oldEvent.engagementTime == null && oldEvent.event == EventType.TIME_SPENT) {
                val duration = Duration.between(oldEvent.createdAt, timestamp)
                val secondsPassed =  duration.seconds
                if (secondsPassed > 0) {
                    newEvent.engagementTime = secondsPassed.toInt()
                }
            }
            newEvent
        }

        // Send the time-spent events to the backend and add the new event
        sendTimeSpentEventsToBackend()
        eventsStorage.addEvent(event)
    }

    /// Adds a link to all stored events that do not already have one.
    /// - Parameter link: The link to add
    private suspend fun addLinkToEvents(link: String) {
        // Add a link to the stored events
        changeStorageEvents { oldEvent ->
            val newEvent = oldEvent
            if (newEvent.link == null) {
                newEvent.link = link
            }
            newEvent
        }

        sendNormalEventsToBackend()
    }

    /// Changes stored events based on a closure and performs a completion handler.
    /// - Parameter eventHandling: A lambda function that defines how to modify each event
    private suspend fun changeStorageEvents(eventHandling: (oldEvent: Event) -> Event) {
        // Change stored events based on a closure and perform completion
        val events = eventsStorage.getEvents()
        var newEvents = mutableListOf<Event>()

        for (event in events) {
            val newEvent = eventHandling(event)
            newEvents.add(newEvent)
        }

        eventsStorage.addOrReplaceEvents(newEvents)
    }

    /// Sends normal events (non-time-spent) to the backend.
    private suspend fun sendNormalEventsToBackend() = runBlocking {
        val events = eventsStorage.getEvents()
        DebugLogger.instance.log(LogLevel.INFO, "Sending regular logs to the backend")

        for (event in events) {
            if (event.event != EventType.TIME_SPENT) {
                val result = linksquaredService.addEvent(event)
                when (result) {
                    is LSResult.Success -> {
                        eventsStorage.removeEvent(event)
                    }

                    is LSResult.Error -> {
                        DebugLogger.instance.log(LogLevel.INFO, "Failed to send normal: $event error: $result")
                        delay(5000)
                    }
                }
            }
        }
    }

    /// Sends time-spent events to the backend.
    private suspend fun sendTimeSpentEventsToBackend() = runBlocking {
        val events = eventsStorage.getEvents()
        DebugLogger.instance.log(LogLevel.INFO, "Sending time-spent logs to the backend")

        for (event in events) {
            if (event.event == EventType.TIME_SPENT) {
                val result = linksquaredService.addEvent(event)
                when (result) {
                    is LSResult.Success -> {
                        eventsStorage.removeEvent(event)
                    }

                    is LSResult.Error -> {
                        DebugLogger.instance.log(LogLevel.INFO, "Failed to send time-spent: $event error: $result")
                        delay(5000)
                    }
                }
            }
        }
    }
}