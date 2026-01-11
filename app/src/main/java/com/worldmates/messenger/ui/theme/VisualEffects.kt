package com.worldmates.messenger.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Варіанти візуальних ефектів для повідомлень
 */
enum class MessageBubbleStyle(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    MODERN(
        displayName = "Сучасний",
        emoji = "✨",
        description = "Чистий і мінімалістичний стиль"
    ),

    GLASS(
        displayName = "Скляний",
        emoji = "🪟",
        description = "Прозорі bubble з ефектом скла"
    ),

    GRADIENT(
        displayName = "Градієнт",
        emoji = "🌈",
        description = "Плавні переходи кольорів"
    ),

    NEON(
        displayName = "Неон",
        emoji = "💡",
        description = "Яскравий неоновий ефект"
    ),

    SHADOW(
        displayName = "Тіні",
        emoji = "🌑",
        description = "М'які тіні для глибини"
    ),

    FLAT(
        displayName = "Плоский",
        emoji = "📱",
        description = "Класичний плоский дизайн"
    ),

    ROUNDED(
        displayName = "Округлий",
        emoji = "⚪",
        description = "Максимально округлі края"
    ),

    MINIMAL(
        displayName = "Мінімал",
        emoji = "⬜",
        description = "Мінімалістичний стиль iOS"
    );

    companion object {
        fun fromOrdinal(ordinal: Int): MessageBubbleStyle {
            return values().getOrNull(ordinal) ?: MODERN
        }
    }
}

/**
 * Варіанти анімацій для повідомлень
 */
enum class MessageAnimationStyle(
    val displayName: String,
    val emoji: String
) {
    NONE("Без анімації", "⏸️"),
    FADE("Плавна поява", "🌫️"),
    SLIDE("Ковзання", "➡️"),
    SCALE("Масштабування", "🔍"),
    BOUNCE("Підстрибування", "🎾"),
    WAVE("Хвиля", "🌊");

    companion object {
        fun fromOrdinal(ordinal: Int): MessageAnimationStyle {
            return values().getOrNull(ordinal) ?: FADE
        }
    }
}

/**
 * Варіанти шрифтів
 */
enum class FontVariant(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    DEFAULT(
        displayName = "За замовчуванням",
        emoji = "📝",
        description = "Системний шрифт"
    ),
    ROBOTO(
        displayName = "Roboto",
        emoji = "🤖",
        description = "Сучасний і чіткий"
    ),
    OPEN_SANS(
        displayName = "Open Sans",
        emoji = "📖",
        description = "Відкритий і читабельний"
    ),
    LATO(
        displayName = "Lato",
        emoji = "✍️",
        description = "Елегантний і легкий"
    ),
    MONTSERRAT(
        displayName = "Montserrat",
        emoji = "🎨",
        description = "Геометричний і стильний"
    ),
    POPPINS(
        displayName = "Poppins",
        emoji = "✨",
        description = "Модерновий і округлий"
    );

    companion object {
        fun fromOrdinal(ordinal: Int): FontVariant {
            return values().getOrNull(ordinal) ?: DEFAULT
        }
    }
}

/**
 * Модифікатор для застосування glass-ефекту
 */
fun Modifier.glassEffect(
    blurRadius: Dp = 10.dp,
    alpha: Float = 0.7f,
    backgroundColor: Color = Color.White
): Modifier = this
    .clip(RoundedCornerShape(20.dp))
    .background(backgroundColor.copy(alpha = alpha))
    .blur(blurRadius)

/**
 * Модифікатор для неонового ефекту
 */
fun Modifier.neonEffect(
    glowColor: Color,
    glowRadius: Dp = 8.dp
): Modifier = this
    .shadow(
        elevation = glowRadius,
        shape = RoundedCornerShape(20.dp),
        spotColor = glowColor,
        ambientColor = glowColor
    )

/**
 * Отримати форму bubble згідно стилю
 */
fun MessageBubbleStyle.getShape(): Shape {
    return when (this) {
        MessageBubbleStyle.ROUNDED -> RoundedCornerShape(28.dp)
        MessageBubbleStyle.MODERN -> RoundedCornerShape(20.dp)
        MessageBubbleStyle.MINIMAL -> RoundedCornerShape(18.dp)
        MessageBubbleStyle.FLAT -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(20.dp)
    }
}

/**
 * Отримати модифікатор для стилю bubble
 */
fun MessageBubbleStyle.getModifier(
    isOwn: Boolean,
    primaryColor: Color,
    secondaryColor: Color = Color.LightGray
): Modifier {
    val baseColor = if (isOwn) primaryColor else secondaryColor

    return when (this) {
        MessageBubbleStyle.GLASS -> Modifier.glassEffect(
            alpha = 0.6f,
            backgroundColor = baseColor
        )

        MessageBubbleStyle.GRADIENT -> Modifier
            .clip(getShape())
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isOwn) {
                        listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                    } else {
                        listOf(secondaryColor, secondaryColor.copy(alpha = 0.8f))
                    }
                )
            )

        MessageBubbleStyle.NEON -> Modifier
            .clip(getShape())
            .neonEffect(
                glowColor = if (isOwn) primaryColor else Color.Gray,
                glowRadius = 4.dp
            )
            .background(baseColor)

        MessageBubbleStyle.SHADOW -> Modifier
            .shadow(
                elevation = 4.dp,
                shape = getShape(),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .background(baseColor, shape = getShape())

        MessageBubbleStyle.FLAT, MessageBubbleStyle.MODERN,
        MessageBubbleStyle.ROUNDED, MessageBubbleStyle.MINIMAL -> Modifier
            .clip(getShape())
            .background(baseColor)
    }
}

/**
 * Composable-wrapper для анімованих bubble
 */
@Composable
fun AnimatedMessageBubble(
    style: MessageBubbleStyle,
    animationStyle: MessageAnimationStyle,
    isOwn: Boolean,
    primaryColor: Color,
    secondaryColor: Color = Color.LightGray,
    content: @Composable () -> Unit
) {
    // Анімація появи
    val animationSpec: AnimationSpec<Float> = when (animationStyle) {
        MessageAnimationStyle.BOUNCE -> spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        MessageAnimationStyle.SCALE, MessageAnimationStyle.FADE -> tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
        MessageAnimationStyle.SLIDE -> tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )
        MessageAnimationStyle.WAVE -> tween(
            durationMillis = 400,
            easing = LinearOutSlowInEasing
        )
        else -> snap()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "message_animation")

    Box(
        modifier = style.getModifier(isOwn, primaryColor, secondaryColor)
    ) {
        content()
    }
}

/**
 * Пульсуючий ефект для typing indicator
 */
@Composable
fun PulsingDot(color: Color = Color.Gray, size: Dp = 8.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color, shape = androidx.compose.foundation.shape.CircleShape)
    )
}
