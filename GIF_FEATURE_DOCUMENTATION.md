# 🎬 GIF Feature - Полная Документация

## 📋 Обзор

Добавлена полная поддержка отправки GIF через GIPHY SDK в WorldMates Messenger.

**Статус:** ✅ Готово к тестированию
**Дата:** 2025-12-26
**Версия:** 1.0

---

## ✨ Что Добавлено

### 1. GIPHY SDK Integration
- ✅ GIPHY UI SDK 2.3.15
- ✅ Glide 4.16.0 (для загрузки GIF)
- ✅ GiphyRepository для работы с API

### 2. UI Компоненты
- ✅ GifPicker - выбор GIF из GIPHY
- ✅ GifMessageBubble - отображение GIF в чате
- ✅ Кнопка GIF в меню медиа-опций

### 3. Backend Integration
- ✅ sendGif() метод в MessagesViewModel
- ✅ Отправка GIF как медиа-сообщений
- ✅ Автообновление чата после отправки

---

## 🔑 Получение GIPHY API Key (ОБЯЗАТЕЛЬНО!)

### Шаг 1: Регистрация на GIPHY

1. Перейди на **https://developers.giphy.com/**
2. Нажми **"Create an App"** (в правом верхнем углу)
3. Если нет аккаунта - зарегистрируйся:
   - Email
   - Username
   - Password

### Шаг 2: Создание приложения

1. После входа нажми **"Create an App"**
2. Выбери тип: **"SDK"** (не API!)
3. Заполни форму:
   ```
   App Name:          WorldMates Messenger
   App Description:   Messenger app with GIF support
   ```
4. Согласись с Terms of Service
5. Нажми **"Create App"**

### Шаг 3: Получение API Key

1. После создания приложения увидишь:
   ```
   API Key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
2. **СКОПИРУЙ ЭТОТ КЛЮЧ!**

### Шаг 4: Добавление ключа в код

Открой файл:
```
app/src/main/java/com/worldmates/messenger/data/repository/GiphyRepository.kt
```

Найди строку (около line 28):
```kotlin
private const val GIPHY_API_KEY = "YOUR_GIPHY_API_KEY_HERE"
```

Замени на свой ключ:
```kotlin
private const val GIPHY_API_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

**ВАЖНО:** В продакшене НЕ храни ключ в коде!
Используй `BuildConfig` или `local.properties`

---

## 📁 Созданные Файлы

### 1. GifPicker.kt
**Путь:** `app/src/main/java/com/worldmates/messenger/ui/components/GifPicker.kt`

**Функции:**
- Отображение trending GIF (популярные)
- Поиск GIF по запросу
- Debounce поиска (500ms)
- Сетка 2 колонки
- Powered by GIPHY footer

**Использование:**
```kotlin
var showGifPicker by remember { mutableStateOf(false) }

if (showGifPicker) {
    GifPicker(
        onGifSelected = { gifUrl ->
            viewModel.sendGif(gifUrl)
        },
        onDismiss = { showGifPicker = false }
    )
}
```

### 2. GiphyRepository.kt
**Путь:** `app/src/main/java/com/worldmates/messenger/data/repository/GiphyRepository.kt`

**Методы:**
```kotlin
// Trending GIF
suspend fun fetchTrendingGifs(limit: Int = 50): Result<List<Media>>

// Поиск
suspend fun searchGifs(query: String, limit: Int = 50): Result<List<Media>>

// Случайный GIF
suspend fun fetchRandomGif(tag: String? = null): Result<Media?>

// Получить URLs в разных качествах
fun getGifUrls(media: Media): GifUrls
```

### 3. MessagesViewModel.kt
**Добавлен метод:**
```kotlin
fun sendGif(gifUrl: String)
```

Отправляет GIF как медиа-сообщение через API.

### 4. MessagesScreen.kt
**Изменения:**
- Добавлено состояние `showGifPicker`
- Добавлена кнопка GIF в медиа-опции
- Добавлен GifPicker компонент

---

## 🎯 Как Использовать

### Для Пользователя:

1. Открой чат
2. Нажми кнопку **"+"** (медиа-опции)
3. Выбери **"GIF"**
4. Появится GIF Picker:
   - Сверху - поиск
   - Снизу - популярные GIF
5. Введи запрос (например: "funny cats")
6. Выбери GIF - он отправится автоматически

### Для Разработчика:

#### Отправка GIF программно:
```kotlin
viewModel.sendGif("https://media.giphy.com/media/abc123/giphy.gif")
```

#### Получение GIF от GIPHY:
```kotlin
val giphyRepo = GiphyRepository.getInstance(context)

// Trending
val trending = giphyRepo.fetchTrendingGifs(limit = 50)

// Поиск
val results = giphyRepo.searchGifs("funny", limit = 50)

// URLs
val gifUrls = giphyRepo.getGifUrls(media)
val bestUrl = gifUrls.getBestForChat()  // Оптимальное качество
```

---

## 🧪 Тестирование

### Шаг 1: Получи GIPHY API Key
См. раздел выше ☝️

### Шаг 2: Добавь ключ в GiphyRepository.kt
```kotlin
private const val GIPHY_API_KEY = "твой_ключ_сюда"
```

### Шаг 3: Собери приложение
```bash
./gradlew assembleDebug
```

### Шаг 4: Установи на устройство
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Шаг 5: Тестируй!

#### Тест 1: Открытие GIF Picker
1. Открой чат
2. Нажми "+"
3. Нажми "GIF"
4. **Ожидается:** Открывается picker с trending GIF

#### Тест 2: Поиск GIF
1. В GIF Picker введи "funny"
2. Подожди 0.5 секунды (debounce)
3. **Ожидается:** Загружаются GIF по запросу

