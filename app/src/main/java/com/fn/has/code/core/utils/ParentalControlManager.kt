package com.fn.has.code.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.fn.has.code.data.local.db.AppDatabase
import java.security.MessageDigest

class ParentalControlManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("flownet_parental_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(context)

    // القوائم الجاهزة للتصنيفات المحجوبة
    private val adultDomains = setOf(
        "pornhub.com", "xvideos.com", "xhamster.com", "redtube.com", "youporn.com",
        "xnxx.com", "chaturbate.com", "stripchat.com", "onlyfans.com"
    )

    private val gamblingDomains = setOf(
        "bet365.com", "pokerstars.com", "1xbet.com", "stake.com", "bwin.com",
        "888casino.com", "draftkings.com", "fanduel.com"
    )

    private val socialDomains = setOf(
        "tiktok.com", "instagram.com", "facebook.com", "x.com", "twitter.com",
        "snapchat.com", "reddit.com", "discord.com"
    )

    private val adMalwareDomains = setOf(
        "doubleclick.net", "adservice.google.com", "ads.twitter.com",
        "popads.net", "adcolony.com", "unityads.unity3d.com"
    )

    fun isPinSet(): Boolean {
        return prefs.contains("parental_pin_hash")
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val hashedPin = hashPin(pin)
        prefs.edit().putString("parental_pin_hash", hashedPin).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("parental_pin_hash", null) ?: return true
        return hashPin(pin) == storedHash
    }

    var isAdultBlockingEnabled: Boolean
        get() = prefs.getBoolean("block_adult", true)
        set(value) = prefs.edit().putBoolean("block_adult", value).apply()

    var isGamblingBlockingEnabled: Boolean
        get() = prefs.getBoolean("block_gambling", true)
        set(value) = prefs.edit().putBoolean("block_gambling", value).apply()

    var isSocialBlockingEnabled: Boolean
        get() = prefs.getBoolean("block_social", false)
        set(value) = prefs.edit().putBoolean("block_social", value).apply()

    var isAdsBlockingEnabled: Boolean
        get() = prefs.getBoolean("block_ads", true)
        set(value) = prefs.edit().putBoolean("block_ads", value).apply()

    var selectedUpstreamDns: String
        get() = prefs.getString("upstream_dns", "1.1.1.3") ?: "1.1.1.3" // Default: Cloudflare Family
        set(value) = prefs.edit().putString("upstream_dns", value).apply()

    suspend fun isDomainBlocked(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim('.')

        // 1. فحص التصنيفات المحددة
        if (isAdultBlockingEnabled && isMatchInSet(cleanDomain, adultDomains)) return true
        if (isGamblingBlockingEnabled && isMatchInSet(cleanDomain, gamblingDomains)) return true
        if (isSocialBlockingEnabled && isMatchInSet(cleanDomain, socialDomains)) return true
        if (isAdsBlockingEnabled && isMatchInSet(cleanDomain, adMalwareDomains)) return true

        // 2. فحص النطاقات المخصصة في قاعدة البيانات
        val activeCustomDomains = db.blockedDomainDao().getActiveBlockedDomains()
        for (custom in activeCustomDomains) {
            val customClean = custom.domain.lowercase().trim('.')
            if (cleanDomain == customClean || cleanDomain.endsWith(".$customClean")) {
                return true
            }
        }

        return false
    }

    private fun isMatchInSet(targetDomain: String, domainSet: Set<String>): Boolean {
        for (baseDomain in domainSet) {
            if (targetDomain == baseDomain || targetDomain.endsWith(".$baseDomain")) {
                return true
            }
        }
        return false
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
