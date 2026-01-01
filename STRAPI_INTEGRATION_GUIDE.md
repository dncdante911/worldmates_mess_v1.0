# 📦 Інтеграція Strapi CMS для Стікерів, GIF та Емодзі

## ✅ Що вже зроблено

### 1. Backend (Strapi API)
- ✅ **StrapiApiService** - Retrofit API сервіс
- ✅ **StrapiClient** - HTTP клієнт з Bearer токеном
- ✅ Автоматична підстановка API токену до кожного запиту
- ✅ Підтримка `populate=*` для завантаження всіх медіа файлів

### 2. Моделі даних
- ✅ **StrapiResponse** - відповідь від Strapi API
- ✅ **StrapiContentPack** - локальна модель паку контенту
- ✅ **StrapiContentItem** - окремий стікер/GIF/емодзі
- ✅ Extension функції для конвертації

### 3. Репозиторій
- ✅ **StrapiStickerRepository** - бізнес-логіка
- ✅ Автоматичне кешування на 1 годину
- ✅ StateFlow для реактивних оновлень
- ✅ Окремі StateFlow для стікерів, GIF та емодзі

### 4. ViewModel
- ✅ **StrapiContentViewModel** - управління станом
- ✅ Підтримка вкладок (All/Stickers/GIFs/Emojis)
- ✅ Пошук по назві паків
- ✅ Вибір паків та елементів

### 5. UI Компоненти
- ✅ **StrapiContentPicker** - ModalBottomSheet для вибору контенту
- ✅ Сіткове відображення паків та елементів
- ✅ AsyncImage з Coil для завантаження медіа

---

## 🚀 Як інтегрувати в MessagingActivity

### Крок 1: Додати до build.gradle (якщо ще не додано)

```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // OkHttp
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Coil для завантаження зображень
    implementation 'io.coil-kt:coil-compose:2.5.0'
}
```

### Крок 2: Використання в Composable Activity

```kotlin
// У вашому MessagingActivity.kt або ChatActivity.kt

import com.worldmates.messenger.ui.strapi.StrapiContentPicker

@Composable
fun MessagingScreen() {
    var showStrapiPicker by remember { mutableStateOf(false) }

    // Ваш існуючий UI...

    // Кнопка для відкриття стікерів
    IconButton(onClick = { showStrapiPicker = true }) {
        Icon(Icons.Default.EmojiEmotions, "Стікери")
    }

    // Bottom Sheet з стікерами
    if (showStrapiPicker) {
        StrapiContentPicker(
            onDismiss = { showStrapiPicker = false },
            onItemSelected = { url ->
                // Тут URL обраного стікера/GIF/емодзі
                sendStrapiContent(url)
            }
        )
    }
}
```

### Крок 3: Відправка через існуючий API

```kotlin
fun sendStrapiContent(url: String) {
    viewModelScope.launch {
        try {
            val response = RetrofitClient.apiService.sendMessage(
                accessToken = UserSession.accessToken!!,
                recipientId = chatUserId,
                text = "", // Порожній текст
                imageUrl = url, // URL стікера/GIF з Strapi
                messageType = "sticker" // або "gif"
            )

            if (response.apiStatus == 200) {
                Log.d("Strapi", "Стікер відправлено: $url")
            }
        } catch (e: Exception) {
            Log.e("Strapi", "Помилка відправки", e)
        }
    }
}
```

### Крок 4: Альтернатива - View-based Activity

```kotlin
class MessagingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging)

        // Кнопка стікерів
        findViewById<ImageButton>(R.id.stickerButton).setOnClickListener {
            showStrapiPicker()
        }
    }

    private fun showStrapiPicker() {
        setContent {
            StrapiContentPicker(
                onDismiss = { /* закрити */ },
                onItemSelected = { url ->
                    sendStrapiContent(url)
                }
            )
        }
    }
}
```

---

## 📊 Як працює автоматичне оновлення

### Додаєте новий пак в Strapi:
1. Заходите в Strapi Admin Panel: https://cdn.worldmates.club/admin
2. Створюєте новий Gif Pack
3. Завантажуєте файли в `upload_gifs`
4. Публікуєте

