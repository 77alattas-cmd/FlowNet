package com.fn.has.code.core.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParentalControlManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    private lateinit var parentalControlManager: ParentalControlManager
    private val memoryStore = HashMap<String, String>()

    @Before
    fun setup() {
        val keySlot = slot<String>()
        val valueSlot = slot<String>()

        every { context.getSharedPreferences("flownet_parental_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.contains(any()) } answers { memoryStore.containsKey(firstArg()) }
        every { prefs.getString(any(), any()) } answers { memoryStore[firstArg()] ?: secondArg() }
        every { editor.putString(capture(keySlot), capture(valueSlot)) } answers {
            memoryStore[keySlot.captured] = valueSlot.captured
            editor
        }

        parentalControlManager = ParentalControlManager(context)
    }

    @Test
    fun setPin_validFourDigits_storesAndVerifiesSuccessfully() {
        val isSet = parentalControlManager.setPin("1234")
        assertTrue(isSet)
        assertTrue(parentalControlManager.verifyPin("1234"))
        assertFalse(parentalControlManager.verifyPin("9999"))
    }

    @Test
    fun setPin_invalidLengthOrAlpha_returnsFalse() {
        assertFalse(parentalControlManager.setPin("123"))
        assertFalse(parentalControlManager.setPin("12345"))
        assertFalse(parentalControlManager.setPin("abcd"))
    }

    @Test
    fun isDomainBlocked_detectsAdultAndGamblingDomains() = runBlocking {
        parentalControlManager.isAdultBlockingEnabled = true
        parentalControlManager.isGamblingBlockingEnabled = true

        assertTrue(parentalControlManager.isDomainBlocked("pornhub.com"))
        assertTrue(parentalControlManager.isDomainBlocked("sub.bet365.com"))
        assertFalse(parentalControlManager.isDomainBlocked("wikipedia.org"))
    }
}
