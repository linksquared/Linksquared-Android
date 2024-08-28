package io.linksquared.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Build
import io.linksquared.model.DebugLogger
import io.linksquared.model.LogLevel

fun Context.hasURISchemesConfigured(): Boolean {
    // This check is not possible on android
    return true
}