### В додатку:
- Через 1 годину кеш автоматично застаріє
- При наступному відкритті стікерів завантажиться новий контент
- Або натисніть кнопку "Оновити" (іконка Refresh)

---

## 🔧 Налаштування

### Змінити час кешування

У файлі `StrapiStickerRepository.kt`:

```kotlin
companion object {
    private const val CACHE_VALIDITY_MS = 3600000L // 1 година
    // Змініть на інший час (в мілісекундах):
    // 30 хв = 1800000L
    // 2 години = 7200000L
}
```

### Змінити API токен

У файлі `StrapiClient.kt`:

```kotlin
private const val API_TOKEN = "ВАШ_НОВИЙ_ТОКЕН"
```

**ВАЖЛИВО:** В production середовищі краще зберігати токен в `BuildConfig` або secure storage!

---

## 📱 Приклади використання

### 1. Відкрити стікери зі стандартною вкладкою

```kotlin
val viewModel: StrapiContentViewModel = viewModel()

// Відкрити на вкладці GIF
viewModel.selectTab(StrapiContentViewModel.ContentTab.GIFS)

StrapiContentPicker(...)
```

### 2. Отримати популярні стікери

```kotlin
val viewModel: StrapiContentViewModel = viewModel()

// Отримати перші 20 елементів
val recentItems = viewModel.getRecentItems(limit = 20)
```

### 3. Примусове оновлення

```kotlin
val viewModel: StrapiContentViewModel = viewModel()

// Очистити кеш та завантажити заново
viewModel.clearCacheAndReload()
```

---

## 🔍 Структура Strapi API

### Endpoint: `/api/gifs-packs?populate=*`

**Відповідь:**
```json
{
  "data": [
    {
      "id": 1,
      "attributes": {
        "Name_pack": "Мої Стікери",
        "gif": "sticker",
        "slug": "my-stickers",
        "upload_gifs": {
          "data": [
            {
              "id": 1,
              "attributes": {
                "url": "/uploads/sticker1.webp",
                "name": "Happy",
                "width": 512,
                "height": 512
              }
            }
          ]
        }
      }
    }
  ]
}
```

---

## 🐛 Debugging

### Увімкнути логування

Логи автоматично включені в DEBUG режимі:
- `StrapiStickerRepository` → тег `StrapiStickerRepo`
- `StrapiContentViewModel` → тег `StrapiContentVM`
- `StrapiClient` → HTTP логування через `HttpLoggingInterceptor`

### Перевірити що завантажилось

```kotlin
lifecycleScope.launch {
    val repo = StrapiStickerRepository.getInstance(context)
    repo.fetchAllPacks(forceRefresh = true).onSuccess { packs ->
        packs.forEach { pack ->
            Log.d("Strapi", "Пак: ${pack.name}, елементів: ${pack.items.size}")
            pack.items.forEach { item ->
                Log.d("Strapi", "  - ${item.url}")
            }
        }
    }
}
```

---

## ✅ Переваги цієї інтеграції

1. **Автоматичне оновлення** - додаєте контент в Strapi → з'являється в додатку
2. **Без перекомпіляції** - не потрібно оновлювати код для нового контенту
3. **Кешування** - швидке завантаження, економія трафіку
4. **Типізація** - безпека типів завдяки Kotlin data class
5. **Reactive UI** - StateFlow автоматично оновлює UI
6. **Легка підтримка** - зміни тільки в Strapi, код залишається незмінним

---

## 📞 Наступні кроки

- [ ] Інтегрувати `StrapiContentPicker` в `MessagingActivity`
- [ ] Додати можливість відправки через існуючий API
- [ ] Додати історію використаних стікерів (RecentsRepository)
- [ ] Додати пошук по стікерах
- [ ] Додати попередній перегляд GIF перед відправкою
- [ ] Додати збереження улюблених паків

---

**Готово! 🎉**

Тепер у вас є повна інтеграція Strapi CMS для динамічного контенту без потреби оновлення додатку!
