package ir.exam.app.core.network

import java.io.IOException
import java.net.SocketTimeoutException

object NetworkFailureClassifier {
    fun isNetworkFailure(error: Throwable): Boolean {
        if (generateSequence(error) { it.cause }.any { it is IOException || it is SocketTimeoutException }) return true
        val text = error.message.orEmpty().lowercase()
        return listOf(
            "failed to connect", "failed to fetch", "network", "timeout", "timed out",
            "unable to resolve host", "unknownhost", "connection reset", "connection refused",
            "اتصال اینترنت", "اینترنت قطع"
        ).any(text::contains)
    }

    fun isAuthFailure(error: Throwable): Boolean {
        val text = error.message.orEmpty().lowercase()
        return listOf("jwt", "unauthorized", "401", "نشست", "ابتدا وارد شوید").any(text::contains)
    }

    fun isAlreadySubmitted(error: Throwable): Boolean {
        val text = error.message.orEmpty().lowercase()
        return listOf("قبلاً", "already submitted", "duplicate answer", "پاسخ ثبت شده").any(text::contains)
    }
}
