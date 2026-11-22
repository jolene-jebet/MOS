package com.example.mos.screens.auth

import com.example.mos.data.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private var succeeded = false

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AuthViewModel(FakeAuthRepository())
        succeeded = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields shows an error`() {
        viewModel.login("", "", onSuccess = { succeeded = true })

        assertEquals("Enter your email and password.", viewModel.uiState.value.error)
        assertFalse(succeeded)
    }

    @Test
    fun `login with a bad email shows an error`() {
        viewModel.login("not-an-email", "secret1", onSuccess = { succeeded = true })

        assertEquals("Enter a valid email address.", viewModel.uiState.value.error)
        assertFalse(succeeded)
    }

    @Test
    fun `valid login succeeds`() {
        viewModel.login("jane@example.com", "secret1", onSuccess = { succeeded = true })

        assertTrue(succeeded)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `signup with mismatched passwords shows an error`() {
        viewModel.signUp("Jane", "jane@example.com", "712345678", "secret1", "secret2") { succeeded = true }

        assertEquals("Passwords do not match.", viewModel.uiState.value.error)
        assertFalse(succeeded)
    }

    @Test
    fun `signup with a short password shows an error`() {
        viewModel.signUp("Jane", "jane@example.com", "712345678", "123", "123") { succeeded = true }

        assertEquals("Password must be at least 6 characters.", viewModel.uiState.value.error)
        assertFalse(succeeded)
    }

    @Test
    fun `signup with a bad phone shows an error`() {
        viewModel.signUp("Jane", "jane@example.com", "123", "secret1", "secret1") { succeeded = true }

        assertEquals("Enter a valid phone number, e.g. 712 345 678.", viewModel.uiState.value.error)
        assertFalse(succeeded)
    }

    @Test
    fun `valid signup succeeds`() {
        viewModel.signUp("Jane", "jane@example.com", "0712 345 678", "secret1", "secret1") { succeeded = true }

        assertTrue(succeeded)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `phone numbers are normalised to +254`() {
        assertEquals("+254712345678", toKenyanPhone("712345678"))
        assertEquals("+254712345678", toKenyanPhone("0712345678"))
        assertEquals("+254712345678", toKenyanPhone("+254 712 345 678"))
        assertNull(toKenyanPhone("12345"))
    }
}
