package com.worldmates.messenger.ui.messages

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp
import com.worldmates.messenger.utils.VoicePlayer
import com.worldmates.messenger.utils.VoiceRecorder

/**
 * ✅ НОВИЙ MessagesActivity - wrapper для MessagesScreen з Phase 2
 *
 * Цей файл створено для сумісності з навігацією з інших активностей
 * (ChatsActivity, GroupsActivity, MyFirebaseMessagingService).
 *
 * Використовує новий MessagesScreen з усіма функціями Phase 2:
 * - Emoji Picker, Sticker Picker
 * - Реакції емоджі
 * - Свайп для відповіді
 * - Галочки прочитання
 * - Анімації
 */
class MessagesActivity : AppCompatActivity() {

    private lateinit var viewModel: MessagesViewModel
    private lateinit var fileManager: FileManager
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var voicePlayer: VoicePlayer

    private var recipientId: Long = 0
    private var groupId: Long = 0
    private var recipientName: String = ""
    private var recipientAvatar: String = ""
    private var isGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Дозволяємо Compose керувати window insets (клавіатура, навігація)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Отримуємо параметри з Intent
        recipientId = intent.getLongExtra("recipient_id", 0)
        groupId = intent.getLongExtra("group_id", 0)
        recipientName = intent.getStringExtra("recipient_name") ?: "Unknown"
        recipientAvatar = intent.getStringExtra("recipient_avatar") ?: ""
        isGroup = intent.getBooleanExtra("is_group", false)

        // Ініціалізуємо утиліти
        fileManager = FileManager(this)
        voiceRecorder = VoiceRecorder(this)
        voicePlayer = VoicePlayer(this)

        viewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)

        // Завантажуємо повідомлення
        if (isGroup) {
            viewModel.initializeGroup(groupId)
        } else {
            viewModel.initialize(recipientId)
        }

        // Ініціалізуємо ThemeManager
        ThemeManager.initialize(this)

        setContent {
            WorldMatesThemedApp {
                MessagesScreenWrapper()
            }
        }
    }

    @Composable
    private fun MessagesScreenWrapper() {
        // Launchers для вибору файлів
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            android.util.Log.d("MessagesActivity", "📸 Image picker result: $uri")
            uri?.let {
                val file = fileManager.copyUriToCache(it)
                android.util.Log.d("MessagesActivity", "📁 Copied file: ${file?.absolutePath}, exists: ${file?.exists()}, size: ${file?.length()}")
                if (file != null && file.exists()) {
                    android.util.Log.d("MessagesActivity", "⬆️ Calling uploadAndSendMedia for image")
                    viewModel.uploadAndSendMedia(file, "image")
                } else {
                    android.util.Log.e("MessagesActivity", "❌ Failed to copy image file")
                }
            }
        }

        val videoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            android.util.Log.d("MessagesActivity", "🎥 Video picker result: $uri")
            uri?.let {
                val file = fileManager.copyUriToCache(it)
                android.util.Log.d("MessagesActivity", "📁 Copied file: ${file?.absolutePath}, exists: ${file?.exists()}, size: ${file?.length()}")
                if (file != null && file.exists()) {
                    android.util.Log.d("MessagesActivity", "⬆️ Calling uploadAndSendMedia for video")
                    viewModel.uploadAndSendMedia(file, "video")
                } else {
                    android.util.Log.e("MessagesActivity", "❌ Failed to copy video file")
                }
            }
        }

        // Використовуємо новий MessagesScreen з Phase 2 функціями
        MessagesScreen(
            viewModel = viewModel,
            fileManager = fileManager,
            voiceRecorder = voiceRecorder,
            voicePlayer = voicePlayer,
            recipientName = recipientName,
            recipientAvatar = recipientAvatar,
            isGroup = isGroup,
            onBackPressed = { finish() },
            onImageSelected = { imagePickerLauncher.launch("image/*") },
            onVideoSelected = { videoPickerLauncher.launch("video/*") }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        voicePlayer.release()
        fileManager.clearOldCacheFiles()
    }
}
