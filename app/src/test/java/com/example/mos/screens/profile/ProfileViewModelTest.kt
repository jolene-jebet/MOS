package com.example.mos.screens.profile

import com.example.mos.data.FakeAuthRepository
import com.example.mos.data.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeAuthRepository()
        repository.signUp("Jane Wanjiku", "jane@example.com", "+254712345678", "secret1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `profile loads the signed up user`() {
        val viewModel = ProfileViewModel(repository)

        viewModel.load()

        val user = viewModel.uiState.value.user!!
        assertEquals("Jane Wanjiku", user.name)
        assertEquals("+254712345678", user.phone)
    }

    @Test
    fun `edit form is filled with saved details`() {
        val viewModel = EditProfileViewModel(repository)

        viewModel.load()

        assertEquals("Jane Wanjiku", viewModel.uiState.value.name)
        assertEquals("712345678", viewModel.uiState.value.phone)
    }

    @Test
    fun `saving with a blank name shows an error`() {
        val viewModel = EditProfileViewModel(repository)
        var saved = false
        viewModel.load()

        viewModel.onNameChange(" ")
        viewModel.save { saved = true }

        assertEquals("Please enter your name.", viewModel.uiState.value.error)
        assertFalse(saved)
    }

    @Test
    fun `saving with a bad phone shows an error`() {
        val viewModel = EditProfileViewModel(repository)
        var saved = false
        viewModel.load()

        viewModel.onPhoneChange("123")
        viewModel.save { saved = true }

        assertEquals("Enter a valid phone number, e.g. 712 345 678.", viewModel.uiState.value.error)
        assertFalse(saved)
    }

    @Test
    fun `valid save updates the profile`() = runTest {
        val viewModel = EditProfileViewModel(repository)
        var saved = false
        viewModel.load()

        viewModel.onNameChange("Jane W.")
        viewModel.onPhoneChange("0722 000 111")
        viewModel.onModeChange(TransportMode.BODA)
        viewModel.save { saved = true }

        assertTrue(saved)
        assertNull(viewModel.uiState.value.error)
        val user = repository.getProfile().getOrThrow()
        assertEquals("Jane W.", user.name)
        assertEquals("+254722000111", user.phone)
        assertEquals(TransportMode.BODA, user.defaultMode)
    }

    @Test
    fun `new users default to Car`() {
        val viewModel = ProfileViewModel(repository)

        viewModel.load()

        assertEquals(TransportMode.PRIVATE, viewModel.uiState.value.user!!.defaultMode)
    }

    @Test
    fun `a saved Matatu mode shows as Car on the profile`() = runTest {
        repository.updateProfile("Jane Wanjiku", "+254712345678", TransportMode.MATATU)
        val profile = ProfileViewModel(repository)
        val edit = EditProfileViewModel(repository)

        profile.load()
        edit.load()

        assertEquals(TransportMode.PRIVATE, profile.uiState.value.user!!.defaultMode)
        assertEquals(TransportMode.PRIVATE, edit.uiState.value.defaultMode)
    }

    @Test
    fun `password reset shows the pop-up until dismissed`() {
        val viewModel = ProfileViewModel(repository)

        viewModel.sendPasswordReset()
        assertTrue(viewModel.uiState.value.resetEmailSent)

        viewModel.dismissResetDialog()
        assertFalse(viewModel.uiState.value.resetEmailSent)
    }

    @Test
    fun `deleting the account calls onDeleted and signs out`() {
        val viewModel = ProfileViewModel(repository)
        var deleted = false

        viewModel.deleteAccount { deleted = true }

        assertTrue(deleted)
        assertNull(repository.currentUser())
    }
}
