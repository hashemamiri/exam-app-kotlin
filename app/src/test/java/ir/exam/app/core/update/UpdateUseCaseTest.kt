package ir.exam.app.core.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUseCaseTest {
    @Test
    fun `newer server version is returned`() = runBlocking {
        val remote = RemoteVersion(
            code = 12,
            name = "1.2.0",
            notesFa = listOf("بهبود بروزرسانی"),
            apkUrl = "https://example.test/app.apk"
        )
        val result = UpdateUseCase(FakeRepository(Result.success(remote))).check(installedCode = 11)

        assertEquals(remote, result.getOrThrow())
    }

    @Test
    fun `same server version means app is up to date`() = runBlocking {
        val remote = RemoteVersion(
            code = 12,
            name = "1.2.0",
            notesFa = emptyList(),
            apkUrl = "https://example.test/app.apk"
        )
        val result = UpdateUseCase(FakeRepository(Result.success(remote))).check(installedCode = 12)

        assertNull(result.getOrThrow())
    }

    @Test
    fun `empty active release table means app is up to date`() = runBlocking {
        val result = UpdateUseCase(FakeRepository(Result.success(null))).check(installedCode = 12)

        assertNull(result.getOrThrow())
    }

    @Test
    fun `repository failure is propagated`() = runBlocking {
        val result = UpdateUseCase(
            FakeRepository(Result.failure(IllegalStateException("backend unavailable")))
        ).check(installedCode = 1)

        assertTrue(result.isFailure)
    }
}

private class FakeRepository(
    private val result: Result<RemoteVersion?>
) : AppUpdateRepository {
    override suspend fun latest(): Result<RemoteVersion?> = result
}
