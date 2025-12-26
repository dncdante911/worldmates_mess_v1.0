package com.worldmates.messenger.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.worldmates.messenger.ui.preferences.BubbleStyle

/**
 * 🎨 СТАНДАРТНИЙ СТИЛЬ - заокруглені бульбашки
 */
@Composable
fun StandardBubble(
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = if (isOwn) {
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 4.dp,
                bottomEnd = 20.dp
            )
        },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            content = content
        )
    }
}

/**
 * 💭 КОМІКС СТИЛЬ - з хвостиком як в коміксах
 */
@Composable
fun ComicBubble(
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Форма з хвостиком
    val bubbleShape = GenericShape { size, _ ->
        val tailWidth = 20f
        val tailHeight = 15f
        val cornerRadius = 40f

        if (isOwn) {
            // Хвостик справа знизу
            moveTo(cornerRadius, 0f)
            lineTo(size.width - cornerRadius, 0f)
            quadraticBezierTo(size.width, 0f, size.width, cornerRadius)
            lineTo(size.width, size.height - cornerRadius - tailHeight)
            quadraticBezierTo(size.width, size.height - tailHeight, size.width - cornerRadius, size.height - tailHeight)
            lineTo(size.width - 40f, size.height - tailHeight)
            lineTo(size.width - 10f, size.height)
            lineTo(size.width - 50f, size.height - tailHeight)
            lineTo(cornerRadius, size.height - tailHeight)
            quadraticBezierTo(0f, size.height - tailHeight, 0f, size.height - cornerRadius - tailHeight)
            lineTo(0f, cornerRadius)
            quadraticBezierTo(0f, 0f, cornerRadius, 0f)
        } else {
            // Хвостик зліва знизу
            moveTo(cornerRadius, 0f)
            lineTo(size.width - cornerRadius, 0f)
            quadraticBezierTo(size.width, 0f, size.width, cornerRadius)
            lineTo(size.width, size.height - cornerRadius - tailHeight)
            quadraticBezierTo(size.width, size.height - tailHeight, size.width - cornerRadius, size.height - tailHeight)
            lineTo(50f, size.height - tailHeight)
            lineTo(10f, size.height)
            lineTo(40f, size.height - tailHeight)
            lineTo(cornerRadius, size.height - tailHeight)
            quadraticBezierTo(0f, size.height - tailHeight, 0f, size.height - cornerRadius - tailHeight)
            lineTo(0f, cornerRadius)
            quadraticBezierTo(0f, 0f, cornerRadius, 0f)
        }
        close()
    }

    Box(
        modifier = modifier
            .shadow(3.dp, bubbleShape)
            .clip(bubbleShape)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(content = content)
    }
}

/**
 * 📱 TELEGRAM СТИЛЬ - мінімалістичні кутасті
 */
@Composable
fun TelegramBubble(
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isOwn) 12.dp else 2.dp,
                    bottomEnd = if (isOwn) 2.dp else 12.dp
                )
            )
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Column(content = content)
    }
}

/**
 * ⚪ МІНІМАЛ СТИЛЬ - прості без тіней
 */
@Composable
fun MinimalBubble(
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(content = content)
    }
}

/**
 * ✨ МОДЕРН СТИЛЬ - градієнти та glass morphism
 */
@Composable
fun ModernBubble(
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val gradientBrush = if (isOwn) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFDCF8C6),
                Color(0xFFC8E6C9)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF5F5F5),
                Color(0xFFE8E8E8)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(gradientBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(content = content)
    }
}

/**
 * 🔴 РЕТРО СТИЛЬ - яскраві кольори з товстими рамками
 */
@Composable
fun RetroBubble(
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val retroBg = if (isOwn) {
        Color(0xFFFFE082)  // Жовтий
    } else {
        Color(0xFFB39DDB)  // Фіолетовий
    }

    val borderColor = if (isOwn) {
        Color(0xFFFF6F00)  // Помаранчевий
    } else {
        Color(0xFF5E35B1)  // Темно-фіолетовий
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(retroBg)
            .border(
                width = 3.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Column(content = content)
    }
}

/**
 * 🎨 Фабрика для створення бульбашки відповідного стилю
 */
@Composable
fun StyledBubble(
    bubbleStyle: BubbleStyle,
    isOwn: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    when (bubbleStyle) {
        BubbleStyle.STANDARD -> StandardBubble(isOwn, bgColor, modifier, content)
        BubbleStyle.COMIC -> ComicBubble(isOwn, bgColor, modifier, content)
        BubbleStyle.TELEGRAM -> TelegramBubble(isOwn, bgColor, modifier, content)
        BubbleStyle.MINIMAL -> MinimalBubble(isOwn, bgColor, modifier, content)
        BubbleStyle.MODERN -> ModernBubble(isOwn, bgColor, modifier, content)
        BubbleStyle.RETRO -> RetroBubble(isOwn, bgColor, modifier, content)
    }
}
