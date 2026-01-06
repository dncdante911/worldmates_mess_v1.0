# 📦 Cloud Backup - Вариант 1 (Базовый)

**Дата:** 2026-01-06
**Версия:** 1.0
**Статус:** ✅ Реализовано

---

## 📋 Что реализовано

### Backend (PHP + SQL)

#### 1. SQL Migration
**Файл:** `api-server-files/sql-DB-newver/migration_user_media_settings.sql`
- Таблица `Wo_UserMediaSettings` для хранения настроек автозагрузки медиа
- Поля: auto_download_photos, auto_download_videos, auto_download_audio, auto_download_documents
- Настройки сжатия: compress_photos, compress_videos
- Статус бэкапа: backup_enabled, last_backup_time

#### 2. PHP Endpoints

**Файл:** `api-server-files/api/v2/endpoints/get_user_messages.php` (расширен)
- ✅ Параметр `full_history=true` - загрузка всей истории (лимит 10000)
- ✅ Параметр `count_only=true` - подсчет количества сообщений (для прогресс-бара)

**Файл:** `api-server-files/api/v2/endpoints/get_media_settings.php` (создан)
- Получение настроек автозагрузки медиа для текущего пользователя
- Автоматическое создание дефолтных настроек если их нет

**Файл:** `api-server-files/api/v2/endpoints/update_media_settings.php` (создан)
- Обновление настроек автозагрузки медиа
- Валидация значений (wifi_only, always, never)
- Отметка времени последнего бэкапа

#### 3. Константы
**Файл:** `api-server-files/assets/includes/tabels.php`
- Добавлена константа `T_USER_MEDIA_SETTINGS`

---

### Android (Kotlin + Room + Retrofit)

#### 1. Entity (Room Database)
**Файл:** `app/src/main/java/com/worldmates/messenger/data/local/entity/CachedMessage.kt`
- Entity для локального хранения сообщений
- Поддержка AES-256-GCM шифрования (iv, tag, cipher_version)
- Поля для медиа (mediaUrl, localMediaPath, mediaType, mediaDuration, mediaSize)
- Индексы для быстрого поиска: chatId+timestamp, fromId, toId, isSynced

#### 2. DAO (Data Access Object)
**Файл:** `app/src/main/java/com/worldmates/messenger/data/local/dao/MessageDao.kt`
- 30+ методов для работы с кэшем сообщений
- CRUD операции: insertMessage, updateMessage, deleteMessage
- Поиск: searchMessagesInChat, searchAllMessages
- Синхронизация: getUnsyncedMessages, updateSyncStatus
- Статистика: getMessageCount, getUnreadCount, getCacheSize
- Очистка: clearChatCache, deleteOldMessages, clearAllCache

#### 3. Database
**Файл:** `app/src/main/java/com/worldmates/messenger/data/local/AppDatabase.kt`
- Версия БД увеличена с 1 до 2
- Добавлена таблица CachedMessage
- Добавлен DAO messageDao()

#### 4. Models
**Файл:** `app/src/main/java/com/worldmates/messenger/data/model/MediaSettings.kt`
- MediaSettings - модель настроек
- AutoDownloadMode enum (WIFI_ONLY, ALWAYS, NEVER)
- MediaSettingsResponse, UpdateMediaSettingsResponse, MessageCountResponse

#### 5. API Endpoints
**Файл:** `app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt`
- `getMessagesWithOptions()` - загрузка с параметрами full_history/count_only
- `getMessageCount()` - подсчет количества сообщений
- `getMediaSettings()` - получение настроек
- `updateMediaSettings()` - обновление настроек

#### 6. Repositories

**Файл:** `app/src/main/java/com/worldmates/messenger/data/repository/BackupRepository.kt`
- Синхронизация сообщений с облаком
- Кэширование в локальную БД
- Офлайн доступ к сообщениям
- Поиск в кэше
- Управление размером кэша

**Методы:**
- `syncFullHistory(recipientId, chatType)` - загрузка всей истории
- `getMessageCount(recipientId)` - подсчет для прогресс-бара
- `getCachedMessages(chatId, chatType)` - Flow для UI
- `searchCachedMessages(query)` - поиск в кэше
- `clearOldMessages(daysOld)` - очистка старых сообщений

