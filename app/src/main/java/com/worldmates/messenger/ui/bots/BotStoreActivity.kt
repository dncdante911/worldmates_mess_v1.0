package com.worldmates.messenger.ui.bots

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.worldmates.messenger.ui.messages.MessagesActivity
import com.worldmates.messenger.ui.theme.ThemeManager
import com.worldmates.messenger.ui.theme.WorldMatesThemedApp
import com.worldmates.messenger.ui.theme.ExpressiveIconButton
import com.worldmates.messenger.ui.theme.GlassTopAppBar

/**
 * BotStoreActivity - Bot Store как в Telegram
 *
 * Точка входа для пользователя:
 * - Каталог ботов (поиск, категории, популярные)
 * - Управление своими ботами (My Bots)
 * - Создание нового бота
 * - Клик по боту -> открывает чат с ботом (MessagesActivity с is_bot=true)
 *
 * Доступ:
 * - Из ChatsActivity через FAB на вкладке Чаты
 * - Из меню / поиска
 */
class BotStoreActivity : AppCompatActivity() {

    private lateinit var botViewModel: BotViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.initialize(this)

        botViewModel = ViewModelProvider(this).get(BotViewModel::class.java)

        setContent {
            WorldMatesThemedApp {
                BotStoreScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BotStoreScreen() {
        // Навигация: catalog -> detail -> create -> myBots
        var currentScreen by remember { mutableStateOf<BotScreen>(BotScreen.Catalog) }

        when (val screen = currentScreen) {
            is BotScreen.Catalog -> {
                BotCatalogScreen(
                    viewModel = botViewModel,
                    onBotClick = { bot ->
                        currentScreen = BotScreen.Detail(bot.botId)
                    },
                    onMyBotsClick = {
                        currentScreen = BotScreen.MyBots
                    },
                    onBackClick = { finish() }
                )
            }
            is BotScreen.Detail -> {
                BotDetailScreen(
                    viewModel = botViewModel,
                    botId = screen.botId,
                    onStartChat = { bot ->
                        // Как в Telegram: клик "Start" -> открываем чат с ботом
                        startActivity(Intent(this@BotStoreActivity, MessagesActivity::class.java).apply {
                            putExtra("recipient_id", bot.botId.hashCode().toLong())
                            putExtra("recipient_name", bot.displayName)
                            putExtra("recipient_avatar", bot.avatar ?: "")
                            putExtra("is_bot", true)
                            putExtra("bot_id", bot.botId)
                            putExtra("bot_username", bot.username)
                        })
                    },
                    onBackClick = {
                        currentScreen = BotScreen.Catalog
                    }
                )
            }
            is BotScreen.MyBots -> {
                BotManagementScreen(
                    viewModel = botViewModel,
                    onCreateBotClick = {
                        currentScreen = BotScreen.CreateBot
                    },
                    onBackClick = {
                        currentScreen = BotScreen.Catalog
                    }
                )
            }
            is BotScreen.CreateBot -> {
                CreateBotScreen(
                    viewModel = botViewModel,
                    onBackClick = {
                        currentScreen = BotScreen.MyBots
                    }
                )
            }
        }
    }
}

/**
 * Экраны навигации внутри BotStoreActivity
 */
sealed class BotScreen {
    object Catalog : BotScreen()
    data class Detail(val botId: String) : BotScreen()
    object MyBots : BotScreen()
    object CreateBot : BotScreen()
}
