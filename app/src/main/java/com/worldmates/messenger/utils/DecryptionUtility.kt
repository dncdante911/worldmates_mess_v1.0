package com.worldmates.messenger.utils

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

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
        // PHP's openssl_encrypt конвертирует число в строку и дополняет нулевыми байтами В КОНЦЕ.
        // Например, 1754067404 -> "1754067404" -> "1754067404\x00\x00\x00\x00\x00\x00" (16 байт)
        val timestampString = timestamp.toString()
        val timestampBytes = timestampString.toByteArray(Charsets.UTF_8)

        // Создаем 16-байтовый массив, заполненный нулями
        val keyBytes = ByteArray(16)
        // Копируем байты timestamp в начало, остальное останется нулями
        timestampBytes.copyInto(keyBytes, 0, 0, minOf(timestampBytes.size, 16))

        Log.d("DecryptionUtility", "Ключ дешифрования: timestamp=$timestamp, key=${keyBytes.joinToString("") { "%02x".format(it) }}")

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

    /**
     * Проверяет, является ли строка валидным Base64.
     */
    private fun isBase64(text: String): Boolean {
        return try {
            // Base64 строки обычно содержат только A-Z, a-z, 0-9, +, /, =
            val base64Pattern = "^[A-Za-z0-9+/]+=*$".toRegex()
            base64Pattern.matches(text) && text.length % 4 == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Пытается расшифровать сообщение. Если не получается - возвращает исходный текст.
     * Это нужно для поддержки как зашифрованных (с веб-версии), так и незашифрованных сообщений.
     *
     * @param text Текст сообщения (зашифрованный или нет).
     * @param timestamp Unix-метка времени сообщения.
     * @return Расшифрованный текст или исходный текст, если расшифровка не удалась.
     */
    fun decryptMessageOrOriginal(text: String, timestamp: Long): String {
        // Проверяем, пустая ли строка
        if (text.isEmpty()) return text

        // Проверяем, похоже ли это на Base64 строку
        if (!isBase64(text)) {
            Log.d("DecryptionUtility", "Текст не похож на Base64, возвращаю как есть: '$text'")
            return text
        }

        Log.d("DecryptionUtility", "Попытка расшифровки Base64: text='$text', timestamp=$timestamp")

        // Пытаемся расшифровать
        val decrypted = decryptMessage(text, timestamp)

        // Если расшифровка успешна и результат не пустой, возвращаем его
        return if (!decrypted.isNullOrEmpty()) {
            Log.d("DecryptionUtility", "Успешно расшифровано: '$decrypted'")
            decrypted
        } else {
            // Иначе возвращаем исходный текст (возможно он не был зашифрован)
            Log.d("DecryptionUtility", "Не удалось расшифровать, возвращаю исходный текст: '$text'")
            text
        }
    }

    /**
     * Дешифрует URL медиа-файла, если он зашифрован.
     * URL может быть:
     * - Обычным URL (http://, https://) - возвращается как есть
     * - Base64 зашифрованным URL - дешифруется
     * - Относительным путём к файлу - возвращается как есть
     *
     * @param mediaUrl URL или зашифрованная строка
     * @param timestamp Unix-метка времени сообщения
     * @return Дешифрованный URL или исходный URL
     */
    fun decryptMediaUrl(mediaUrl: String?, timestamp: Long): String? {
        if (mediaUrl.isNullOrEmpty()) return mediaUrl

        // Если это обычный URL - возвращаем как есть
        if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
            Log.d("DecryptionUtility", "URL вже розшифрований: $mediaUrl")
            return mediaUrl
        }

        // Если это путь к файлу на сервере (начинается с /)
        if (mediaUrl.startsWith("/")) {
            Log.d("DecryptionUtility", "Це шлях до файлу: $mediaUrl")
            return mediaUrl
        }

        // Проверяем, похоже ли это на Base64
        if (!isBase64(mediaUrl)) {
            Log.d("DecryptionUtility", "Media URL не Base64, повертаю як є: $mediaUrl")
            return mediaUrl
        }

        Log.d("DecryptionUtility", "Спроба розшифровки media URL: $mediaUrl")

        // Пытаемся расшифровать
        val decrypted = decryptMessage(mediaUrl, timestamp)

        return if (!decrypted.isNullOrEmpty()) {
            Log.d("DecryptionUtility", "Media URL розшифровано: $decrypted")
            decrypted
        } else {
            Log.d("DecryptionUtility", "Не вдалося розшифрувати media URL, повертаю оригінал")
            mediaUrl
        }
    }

    /**
     * Проверяет, содержит ли текст зашифрованный URL внутри.
     * Иногда URL медиа-файла может быть внутри зашифрованного текста сообщения.
     *
     * @param decryptedText Расшифрованный текст сообщения
     * @return URL, если найден, иначе null
     */
    fun extractMediaUrlFromText(decryptedText: String?): String? {
        if (decryptedText.isNullOrEmpty()) return null

        // Ищем URL в тексте
        val urlPattern = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()
        val match = urlPattern.find(decryptedText)

        return match?.value?.let { url ->
            // Проверяем, является ли это URL медиа-файла
            if (url.contains("/upload/photos/") ||
                url.contains("/upload/videos/") ||
                url.contains("/upload/files/") ||
                url.contains("/upload/sounds/") ||
                url.contains(".jpg") ||
                url.contains(".jpeg") ||
                url.contains(".png") ||
                url.contains(".gif") ||
                url.contains(".mp4") ||
                url.contains(".mp3") ||
                url.contains(".wav") ||
                url.contains(".webm")) {
                Log.d("DecryptionUtility", "Знайдено URL медіа у тексті: $url")
                url
            } else {
                null
            }
        }
    }
}