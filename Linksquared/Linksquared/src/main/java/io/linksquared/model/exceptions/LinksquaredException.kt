package io.linksquared.model.exceptions

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.getStackTraceAsString(): String {
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    this.printStackTrace(printWriter)
    return stringWriter.toString()
}

enum class LinksquaredErrorCode {
    LINK_GENERATION_ERROR, SDK_NOT_INITIALIZED
}

class LinksquaredException(message: String?) : Exception(message) {
    var errorCode: LinksquaredErrorCode? = null

    constructor(message: String?, errorCode: LinksquaredErrorCode) : this(message) {
        this.errorCode = errorCode
    }

    override fun toString(): String {
        return "LinksquaredException(errorCode=$errorCode, message=${super.message})"
    }
}