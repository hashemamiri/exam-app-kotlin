package ir.exam.app.ui.app

import ir.exam.app.core.network.UserFacingError
import org.junit.Assert.assertEquals
import org.junit.Test

/** V266 — تحمل بار: خطای ۴۲۹ (محدودیت نرخ Auth سوپابیس، ۳۰ ورود در ۵ دقیقه به ازای هر IP) پیام فارسی روشن می‌گیرد. */
class V266_RateLimitHardeningTest {
    @Test
    fun `auth 429 maps to persian rate-limit message`() {
        assertEquals(UserFacingError.RATE_LIMIT, UserFacingError.fromText("Request rate limit reached", "x"))
        assertEquals(UserFacingError.RATE_LIMIT, UserFacingError.fromText("over_request_rate_limit (429)", "x"))
        assertEquals(UserFacingError.RATE_LIMIT, UserFacingError.of(IllegalStateException("Too Many Requests"), "x"))
    }

    @Test
    fun `other errors unchanged`() {
        assertEquals("موجودی کافی نیست", UserFacingError.fromText("موجودی کافی نیست", "x"))
        assertEquals(UserFacingError.SERVER, UserFacingError.fromText("HTTP request to server failed", "x"))
    }
}
