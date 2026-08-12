package ir.exam.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthIdentifierTest {
    @Test
    fun `student username maps to the exact managed auth domain`() {
        assertEquals(
            "student_25@student.exam.local",
            AuthIdentifier.passwordLoginEmail(" Student_25 ")
        )
    }

    @Test
    fun `teacher email remains normalized email`() {
        assertEquals(
            "teacher@example.com",
            AuthIdentifier.passwordLoginEmail(" Teacher@Example.COM ")
        )
    }

    @Test
    fun `invalid username cannot become an auth email`() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthIdentifier.passwordLoginEmail("نام فارسی")
        }
    }
}
