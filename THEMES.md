# 🎨 WorldMates Messenger - Система Тем

## Обзор

WorldMates Messenger теперь поддерживает мощную и гибкую систему тем с **10 предустановленными темами**, включая поддержку **Material You** (динамические цвета из обоев для Android 12+).

## ✨ Возможности

- **10 красивых тем**: Classic Blue, Deep Ocean, Sunset Dreams, Forest Green, Purple Dream, Rose Gold, Monochrome, Nord Frost, Dracula Night, Material You
- **Material You поддержка**: Динамические цвета из обоев системы (Android 12+)
- **Светлая/Темная тема**: Каждая тема поддерживает оба режима
- **Автоматическое следование системной теме**: Опция автоматической смены темы в зависимости от системных настроек
- **Сохранение настроек**: Все настройки сохраняются локально через DataStore
- **Плавные анимации**: Анимированные переходы между темами
- **Красивый UI**: Интуитивный интерфейс выбора тем с превью цветов

## 📁 Структура файлов

```
app/src/main/java/com/worldmates/messenger/ui/theme/
├── Theme.kt                    # Главная тема приложения
├── Colors.kt                   # Базовые цвета
├── ThemeVariant.kt            # Варианты тем и палитры
├── ThemeManager.kt            # Управление состоянием темы
├── ThemeRepository.kt         # Сохранение настроек в DataStore
├── ThemeSettingsScreen.kt     # UI экрана настроек тем
├── Typography.kt              # Типография
└── Shapes.kt                  # Формы элементов
```

## 🎨 Доступные темы

### 1. Classic Blue 💙
Классическая синяя тема в стиле Messenger
- Primary: `#0084FF`
- Идеально для поклонников Facebook Messenger

### 2. Deep Ocean 🌊
Глубокие океанские оттенки синего и бирюзового
- Primary: `#006BA6`
- Спокойные, профессиональные тона

### 3. Sunset Dreams 🌅
Теплые оранжево-розовые тона заката
- Primary: `#FF6B35`
- Яркая и энергичная

### 4. Forest Green 🌲
Природные зеленые и изумрудные оттенки
- Primary: `#2E7D32`
- Успокаивающая природная палитра

### 5. Purple Dream 💜
Элегантные фиолетовые и сиреневые тона
- Primary: `#6A1B9A`
- Роскошная и изысканная

### 6. Rose Gold 🌹
Утонченное сочетание розового и золотого
- Primary: `#E91E63`
- Элегантная и стильная

### 7. Monochrome ⚫
Минималистичная черно-белая тема
- Primary: `#212121`
- Строгий минимализм

### 8. Nord Frost ❄️
Холодные северные оттенки Nord палитры
- Primary: `#5E81AC`
- Спокойная скандинавская эстетика

### 9. Dracula Night 🦇
Темная тема с яркими акцентами
- Primary: `#BD93F9`
- Идеально для ночного использования

### 10. Material You 🎨
Динамические цвета из обоев (Android 12+)
- Автоматически извлекает цвета из системных обоев
- Уникальная палитра для каждого пользователя

## 🚀 Использование

### Базовое использование

В любой Activity просто используйте `WorldMatesThemedApp`:

```kotlin
class YourActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируйте ThemeManager один раз
        ThemeManager.initialize(this)

        setContent {
            WorldMatesThemedApp {
                // Ваш контент здесь
                YourScreen()
            }
        }
    }
}
```

### Доступ к настройкам темы

```kotlin
@Composable
fun YourScreen() {
    val themeState = rememberThemeState()
    val themeViewModel = rememberThemeViewModel()

    // Получить текущий вариант темы
    val currentTheme = themeState.variant

    // Изменить тему
    Button(onClick = {
        themeViewModel.setThemeVariant(ThemeVariant.SUNSET)
    }) {
        Text("Установить Sunset тему")
    }

    // Переключить темную тему
    Switch(
        checked = themeState.isDark,
        onCheckedChange = { themeViewModel.toggleDarkTheme() }
    )
}
```

