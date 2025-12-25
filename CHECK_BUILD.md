# Checklist перед збіркою

## 1. Перевір ці файли на помилки:

### MessagesScreen.kt
- Рядок 92: `var editingMessage by remember { mutableStateOf<Message?>(null) }` ✅
- Рядок 374-378: `onEdit` callback ✅
- Рядок 420-426: `EditIndicator` виклик ✅
- Рядок 451-462: Logic для editMessage ✅
- Рядок 1573-1581: `MessageContextMenu` signature з `onEdit` ✅
- Рядок 1612-1618: Edit menu item ✅
- Рядок 1743-1803: `EditIndicator` composable ✅

### MessagesViewModel.kt
- Рядок 245-289: `editMessage()` функція ✅
- Рядок 294-329: `deleteMessage()` функція ✅
- Рядок 336-380: `toggleReaction()` функція ✅

### WorldMatesApi.kt
- Рядок 262-265: `deleteMessage` endpoint ✅
- Рядок 269-273: `editMessage` endpoint ✅
- Рядок 279-283: `addReaction` endpoint ✅
- Рядок 287-291: `removeReaction` endpoint ✅

## 2. Gradle Build команди:

```bash
# Очистка
./gradlew clean

# Збірка
./gradlew assembleDebug

# Або в Android Studio:
# Build → Rebuild Project
```

## 3. Перевір логи:

Якщо збірка провалюється, логи будуть містити:
- **Unresolved reference** - відсутні імпорти
- **Type mismatch** - неправильні типи параметрів
- **Duplicate class** - конфлікт класів

## 4. Інсталяція APK:

```bash
# Знайди APK:
app/build/outputs/apk/debug/app-debug.apk

# Встанови через ADB:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 5. Якщо все ще не працює:

### Перевір що ти:
1. ✅ Видалив старий додаток з телефону
2. ✅ Зробив Rebuild Project (не просто Build!)
3. ✅ Встановив НОВИЙ APK (перевір дату модифікації файлу)
4. ✅ Перезапустив додаток після встановлення

### Тестування функцій:
1. **Edit**: Довгий тап на своє текстове повідомлення → "Редагувати"
2. **Delete**: Довгий тап на своє повідомлення → "Видалити"
3. **Reply**: Довгий тап на будь-яке повідомлення → "Відповісти"
4. **Forward**: Довгий тап → "Переслати"
5. **Copy**: Довгий тап на текстове повідомлення → "Копіювати текст"
6. **Reactions**: Довгий тап → панель з 8 емоджі

## 6. Дебаг логи:

Якщо функції не працюють, перевір Logcat:
```bash
adb logcat | grep -i "MessagesScreen\|MessagesViewModel"
```

Шукай:
- "Повідомлення відредаговано" - підтверджує успішне редагування
- "Повідомлення видалено" - підтверджує видалення
- API Error - проблеми з сервером
