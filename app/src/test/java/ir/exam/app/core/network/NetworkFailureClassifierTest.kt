package ir.exam.app.core.network

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkFailureClassifierTest {
    @Test
    fun `network errors are retryable but validation errors are permanent`() {
        assertTrue(NetworkFailureClassifier.isNetworkFailure(IOException("connection reset")))
        assertTrue(NetworkFailureClassifier.isNetworkFailure(IllegalStateException("Network request timeout")))
        assertFalse(NetworkFailureClassifier.isNetworkFailure(IllegalArgumentException("نمره خارج از بازه است")))
    }

    @Test
    fun `auth and duplicate responses have separate handling`() {
        assertTrue(NetworkFailureClassifier.isAuthFailure(IllegalStateException("JWT expired")))
        assertTrue(NetworkFailureClassifier.isAlreadySubmitted(IllegalStateException("شما قبلاً پاسخ داده‌اید")))
        assertFalse(NetworkFailureClassifier.isAlreadySubmitted(IllegalStateException("آزمون بسته است")))
    }
}