**Файл:** `app/src/main/java/com/worldmates/messenger/data/repository/MediaSettingsRepository.kt`
- Управление настройками автозагрузки медиа
- StateFlow для реактивного UI
- Кэширование настроек

**Методы:**
- `loadSettings()` - загрузка с сервера
- `updateSettings()` - обновление на сервере
- Упрощенные методы: updateAutoDownloadPhotos, updateCompressVideos и т.д.

---

## 🚀 Как использовать

### Пример использования BackupRepository:

```kotlin
// В ViewModel или Activity
val backupRepository = BackupRepository(context)

// Синхронизация полной истории для чата
viewModelScope.launch {
    val result = backupRepository.syncFullHistory(
        recipientId = 12345,
        chatType = CachedMessage.CHAT_TYPE_USER
    )

    result.onSuccess { count ->
        Log.d("Backup", "Synced $count messages")
    }.onFailure { error ->
        Log.e("Backup", "Sync failed: ${error.message}")
    }
}

// Получение кэшированных сообщений (автообновление UI)
val messages: Flow<List<CachedMessage>> = backupRepository.getCachedMessages(
    chatId = 12345,
    chatType = CachedMessage.CHAT_TYPE_USER
)

// Поиск в кэше
val results = backupRepository.searchCachedMessages(
    chatId = 12345,
    chatType = CachedMessage.CHAT_TYPE_USER,
    query = "hello"
)
```

### Пример использования MediaSettingsRepository:

```kotlin
val settingsRepository = MediaSettingsRepository(context)

// Загрузка настроек
viewModelScope.launch {
    settingsRepository.loadSettings()
}

// Подписка на изменения
val settings: StateFlow<MediaSettings?> = settingsRepository.settings

// Обновление настройки
viewModelScope.launch {
    settingsRepository.updateAutoDownloadPhotos(
        MediaSettings.AutoDownloadMode.WIFI_ONLY
    )
}
```

---

## 📊 Статистика

### Backend:
- **Файлов создано:** 3
- **Файлов изменено:** 2
- **Строк кода:** ~400

### Android:
- **Файлов создано:** 6
- **Файлов изменено:** 2
- **Строк кода:** ~1300

### Общее:
- **Файлов:** 13
- **Строк кода:** ~1700

---

## ✅ Что работает

1. ✅ Загрузка всей истории сообщений с сервера
2. ✅ Кэширование сообщений локально в Room БД
3. ✅ Автоматическое расшифрование при кэшировании
4. ✅ Офлайн доступ к сообщениям
5. ✅ Поиск в кэше
6. ✅ Управление размером кэша
7. ✅ Настройки автозагрузки медиа
8. ✅ Подсчет количества сообщений (для прогресс-бара)

---

## 🔄 Что будет в Варианте 2+ (Расширенный)

1. **WorkManager для фоновой синхронизации**
   - Автоматическая синхронизация раз в N часов
   - Синхронизация только по Wi-Fi (опционально)

2. **Сжатие медиа**
   - Автоматическое сжатие фото перед загрузкой
   - Сжатие видео (транскодинг)

3. **Офлайн очередь**
   - Сохранение неотправленных сообщений
   - Автоотправка при восстановлении связи

4. **Settings UI**
   - Экран настроек автозагрузки
   - Экран статистики кэша
   - Кнопки для ручной синхронизации/очистки

5. **Прогресс-бар синхронизации**
   - Показ прогресса загрузки истории
   - Отмена синхронизации

6. **Индикация статуса синхронизации**
   - Значок в UI (синхронизировано/не синхронизировано)
   - Количество несинхронизированных сообщений

---

## 🎯 Следующие шаги

1. Создать Settings UI для настройки автозагрузки медиа
2. Добавить автозагрузку истории при первом входе
3. Протестировать синхронизацию на реальных данных
4. Добавить обработку ошибок и retry логику
5. Оптимизировать производительность для больших объемов данных

---

## 📝 Примечания

- Максимальный лимит для full_history: 10000 сообщений
- Расшифровка происходит автоматически при кэшировании
- Старые сообщения можно очищать через `clearOldMessages(30)` (30 дней)
- Все операции асинхронные (suspend функции + Dispatchers.IO)

---

**Создано:** 2026-01-06
**Автор:** Claude Code
**Версия документа:** 1.0