### Использование расширенных цветов

Для доступа к специфичным цветам мессенджера:

```kotlin
@Composable
fun MessageBubble() {
    val extendedColors = WMColors.extendedColors

    Box(
        modifier = Modifier.background(
            color = extendedColors.messageBubbleOwn
        )
    ) {
        Text(
            text = "Мое сообщение",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
```

### Программное изменение темы

```kotlin
class SettingsViewModel : ViewModel() {
    private val themeViewModel = ThemeManager.getViewModel(context)

    fun changeTheme(variant: ThemeVariant) {
        themeViewModel.setThemeVariant(variant)
    }

    fun enableMaterialYou() {
        themeViewModel.setDynamicColor(true)
    }

    fun followSystemTheme(enabled: Boolean) {
        themeViewModel.setSystemTheme(enabled)
    }
}
```

## 🎯 API Reference

### ThemeVariant (enum)

Все доступные варианты тем:

```kotlin
enum class ThemeVariant {
    CLASSIC,      // Классическая синяя
    OCEAN,        // Океан
    SUNSET,       // Закат
    FOREST,       // Лес
    PURPLE,       // Фиолетовая
    ROSE_GOLD,    // Розовое золото
    MONOCHROME,   // Монохромная
    NORD,         // Nord
    DRACULA,      // Dracula
    MATERIAL_YOU  // Material You
}
```

### ThemeState (data class)

```kotlin
data class ThemeState(
    val variant: ThemeVariant,           // Текущий вариант темы
    val isDark: Boolean,                 // Темный режим
    val useDynamicColor: Boolean,        // Использовать Material You
    val useSystemTheme: Boolean          // Следовать системной теме
)
```

### ThemeViewModel

```kotlin
class ThemeViewModel {
    val themeState: StateFlow<ThemeState>

    fun setThemeVariant(variant: ThemeVariant)
    fun toggleDarkTheme()
    fun setDarkTheme(dark: Boolean)
    fun toggleDynamicColor()
    fun setDynamicColor(enabled: Boolean)
    fun toggleSystemTheme()
    fun setSystemTheme(enabled: Boolean)
    fun resetToDefaults()
    fun isDynamicColorAvailable(): Boolean
}
```

### ExtendedColors

Дополнительные цвета для мессенджера:

```kotlin
data class ExtendedColors(
    val messageBubbleOwn: Color,         // Цвет своих сообщений
    val messageBubbleOther: Color,       // Цвет чужих сообщений
    val messageBubbleOwnDark: Color,
    val messageBubbleOtherDark: Color,
    val onlineGreen: Color,              // Индикатор онлайн
    val awayYellow: Color,               // Индикатор отошел
    val busyRed: Color,                  // Индикатор занят
    val offlineGray: Color,              // Индикатор оффлайн
    val unreadBadge: Color,              // Значок непрочитанных
    val typingIndicator: Color,          // Индикатор печатает
    val searchBarBackground: Color       // Фон поисковой строки
)
```

## 🛠️ Кастомизация

### Добавление новой темы

1. Добавьте новый вариант в `ThemeVariant` enum:

```kotlin
enum class ThemeVariant {
    // ... существующие варианты
    CUSTOM_THEME(
        displayName = "My Custom Theme",
        emoji = "🔥",
        description = "Описание моей темы"
    )
}
```

2. Добавьте палитру в функцию `getPalette()`:

```kotlin
fun ThemeVariant.getPalette(): ThemePalette {
    return when (this) {
        // ... существующие палитры
        ThemeVariant.CUSTOM_THEME -> ThemePalette(
            primary = Color(0xFFYOUR_COLOR),
            primaryDark = Color(0xFFYOUR_COLOR),
            primaryLight = Color(0xFFYOUR_COLOR),
            secondary = Color(0xFFYOUR_COLOR),
            secondaryDark = Color(0xFFYOUR_COLOR),
            secondaryLight = Color(0xFFYOUR_COLOR),
            messageBubbleOwn = Color(0xFFYOUR_COLOR),
            messageBubbleOther = Color(0xFFYOUR_COLOR),
            accent = Color(0xFFYOUR_COLOR)
        )
    }
}
```

