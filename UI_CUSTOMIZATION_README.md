# 🎨 WorldMates Messenger - Посібник з кастомізації UI

## 📋 Зміст
1. [Огляд нового дизайну](#огляд-нового-дизайну)
2. [Реалізовані функції](#реалізовані-функції)
3. [Кастомізація кольорів та тем](#кастомізація-кольорів-та-тем)
4. [Додавання власних іконок](#додавання-власних-іконок)
5. [Налаштування анімацій](#налаштування-анімацій)
6. [Додавання градієнтів](#додавання-градієнтів)
7. [Подальший розвиток](#подальший-розвиток)

---

## 🎯 Огляд нового дизайну

### Що було реалізовано:

#### ✨ Повний редизайн головного інтерфейсу
- **ModernChatCard** - сучасні карточки чатів з:
  - Градієнтними рамками навколо аватара
  - Анімацією масштабування при натисканні
  - Online індикатором з зеленим кружечком
  - Тінями та rounded corners (20dp)
  - Форматуванням часу (Сьогодні, Вчора, День тижня)

- **ModernSearchBar** - пошукова панель з:
  - Динамічною тінню при фокусі
  - Анімованим border
  - Іконками Search та Clear

- **ModernTabsRow** - вкладки Чати/Групи з:
  - Плавною анімацією переключення
  - Градієнтним фоном для активної вкладки
  - Іконками Chat та Group

- **AnimatedUnreadBadge** - бейдж непрочитаних повідомлень:
  - Пульсуюча анімація (scale 0.9-1.1)
  - Градієнтний фон
  - Підтримка 99+ повідомлень

#### 🎨 Контекстне меню для повідомлень
- Long-press на повідомленні відкриває ModalBottomSheet
- Функції: Відповісти, Переслати, Копіювати, Видалити
- Reply Indicator показує повідомлення, на яке відповідаєте

#### 👤 Перейменування та видалення контактів
- Long-press на контакт відкриває меню
- Локальне зберігання псевдонімів у DataStore
- Функція приховування чатів

#### 🖼️ Вибір фону та аватара групи
- Picker для вибору фонового зображення
- Діалог зміни аватара групи
- Превю вибраного фону

---

## 🎨 Реалізовані функції

### ✅ Завершено:
1. ✨ Повний редизайн інтерфейсу чатів
2. ✨ Контекстне меню для повідомлень (Reply, Forward, Delete, Copy)
3. ✨ Перейменування контактів з локальним збереженням
4. ✨ Видалення (приховування) контактів
5. ✨ Вибір фонового зображення в налаштуваннях
6. ✨ Зміна аватара групи

### ⏳ В розробці:
1. Rich Links Preview для посилань на фото/відео
2. Пересилання повідомлень між чатами
3. Інтеграція завантаження фону та аватара на сервер

---

## 🎨 Кастомізація кольорів та тем

### Файл: `app/src/main/java/com/worldmates/messenger/ui/theme/Color.kt`

Змініть основні кольори додатку:

```kotlin
// Основні кольори
val Primary = Color(0xFF2196F3)  // Синій
val Secondary = Color(0xFF03DAC5) // Бірюзовий
val Tertiary = Color(0xFFFF6B6B) // Червоний

// Фон
val SurfaceLight = Color(0xFFF5F5F5)
val SurfaceDark = Color(0xFF1E1E1E)

// Градієнти для карточок
val CardGradientStart = Color(0xFF6200EA)
val CardGradientEnd = Color(0xFF3700B3)
```

### Файл: `app/src/main/java/com/worldmates/messenger/ui/chats/ModernChatsUI.kt`

Змініть градієнт аватара:

```kotlin
// Знайдіть цей код (лінія ~84):
border(
    width = 2.dp,
    brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2196F3),  // Ваш перший колір
            Color(0xFF1976D2)   // Ваш другий колір
        )
    ),
    shape = CircleShape
)
```

---

## 🎭 Додавання власних іконок

### 1. Використання Material Icons

Додайте залежність у `build.gradle`:

```gradle
dependencies {
    implementation "androidx.compose.material:material-icons-extended:1.5.4"
}
```

Використовуйте іконки:

```kotlin
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

Icon(
    imageVector = Icons.Default.Favorite,
    contentDescription = "Favorite"
)
```

### 2. Власні векторні іконки

Додайте SVG файли в `res/drawable/`:

```xml
<!-- res/drawable/ic_custom_chat.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
</vector>
```

Використовуйте в коді:

```kotlin
Icon(
    painter = painterResource(id = R.drawable.ic_custom_chat),
    contentDescription = "Custom Chat"
)
```

### 3. Іконки з інтернету (Coil)

Додайте залежність:

```gradle
implementation("io.coil-kt:coil-compose:2.5.0")
implementation("io.coil-kt:coil-svg:2.5.0")
```

---

## ⚡ Налаштування анімацій

### Швидкість анімації масштабування карточок

У файлі `ModernChatsUI.kt`, змініть параметри spring:

```kotlin
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy, // Змініть на LowBouncy або NoBouncy
        stiffness = Spring.StiffnessLow                 // Або StiffnessMedium, StiffnessHigh
    )
)
```

### Швидкість пульсації бейджа

```kotlin
val scale by infiniteTransition.animateFloat(
    initialValue = 0.9f,
    targetValue = 1.1f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing), // Змініть 800 на бажану швидкість (мс)
        repeatMode = RepeatMode.Reverse
    )
)
```

### Тривалість анімації кольору вкладок

```kotlin
val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) Primary else SurfaceVariant,
    animationSpec = tween(300) // Змініть 300 на бажану тривалість (мс)
)
```

---

## 🌈 Додавання градієнтів

### Градієнт для карточок чатів

```kotlin
// У ModernChatCard
Card(
    modifier = Modifier
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE3F2FD),
                    Color(0xFFBBDEFB)
                )
            )
        )
)
```

### Градієнт для фону екрану

У `ChatsActivity.kt`:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.background
                )
            )
        )
)
```

---

## 🚀 Подальший розвиток

### Рекомендовані покращення:

#### 1. **Rich Link Previews**
Додайте превью для посилань:

```kotlin
@Composable
fun LinkPreview(url: String) {
    // Завантажте метадані
    // Покажіть зображення, заголовок, опис
}
```

Бібліотеки:
- `org.jsoup:jsoup:1.16.1` - парсинг HTML
- OG tags для превью

#### 2. **Свайпи на карточках**
Додайте swipe-to-delete або swipe-to-reply:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
val dismissState = rememberDismissState()

SwipeToDismiss(
    state = dismissState,
    directions = setOf(DismissDirection.EndToStart),
    background = { /* Фон при свайпі */ },
    dismissContent = { ModernChatCard(...) }
)
```

#### 3. **Кастомні шрифти**
Додайте власні шрифти у `res/font/`:

```kotlin
val customFontFamily = FontFamily(
    Font(R.font.roboto_regular),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

MaterialTheme(
    typography = Typography(defaultFontFamily = customFontFamily)
)
```

#### 4. **Темна тема**
Розширте підтримку темної теми у `Theme.kt`:

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF81C784),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0)
)
```

#### 5. **Анімовані переходи між екранами**
Використайте Navigation Compose з анімаціями:

```kotlin
composable(
    route = "messages/{chatId}",
    enterTransition = {
        slideInHorizontally(initialOffsetX = { 1000 }) +
        fadeIn(animationSpec = tween(300))
    },
    exitTransition = {
        slideOutHorizontally(targetOffsetX = { -1000 }) +
        fadeOut(animationSpec = tween(300))
    }
) { /* MessagesScreen */ }
```

#### 6. **Стікери та GIF**
Інтегруйте Giphy SDK або власні стікери:

```gradle
implementation 'com.giphy.sdk:ui:2.3.8'
```

---

## 📦 Структура проекту

```
app/src/main/java/com/worldmates/messenger/
├── data/
│   ├── ContactNicknameRepository.kt    # Зберігання псевдонімів
│   └── model/
│       └── Group.kt                    # Моделі даних
├── ui/
│   ├── chats/
│   │   ├── ChatsActivity.kt           # Головний екран чатів
│   │   ├── ChatsViewModel.kt          # Логіка чатів
│   │   └── ModernChatsUI.kt           # 🆕 Нові UI компоненти
│   ├── messages/
│   │   └── MessagesScreen.kt          # Екран повідомлень з контекстним меню
│   └── theme/
│       ├── Color.kt                    # Кольори
│       ├── Theme.kt                    # Теми
│       └── ThemeManager.kt            # Управління темами
```

---

## 🛠️ Інструменти та бібліотеки

### Поточні залежності:

```gradle
// Jetpack Compose
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.compose.material3:material3:1.2.1"
implementation "androidx.compose.material:material-icons-extended:1.5.4"

