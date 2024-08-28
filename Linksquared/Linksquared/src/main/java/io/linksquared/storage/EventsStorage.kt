package io.linksquared.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.linksquared.model.DebugLogger
import io.linksquared.model.Event
import io.linksquared.model.LogLevel
import io.linksquared.utils.LSJsonDateTypeAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

class EventsStorage(context: Context) {
    private val preferences = context.getSharedPreferences(LINKSQUARED_STORAGE, Context.MODE_PRIVATE)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val storageSerialDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val gson = GsonBuilder().setLenient().registerTypeAdapterFactory(
        LSJsonDateTypeAdapterFactory()
    ).create()

    companion object {
        const val LINKSQUARED_STORAGE = "LinksquaredStorage"
        private const val STORED_EVENTS = "stored_events"
    }

    /// Adds or replaces events in the storage.
    ///
    /// - Parameter events: The events to add or replace.
    suspend fun addOrReplaceEvents(events: List<Event>) = withContext(storageSerialDispatcher) {
        val currentEvents = getEvents().toMutableList()
        events.forEach { event ->
            if (currentEvents.contains(event)) {
                val index = currentEvents.indexOf(event)
                currentEvents[index] = event
            } else {
                currentEvents.add(event)
            }
        }

        val type = object : TypeToken<List<Event>>() {}.type
        val editor = preferences.edit()
        val jsonString = gson.toJson(currentEvents)
        editor.putString(STORED_EVENTS, jsonString)
        editor.apply()

        try {
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            DebugLogger.instance.log(LogLevel.INFO, "Caching events - Failed. ${e.stackTrace}")
        }
    }

    /// Adds an event to the storage.
    ///
    /// - Parameter event: The event to add.
    suspend fun addEvent(event: Event) = withContext(storageSerialDispatcher) {
        var currentEvents = getEvents().toMutableList()
        currentEvents.add(event)

        val type = object : TypeToken<List<Event>>() {}.type
        val editor = preferences.edit()
        val jsonString = gson.toJson(currentEvents)
        editor.putString(STORED_EVENTS, jsonString)
        editor.apply()

        try {
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            DebugLogger.instance.log(LogLevel.INFO, "Caching events - Failed. ${e.stackTrace}")
        }
    }

    /// Removes an event from the storage.
    ///
    /// - Parameter event: The event to remove.
    suspend fun removeEvent(event: Event) = withContext(storageSerialDispatcher) {
        val currentEvents = getEvents().toMutableList()
        currentEvents.remove(event)

        val type = object : TypeToken<List<Event>>() {}.type
        val editor = preferences.edit()
        val jsonString = gson.toJson(currentEvents)
        editor.putString(STORED_EVENTS, jsonString)
        editor.apply()

        try {
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            DebugLogger.instance.log(LogLevel.INFO, "Caching events - Failed. ${e.stackTrace}")
        }
    }

    /// Retrieves all events from the storage.
    suspend fun getEvents(): List<Event> = withContext(storageSerialDispatcher) {
            val jsonString = preferences.getString(STORED_EVENTS, null)
            val type = object : TypeToken<List<Event>>() {}.type

            try {
                gson.fromJson(jsonString, type)
            } catch (e: Exception) {
                emptyList()
            }
    }
}