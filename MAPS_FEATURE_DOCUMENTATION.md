# 📍 Google Maps + Location Feature - Документация

## Обзор

Полная интеграция Google Maps для отправки геолокации в WorldMates Messenger.

### Реализованные возможности:
- ✅ **Location Picker** - выбор места на карте
- ✅ **Current Location** - отправка текущего местоположения
- ✅ **Reverse Geocoding** - получение адреса по координатам
- ✅ **Runtime Permissions** - запрос разрешений на геолокацию
- ⏳ **Live Location** - постоянное отслеживание (в разработке)
- ⏳ **Location Message Bubble** - отображение карты в чате (в разработке)

---

## 🚀 Установка и Настройка

### 1. Получить Google Maps API Key

1. Перейди на [Google Cloud Console](https://console.cloud.google.com/)
2. Создай новый проект или выбери существующий
3. Включи **Maps SDK for Android**:
   - Перейди в "APIs & Services" → "Library"
   - Найди "Maps SDK for Android"
   - Нажми "Enable"
4. Создай API ключ:
   - Перейди в "APIs & Services" → "Credentials"
   - Нажми "Create Credentials" → "API Key"
   - Скопируй созданный ключ
5. (Рекомендуется) Ограничь ключ:
   - Нажми на созданный ключ
   - В "Application restrictions" выбери "Android apps"
   - Добавь package name: `com.worldmates.messenger`
   - Добавь SHA-1 fingerprint (получи через `./gradlew signingReport`)

### 2. Добавить API Key в проект

Открой `app/src/main/AndroidManifest.xml` и найди строку:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
```

Замени `YOUR_GOOGLE_MAPS_API_KEY_HERE` на свой ключ:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyD..." />
```

### 3. Собрать проект

```bash
./gradlew assembleDebug
```

---

## 📱 Использование

### Отправка геолокации

1. Открой чат
2. Нажми кнопку "+" (медиа-опции)
3. Выбери "Локация" 📍
4. Разреши доступ к геолокации (если требуется)
5. Перемещай карту, чтобы выбрать место
6. Нажми "Отправить место"

### Клавиши:
- **"Моя геолокация"** (кнопка справа снизу) - переместить карту к текущему местоположению
- **Центральный pin** - показывает выбранное место
- **Адрес** (внизу) - автоматически определяется по координатам

---

## 🏗️ Архитектура

### Файлы проекта:

#### 1. **LocationRepository.kt** (Data Layer)
`app/src/main/java/com/worldmates/messenger/data/repository/LocationRepository.kt`

Репозиторий для работы с геолокацией:
```kotlin
class LocationRepository {
    // Получить текущую геолокацию
    suspend fun getCurrentLocation(): Result<LatLng>

    // Начать отслеживание (Live Location)
    suspend fun startLocationTracking(intervalMs: Long): Result<Unit>

    // Остановить отслеживание
    fun stopLocationTracking()

    // Получить адрес по координатам
    suspend fun getAddressFromLocation(latLng: LatLng): Result<String>

    // Проверить разрешения
    fun hasLocationPermission(): Boolean
}
```

**Особенности:**
- Singleton pattern через `getInstance(context)`
- Использует `FusedLocationProviderClient` для получения геолокации
- Geocoder для reverse geocoding
- StateFlow для Live Location
- Поддержка Android 13+ асинхронного Geocoder API

#### 2. **LocationPicker.kt** (UI Layer)
`app/src/main/java/com/worldmates/messenger/ui/components/LocationPicker.kt`

UI компонент для выбора места на карте:
```kotlin
@Composable
fun LocationPicker(
    onLocationSelected: (LocationData) -> Unit,
    onDismiss: () -> Unit,
    initialLocation: LatLng? = null
)
```

**Особенности:**
- Google Maps Compose integration
- Runtime permissions через Accompanist
- Автоматическое определение адреса при перемещении карты
- Debounce для reverse geocoding (не делаем запрос при каждом движении)
- Кнопка "Моя геолокация" для быстрого перехода

#### 3. **MessagesViewModel.kt** (Business Logic)
`app/src/main/java/com/worldmates/messenger/ui/messages/MessagesViewModel.kt`

Метод для отправки геолокации:
```kotlin
fun sendLocation(locationData: LocationData) {
    val locationText = """
        📍 ${locationData.address}
        ${locationData.latLng.latitude},${locationData.latLng.longitude}
    """.trimIndent()

    // Отправляем как текстовое сообщение
    RetrofitClient.apiService.sendMessage(
        text = locationText,
        ...
    )
}
```

**Примечание:** В текущей версии геолокация отправляется как текстовое сообщение. В будущем можно добавить специальный тип сообщения для геолокации.

#### 4. **MessagesScreen.kt** (UI Integration)
`app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt`

Интеграция LocationPicker в чат:
```kotlin
// State
var showLocationPicker by remember { mutableStateOf(false) }

// Location Picker
if (showLocationPicker) {
    LocationPicker(
        onLocationSelected = { locationData ->
            viewModel.sendLocation(locationData)
            showLocationPicker = false
        },
        onDismiss = { showLocationPicker = false }
    )
}

// Кнопка в медиа-опциях
MediaOptionButton(
    icon = Icons.Default.LocationOn,
    label = "Локація",
    onClick = { showLocationPicker = true }
)
```

---

## 📦 Зависимости

В `app/build.gradle`:

```gradle
// Google Maps
implementation 'com.google.android.gms:play-services-maps:18.2.0'
implementation 'com.google.android.gms:play-services-location:21.1.0'
implementation 'com.google.maps.android:maps-compose:4.3.3'
implementation 'com.google.accompanist:accompanist-permissions:0.34.0'
```

---

## 🔒 Разрешения

В `AndroidManifest.xml`:

```xml
<!-- Базовые разрешения на геолокацию -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Для Live Location (фоновое отслеживание) -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

### Runtime Permissions Flow:

1. Пользователь открывает LocationPicker
2. Если разрешения нет - показывается экран с кнопкой "Разрешить доступ"
3. При нажатии - системный диалог запроса разрешений
4. После предоставления - карта загружается автоматически

---

## 🎯 Режимы работы

### 1. PICK Mode (Выбор места)
- Пользователь перемещает карту
- Центральный pin показывает выбранное место
- Адрес обновляется автоматически
- Отправляется выбранное место

### 2. LIVE Mode (Live Location) - В РАЗРАБОТКЕ
- Постоянное отслеживание геолокации
- Обновление позиции в реальном времени
- Отправка текущей позиции каждые N секунд
- Маркер на карте показывает текущее местоположение

---

## 🔧 Продвинутые возможности

### Live Location (TODO)

Для реализации Live Location:

1. **Добавить UI toggle** для выбора между PICK и LIVE режимами
2. **Запустить tracking** при выборе LIVE:
   ```kotlin
   locationRepo.startLocationTracking(intervalMs = 5000L)
   ```
3. **Собирать updates** из `locationRepo.currentLocation` StateFlow
4. **Отправлять обновления** на сервер каждые N секунд
5. **Остановить tracking** при закрытии:
   ```kotlin
   locationRepo.stopLocationTracking()
   ```

### Location Message Bubble (TODO)

Отображение карты в чате:

1. **Определить location messages** по паттерну текста
2. **Создать LocationMessageBubble** composable:
   ```kotlin
   @Composable
   fun LocationMessageBubble(
       latitude: Double,
       longitude: Double,
       address: String
   ) {
       // Маленькая Google Map (read-only)
       // Адрес
       // Кнопка "Открыть в картах"
   }
   ```
3. **Использовать StaticMap API** для thumbnail (быстрее) или GoogleMap в read-only режиме

---

## ⚠️ Важные замечания

### 1. API Key Security
**НЕ коммитить API ключ в Git!**

Используй один из способов:
- **BuildConfig** (рекомендуется):
  ```gradle
  // build.gradle
  android {
      defaultConfig {
          manifestPlaceholders = [
              googleMapsApiKey: project.findProperty("GOOGLE_MAPS_API_KEY") ?: ""
          ]
      }
  }

  // AndroidManifest.xml
  <meta-data
      android:name="com.google.android.geo.API_KEY"
      android:value="${googleMapsApiKey}" />
  ```

  В `local.properties`:
  ```
  GOOGLE_MAPS_API_KEY=твой_ключ
  ```

- **Backend Proxy** (самое безопасное):
  - Android App → Your Server → Google Maps API
  - API ключ хранится только на сервере

### 2. Billing
- Google Maps SDK требует включенного биллинга
- Free tier: $200 кредитов в месяц
- Следи за использованием: [Quotas](https://console.cloud.google.com/google/maps-apis/quotas)

### 3. Permissions
- `ACCESS_FINE_LOCATION` - точная геолокация (GPS)
- `ACCESS_COARSE_LOCATION` - примерная (WiFi/Cell towers)
- `ACCESS_BACKGROUND_LOCATION` - только для Live Location (Android 10+)

### 4. Battery Usage
Live Location интенсивно использует батарею! Рекомендации:
- Интервал обновления: минимум 5-10 секунд
- Автоматическая остановка через N минут
- Уведомление пользователя об активном tracking

---

## 🐛 Troubleshooting

### Карта не загружается (серый экран)
1. Проверь, что API ключ добавлен в `AndroidManifest.xml`
2. Убедись, что Maps SDK for Android включен в Google Cloud Console
3. Проверь логи: `adb logcat | grep "Google Maps"`
4. Проверь ограничения ключа (package name, SHA-1)

### Разрешения не запрашиваются
1. Проверь, что permissions есть в `AndroidManifest.xml`
2. Убедись, что `accompanist-permissions` library подключена
3. Для Android 13+: `READ_MEDIA_*` permissions требуют отдельного запроса

### Reverse geocoding не работает
1. Проверь интернет-соединение
2. Geocoding API может быть отключен - включи в Google Cloud Console
3. Достигнут лимит запросов - проверь квоты

### Location не обновляется
1. Проверь, что GPS включен на устройстве
2. Тестируй на реальном устройстве (эмулятор может глючить)
3. Включи "Mock locations" в Developer Options для тестирования

---

## 📊 Метрики

### Производительность:
- Первоначальная загрузка карты: ~1-2 сек
- Reverse geocoding: ~200-500мс
- Location update: ~100-300мс

### Размер APK:
- Google Maps SDK: ~10-15 MB
- Play Services Location: ~5 MB
- **Итого:** +15-20 MB к размеру APK

---

## 🚀 Roadmap

### Версия 1.0 (Текущая)
- ✅ Базовая отправка геолокации
- ✅ Location Picker с Google Maps
- ✅ Reverse geocoding
- ✅ Runtime permissions

### Версия 1.1 (Планируется)
- ⏳ Live Location sharing
- ⏳ Location Message Bubble в чате
- ⏳ Кнопка "Открыть в Google Maps"
- ⏳ Sharing multiple locations (маршрут)

### Версия 2.0 (Будущее)
- ⏳ Nearby places (рестораны, кафе и т.д.)
- ⏳ Location history
- ⏳ Geofencing notifications
- ⏳ Offline maps support

---

## 📚 Полезные ссылки

- [Google Maps Platform](https://developers.google.com/maps)
- [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk)
- [Maps Compose](https://github.com/googlemaps/android-maps-compose)
- [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)
- [Accompanist Permissions](https://google.github.io/accompanist/permissions/)

---

## 💡 Примеры кода

### Пример использования LocationRepository:

```kotlin
val locationRepo = LocationRepository.getInstance(context)

// Получить текущую геолокацию
scope.launch {
    locationRepo.getCurrentLocation().onSuccess { latLng ->
        println("Current location: ${latLng.latitude}, ${latLng.longitude}")
    }.onFailure { error ->
        println("Error: ${error.message}")
    }
}

// Получить адрес
scope.launch {
    val latLng = LatLng(50.4501, 30.5234)
    locationRepo.getAddressFromLocation(latLng).onSuccess { address ->
        println("Address: $address")
    }
}

// Live Location
scope.launch {
    locationRepo.startLocationTracking(intervalMs = 5000L)

    locationRepo.currentLocation.collect { location ->
        location?.let {
            println("Live update: ${it.latitude}, ${it.longitude}")
        }
    }
}

// Остановить
locationRepo.stopLocationTracking()
```

---

**Автор:** Claude + WorldMates Team
**Дата:** Декабрь 2024
**Версия:** 1.0
