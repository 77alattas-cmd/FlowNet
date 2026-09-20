package com.fn.has.code.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "flownet_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getUsername(): String =
        prefs.getString(KEY_USER, null) ?: DEFAULT_USER.also { setUsername(it) }

    fun getPassword(): String =
        prefs.getString(KEY_PASS, null) ?: generatePassword().also { setPassword(it) }

    fun setUsername(user: String) = prefs.edit().putString(KEY_USER, user).apply()
    fun setPassword(pass: String) = prefs.edit().putString(KEY_PASS, pass).apply()

    /** يولّد كلمة مرور عشوائية 16 حرفاً بدل "1234" */
    private fun generatePassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val rnd = SecureRandom()
        return (1..16).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        private const val KEY_USER = "server_username"
        private const val KEY_PASS = "server_password"
        private const val DEFAULT_USER = "admin"
    }
}
