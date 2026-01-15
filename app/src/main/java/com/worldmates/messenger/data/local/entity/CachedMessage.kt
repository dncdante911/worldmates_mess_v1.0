<<<<<<< HEAD
package com.worldmates.messenger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 📦 CachedMessage - локально закэшированное сообщение
 *
 * Хранит сообщения из облака для офлайн доступа и быстрой загрузки.
 * Поддерживает AES-256-GCM шифрование.
 */
@Entity(
    tableName = "cached_messages",
    indices = [
        Index(value = ["chatId", "timestamp"], name = "idx_chat_timestamp"),
        Index(value = ["fromId"], name = "idx_from_id"),
        Index(value = ["toId"], name = "idx_to_id"),
        Index(value = ["isSynced"], name = "idx_is_synced")
    ]
)
data class CachedMessage(
    /**
     * Уникальный ID сообщения с сервера
     */
    @PrimaryKey
    val id: Long,

    /**
     * ID чата (recipientId для личных чатов, groupId для групп)
     * Используется для быстрой фильтрации сообщений по чату
     */
    val chatId: Long,

    /**
     * Тип чата: "user" или "group"
     */
    val chatType: String,

    /**
     * ID отправителя
     */
    val fromId: Long,

    /**
     * ID получателя
     */
    val toId: Long,

    /**
     * ID группы (null для личных чатов)
     */
    val groupId: Long? = null,

    /**
     * Зашифрованный текст сообщения (AES-256-GCM)
     */
    val encryptedText: String?,

    /**
     * Initialization Vector для AES-GCM (Base64)
     */
    val iv: String?,

    /**
     * Authentication Tag для AES-GCM (Base64)
     */
    val tag: String?,

    /**
     * Версия алгоритма шифрования (1=ECB, 2=GCM)
     */
    val cipherVersion: Int?,

    /**
     * Расшифрованный текст (хранится локально для быстрого доступа)
     * Null если еще не расшифровано
     */
    val decryptedText: String? = null,

    /**
     * Timestamp создания сообщения (Unix time)
     */
    val timestamp: Long,

    /**
     * URL медиафайла (фото/видео/аудио/документ)
     */
    val mediaUrl: String? = null,

    /**
     * Тип сообщения: "text", "image", "video", "audio", "voice", "file", "call"
     */
    val type: String = "text",

    /**
     * Тип медиа (если есть медиа)
     */
    val mediaType: String? = null,

    /**
     * Длительность медиа в секундах (для видео/аудио)
     */
    val mediaDuration: Long? = null,

    /**
     * Размер медиафайла в байтах
     */
    val mediaSize: Long? = null,

    /**
     * Путь к локально скачанному медиа (null если не скачано)
     */
    val localMediaPath: String? = null,

    /**
     * Путь к превью (thumbnail) медіа
     */
    val thumbnailPath: String? = null,

    /**
     * Стан завантаження медіа: "idle", "loading_thumb", "thumb_loaded", "loading_full", "full_loaded", "error"
     */
    val mediaLoadingState: String = "idle",

    /**
     * Имя отправителя
     */
    val senderName: String? = null,

    /**
     * URL аватара отправителя
     */
    val senderAvatar: String? = null,

    /**
     * Было ли сообщение отредактировано
     */
    val isEdited: Boolean = false,

    /**
     * Время редактирования (Unix time)
     */
    val editedTime: Long? = null,

    /**
     * Удалено ли сообщение
     */
    val isDeleted: Boolean = false,

    /**
     * ID сообщения, на которое это сообщение является ответом
     */
    val replyToId: Long? = null,

    /**
     * Текст сообщения, на которое отвечаем (краткий превью)
     */
    val replyToText: String? = null,

    /**
     * Прочитано ли сообщение
     */
    val isRead: Boolean = false,

    /**
     * Время прочтения (Unix time)
     */
    val readAt: Long? = null,

    /**
     * Синхронизировано ли с сервером
     * false = локальное сообщение, ожидающее отправки
     * true = полученное с сервера или успешно отправленное
     */
    val isSynced: Boolean = true,

    /**
     * Время последней синхронизации с сервером
     */
    val syncedAt: Long = System.currentTimeMillis(),

    /**
     * Время создания локальной записи
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CHAT_TYPE_USER = "user"
        const val CHAT_TYPE_GROUP = "group"

        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_VOICE = "voice"
        const val TYPE_FILE = "file"
        const val TYPE_CALL = "call"
    }
}
=======
package com.worldmates.messenger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 📦 CachedMessage - локально закэшированное сообщение
 *
 * Хранит сообщения из облака для офлайн доступа и быстрой загрузки.
 * Поддерживает AES-256-GCM шифрование.
 */
@Entity(
    tableName = "cached_messages",
    indices = [
        Index(value = ["chatId", "timestamp"], name = "idx_chat_timestamp"),
        Index(value = ["fromId"], name = "idx_from_id"),
        Index(value = ["toId"], name = "idx_to_id"),
        Index(value = ["isSynced"], name = "idx_is_synced")
    ]
)
data class CachedMessage(
    /**
     * Уникальный ID сообщения с сервера
     */
    @PrimaryKey
    val id: Long,

    /**
     * ID чата (recipientId для личных чатов, groupId для групп)
     * Используется для быстрой фильтрации сообщений по чату
     */
    val chatId: Long,

    /**
     * Тип чата: "user" или "group"
     */
    val chatType: String,

    /**
     * ID отправителя
     */
    val fromId: Long,

    /**
     * ID получателя
     */
    val toId: Long,

    /**
     * ID группы (null для личных чатов)
     */
    val groupId: Long? = null,

    /**
     * Зашифрованный текст сообщения (AES-256-GCM)
     */
    val encryptedText: String?,

    /**
     * Initialization Vector для AES-GCM (Base64)
     */
    val iv: String?,

    /**
     * Authentication Tag для AES-GCM (Base64)
     */
    val tag: String?,

    /**
     * Версия алгоритма шифрования (1=ECB, 2=GCM)
     */
    val cipherVersion: Int?,

    /**
     * Расшифрованный текст (хранится локально для быстрого доступа)
     * Null если еще не расшифровано
     */
    val decryptedText: String? = null,

    /**
     * Timestamp создания сообщения (Unix time)
     */
    val timestamp: Long,

    /**
     * URL медиафайла (фото/видео/аудио/документ)
     */
    val mediaUrl: String? = null,

    /**
     * Тип сообщения: "text", "image", "video", "audio", "voice", "file", "call"
     */
    val type: String = "text",

    /**
     * Тип медиа (если есть медиа)
     */
    val mediaType: String? = null,

    /**
     * Длительность медиа в секундах (для видео/аудио)
     */
    val mediaDuration: Long? = null,

    /**
     * Размер медиафайла в байтах
     */
    val mediaSize: Long? = null,

    /**
     * Путь к локально скачанному медиа (null если не скачано)
     */
    val localMediaPath: String? = null,

    /**
     * Путь к превью (thumbnail) медіа
     */
    val thumbnailPath: String? = null,

    /**
     * Стан завантаження медіа: "idle", "loading_thumb", "thumb_loaded", "loading_full", "full_loaded", "error"
     */
    val mediaLoadingState: String = "idle",

    /**
     * Имя отправителя
     */
    val senderName: String? = null,

    /**
     * URL аватара отправителя
     */
    val senderAvatar: String? = null,

    /**
     * Было ли сообщение отредактировано
     */
    val isEdited: Boolean = false,

    /**
     * Время редактирования (Unix time)
     */
    val editedTime: Long? = null,

    /**
     * Удалено ли сообщение
     */
    val isDeleted: Boolean = false,

    /**
     * ID сообщения, на которое это сообщение является ответом
     */
    val replyToId: Long? = null,

    /**
     * Текст сообщения, на которое отвечаем (краткий превью)
     */
    val replyToText: String? = null,

    /**
     * Прочитано ли сообщение
     */
    val isRead: Boolean = false,

    /**
     * Время прочтения (Unix time)
     */
    val readAt: Long? = null,

    /**
     * Синхронизировано ли с сервером
     * false = локальное сообщение, ожидающее отправки
     * true = полученное с сервера или успешно отправленное
     */
    val isSynced: Boolean = true,

    /**
     * Время последней синхронизации с сервером
     */
    val syncedAt: Long = System.currentTimeMillis(),

    /**
     * Время создания локальной записи
     */
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CHAT_TYPE_USER = "user"
        const val CHAT_TYPE_GROUP = "group"

        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_VOICE = "voice"
        const val TYPE_FILE = "file"
        const val TYPE_CALL = "call"
    }
}
>>>>>>> ee7949e8573d24ecdb81dbde3aeede26ef7efb2f