### Изменение анимаций

Анимации настраиваются в `ThemeSettingsScreen.kt`:

```kotlin
val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.05f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

## 📱 Пользовательский интерфейс

### Доступ к настройкам тем

1. Откройте **Настройки** в главном меню
2. Нажмите на **"Тема"**
3. Выберите желаемую тему из сетки
4. Настройте темную тему и следование системной теме
5. На Android 12+ включите **Material You** для динамических цветов

### Функции UI

- **Сетка тем**: 2 колонки с превью каждой темы
- **Цветовые кружки**: Показывают основные цвета темы
- **Индикатор выбора**: Галочка на активной теме
- **Анимации**: Плавное масштабирование при выборе
- **Переключатели**: Быстрое управление темной темой и системной темой

## 🔧 Технические детали

### Хранение данных

Настройки сохраняются в **DataStore Preferences**:

```kotlin
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)
```

Ключи:
- `theme_variant`: Int (ordinal варианта темы)
- `dark_theme`: Boolean
- `dynamic_color`: Boolean
- `system_theme`: Boolean

### Material Design 3

Система тем полностью интегрирована с Material Design 3:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,  // Динамическая схема цветов
    typography = WMTypography,  // Кастомная типография
    shapes = Shapes,            // Кастомные формы
    content = content
)
```

### CompositionLocal для расширенных цветов

```kotlin
val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> { ... }

CompositionLocalProvider(
    LocalExtendedColors provides extendedColors
) {
    MaterialTheme { ... }
}
```

## 🎓 Примеры

### Пример 1: Карточка с темой

```kotlin
@Composable
fun ThemedCard() {
    val extendedColors = WMColors.extendedColors

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Индикатор онлайн
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = extendedColors.onlineGreen,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Пользователь онлайн",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

### Пример 2: Пузырь сообщения

```kotlin
@Composable
fun MessageBubble(text: String, isOwn: Boolean) {
    val extendedColors = WMColors.extendedColors
    val backgroundColor = if (isOwn) {
        extendedColors.messageBubbleOwn
    } else {
        extendedColors.messageBubbleOther
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = text,
            color = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
```

## 🐛 Отладка

### Проверка текущей темы

```kotlin
@Composable
fun ThemeDebugInfo() {
    val themeState = rememberThemeState()

    Column {
        Text("Variant: ${themeState.variant.displayName}")
        Text("Dark mode: ${themeState.isDark}")
        Text("Dynamic: ${themeState.useDynamicColor}")
        Text("System: ${themeState.useSystemTheme}")
    }
}
```

### Сброс к настройкам по умолчанию

```kotlin
val themeViewModel = rememberThemeViewModel()
themeViewModel.resetToDefaults()
```

## 📊 Производительность

- **Минимальные пересоздания**: Использование `StateFlow` и `collectAsState()`
- **Эффективное хранение**: DataStore Preferences с кешированием
- **Плавные анимации**: Spring-анимации с оптимальными параметрами
- **Ленивая инициализация**: ThemeManager инициализируется только при первом использовании

## 🔮 Будущие улучшения

- [ ] Экспорт/импорт тем
- [ ] Кастомные пользовательские темы с Color Picker
- [ ] Расписание смены тем (дневная/ночная)
- [ ] Градиентные темы
- [ ] Анимированные фоны
- [ ] Синхронизация тем между устройствами

## 📄 Лицензия

Часть проекта WorldMates Messenger.

## 👨‍💻 Автор

Создано с ❤️ для WorldMates Messenger

---

**Наслаждайтесь красивыми темами!** 🎨✨
