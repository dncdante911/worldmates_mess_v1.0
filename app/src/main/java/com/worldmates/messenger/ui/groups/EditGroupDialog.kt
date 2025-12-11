package com.worldmates.messenger.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.worldmates.messenger.data.model.Group

@Composable
fun EditGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onUpdate: (name: String) -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean = false
) {
    var groupName by remember { mutableStateOf(group.name) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Редагування групи",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Назва групи
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Назва групи") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка видалення
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("Видалити")
                    }

                    // Кнопка збереження
                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                onUpdate(groupName)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && groupName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0084FF)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Зберегти")
                        }
                    }
                }

                // Кнопка скасування
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Скасувати")
                }
            }
        }
    }

    // Діалог підтвердження видалення
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Видалити групу?") },
            text = { Text("Ви впевнені, що хочете видалити групу \"${group.name}\"? Цю дію неможливо скасувати.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}
