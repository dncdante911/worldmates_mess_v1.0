package com.worldmates.messenger.ui.messages.selection

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

/**
 * ⚡ Кнопки дій в TopBar (режим вибору)
 *
 * Показує іконки "Редагувати" та "Видалити" справа в шапці.
 * - Кнопка "Вибрати все" (тільки якщо не всі вибрані)
 * - Кнопка "Редагувати" (тільки для 1 свого повідомлення)
 * - Кнопка "Видалити" (червона)
 * - Кнопка "Закрити" режим вибору
 *
 * @param selectedCount Кількість вибраних повідомлень
 * @param totalCount Загальна кількість повідомлень
 * @param canEdit Чи можна редагувати (тільки для 1 свого повідомлення)
 * @param onEdit Callback для редагування
 * @param onDelete Callback для видалення
 * @param onSelectAll Callback для вибору всіх повідомлень
 * @param onClose Callback для закриття режиму вибору
 */
@Composable
fun SelectionTopBarActions(
    selectedCount: Int,
    totalCount: Int = 0,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit = {},
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка "Вибрати все" (тільки якщо не всі вибрані)
        if (totalCount > 0 && selectedCount < totalCount) {
            IconButton(onClick = onSelectAll) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Вибрати все",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Кнопка "Редагувати" (тільки для 1 свого повідомлення)
        if (canEdit && selectedCount == 1) {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Редагувати",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Кнопка "Видалити"
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Видалити",
                tint = MaterialTheme.colorScheme.error
            )
        }

        // Кнопка "Закрити режим вибору"
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Закрити",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
