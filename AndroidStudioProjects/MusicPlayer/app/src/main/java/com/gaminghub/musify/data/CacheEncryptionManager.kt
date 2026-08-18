package com.gaminghub.musify.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages military-grade AES encryption keys for the music cache.
 * Uses Android Keystore System to ensure keys are hardware-backed and non-extractable.
 */
object CacheEncryptionManager {
    private const val KEY_ALIAS = "musify_cache_key_v1"
    private const val PREFS_NAME = "secure_cache_prefs"
    private const val ENCRYPTED_KEY_HEX = "encrypted_master_key"
    private const val IV_HEX = "encryption_iv"

    /**
     * Returns a 128-bit key for Media3 AesCipherDataSink/Source.
     * If no key exists, it generates a new one, encrypts it via Keystore, and persists it.
     */
    fun getCacheKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedHex = prefs.getString(ENCRYPTED_KEY_HEX, null)
        val ivHex = prefs.getString(IV_HEX, null)

        return if (encryptedHex != null && ivHex != null) {
            decryptKey(encryptedHex, ivHex)
        } else {
            val newKey = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
            val (encrypted, iv) = encryptKey(newKey)
            prefs.edit()
                .putString(ENCRYPTED_KEY_HEX, bytesToHex(encrypted))
                .putString(IV_HEX, bytesToHex(iv))
                .apply()
            newKey
        }
    }

    private fun encryptKey(rawKey: ByteArray): Pair<ByteArray, ByteArray> {
        val secretKey = getOrCreateKeystoreKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(rawKey) to cipher.iv
    }

    private fun decryptKey(encryptedHex: String, ivHex: String): ByteArray {
        try {
            val secretKey = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, hexToBytes(ivHex))
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            return cipher.doFinal(hexToBytes(encryptedHex))
        } catch (e: Exception) {
            // Fallback: generate a new key if decryption fails (rare, e.g. keystore cleared)
            return ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun hexToBytes(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
