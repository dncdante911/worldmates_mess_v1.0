package com.worldmates.messenger.utils

import android.util.Base64
import java.security.spec.SecretKeySpec
import javax.crypto.Cipher

/**
 * Утилита для дешифрования сообщений Wowonder/Worldmates.
 *
 * Шифрование на сервере: openssl_encrypt($text, "AES-128-ECB", $time);
 * Где $time - это Unix-метка времени (timestamp) сообщения.
 */
object DecryptionUtility {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding" // PKCS5Padding может потребоваться
    // Хотя в PHP AES-128-ECB не требует IV (Initialization Vector),
    // нам нужно корректно обработать ключ, который является меткой времени.

    /**
     * Дешифрует строку Base64, зашифрованную AES-128-ECB.
     *
     * @param encryptedText Зашифрованный текст в формате Base64.
     * @param timestamp Unix-метка времени сообщения (используется как ключ).
     * @return Дешифрованный текст или null в случае ошибки.
     */
    fun decryptMessage(encryptedText: String, timestamp: Long): String? {
        // 1. Создаем 16-байтовый ключ (128 бит) из метки времени.
        // PHP-функция Wo_Secure создает 16-байтовую строку из числа. Мы имитируем это,
        // используя первые 16 символов строки, полученной из timestamp.
        val timestampString = timestamp.toString().padStart(16, '0')
        val keyBytes = timestampString.toByteArray(Charsets.UTF_8).sliceArray(0 until 16)

        try {
            val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(TRANSFORMATION)

            // Используем ECB-режим. Поскольку ключ меняется для каждого сообщения,
            // это обеспечивает минимальную безопасность (но это то, что использует Wowonder).
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            // Декодируем Base64-строку в массив байтов
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)

            // Выполняем дешифрование
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8).trim() // trim() удалит потенциальные нули или пробелы от дополнения
        } catch (e: Exception) {
            e.printStackTrace()
            // Внимание: Если дешифрование не удается, это может быть из-за:
            // 1. Неправильного ключа/timestamp.
            // 2. Неверного режима заполнения (Padding).
            // 3. Неправильного Base64 кодирования.
            return null
        }
    }
}