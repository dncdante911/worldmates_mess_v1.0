package com.worldmates.messenger.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.UserSession
import com.worldmates.messenger.data.model.Group
import com.worldmates.messenger.data.model.User
import com.worldmates.messenger.network.FileManager
import com.worldmates.messenger.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val api = RetrofitClient.apiService
    private val fileManager = FileManager(application.applicationContext)

    private val _username = MutableStateFlow(UserSession.username ?: "")
    val username: StateFlow<String> = _username

    private val _avatar = MutableStateFlow(UserSession.avatar)
    val avatar: StateFlow<String?> = _avatar

    private val _userId = MutableStateFlow(UserSession.userId)
    val userId: StateFlow<Long> = _userId

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    private val _myGroups = MutableStateFlow<List<Group>>(emptyList())
    val myGroups: StateFlow<List<Group>> = _myGroups

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _username.value = UserSession.username ?: ""
            _avatar.value = UserSession.avatar
            _userId.value = UserSession.userId
        }
    }

    /**
     * Загрузить полные данные пользователя из API
     * ВРЕМЕННО ОТКЛЮЧЕНО: endpoint ?type=get-user-data может не существовать
     * Используем данные из UserSession
     */
    fun fetchUserData() {
        viewModelScope.launch {
            try {
                // Создаем User объект из данных UserSession
                // Полные данные пользователя обычно загружаются при авторизации
                // и должны быть доступны через основной API WoWonder

                // Пока используем базовые данные из сессии
                _username.value = UserSession.username ?: ""
                _avatar.value = UserSession.avatar
                _userId.value = UserSession.userId

                Log.d(TAG, "User data loaded from session")
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading user data from session", e)
            }
        }
    }

    /**
     * Обновить профиль пользователя
     * ВРЕМЕННО: API endpoint ?type=update-user-data должен быть создан на сервере
     */
    fun updateUserProfile(
        firstName: String? = null,
        lastName: String? = null,
        about: String? = null,
        birthday: String? = null,
        gender: String? = null,
        phoneNumber: String? = null,
        website: String? = null,
        working: String? = null,
        address: String? = null,
        city: String? = null,
        school: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // ВРЕМЕННО: показываем сообщение, что функция в разработке
                // Для работы нужно создать соответствующий PHP скрипт на сервере
                _errorMessage.value = "Функція оновлення профілю в розробці. Будь ласка, використовуйте веб-версію worldmates.club для редагування профілю."
                Log.w(TAG, "Update profile API endpoint not implemented yet")

                /*
                // Раскомментировать когда будет создан PHP endpoint
                val accessToken = UserSession.accessToken
                if (accessToken == null) {
                    _errorMessage.value = "Токен доступу відсутній"
                    _isLoading.value = false
                    return@launch
                }

                val response = api.updateUserData(
                    accessToken = accessToken,
                    firstName = firstName,
                    lastName = lastName,
                    about = about,
                    birthday = birthday,
                    gender = gender,
                    phoneNumber = phoneNumber,
                    website = website,
                    working = working,
                    address = address,
                    city = city,
                    school = school
                )

                if (response.apiStatus == 200) {
                    _successMessage.value = "Профіль успішно оновлено"
                    fetchUserData()
                    Log.d(TAG, "User profile updated successfully")
                } else {
                    _errorMessage.value = response.errorMessage ?: "Помилка оновлення профілю"
                    Log.e(TAG, "Error updating profile: ${response.errorMessage}")
                }
                */
            } catch (e: Exception) {
                _errorMessage.value = "Помилка: ${e.message}"
                Log.e(TAG, "Exception updating profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить настройки конфиденциальности
     * ВРЕМЕННО: API endpoint ?type=update-privacy-settings должен быть создан на сервере
     */
    fun updatePrivacySettings(
        followPrivacy: String? = null,
        friendPrivacy: String? = null,
        postPrivacy: String? = null,
        messagePrivacy: String? = null,
        confirmFollowers: String? = null,
        showActivitiesPrivacy: String? = null,
        birthPrivacy: String? = null,
        visitPrivacy: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                _errorMessage.value = "Функція налаштування конфіденційності в розробці. Будь ласка, використовуйте веб-версію worldmates.club."
                Log.w(TAG, "Update privacy settings API endpoint not implemented yet")

                /*
                val accessToken = UserSession.accessToken
                if (accessToken == null) {
                    _errorMessage.value = "Токен доступу відсутній"
                    _isLoading.value = false
                    return@launch
                }

                val response = api.updatePrivacySettings(
                    accessToken = accessToken,
                    followPrivacy = followPrivacy,
                    friendPrivacy = friendPrivacy,
                    postPrivacy = postPrivacy,
                    messagePrivacy = messagePrivacy,
                    confirmFollowers = confirmFollowers,
                    showActivitiesPrivacy = showActivitiesPrivacy,
                    birthPrivacy = birthPrivacy,
                    visitPrivacy = visitPrivacy
                )

                if (response.apiStatus == 200) {
                    _successMessage.value = "Налаштування конфіденційності оновлено"
                    Log.d(TAG, "Privacy settings updated successfully")
                } else {
                    _errorMessage.value = response.errorMessage ?: "Помилка оновлення налаштувань"
                    Log.e(TAG, "Error updating privacy: ${response.errorMessage}")
                }
                */
            } catch (e: Exception) {
                _errorMessage.value = "Помилка: ${e.message}"
                Log.e(TAG, "Exception updating privacy", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновить настройки уведомлений
     * ВРЕМЕННО: API endpoint ?type=update-notification-settings должен быть создан на сервере
     */
    fun updateNotificationSettings(
        emailNotification: Int? = null,
        eLiked: Int? = null,
        eWondered: Int? = null,
        eShared: Int? = null,
        eFollowed: Int? = null,
        eCommented: Int? = null,
        eVisited: Int? = null,
        eLikedPage: Int? = null,
        eMentioned: Int? = null,
        eJoinedGroup: Int? = null,
        eAccepted: Int? = null,
        eProfileWallPost: Int? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                _errorMessage.value = "Функція налаштування сповіщень в розробці. Будь ласка, використовуйте веб-версію worldmates.club."
                Log.w(TAG, "Update notification settings API endpoint not implemented yet")

                /*
                val accessToken = UserSession.accessToken
                if (accessToken == null) {
                    _errorMessage.value = "Токен доступу відсутній"
                    _isLoading.value = false
                    return@launch
                }

                val response = api.updateNotificationSettings(
                    accessToken = accessToken,
                    emailNotification = emailNotification,
                    eLiked = eLiked,
                    eWondered = eWondered,
                    eShared = eShared,
                    eFollowed = eFollowed,
                    eCommented = eCommented,
                    eVisited = eVisited,
                    eLikedPage = eLikedPage,
                    eMentioned = eMentioned,
                    eJoinedGroup = eJoinedGroup,
                    eAccepted = eAccepted,
                    eProfileWallPost = eProfileWallPost
                )

                if (response.apiStatus == 200) {
                    _successMessage.value = "Налаштування сповіщень оновлено"
                    Log.d(TAG, "Notification settings updated successfully")
                } else {
                    _errorMessage.value = response.errorMessage ?: "Помилка оновлення налаштувань"
                    Log.e(TAG, "Error updating notifications: ${response.errorMessage}")
                }
                */
            } catch (e: Exception) {
                _errorMessage.value = "Помилка: ${e.message}"
                Log.e(TAG, "Exception updating notifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Загрузить аватар пользователя
     * ВРЕМЕННО: API endpoint ?type=update-profile-picture должен быть создан на сервере
     */
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                _errorMessage.value = "Функція зміни аватара в розробці. Будь ласка, використовуйте веб-версію worldmates.club."
                Log.w(TAG, "Upload avatar API endpoint not implemented yet")

                /*
                val accessToken = UserSession.accessToken
                if (accessToken == null) {
                    _errorMessage.value = "Токен доступу відсутній"
                    _isLoading.value = false
                    return@launch
                }

                // Копировать файл из URI в кеш
                val file = fileManager.copyUriToCache(uri)
                if (file == null) {
                    _errorMessage.value = "Не вдалося прочитати файл"
                    _isLoading.value = false
                    return@launch
                }

                // Получить MIME-тип
                val mimeType = fileManager.getMimeType(uri) ?: "image/jpeg"

                // Создать RequestBody из файла
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    "avatar",
                    file.name,
                    requestFile
                )

                val response = api.updateProfilePicture(
                    accessToken = accessToken,
                    avatar = filePart
                )

                // Удалить временный файл
                fileManager.deleteFile(file)

                if (response.apiStatus == 200 && response.url != null) {
                    _successMessage.value = "Аватар успішно оновлено"
                    UserSession.avatar = response.url
                    _avatar.value = response.url

                    // Обновить данные пользователя
                    fetchUserData()
                    Log.d(TAG, "Avatar uploaded successfully")
                } else {
                    _errorMessage.value = response.errorMessage ?: "Помилка завантаження аватара"
                    Log.e(TAG, "Error uploading avatar: ${response.errorMessage}")
                }
                */
            } catch (e: Exception) {
                _errorMessage.value = "Помилка: ${e.message}"
                Log.e(TAG, "Exception uploading avatar", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Загрузить группы пользователя
     * Используем endpoint get_chats с фильтром data_type=groups
     */
    fun loadMyGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val accessToken = UserSession.accessToken
                if (accessToken == null) {
                    _errorMessage.value = "Токен доступу відсутній"
                    _isLoading.value = false
                    return@launch
                }

                // Используем существующий endpoint get_chats с фильтром для групп
                val response = api.getChats(
                    accessToken = accessToken,
                    limit = 100,
                    dataType = "groups", // Фильтр только для групп
                    setOnline = 0,
                    offset = 0
                )

                if (response.apiStatus == 200 && response.chats != null) {
                    // Конвертируем Chat объекты в Group объекты
                    val groups = response.chats
                        .filter { it.isGroup || it.chatType == "group" }
                        .map { it.toGroup() }

                    _myGroups.value = groups
                    Log.d(TAG, "Loaded ${groups.size} groups from chats")
                } else {
                    _errorMessage.value = response.errorMessage ?: "Помилка завантаження груп"
                    Log.e(TAG, "Error loading groups: ${response.errorMessage}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Помилка: ${e.message}"
                Log.e(TAG, "Exception loading groups", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUsername(newUsername: String) {
        _username.value = newUsername
        UserSession.username = newUsername
    }

    fun updateAvatar(newAvatar: String) {
        _avatar.value = newAvatar
        UserSession.avatar = newAvatar
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}
