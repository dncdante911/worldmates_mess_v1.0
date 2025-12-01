# 🚀 Инструкции по настройке WorldMates Messenger

## 📋 Необходимые шаги перед сборкой

### 1. Firebase Configuration

**⚠️ ВАЖНО**: Приложение использует Firebase для push-уведомлений!

1. Перейдите на [Firebase Console](https://console.firebase.google.com/)
2. Создайте новый проект или используйте существующий
3. Добавьте Android приложение с package name: `com.worldmates.messenger`
4. Скачайте файл `google-services.json`
5. Поместите его в корень проекта: `/worldmates_mess_v1.0/google-services.json`

**Без этого файла проект НЕ СОБЕРЕТСЯ!**

Альтернатива для тестирования:
```bash
cp google-services.json.template google-services.json
# Отредактируйте файл, заменив все YOUR_* на реальные значения
```

---

### 2. Структура проекта

Текущая структура нестандартная. Для Android Studio нужно:

**Вариант A: Использовать как есть** (текущая структура)
```
worldmates_mess_v1.0/
├── com/worldmates/messenger/    # Исходники (вместо app/src/main/java/)
├── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

**Вариант B: Стандартная структура** (рекомендуется)
```
worldmates_mess_v1.0/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/worldmates/messenger/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

Для миграции на стандартную структуру:
```bash
mkdir -p app/src/main/java
mv com app/src/main/java/
mv AndroidManifest.xml app/src/main/
mv build.gradle app/
# Создайте новый root build.gradle
```

---

### 3. Зависимости и требования

**SDK Requirements:**
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 34

**Build Tools:**
- Gradle: 8.1.4
- Kotlin: 1.9.20
- Compose: 1.5.8

**Ключевые зависимости:**
```gradle
// Compose UI
implementation("androidx.compose.material3:material3:1.1.2")
implementation("androidx.activity:activity-compose:1.8.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("io.socket:socket.io-client:2.1.1")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging-ktx")

// Image Loading
implementation("io.coil-kt:coil-compose:2.5.0")
```

---

### 4. Сборка проекта

**Через Android Studio:**
1. File → Open → Выберите папку `worldmates_mess_v1.0`
2. Дождитесь синхронизации Gradle
3. Build → Make Project

**Через командную строку:**
```bash
cd worldmates_mess_v1.0

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug
```

---

### 5. Конфигурация сервера

Приложение требует работающий бэкенд:

**API Endpoints** (в Constants.kt):
```kotlin
const val BASE_URL = "https://worldmates.club/api/v2/"
const val SOCKET_URL = "https://worldmates.club:449/"
```

**Необходимые сервисы:**
1. **REST API** - Node.js/PHP сервер на порту 443/80
2. **Socket.IO** - WebSocket сервер на порту 449
3. **TURN Server** - Coturn для WebRTC на порту 3478/5349
4. **MySQL** - База данных

Инструкции по настройке сервера в файле `webrtc+other.md`

---

### 6. Разрешения (Permissions)

Приложение запрашивает:
- ✅ INTERNET - сетевые запросы
- ✅ CAMERA - фото/видео
- ✅ RECORD_AUDIO - голосовые сообщения и звонки
- ✅ READ_MEDIA_* - доступ к медиа файлам (Android 13+)
- ✅ POST_NOTIFICATIONS - push уведомления
- ✅ ACCESS_*_LOCATION - геолокация

Runtime permissions обрабатываются автоматически через Jetpack Compose.

---

### 7. Проблемы и решения

**Ошибка: "google-services.json not found"**
```bash
cp google-services.json.template google-services.json
# Отредактируйте файл с реальными данными Firebase
```

**Ошибка: "Unresolved reference: BuildConfig"**
```gradle
// В build.gradle добавьте:
android {
    buildFeatures {
        buildConfig = true
    }
}
```

**Ошибка компиляции Compose**
```gradle
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.8"
}
```

**Socket.IO не подключается**
- Проверьте что сервер запущен
- Проверьте URL в Constants.kt
- Убедитесь что `usesCleartextTraffic="true"` в манифесте

---

### 8. Тестирование

**Запуск тестов:**
```bash
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

**Эмулятор:**
- Рекомендуется: Pixel 5, API 34
- Минимум: API 24

**Реальное устройство:**
- Включите USB debugging
- Установите приложение через `adb install`

---

### 9. Дебаг и логирование

Используется Timber для логов:
```kotlin
Timber.d("Debug message")
Timber.e(exception, "Error message")
```

Логи Socket.IO:
```kotlin
// В SocketManager.kt
Log.d("SocketManager", "Connection status")
```

Просмотр логов:
```bash
adb logcat -s WMApplication SocketManager ChatsViewModel
```

---

### 10. Release build

Для production сборки:

1. Создайте keystore:
```bash
keytool -genkey -v -keystore worldmates.keystore \
  -alias worldmates -keyalg RSA -keysize 2048 -validity 10000
```

2. Обновите `build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("worldmates.keystore")
            storePassword "your_password"
            keyAlias "worldmates"
            keyPassword "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

3. Соберите release APK:
```bash
./gradlew assembleRelease
```

APK будет в: `app/build/outputs/apk/release/`

---

## 🎯 Checklist перед первым запуском

- [ ] `google-services.json` скопирован и настроен
- [ ] Gradle sync завершен успешно
- [ ] Бэкенд сервер запущен и доступен
- [ ] TURN сервер настроен (для звонков)
- [ ] MySQL база данных создана
- [ ] Разрешения в манифесте корректны
- [ ] Версия Kotlin и Compose совпадают

---

## 📞 Поддержка

При возникновении проблем:
1. Проверьте логи: `adb logcat`
2. Читайте `README.md` и `webrtc+other.md`
3. Проверьте версии зависимостей в `build.gradle`

---

Удачной сборки! 🚀
