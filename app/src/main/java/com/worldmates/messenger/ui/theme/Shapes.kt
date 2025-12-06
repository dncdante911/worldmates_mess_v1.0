package com.worldmates.messenger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Формы для современного дизайна в стиле Material Design 3
 * Telegram-подобные скруглённые углы
 */
val Shapes = Shapes(
    // Экстрамалые формы (чипы, бейджи)
    extraSmall = RoundedCornerShape(4.dp),

    // Малые формы (кнопки, карточки сообщений)
    small = RoundedCornerShape(12.dp),

    // Средние формы (диалоги, bottom sheets)
    medium = RoundedCornerShape(16.dp),

    // Большие формы (модальные окна, карточки медиа)
    large = RoundedCornerShape(20.dp),

    // Экстрабольшие формы (полноэкранные карточки)
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Кастомные формы для специфичных элементов
 */
object WMShapes {
    // Собственные сообщения (скруглены все углы кроме правого нижнего)
    val ownMessageBubble = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 4.dp
    )

    // Чужие сообщения (скруглены все углы кроме левого нижнего)
    val otherMessageBubble = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 4.dp,
        bottomEnd = 18.dp
    )

    // Голосовое сообщение
    val voiceMessage = RoundedCornerShape(20.dp)

    // Медиа-сообщения (фото/видео)
    val mediaMessage = RoundedCornerShape(12.dp)

    // Файлы
    val fileAttachment = RoundedCornerShape(14.dp)

    // Аватар пользователя
    val avatar = RoundedCornerShape(50) // Круглый

    // Аватар группы
    val groupAvatar = RoundedCornerShape(16.dp) // Скруглённый квадрат

    // Поле ввода сообщения
    val messageInput = RoundedCornerShape(24.dp)

    // Кнопки
    val button = RoundedCornerShape(12.dp)
    val roundButton = RoundedCornerShape(50) // Круглая кнопка

    // Карточка чата в списке
    val chatCard = RoundedCornerShape(0.dp) // Без скругления (как в Telegram)

    // Карточка группы
    val groupCard = RoundedCornerShape(16.dp)

    // Диалоговые окна
    val dialog = RoundedCornerShape(28.dp)

    // Bottom Sheet
    val bottomSheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Бейдж непрочитанных
    val unreadBadge = RoundedCornerShape(12.dp)

    // Индикатор "печатает"
    val typingIndicator = RoundedCornerShape(8.dp)

    // Превью медиа
    val mediaPreview = RoundedCornerShape(8.dp)

    // Реакции на сообщения
    val reaction = RoundedCornerShape(16.dp)

    // Панель выбора эмодзи
    val emojiPanel = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
}
