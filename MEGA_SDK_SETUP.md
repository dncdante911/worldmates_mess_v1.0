# 📦 Інструкція з підключення MEGA SDK

## ⚠️ Чому MEGA SDK недоступний?

MEGA SDK не публікується в Maven Central, тому його потрібно додавати вручну.

## 🎯 Варіант 1: Завантажити готовий AAR файл

### Крок 1: Завантажити MEGA SDK

```bash
# Клонувати репозиторій MEGA SDK
git clone https://github.com/meganz/sdk.git
cd sdk

# АБО завантажити останній release
wget https://github.com/meganz/sdk/releases/latest/download/mega-sdk-android.aar
```

### Крок 2: Додати AAR в проект

1. Створіть директорію `app/libs/` якщо її немає
2. Скопіюйте `mega-sdk-android.aar` в `app/libs/`
3. У файлі `app/build.gradle` додайте:

```gradle
dependencies {
    // ... інші залежності

    // MEGA SDK (manual AAR)
    implementation files('libs/mega-sdk-android.aar')
}
```

### Крок 3: Розкоментувати код в MegaBackupManager.kt

Відкрийте `app/src/main/java/com/worldmates/messenger/data/backup/MegaBackupManager.kt` і замініть заглушку на повну реалізацію (код знаходиться в коментарях у файлі).

### Крок 4: Додати App Key

1. Зареєструйтеся на https://mega.nz
2. Перейдіть на https://mega.nz/developers
3. Створіть новий App
4. Скопіюйте App Key
5. Вставте в `MegaBackupManager.kt`:

```kotlin
private const val MEGA_APP_KEY = "ВАШ_APP_KEY"
```

## 🌐 Варіант 2: Використати MEGA REST API

MEGA надає REST API для роботи з файлами без SDK:

### Документація:
- API Reference: https://mega.nz/developers
- API Commands: https://github.com/meganz/MEGAcmd

### Приклад реалізації:

```kotlin
// В MegaBackupManager.kt замініть методи на HTTP запити

suspend fun uploadFile(localFile: File): Boolean = withContext(Dispatchers.IO) {
    try {
        // 1. Логін через REST API
        val loginResponse = megaApiLogin(email, password)

        // 2. Отримати URL для завантаження
        val uploadUrl = megaApiGetUploadUrl()

        // 3. Завантажити файл
        val request = Request.Builder()
            .url(uploadUrl)
            .post(localFile.asRequestBody())
            .build()

        val response = httpClient.newCall(request).execute()
        response.isSuccessful

    } catch (e: Exception) {
        Log.e(TAG, "Upload failed", e)
        false
    }
}
```

## 🎯 Варіант 3: Використати тільки Google Drive + Dropbox

Найпростіший варіант - використовувати тільки **Google Drive** та **Dropbox**, які вже повністю інтегровані та працюють.

### Переваги:
✅ Не потрібно додавати додаткові залежності
✅ Повна підтримка OAuth 2.0
✅ Автоматичне оновлення токенів
✅ Готові до використання

### Налаштування:

**Google Drive:**
```kotlin
// GoogleDriveBackupManager.kt
private const val GOOGLE_CLIENT_ID = "ВАШ_CLIENT_ID.apps.googleusercontent.com"
```

**Dropbox:**
```kotlin
// DropboxBackupManager.kt
private const val DROPBOX_APP_KEY = "ВАШ_APP_KEY"
private const val DROPBOX_APP_SECRET = "ВАШ_APP_SECRET"
```

## 📊 Порівняння провайдерів

| Провайдер | Безкоштовно | Складність | Статус |
|-----------|-------------|------------|--------|
| **LOCAL_SERVER** | Залежить від сервера | ✅ Легко | ✅ Працює |
| **Google Drive** | 15 GB | ✅ Легко | ✅ Працює |
| **Dropbox** | 2 GB | ✅ Легко | ✅ Працює |
| **MEGA** | 20 GB | ⚠️ Вручну | ⚠️ Заглушка |

## 🚀 Рекомендація

**Для більшості користувачів:**
Використовуйте **Google Drive** або **Dropbox** - вони вже готові та працюють.

**Для тих, кому потрібно багато місця:**
MEGA дає 20 GB безкоштовно, але потребує ручного налаштування (Варіант 1 або 2).

**За замовчуванням:**
Всі бекапи зберігаються на **LOCAL_SERVER** (ваш сервер worldmates.club).
