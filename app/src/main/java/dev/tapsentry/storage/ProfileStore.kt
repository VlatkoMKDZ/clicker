package dev.tapsentry.storage

import android.content.Context
import dev.tapsentry.model.AutomationProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys=true; classDiscriminator="kind" }
    fun loadAll(): List<AutomationProfile> = runCatching { json.decodeFromString<List<AutomationProfile>>(prefs.getString("items", null) ?: "[]") }.getOrDefault(emptyList())
    fun save(items:List<AutomationProfile>) = prefs.edit().putString("items", json.encodeToString(items)).apply()
}
