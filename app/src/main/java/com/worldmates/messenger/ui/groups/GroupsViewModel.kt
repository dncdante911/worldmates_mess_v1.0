// ============ GroupsViewModel.kt ============

package com.worldmates.messenger.ui.groups

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.CreateGroupRequest
import com.worldmates.messenger.data.model.Group
import com.worldmates.messenger.data.model.GroupMember
import com.worldmates.messenger.network.RetrofitClient
import com.worldmates.messenger.network.SearchUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType

class GroupsViewModel : ViewModel() {

    private val _groupList = MutableStateFlow<List<Group>>(emptyList())
    val groupList: StateFlow<List<Group>> = _groupList

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers

    private val _availableUsers = MutableStateFlow<List<SearchUser>>(emptyList())
    val availableUsers: StateFlow<List<SearchUser>> = _availableUsers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCreatingGroup = MutableStateFlow(false)
    val isCreatingGroup: StateFlow<Boolean> = _isCreatingGroup

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Добавляем задержку перед первым запросом
        // чтобы токен успел активироваться на сервере
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500) // 2.5 секунды задержка (чуть больше чем в ChatsViewModel)
            fetchGroups()
        }
    }

    fun fetchGroups() {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Використовуємо новий API group_chat_v2.php
                val response = RetrofitClient.apiService.getGroups(
                    accessToken = UserSession.accessToken!!,
                    limit = 100
                )

                if (response.apiStatus == 200 && response.groups != null) {
                    // Отримуємо готові Group об'єкти з нового API
                    _groupList.value = response.groups!!
                    _error.value = null
                    Log.d("GroupsViewModel", "Завантажено ${response.groups!!.size} груп")
                } else {
                    _error.value = response.errorMessage ?: "Помилка завантаження груп"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка завантаження груп", e)
            }
        }
    }

    fun selectGroup(group: Group) {
        _selectedGroup.value = group
        fetchGroupMembers(group.id)
    }

    fun fetchGroupMembers(groupId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getGroupMembers(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId
                )

                if (response.apiStatus == 200 && response.members != null) {
                    _groupMembers.value = response.members!!
                    Log.d("GroupsViewModel", "Завантажено ${response.members!!.size} членів групи")
                } else {
                    _error.value = "Не вдалося завантажити членів групи"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                Log.e("GroupsViewModel", "Помилка завантаження членів групи", e)
            }
        }
    }

    fun loadAvailableUsers() {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        viewModelScope.launch {
            try {
                // Використовуємо searchUsers з порожнім запитом для отримання всіх користувачів
                val response = RetrofitClient.apiService.searchUsers(
                    accessToken = UserSession.accessToken!!,
                    query = "",
                    limit = 1000
                )

                if (response.apiStatus == 200 && response.users != null) {
                    _availableUsers.value = response.users!!
                    Log.d("GroupsViewModel", "Завантажено ${response.users!!.size} користувачів")
                }
            } catch (e: Exception) {
                Log.e("GroupsViewModel", "Помилка завантаження користувачів", e)
                // Не показуємо помилку користувачу, просто залишаємо список порожнім
            }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        memberIds: List<Long>,
        isPrivate: Boolean,
        onSuccess: () -> Unit
    ) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isCreatingGroup.value = true

        viewModelScope.launch {
            try {
                // Group Chat API: uses 'group_name' and 'parts' parameters
                // Note: description is not supported in group-chat API (only in social groups)

                // Додаємо поточного користувача до parts якщо список порожній
                val allMemberIds = if (memberIds.isEmpty()) {
                    listOf(UserSession.userId!!)
                } else {
                    (memberIds + UserSession.userId!!).distinct()
                }

                val partsString = allMemberIds.joinToString(",")
                Log.d("GroupsViewModel", "Створення групи: name=$name, parts=$partsString")

                val response = RetrofitClient.apiService.createGroup(
                    accessToken = UserSession.accessToken!!,
                    name = name,
                    memberIds = partsString
                )

                Log.d("GroupsViewModel", "Response: $response")

                if (response == null) {
                    _error.value = "Сервер повернув порожню відповідь"
                    Log.e("GroupsViewModel", "Response is null!")
                } else if (response.apiStatus == 200) {
                    _error.value = null
                    fetchGroups()
                    onSuccess()
                    Log.d("GroupsViewModel", "Група створена успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося створити групу (код: ${response.apiStatus})"
                    Log.e("GroupsViewModel", "API error: ${response.errorMessage}, code: ${response.errorCode}")
                }

                _isCreatingGroup.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isCreatingGroup.value = false
                Log.e("GroupsViewModel", "Помилка створення групи", e)
            }
        }
    }

    fun updateGroup(
        groupId: Long,
        name: String
    ) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Group Chat API: only group_name can be updated
                val response = RetrofitClient.apiService.updateGroup(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId,
                    name = name
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    fetchGroups()
                    Log.d("GroupsViewModel", "Група оновлена успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося оновити групу"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка оновлення групи", e)
            }
        }
    }

    fun deleteGroup(groupId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteGroup(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    _selectedGroup.value = null
                    fetchGroups()
                    Log.d("GroupsViewModel", "Група видалена успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося видалити групу"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка видалення групи", e)
            }
        }
    }

    fun addGroupMember(groupId: Long, userId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        viewModelScope.launch {
            try {
                // Group Chat API: uses 'parts' parameter (comma-separated user IDs)
                val response = RetrofitClient.apiService.addGroupMember(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId,
                    userIds = userId.toString()
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    fetchGroupMembers(groupId)
                    Log.d("GroupsViewModel", "Члена групи додано успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося додати члена групи"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                Log.e("GroupsViewModel", "Помилка додавання члена групи", e)
            }
        }
    }

    fun removeGroupMember(groupId: Long, userId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        viewModelScope.launch {
            try {
                // Group Chat API: uses 'parts' parameter (comma-separated user IDs)
                val response = RetrofitClient.apiService.removeGroupMember(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId,
                    userIds = userId.toString()
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    fetchGroupMembers(groupId)
                    Log.d("GroupsViewModel", "Члена групи видалено успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося видалити члена групи"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                Log.e("GroupsViewModel", "Помилка видалення члена групи", e)
            }
        }
    }

    fun setGroupRole(groupId: Long, userId: Long, role: String) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.setGroupAdmin(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId,
                    userId = userId,
                    role = role
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    fetchGroupMembers(groupId)
                    Log.d("GroupsViewModel", "Роль користувача змінена успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося змінити роль користувача"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                Log.e("GroupsViewModel", "Помилка зміни ролі користувача", e)
            }
        }
    }

    fun leaveGroup(groupId: Long) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.leaveGroup(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupId
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    _selectedGroup.value = null
                    fetchGroups()
                    Log.d("GroupsViewModel", "Групу вийшли успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося вийти з групи"
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка виходу з групи", e)
            }
        }
    }

    fun uploadGroupAvatar(groupId: Long, imageUri: android.net.Uri, context: android.content.Context) {
        if (UserSession.accessToken == null) {
            _error.value = "Користувач не авторизований"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Конвертуємо Uri в File
                val file = java.io.File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Створюємо RequestBody для groupId
                val groupIdBody = okhttp3.RequestBody.create(
                    "text/plain".toMediaType(),
                    groupId.toString()
                )

                // Створюємо MultipartBody.Part для аватарки
                val requestFile = okhttp3.RequestBody.create(
                    "image/*".toMediaType(),
                    file
                )
                val avatarPart = okhttp3.MultipartBody.Part.createFormData(
                    "avatar",
                    file.name,
                    requestFile
                )

                // Відправляємо запит
                val response = RetrofitClient.apiService.uploadGroupAvatar(
                    accessToken = UserSession.accessToken!!,
                    groupId = groupIdBody,
                    avatar = avatarPart
                )

                if (response.apiStatus == 200) {
                    _error.value = null
                    fetchGroups() // Оновлюємо список груп
                    Log.d("GroupsViewModel", "Аватарка завантажена успішно")
                } else {
                    _error.value = response.errorMessage ?: "Не вдалося завантажити аватарку"
                }

                // Видаляємо тимчасовий файл
                file.delete()

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.localizedMessage}"
                _isLoading.value = false
                Log.e("GroupsViewModel", "Помилка завантаження аватарки", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("GroupsViewModel", "ViewModel очищена")
    }
}