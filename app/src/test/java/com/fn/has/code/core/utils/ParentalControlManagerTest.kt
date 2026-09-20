package com.fn.has.code.core.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParentalControlManagerTest {

    private lateinit var parentalControlManager: ParentalControlManager
    private val memoryStore = HashMap<String, Any?>()

    @Before
    fun setup() {
        memoryStore.clear()
        val fakePrefs = FakeSharedPreferences(memoryStore)

        parentalControlManager = ParentalControlManager(
            context = null,
            database = null,
            sharedPreferences = fakePrefs
        )
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

    private class FakeSharedPreferences(private val store: HashMap<String, Any?>) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = HashMap(store)

        override fun getString(key: String?, defValue: String?): String? =
            store[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            store[key] as? MutableSet<String> ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            store[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            store[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            store[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            store[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = store.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(store)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private class FakeEditor(private val store: HashMap<String, Any?>) : SharedPreferences.Editor {
        private val tempStore = HashMap<String, Any?>()
        private val removedKeys = HashSet<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) tempStore[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) tempStore[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) tempStore[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) tempStore[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) tempStore[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) tempStore[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removedKeys.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) store.clear()
            removedKeys.forEach { store.remove(it) }
            store.putAll(tempStore)
        }
    }
}