// Coil для зображень
implementation "io.coil-kt:coil-compose:2.5.0"

// DataStore для збереження налаштувань
implementation "androidx.datastore:datastore-preferences:1.0.0"

// Анімації
implementation "androidx.compose.animation:animation:1.5.4"
```

### Рекомендовані додаткові бібліотеки:

```gradle
// Lottie анімації
implementation "com.airbnb.android:lottie-compose:6.1.0"

// Shimmer ефект для loading
implementation "com.valentinilk.shimmer:compose-shimmer:1.2.0"

// Accompanist для додаткових Compose UI
implementation "com.google.accompanist:accompanist-systemuicontroller:0.32.0"
implementation "com.google.accompanist:accompanist-navigation-animation:0.32.0"

// Rich text editor
implementation "com.mohamedrejeb.richeditor:richeditor-compose:1.0.0"
```

---

## 💡 Корисні поради

### 1. **Тестування на різних пристроях**
Використовуйте емулятори з різними розмірами екранів та API рівнями.

### 2. **Performance**
- Використовуйте `remember` для об'єктів, які не потребують recomposition
- Додайте `derivedStateOf` для обчислюваних станів
- Профілюйте за допомогою Android Studio Profiler

### 3. **Accessibility**
Додайте `contentDescription` для всіх іконок та зображень.

### 4. **Локалізація**
Створіть файли `strings.xml` для різних мов у `res/values-{lang}/`.

---

## 📚 Додаткові ресурси

- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Compose Animations](https://developer.android.com/jetpack/compose/animation)
- [Material Icons](https://fonts.google.com/icons)
- [Color Tool](https://material.io/resources/color/)

---

## 👨‍💻 Автор

Розроблено з ❤️ для WorldMates Messenger
Версія: 1.0
Дата: 2025-12-24

---

## 📝 Ліцензія

Цей проект є приватним додатком для WorldMates.

---

**Успіхів у кастомізації! 🚀**