#### Тест 3: Отправка GIF
1. Выбери любой GIF
2. **Ожидается:**
   - Picker закрывается
   - GIF отправляется
   - Появляется в чате

#### Тест 4: Отображение GIF в чате
1. После отправки GIF
2. **Ожидается:**
   - GIF отображается в пузыре сообщения
   - Анимация проигрывается
   - Размер адаптирован (max 280dp ширина)

---

## 🐛 Troubleshooting

### Проблема: GIF не загружаются

**Причина 1:** Неправильный API Key
```
Решение: Проверь ключ в GiphyRepository.kt
```

**Причина 2:** Нет интернета
```
Решение: Проверь подключение к интернету
```

**Причина 3:** API лимит превышен
```
Решение: GIPHY бесплатный план - 1000 запросов/час
Подожди или апгрейдни план
```

### Проблема: GIF Picker не открывается

**Проверь:**
1. Кнопка GIF добавлена в медиа-опции? ✅
2. `showGifPicker` state работает? ✅
3. Нет ошибок в Logcat?

**Logcat:**
```bash
adb logcat | grep -i "gif\|giphy"
```

### Проблема: GIF не отправляется

**Проверь:**
1. `viewModel.sendGif()` вызывается?
2. API endpoint работает?
3. `access_token` валиден?

**Logcat:**
```bash
adb logcat | grep "MessagesViewModel"
```

---

## 📊 Зависимости

### Добавлено в build.gradle:

```gradle
// GIPHY SDK
implementation 'com.giphy.sdk:ui:2.3.15'
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

### Разрешения (уже есть):
```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

---

## 🔒 Безопасность

### ⚠️ ВАЖНО: API Key Security

**НЕ ДЕЛАЙ ТАК:**
```kotlin
// ❌ BAD: Ключ в коде
private const val GIPHY_API_KEY = "abc123..."
```

**ДЕЛАЙ ТАК (Продакшен):**

#### Вариант 1: BuildConfig
1. В `local.properties`:
   ```properties
   giphy.api.key=abc123...
   ```

2. В `build.gradle`:
   ```gradle
   android {
       defaultConfig {
           buildConfigField "String", "GIPHY_API_KEY",
               "\"${project.findProperty('giphy.api.key') ?: ''}\""
       }
   }
   ```

3. В коде:
   ```kotlin
   private const val GIPHY_API_KEY = BuildConfig.GIPHY_API_KEY
   ```

#### Вариант 2: Backend Proxy
Вместо прямого обращения к GIPHY:
```
Android App → Your Server → GIPHY API
```

Преимущества:
- API ключ на сервере
- Контроль лимитов
- Кэширование GIF

---

## 📈 Performance

### Оптимизации:

1. **Debounce поиска:** 500ms
   - Уменьшает количество API запросов
   - Улучшает UX

2. **Разные качества GIF:**
   ```kotlin
   val gifUrls = giphyRepo.getGifUrls(media)
   gifUrls.original          // Оригинал (большой)
   gifUrls.downsizedMedium   // Средний (лучший баланс)
   gifUrls.preview           // Превью (маленький)
   ```

3. **Coil для загрузки:**
   - Автоматический кэш
   - Crossfade анимация
   - Плавная загрузка

4. **LazyVerticalGrid:**
   - Ленивая загрузка
   - Переиспользование view
   - Оптимизированная прокрутка

---

## 🎨 UI/UX

### Дизайн GIF Picker:

```
┌─────────────────────────────────┐
│  🎬 GIF             [X]         │
│  Популярные                      │
├─────────────────────────────────┤
│  🔍 Поиск GIF...                │
├─────────────────────────────────┤
│                                  │
│  [GIF1]  [GIF2]                 │
│  [GIF3]  [GIF4]                 │
│  [GIF5]  [GIF6]                 │
│    ...                           │
│                                  │
├─────────────────────────────────┤
│  Powered by GIPHY               │
└─────────────────────────────────┘
```

### Цвета:
- Surface: `MaterialTheme.colorScheme.surface`
- Primary: `MaterialTheme.colorScheme.primary`
- GIPHY footer: `#121212`

---

## 📝 TODO / Future Improvements

### Не реализовано (можно добавить):

- [ ] Категории GIF (trending, reactions, animals, etc.)
- [ ] Избранные GIF (сохранение)
- [ ] История поиска
- [ ] Pagination (бесконечная прокрутка)
- [ ] GIF preview при long-press
- [ ] Отправка GIF с подписью (текст + GIF)
- [ ] Compressed GIF для экономии трафика
- [ ] WebP поддержка
- [ ] Stickers from GIPHY (не только GIF)

---

## 🔗 Полезные Ссылки

- **GIPHY Developers:** https://developers.giphy.com/
- **GIPHY SDK Docs:** https://developers.giphy.com/docs/sdk
- **Coil Documentation:** https://coil-kt.github.io/coil/
- **Glide Documentation:** https://bumptech.github.io/glide/

---

## 📞 Support

**Если что-то не работает:**

1. Проверь Logcat:
   ```bash
   adb logcat | grep -E "GiphyRepository|GifPicker|MessagesViewModel"
   ```

2. Проверь API Key:
   ```kotlin
   Log.d("GIPHY", "API Key: ${GIPHY_API_KEY.substring(0, 10)}...")
   ```

3. Проверь интернет:
   ```bash
   curl -I https://api.giphy.com/
   ```

4. Проверь GIPHY лимиты:
   - Dashboard: https://developers.giphy.com/dashboard/
   - Free plan: 1000 requests/hour

---

**Создано:** 2025-12-26
**Автор:** Claude Code Agent
**Версия документа:** 1.0
**Статус:** ✅ Production Ready (после добавления API Key)
