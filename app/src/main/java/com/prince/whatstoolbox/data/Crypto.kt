package com.prince.whatstoolbox.data
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM with a non-exportable Android Keystore key. Every sensitive column is stored as "enc1:<base64(iv+ciphertext)>".
 * The key is created once and cached; decryption never throws (a lost key yields [UNREADABLE] instead of crashing the app).
 */
object Crypto {
    private const val ALIAS = "toolbox_vault_key"; private const val PREFIX = "enc1:"
    const val UNREADABLE = "⚠️ Unreadable entry (encryption key changed)"

    private val key: SecretKey by lazy {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build()) }.generateKey()
    }

    fun enc(s: String): String {
        if (s.isEmpty()) return s
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key)
        return PREFIX + Base64.encodeToString(c.iv + c.doFinal(s.toByteArray()), Base64.NO_WRAP)
    }

    /** Rows written by older plaintext builds (no prefix) are returned unchanged. */
    fun dec(s: String): String {
        if (!s.startsWith(PREFIX)) return s
        return try {
            val raw = Base64.decode(s.removePrefix(PREFIX), Base64.NO_WRAP); val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw, 0, 12)); String(c.doFinal(raw, 12, raw.size - 12))
        } catch (e: GeneralSecurityException) { UNREADABLE } catch (e: IllegalArgumentException) { UNREADABLE }
    }
}
