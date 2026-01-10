# ☁️ Інтеграція облачних провайдерів для бекапу

**Дата:** 2026-01-10
**Версія:** 1.0

---

## 📋 Зміст

1. [Google Drive API](#google-drive)
2. [MEGA API](#mega)
3. [Dropbox API](#dropbox)
4. [Структура бекапу](#структура-бекапу)
5. [Як працює синхронізація](#синхронізація)

---

## <a name="google-drive"></a>1️⃣ Google Drive API

### Крок 1: Налаштування Google Cloud Console

1. Перейти на https://console.cloud.google.com/
2. Створити новий проєкт "WorldMates Messenger"
3. Увімкнути **Google Drive API**:
   - APIs & Services → Library
   - Знайти "Google Drive API"
   - Натиснути Enable

4. Створити OAuth 2.0 credentials:
   - APIs & Services → Credentials
   - Create Credentials → OAuth 2.0 Client ID
   - Application type: Android
   - Package name: `com.worldmates.messenger`
   - SHA-1: отримати командою:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

### Крок 2: Gradle Dependencies

```kotlin
// build.gradle (Project level)
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

// build.gradle (:app)
dependencies {
    // Google Drive API
    implementation 'com.google.android.gms:play-services-auth:21.0.0'
    implementation 'com.google.apis:google-api-services-drive:v3-rev20231212-2.0.0'
    implementation 'com.google.api-client:google-api-client-android:2.2.0'
    implementation 'com.google.http-client:google-http-client-gson:1.44.1'
}
```

### Крок 3: Код для авторизації (Kotlin)

```kotlin
// GoogleDriveBackupManager.kt
package com.worldmates.messenger.data.backup

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class GoogleDriveBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveBackup"
        private const val BACKUP_FOLDER_NAME = "WorldMates_Backup"
    }

    private var driveService: Drive? = null

    /**
     * Крок 1: Авторизація
     */
    fun requestSignIn(): Intent {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val client = GoogleSignIn.getClient(context, signInOptions)
        return client.signInIntent
    }

    /**
     * Крок 2: Після авторизації - створити Drive service
     */
    fun handleSignInResult(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("WorldMates Messenger")
            .build()
    }

    /**
     * Перевірити чи авторизований
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && driveService != null
    }

    /**
     * Завантажити файл на Google Drive
     */
    suspend fun uploadFile(
        localFile: File,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Not signed in")

            // Знайти або створити папку для бекапів
            val folderId = getOrCreateBackupFolder()

            // Metadata файлу
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = fileName
            fileMetadata.parents = listOf(folderId)

            // Завантажити файл
            val mediaContent = com.google.api.client.http.FileContent(mimeType, localFile)
            val file = service.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name")
                .execute()

            Log.d(TAG, "✅ File uploaded: ${file.name} (${file.id})")
            file.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed: ${e.message}", e)
            null
        }
    }

    /**
     * Скачати файл з Google Drive
     */
    suspend fun downloadFile(fileId: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Not signed in")

            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)

            Log.d(TAG, "✅ File downloaded: $fileId")
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download failed: ${e.message}", e)
            null
        }
    }

    /**
     * Знайти або створити папку для бекапів
     */
    private suspend fun getOrCreateBackupFolder(): String = withContext(Dispatchers.IO) {
        val service = driveService ?: throw Exception("Not signed in")

        // Пошук існуючої папки
        val result = service.files().list()
            .setQ("name='$BACKUP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val folder = result.files.firstOrNull()

        if (folder != null) {
            Log.d(TAG, "📁 Backup folder found: ${folder.id}")
            return@withContext folder.id
        }

        // Створити нову папку
        val folderMetadata = com.google.api.services.drive.model.File()
        folderMetadata.name = BACKUP_FOLDER_NAME
        folderMetadata.mimeType = "application/vnd.google-apps.folder"

        val createdFolder = service.files()
            .create(folderMetadata)
            .setFields("id")
            .execute()

        Log.d(TAG, "✅ Backup folder created: ${createdFolder.id}")
        createdFolder.id
    }

    /**
     * Список всіх бекапів
     */
    suspend fun listBackups(): List<BackupFile> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Not signed in")
            val folderId = getOrCreateBackupFolder()

            val result = service.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name, createdTime, size)")
                .setOrderBy("createdTime desc")
                .execute()

            result.files.map { file ->
                BackupFile(
                    id = file.id,
                    name = file.name,
                    size = file.size ?: 0L,
                    createdTime = file.createdTime?.value ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ List backups failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Видалити бекап
     */
    suspend fun deleteBackup(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: throw Exception("Not signed in")
            service.files().delete(fileId).execute()
            Log.d(TAG, "✅ Backup deleted: $fileId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete failed: ${e.message}", e)
            false
        }
    }
}

data class BackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: Long
)
```

### Крок 4: Використання в Activity

```kotlin
// CloudBackupActivity.kt
class CloudBackupActivity : ComponentActivity() {

    private lateinit var googleDriveManager: GoogleDriveBackupManager

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                googleDriveManager.handleSignInResult(account)
                Toast.makeText(this, "✅ Google Drive підключено", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Log.e(TAG, "Sign in failed: ${e.message}")
                Toast.makeText(this, "❌ Помилка авторизації", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleDriveManager = GoogleDriveBackupManager(this)

        setContent {
            CloudBackupScreen(
                onConnectGoogleDrive = {
                    val signInIntent = googleDriveManager.requestSignIn()
                    signInLauncher.launch(signInIntent)
                },
                onCreateBackup = {
                    lifecycleScope.launch {
                        createBackup()
                    }
                }
            )
        }
    }

    private suspend fun createBackup() {
        // Створити JSON з повідомленнями
        val backupData = createBackupJson()
        val backupFile = File(cacheDir, "backup_${System.currentTimeMillis()}.json")
        backupFile.writeText(backupData)

        // Завантажити на Google Drive
        val fileId = googleDriveManager.uploadFile(
            localFile = backupFile,
            fileName = backupFile.name,
            mimeType = "application/json"
        )

        if (fileId != null) {
            Toast.makeText(this, "✅ Бекап створено", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## <a name="mega"></a>2️⃣ MEGA API

### Gradle Dependencies

```kotlin
dependencies {
    // MEGA SDK
    implementation 'nz.mega.sdk:sdk:4.23.1'
}
```

### Код авторизації

```kotlin
class MegaBackupManager(private val context: Context) {

    private val megaApi = MegaApiAndroid(
        "YOUR_MEGA_APP_KEY", // Отримати на mega.io/developers
        context.filesDir.absolutePath
    )

    /**
     * Авторізація
     */
    suspend fun login(email: String, password: String): Boolean = suspendCoroutine { continuation ->
        megaApi.login(email, password, object : MegaRequestListenerInterface {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
                if (error.errorCode == MegaError.API_OK) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        })
    }

    /**
     * Завантажити файл
     */
    suspend fun uploadFile(localFile: File): Boolean = suspendCoroutine { continuation ->
        val megaParent = megaApi.rootNode

        megaApi.startUpload(
            localFile.absolutePath,
            megaParent,
            object : MegaTransferListenerInterface {
                override fun onTransferFinish(api: MegaApiJava, transfer: MegaTransfer, error: MegaError) {
                    continuation.resume(error.errorCode == MegaError.API_OK)
                }
            }
        )
    }
}
```

**API Key:**
- Зареєструватись на https://mega.io/developers
- Створити app
- Отримати APP_KEY

---

## <a name="dropbox"></a>3️⃣ Dropbox API

### Gradle Dependencies

```kotlin
dependencies {
    // Dropbox SDK
    implementation 'com.dropbox.core:dropbox-core-sdk:5.4.5'
    implementation 'com.dropbox.core:dropbox-android-sdk:5.4.5'
}
```

### AndroidManifest.xml

```xml
<activity
    android:name="com.dropbox.core.android.AuthActivity"
    android:launchMode="singleTask"
    android:configChanges="orientation|keyboard">
    <intent-filter>
        <data android:scheme="db-YOUR_APP_KEY" />
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### Код авторизації

```kotlin
class DropboxBackupManager(private val context: Context) {

    companion object {
        private const val APP_KEY = "YOUR_DROPBOX_APP_KEY"
    }

    private var dbxClient: DbxClientV2? = null

    /**
     * Почати OAuth авторизацію
     */
    fun startOAuth(activity: Activity) {
        Auth.startOAuth2Authentication(activity, APP_KEY)
    }

    /**
     * Після повернення з OAuth
     */
    fun finishOAuth() {
        val credential = Auth.getDbxCredential()
        if (credential != null) {
            val config = DbxRequestConfig.newBuilder("WorldMates").build()
            dbxClient = DbxClientV2(config, credential.toString())
        }
    }

    /**
     * Завантажити файл
     */
    suspend fun uploadFile(localFile: File, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dbxClient ?: return@withContext false

            localFile.inputStream().use { inputStream ->
                client.files().uploadBuilder(remotePath)
                    .uploadAndFinish(inputStream)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}")
            false
        }
    }

    /**
     * Скачати файл
     */
    suspend fun downloadFile(remotePath: String, localFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = dbxClient ?: return@withContext false

            localFile.outputStream().use { outputStream ->
                client.files().download(remotePath).download(outputStream)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            false
        }
    }
}
```

**API Credentials:**
1. Створити app на https://www.dropbox.com/developers/apps
2. Вибрати "Scoped access"
3. Permissions: `files.content.write`, `files.content.read`
4. Отримати App key та App secret

---

## <a name="структура-бекапу"></a>📦 Структура бекапу

### Формат бекапу (ZIP архів):

```
worldmates_backup_2026-01-10.zip
├── manifest.json              # Метадані бекапу
├── messages.db                # База даних повідомлень (SQLite або JSON)
├── contacts.json              # Контакти
├── groups.json                # Групи
├── channels.json              # Канали
├── settings.json              # Налаштування
└── media/                     # Медіа файли
    ├── photos/
    ├── videos/
    ├── audio/
    └── documents/
```

### manifest.json:

```json
{
  "version": "2.0",
  "created_at": 1704889200000,
  "user_id": 123,
  "device_id": "android_abc123",
  "app_version": "2.0-EDIT-FIX",
  "encryption": "AES-256-GCM",
  "total_size": 157286400,
  "files": {
    "messages": 1234,
    "media": 567,
    "contacts": 89,
    "groups": 12,
    "channels": 5
  }
}
```

---

## <a name="синхронізація"></a>🔄 Як працює синхронізація

### При вході з нового пристрою:

```
1️⃣ Користувач логінується
2️⃣ App перевіряє: чи є бекап на сервері?
3️⃣ Якщо ТАК:
   ├── Показати діалог: "Відновити дані з бекапу?"
   ├── Скачати ZIP з сервера/Google Drive
   ├── Розпакувати
   ├── Імпортувати в локальну БД
   └── Скачати медіа файли (поступово, в фоні)
4️⃣ Якщо НІ:
   └── Почати з чистого листа
```

### Автоматичний бекап:

```
🔄 Кожні 24 години (або ручно):
1️⃣ Експорт всіх даних в ZIP
2️⃣ Шифрування AES-256-GCM
3️⃣ Завантаження на:
   ├── Ваш сервер (завжди)
   └── Обраний cloud provider (опціонально)
```

---

## 🎯 План реалізації

### Пріоритет 1 (Зараз):
- ✅ Статистика (готово)
- 🔄 Експорт/імпорт JSON
- 🔄 Google Drive інтеграція

### Пріоритет 2:
- MEGA інтеграція
- Dropbox інтеграція
- Автоматичний бекап (WorkManager)

### Пріоритет 3:
- Шифрування бекапів
- Інкрементальний бекап (тільки зміни)
- Compression (ZIP)

---

**Створено:** 2026-01-10
**Оновлено:** 2026-01-10
**Статус:** В розробці
