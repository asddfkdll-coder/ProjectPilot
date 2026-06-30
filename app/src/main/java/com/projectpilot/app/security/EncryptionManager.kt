package com.projectpilot.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps EncryptedSharedPreferences (AES-256-GCM) backed by an Android-Keystore master key.
 * Used to store per-project .env content and any other sensitive values.
 *
 * Key per project: "env:<projectId>"
 */
@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putEnv(projectId: Long, content: String) {
        prefs.edit().putString(envKey(projectId), content).apply()
    }

    fun getEnv(projectId: Long): String? = prefs.getString(envKey(projectId), null)

    fun clearEnv(projectId: Long) {
        prefs.edit().remove(envKey(projectId)).apply()
    }

    private fun envKey(id: Long) = "env:$id"

    companion object { const val FILE_NAME = "pp_secure_prefs" }
}
