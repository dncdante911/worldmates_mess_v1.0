# Stories UI Implementation - Документація

## Огляд

Реалізовано повний UI для функціоналу Stories з двома окремими інтерфейсами:
1. **Особисті Stories** - для загального користування (як в Instagram/Telegram)
2. **Канальні Stories** - для комерційного/рекламного використання

## Створені файли

### 1. StoryViewerActivity.kt
**Розташування:** `app/src/main/java/com/worldmates/messenger/ui/stories/StoryViewerActivity.kt`

Основний екран для перегляду stories. Містить:
- Повноекранний перегляд фото/відео
- Автоматичний прогрес-бар (5 сек для фото, реальна тривалість для відео)
- Свайп вліво/вправо для перемикання stories
- Довге натискання для паузи
- Можливість додавати реакції
- Система коментарів
- Перегляд списку переглядів (для власних stories)
- Bottom sheets для всіх взаємодій

**Функціонал:**
```kotlin
// Відкриття особистої story
Intent(context, StoryViewerActivity::class.java).apply {
    putExtra("user_id", userId)
    putExtra("is_channel_story", false)
}

// Відкриття канальної story
Intent(context, StoryViewerActivity::class.java).apply {
    putExtra("user_id", channelUserId)
    putExtra("is_channel_story", true)
}
```

### 2. PersonalStoriesRow.kt
**Розташування:** `app/src/main/java/com/worldmates/messenger/ui/stories/PersonalStoriesRow.kt`

Горизонтальний список особистих stories для відображення вгорі екрану чатів.

**Компоненти:**
- `PersonalStoriesRow` - повний список з кнопкою створення
- `CreateStoryButton` - кнопка створення нової story
- `StoryItem` - елемент story користувача з анімованим градієнтом для непереглянутих
- `CompactStoriesRow` - компактна версія без кнопки створення

**Особливості:**
- Анімований градієнт для непереглянутих stories
- Сірий бордер для переглянутих
- Групування stories по користувачам
- Автоматичне відкриття StoryViewerActivity при кліку

### 3. ChannelStoriesSection.kt
**Розташування:** `app/src/main/java/com/worldmates/messenger/ui/stories/ChannelStoriesSection.kt`

Окрема секція для відображення комерційних stories каналів.

**Компоненти:**
- `ChannelStoriesSection` - повна секція з золотистим фоном
- `ChannelStoryCard` - детальна карточка з превью, інфо про канал, кількістю переглядів
- `CompactChannelStoriesRow` - компактна версія для інших місць
- `CompactChannelStoryItem` - мінімальний елемент канальної story

**Візуальні відмінності від особистих stories:**
- Золотистий градієнтний фон секції
- Зірочка (⭐) поруч з назвою каналу
- Золотистий бордер замість градієнтного
- Карточки з превью та додатковою інформацією
- Відображення кількості переглядів

### 4. CreateStoryDialog.kt
**Розташування:** `app/src/main/java/com/worldmates/messenger/ui/stories/CreateStoryDialog.kt`

Діалог створення нової story з перевіркою обмежень підписки.

**Функціонал:**
- Вибір фото або відео через Photo Picker
- Додавання заголовка та опису (опціонально)
- Показ інформації про ліміти користувача:
  - Кількість активних stories
  - Максимальна тривалість відео
  - Час зберігання stories
- Автоматична валідація перед публікацією
- Превью вибраного медіа
- Індикатор типу медіа (фото/відео)

**Компоненти діалогу:**
- `LimitsInfoCard` - інфо про ліміти з кольоровою індикацією
- `MediaPickerButton` - кнопки вибору фото/відео
- `MediaPreview` - превью з можливістю видалення

## Інтеграція в ChatsActivity

### Зміни в ChatsActivity.kt

Додано `StoryViewModel`:
```kotlin
private lateinit var storyViewModel: com.worldmates.messenger.ui.stories.StoryViewModel

storyViewModel = ViewModelProvider(this).get(com.worldmates.messenger.ui.stories.StoryViewModel::class.java)
```

Автоматичне оновлення stories в `onResume()`:
```kotlin
override fun onResume() {
    super.onResume()
    viewModel.fetchChats()
    groupsViewModel.fetchGroups()
    channelsViewModel.fetchSubscribedChannels()
    storyViewModel.loadStories()  // ← Додано
}
```

### Зміни в ChatsScreenModern.kt

1. **Додано параметр `storyViewModel`**
2. **Додано стан для stories:**
   ```kotlin
   val stories by storyViewModel.stories.collectAsState()
   val isLoadingStories by storyViewModel.isLoading.collectAsState()
   var showCreateStoryDialog by remember { mutableStateOf(false) }
   ```

3. **Створено нові composable функції:**
   - `ChatListTabWithStories` - вкладка чатів з stories вгорі
   - `ChannelListTabWithStories` - вкладка каналів з channel stories

4. **Додано CreateStoryDialog:**
   ```kotlin
   if (showCreateStoryDialog) {
       com.worldmates.messenger.ui.stories.CreateStoryDialog(
           onDismiss = { showCreateStoryDialog = false },
           viewModel = storyViewModel
       )
   }
   ```

## Структура вкладок

### Вкладка "Чати" (Tab 0)
```
┌─────────────────────────────────────┐
│  [Your Story] [User1] [User2] ...  │ ← PersonalStoriesRow
├─────────────────────────────────────┤
│  Chat 1                             │
│  Chat 2                             │
│  ...                                │
└─────────────────────────────────────┘
```

