package io.linksquared.storage

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant

class LocalCache(val context: Context) {
    private val preferences = context.getSharedPreferences(EventsStorage.LINKSQUARED_STORAGE, Context.MODE_PRIVATE)

    companion object {
        private const val LINKSQUARED_NUMBER_OF_OPENS = "linksquared_number_of_opens"
        private const val LINKSQUARED_RESIGN_TIMESTAMP = "linksquared_resign_timestamp"
        private const val LINKSQUARED_LAST_START_TIMESTAMP = "linksquared_last_start_timestamp"
    }

    var numberOfOpens:Int
        set(value) {
            val editor = preferences.edit()
            editor.putInt(LINKSQUARED_NUMBER_OF_OPENS, value)
            editor.apply()
        }
        get() {
            return preferences.getInt(LINKSQUARED_NUMBER_OF_OPENS, 0)
        }

    var resignTimestamp:Instant?
        set(value) {
            val editor = preferences.edit()
            editor.putString(LINKSQUARED_RESIGN_TIMESTAMP, value.toString())
            editor.apply()
        }
        get() {
            val string = preferences.getString(LINKSQUARED_RESIGN_TIMESTAMP, null)
            string?.let {
                val instant = Instant.parse(it)
                return instant
            } ?: run {
                return null
            }
        }

    var lastStartTimestamp:Instant?
        set(value) {
            val editor = preferences.edit()
            editor.putString(LINKSQUARED_LAST_START_TIMESTAMP, value.toString())
            editor.apply()
        }
        get() {
            val string = preferences.getString(LINKSQUARED_LAST_START_TIMESTAMP, null)
            string?.let {
                val instant = Instant.parse(it)
                return instant
            } ?: run {
                return null
            }
        }

}