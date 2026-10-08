package ir.exam.app.core.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V229 — تست رفتاری «کشیدن برای بازخوانی» (قرارداد V227) روی PullRefreshGate با زمان مجازی:
 * تک‌پروازی، روشن‌شدن فوری پرچم، حداقل ۵۰۰ms نمایش، خاموش‌شدن حتی با خطا.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class V229_PullRefreshGateTest {

    @Test
    fun flagTurnsOnImmediatelyAndStaysAtLeastMinVisible() = runTest {
        val clock = testScheduler
        val gate = PullRefreshGate(this, minVisibleMs = 500L, now = { clock.currentTime })
        val flags = mutableListOf<Boolean>()
        assertTrue(gate.run({ flags += it }) { /* بارگذاری فوری */ })
        runCurrent()
        assertEquals(listOf(true), flags)          // فوراً روشن
        advanceTimeBy(499L); runCurrent()
        assertEquals(listOf(true), flags)          // هنوز روشن (کمتر از ۵۰۰ms)
        advanceTimeBy(2L); runCurrent()
        assertEquals(listOf(true, false), flags)   // پس از ۵۰۰ms خاموش
        assertFalse(gate.isActive)
    }

    @Test
    fun secondCallWhileActiveIsIgnored() = runTest {
        val clock = testScheduler
        val gate = PullRefreshGate(this, minVisibleMs = 500L, now = { clock.currentTime })
        val release = CompletableDeferred<Unit>()
        var loads = 0
        assertTrue(gate.run({}) { loads++; release.await() })
        runCurrent()
        assertFalse(gate.run({}) { loads++ })      // تک‌پروازی
        assertFalse(gate.run({}) { loads++ })
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, loads)
        assertTrue(gate.run({}) { loads++ })       // پس از پایان، دوباره مجاز
        advanceUntilIdle()
        assertEquals(2, loads)
    }

    @Test
    fun slowLoadDoesNotAddExtraDelay() = runTest {
        val clock = testScheduler
        val gate = PullRefreshGate(this, minVisibleMs = 500L, now = { clock.currentTime })
        val flags = mutableListOf<Boolean>()
        gate.run({ flags += it }) { kotlinx.coroutines.delay(900L) }
        advanceTimeBy(901L); runCurrent()
        assertEquals(listOf(true, false), flags)   // بارگذاری ۹۰۰ms → بدون ۵۰۰ms اضافه
    }

    @Test
    fun failingLoadStillClearsFlag() = runTest {
        val clock = testScheduler
        val gate = PullRefreshGate(this, minVisibleMs = 100L, now = { clock.currentTime })
        val flags = mutableListOf<Boolean>()
        gate.run({ flags += it }) { throw IllegalStateException("شبکه") }
        advanceUntilIdle()
        assertEquals(listOf(true, false), flags)
        assertFalse(gate.isActive)
    }
}
