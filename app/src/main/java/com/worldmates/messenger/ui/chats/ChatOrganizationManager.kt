package com.worldmates.messenger.ui.chats

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Менеджер організації чатів: папки, архів, теги.
 * Зберігає дані локально в SharedPreferences.
 */
object ChatOrganizationManager {
    private const val PREFS_NAME = "chat_organization"
    private const val KEY_FOLDERS = "folders"
    private const val KEY_ARCHIVED_CHAT_IDS = "archived_chat_ids"
    private const val KEY_CHAT_TAGS = "chat_tags"
    private const val KEY_CHAT_FOLDER_MAPPING = "chat_folder_mapping"

    private val gson = Gson()

    private val _folders = MutableStateFlow<List<ChatFolder>>(emptyList())
    val folders: StateFlow<List<ChatFolder>> = _folders.asStateFlow()

    private val _archivedChatIds = MutableStateFlow<Set<Long>>(emptySet())
    val archivedChatIds: StateFlow<Set<Long>> = _archivedChatIds.asStateFlow()

    private val _chatTags = MutableStateFlow<Map<Long, List<ChatTag>>>(emptyMap())
    val chatTags: StateFlow<Map<Long, List<ChatTag>>> = _chatTags.asStateFlow()

    private val _chatFolderMapping = MutableStateFlow<Map<Long, String>>(emptyMap())
    val chatFolderMapping: StateFlow<Map<Long, String>> = _chatFolderMapping.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAll()
    }

    private fun loadAll() {
        loadFolders()
        loadArchivedIds()
        loadChatTags()
        loadFolderMapping()
    }

    // ==================== FOLDERS ====================

    private fun loadFolders() {
        val json = prefs?.getString(KEY_FOLDERS, null)
        if (json != null) {
            val type = object : TypeToken<List<ChatFolder>>() {}.type
            _folders.value = gson.fromJson(json, type) ?: defaultFolders()
        } else {
            _folders.value = defaultFolders()
            saveFolders()
        }
    }

    private fun defaultFolders(): List<ChatFolder> = listOf(
        ChatFolder("all", "Усi", "💬", 0),
        ChatFolder("personal", "Особисті", "👤", 1),
        ChatFolder("groups", "Групи", "👥", 2),
        ChatFolder("unread", "Непрочитані", "🔴", 3)
    )

    fun addFolder(name: String, emoji: String) {
        val id = "custom_${System.currentTimeMillis()}"
        val order = _folders.value.size
        val newFolder = ChatFolder(id, name, emoji, order, isCustom = true)
        _folders.value = _folders.value + newFolder
        saveFolders()
    }

    fun removeFolder(folderId: String) {
        _folders.value = _folders.value.filter { it.id != folderId }
        // Remove chats from this folder
        _chatFolderMapping.value = _chatFolderMapping.value.filter { it.value != folderId }
        saveFolders()
        saveFolderMapping()
    }

    fun renameFolder(folderId: String, newName: String, newEmoji: String) {
        _folders.value = _folders.value.map {
            if (it.id == folderId) it.copy(name = newName, emoji = newEmoji) else it
        }
        saveFolders()
    }

    private fun saveFolders() {
        prefs?.edit()?.putString(KEY_FOLDERS, gson.toJson(_folders.value))?.apply()
    }

    // ==================== ARCHIVE ====================

    private fun loadArchivedIds() {
        val json = prefs?.getString(KEY_ARCHIVED_CHAT_IDS, null)
        if (json != null) {
            val type = object : TypeToken<Set<Long>>() {}.type
            _archivedChatIds.value = gson.fromJson(json, type) ?: emptySet()
        }
    }

    fun archiveChat(chatId: Long) {
        _archivedChatIds.value = _archivedChatIds.value + chatId
        saveArchivedIds()
    }

    fun unarchiveChat(chatId: Long) {
        _archivedChatIds.value = _archivedChatIds.value - chatId
        saveArchivedIds()
    }

    fun isArchived(chatId: Long): Boolean = _archivedChatIds.value.contains(chatId)

    private fun saveArchivedIds() {
        prefs?.edit()?.putString(KEY_ARCHIVED_CHAT_IDS, gson.toJson(_archivedChatIds.value))?.apply()
    }

    // ==================== TAGS ====================

    private fun loadChatTags() {
        val json = prefs?.getString(KEY_CHAT_TAGS, null)
        if (json != null) {
            val type = object : TypeToken<Map<Long, List<ChatTag>>>() {}.type
            _chatTags.value = gson.fromJson(json, type) ?: emptyMap()
        }
    }

    fun addTagToChat(chatId: Long, tag: ChatTag) {
        val current = _chatTags.value.toMutableMap()
        val tags = current.getOrDefault(chatId, emptyList()).toMutableList()
        if (tags.none { it.name == tag.name }) {
            tags.add(tag)
            current[chatId] = tags
            _chatTags.value = current
            saveChatTags()
        }
    }

    fun removeTagFromChat(chatId: Long, tagName: String) {
        val current = _chatTags.value.toMutableMap()
        val tags = current.getOrDefault(chatId, emptyList()).filter { it.name != tagName }
        if (tags.isEmpty()) {
            current.remove(chatId)
        } else {
            current[chatId] = tags
        }
        _chatTags.value = current
        saveChatTags()
    }

    fun getTagsForChat(chatId: Long): List<ChatTag> {
        return _chatTags.value[chatId] ?: emptyList()
    }

    fun getAllUsedTags(): List<ChatTag> {
        return _chatTags.value.values.flatten().distinctBy { it.name }
    }

    private fun saveChatTags() {
        prefs?.edit()?.putString(KEY_CHAT_TAGS, gson.toJson(_chatTags.value))?.apply()
    }

    // ==================== FOLDER MAPPING ====================

    private fun loadFolderMapping() {
        val json = prefs?.getString(KEY_CHAT_FOLDER_MAPPING, null)
        if (json != null) {
            val type = object : TypeToken<Map<Long, String>>() {}.type
            _chatFolderMapping.value = gson.fromJson(json, type) ?: emptyMap()
        }
    }

    fun moveChatToFolder(chatId: Long, folderId: String) {
        val current = _chatFolderMapping.value.toMutableMap()
        current[chatId] = folderId
        _chatFolderMapping.value = current
        saveFolderMapping()
    }

    fun removeChatFromFolder(chatId: Long) {
        val current = _chatFolderMapping.value.toMutableMap()
        current.remove(chatId)
        _chatFolderMapping.value = current
        saveFolderMapping()
    }

    fun getChatFolder(chatId: Long): String? = _chatFolderMapping.value[chatId]

    private fun saveFolderMapping() {
        prefs?.edit()?.putString(KEY_CHAT_FOLDER_MAPPING, gson.toJson(_chatFolderMapping.value))?.apply()
    }
}

/**
 * Папка для організації чатів
 */
data class ChatFolder(
    val id: String,
    val name: String,
    val emoji: String,
    val order: Int,
    val isCustom: Boolean = false
)

/**
 * Тег/мітка для чату
 */
data class ChatTag(
    val name: String,
    val color: String = "#2196F3"
) {
    companion object {
        val PRESET_TAGS = listOf(
            ChatTag("Робота", "#FF5722"),
            ChatTag("Сім'я", "#E91E63"),
            ChatTag("Друзі", "#2196F3"),
            ChatTag("Важливе", "#FF9800"),
            ChatTag("Покупки", "#4CAF50"),
            ChatTag("Навчання", "#9C27B0"),
            ChatTag("Проекти", "#607D8B"),
            ChatTag("Подорожі", "#00BCD4")
        )
    }
}