### Вкладка "Канали" (Tab 1)
```
┌─────────────────────────────────────┐
│  ⭐ Канали та реклама               │ ← ChannelStoriesSection
│  [Channel1 Story] [Channel2] ...   │
├─────────────────────────────────────┤
│  Channel 1                          │
│  Channel 2                          │
│  ...                                │
└─────────────────────────────────────┘
```

## Логіка роботи

### Особисті Stories

1. **Відображення:**
   - Stories групуються по користувачам
   - Показується аватар + ім'я користувача
   - Непереглянуті мають анімований градієнтний бордер
   - Переглянуті мають сірий бордер

2. **Створення:**
   - Кнопка "Ваша Story" завжди перша
   - Клік відкриває `CreateStoryDialog`
   - Перевірка лімітів відбувається автоматично
   - Після публікації список оновлюється

3. **Перегляд:**
   - Клік на story відкриває `StoryViewerActivity`
   - Автоматичний прогрес
   - Свайпи для навігації
   - Можливість поставити реакцію, прокоментувати

### Канальні Stories

1. **Відображення:**
   - Окрема секція з золотистим фоном
   - Детальні карточки з превью
   - Показ кількості переглядів
   - Іконка зірки для позначення каналу

2. **Перегляд:**
   - Такий же `StoryViewerActivity`, але з `is_channel_story = true`
   - Можливо додаткові функції для комерційних stories

## Управління лімітами

### Безкоштовні користувачі (isPro = 0)
- Максимум 2 активних stories
- Відео до 25 секунд
- Зберігання 24 години

### PRO користувачі (isPro = 1)
- Максимум 15 активних stories
- Відео до 45 секунд
- Зберігання 48 годин

Валідація відбувається:
1. В `StoryViewModel.canCreateStory()` - перед показом діалогу
2. В `StoryViewModel.createStory()` - перед відправкою на сервер
3. В `StoryRepository.createStory()` - перед API запитом
4. На сервері в `create-story.php` - остаточна перевірка

## API Інтеграція

Всі API ендпоінти вже реалізовані в попередніх кроках:

- `GET /api/v2/endpoints/get-stories.php` - отримати список stories
- `POST /api/v2/endpoints/create-story.php` - створити story
- `GET /api/v2/endpoints/get-story-by-id.php` - отримати конкретну story
- `DELETE /api/v2/endpoints/delete-story.php` - видалити story
- `GET /api/v2/endpoints/get_story_views.php` - отримати перегляди
- `POST /api/v2/endpoints/react-to-story.php` - додати реакцію
- `POST /api/v2/endpoints/create_story_comment.php` - створити коментар
- `GET /api/v2/endpoints/get_story_comments.php` - отримати коментарі

## Тестування

### Для тестування UI:

1. **Особисті Stories:**
   ```kotlin
   // Відкрити додаток
   // Перейти на вкладку "Чати"
   // Натиснути "Ваша Story"
   // Вибрати фото або відео
   // Додати заголовок (опціонально)
   // Натиснути "Опублікувати"
   ```

2. **Канальні Stories:**
   ```kotlin
   // Відкрити додаток
   // Перейти на вкладку "Канали"
   // Якщо є канали зі stories - з'явиться секція вгорі
   // Натиснути на карточку каналу
   ```

3. **Перегляд Stories:**
   ```kotlin
   // Натиснути на будь-яку story
   // Тап справа - наступна story / закрити
   // Тап зліва - попередня story
   // Довге натискання - пауза
   // Натиснути іконку серця - показати реакції
   // Натиснути іконку коментаря - показати коментарі
   ```

## Майбутні покращення

1. **Відео:**
   - Автоматичне визначення тривалості відео
   - Генерація превью/обкладинки для відео
   - Відеоплеєр для відтворення відео в StoryViewer

2. **Фічі:**
   - Стікери та емоджі на stories
   - Текст на stories
   - Фільтри
   - Музика для stories
   - Приватні stories (тільки для друзів)

3. **Аналітика:**
   - Детальна статистика переглядів
   - Хто з ким взаємодіяв
   - Час найактивнішого перегляду

4. **Покращення UX:**
   - Кеш stories для офлайн перегляду
   - Попереднє завантаження наступних stories
   - Smooth transitions між stories
   - Більш гнучка система навігації

## Залежності

Stories UI використовує:
- Jetpack Compose
- Coil для завантаження зображень
- Material3 для UI компонентів
- Kotlin Coroutines & Flow для асинхронності
- Activity Result API для вибору медіа

## Troubleshooting

### Stories не відображаються
- Перевірте, чи `storyViewModel.loadStories()` викликається
- Перевірте, чи API повертає дані
- Перевірте логи в `StoryViewModel` та `StoryRepository`

### Не можу створити story
- Перевірте ліміти користувача
- Перевірте, чи `UserSession.isPro` встановлено правильно
- Перевірте логи помилок в `StoryViewModel.createStory()`

### Не відкривається StoryViewerActivity
- Перевірте, чи передаєте `user_id` або `story_id`
- Перевірте в AndroidManifest.xml, чи Activity зареєстрована

## Висновок

Реалізовано повноцінний UI для Stories з двома окремими інтерфейсами:
- ✅ Особисті stories для загального використання
- ✅ Канальні stories для комерційних цілей
- ✅ Діалог створення з валідацією лімітів
- ✅ Повноцінний viewer з реакціями та коментарями
- ✅ Інтеграція в головний екран
- ✅ Підтримка обох UI стилів (WorldMates та Telegram)

Все готове до тестування та використання!
