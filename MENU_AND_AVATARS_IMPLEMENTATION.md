# 📋 РЕАЛИЗАЦИЯ ПОЛНОГО ФУНКЦИОНАЛА БОКОВОГО МЕНЮ И АВАТАРОК

## 🎯 Задачи

### 1. Аватарки по умолчанию (male/female)
### 2. Выбор пола при регистрации
### 3. Полный функционал бокового меню

---

## 🖼️ 1. СИСТЕМА АВАТАРОК

### Текущее состояние в WoWonder:

**Пути к дефолтным аватаркам:**
- `upload/photos/d-avatar.jpg` - мужской аватар (default)
- `upload/photos/f-avatar.jpg` - женский аватар (female)

**Автоматическая смена при изменении пола:**
```php
// api/v2/endpoints/update-user-data.php: lines 272-277
if (!empty($user_data['gender']) && $user_data['gender'] == 'female'
    && $wo['user']['avatar_org'] == 'upload/photos/d-avatar.jpg'
    && empty($_FILES["avatar"])) {
    $user_data['avatar'] = 'upload/photos/f-avatar.jpg';
}
if (!empty($user_data['gender']) && $user_data['gender'] == 'male'
    && $wo['user']['avatar_org'] == 'upload/photos/f-avatar.jpg'
    && empty($_FILES["avatar"])) {
    $user_data['avatar'] = 'upload/photos/d-avatar.jpg';
}
```

### Что нужно сделать:

#### A. На сервере (WoWonder):

**📁 Добавить новые аватарки:**
Заменить стандартные `d-avatar.jpg` и `f-avatar.jpg` на те, что на скриншотах (собачки):
- `d-avatar.jpg` - голубая собачка (мужской)
- `f-avatar.jpg` - розовая собачка (женский)

**Путь:** `/worldmates-clear-source/site/upload/photos/`

```bash
# Команды для замены:
cp male-avatar.jpg /worldmates-clear-source/site/upload/photos/d-avatar.jpg
cp female-avatar.jpg /worldmates-clear-source/site/upload/photos/f-avatar.jpg
```

#### B. В Android приложении:

**📱 1. Добавить ресурсы аватарок:**

`app/src/main/res/drawable/`
- `avatar_male.png` - голубая собачка
- `avatar_female.png` - розовая собачка

**📱 2. Обновить Register API:**

Файл: `app/.../network/WorldMatesApi.kt`
```kotlin
@FormUrlEncoded
@POST("../phone/register_user.php?type=user_registration")
suspend fun register(
    @Field("username") username: String,
    @Field("email") email: String? = null,
    @Field("phone_number") phoneNumber: String? = null,
    @Field("password") password: String,
    @Field("confirm_password") confirmPassword: String,
    @Field("s") sessionId: String,
    @Field("device_type") deviceType: String = "phone",
    @Field("gender") gender: String = "male",  // ✅ УЖЕ ЕСТЬ!
    @Field("android_m_device_id") deviceId: String? = null
): AuthResponse
```

**✅ ПАРАМЕТР GENDER УЖЕ ПОДДЕРЖИВАЕТСЯ!**

**📱 3. Добавить UI выбора пола:**

Файл: `app/.../ui/register/RegisterActivity.kt`

Добавить после полей username/email:

```kotlin
// Выбор пола
var selectedGender by remember { mutableStateOf("male") }

// В RegisterFormCard добавить:
Spacer(modifier = Modifier.height(16.dp))

Text(
    "Оберіть стать:",
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.fillMaxWidth()
)

Spacer(modifier = Modifier.height(8.dp))

// Визуальный выбор с аватарками
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    // Мужской
    GenderSelectionCard(
        gender = "male",
        isSelected = selectedGender == "male",
        onSelect = { selectedGender = "male" },
        avatarRes = R.drawable.avatar_male,
        label = "Чоловік"
    )

    // Женский
    GenderSelectionCard(
        gender = "female",
        isSelected = selectedGender == "female",
        onSelect = { selectedGender = "female" },
        avatarRes = R.drawable.avatar_female,
        label = "Жінка"
    )
}
```

**📱 4. Компонент GenderSelectionCard:**

```kotlin
@Composable
fun GenderSelectionCard(
    gender: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    @DrawableRes avatarRes: Int,
    label: String
) {
    Surface(
        modifier = Modifier
            .size(120.dp, 140.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null,
        tonalElevation = if (isSelected) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = label,
                modifier = Modifier.size(70.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
```

**📱 5. Обновить вызов регистрации:**

Файл: `app/.../ui/register/RegisterViewModel.kt`

```kotlin
fun registerWithEmail(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    gender: String = "male"  // ✅ Добавить параметр
) {
    // ... валидация ...

    val response = RetrofitClient.apiService.register(
        username = username,
        email = email,
        phoneNumber = null,
        password = password,
        confirmPassword = confirmPassword,
        sessionId = sessionId,
        gender = gender  // ✅ Передать
    )
}

fun registerWithPhone(
    username: String,
    phoneNumber: String,
    password: String,
    confirmPassword: String,
    gender: String = "male"  // ✅ Добавить параметр
) {
    // ... аналогично ...
}
```

---

## 📱 2. ПОЛНЫЙ ФУНКЦИОНАЛ БОКОВОГО МЕНЮ

### Текущие пункты меню:

| Пункт | Статус | Действие |
|-------|--------|----------|
| Мій профіль | ⚠️ Toast | ✅ Реализовать экран |
| Нова група | ⚠️ Toast | ✅ Реализовать диалог |
| Створити Story | ✅ Работает | - |
| Контакти | ✅ Работает | - |
| Черновики | ✅ Работает | - |
| Дзвінки | ⚠️ Toast | ✅ Реализовать экран |
| Збережені повідомлення | ⚠️ Toast | ✅ Реализовать экран |
| Налаштування | ✅ Работает | - |
| Запросити друзів | ⚠️ Toast | ✅ Реализовать Share |
| Про додаток | ⚠️ Toast | ✅ Реализовать диалог |

---

### 📋 РЕАЛИЗАЦИЯ КАЖДОГО ПУНКТА

#### 1. МІЙ ПРОФІЛЬ (My Profile)

**Создать:** `app/.../ui/profile/UserProfileActivity.kt`

**Функционал:**
- Показ информации пользователя
- Редактирование профиля
- Смена аватара
- Смена cover фото
- Показ statistics (posts, followers, following)

**API:**
- `GET ?type=get-user-data` - получить данные
- `POST ?type=update-user-data` - обновить данные
- `POST ?type=upload_user_avatar` - загрузить аватар

**UI Sections:**
- Header с avatar и cover
- Информация (имя, username, bio)
- Stats (посты, подписчики, подписки)
- Кнопка Edit Profile

**Где добавить вызов:**
```kotlin
// ChatsActivity.kt: line ~715
DrawerMenuItem(
    icon = Icons.Default.Person,
    title = "Мій профіль",
    onClick = {
        onClose()
        context.startActivity(Intent(context, UserProfileActivity::class.java))
    }
)
```

---

#### 2. НОВА ГРУПА (New Group)

**Уже есть:** `app/.../ui/groups/CreateGroupDialog.kt`

**Нужно:** Интегрировать в ChatsActivity

**Где добавить:**
```kotlin
// ChatsActivity.kt: line ~724
var showCreateGroupDialog by remember { mutableStateOf(false) }

DrawerMenuItem(
    icon = Icons.Default.Group,
    title = "Нова група",
    onClick = {
        onClose()
        showCreateGroupDialog = true
    }
)

// После DrawerContent:
if (showCreateGroupDialog) {
    CreateGroupDialog(
        onDismiss = { showCreateGroupDialog = false },
        onGroupCreated = { group ->
            showCreateGroupDialog = false
            // Navigate to group
        }
    )
}
```

---

#### 3. ДЗВІНКИ (Calls)

**Создать:** `app/.../ui/calls/CallsHistoryActivity.kt`

**Функционал:**
- История звонков (входящие, исходящие, пропущенные)
- Аудио и видео звонки
- Продолжительность
- Callback функция

**API:**
- `GET /api/v2/endpoints/get-calls-history.php`

**Создать API endpoint:**
```php
// api/v2/endpoints/get-calls-history.php
<?php
if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_POST['access_token']);

    // Получить историю из таблиц wo_audiocalls и wo_agoravideocall
    $calls_query = "
        SELECT
            id, from_id, to_id, type, time, status, declined
        FROM (
            SELECT id, from_id, to_id, 'audio' as type, time, status, declined
            FROM " . T_AUDIO_CALLES . "
            WHERE from_id = {$user_id} OR to_id = {$user_id}
            UNION ALL
            SELECT id, from_id, to_id, 'video' as type, time, status, declined
            FROM " . T_AGORA . "
            WHERE from_id = {$user_id} OR to_id = {$user_id}
        ) AS combined_calls
        ORDER BY time DESC
        LIMIT 100
    ";

    $calls = mysqli_query($sqlConnect, $calls_query);
    $calls_data = [];

    while ($call = mysqli_fetch_assoc($calls)) {
        $user_data = Wo_UserData($call['from_id'] == $user_id ? $call['to_id'] : $call['from_id']);
        $calls_data[] = [
            'id' => $call['id'],
            'type' => $call['type'],
            'direction' => $call['from_id'] == $user_id ? 'outgoing' : 'incoming',
            'status' => $call['status'],
            'declined' => $call['declined'],
            'time' => $call['time'],
            'user' => [
                'user_id' => $user_data['user_id'],
                'username' => $user_data['username'],
                'name' => $user_data['name'],
                'avatar' => $user_data['avatar']
            ]
        ];
    }

    $data = [
        'api_status' => 200,
        'calls' => $calls_data
    ];
}
?>
```

---

#### 4. ЗБЕРЕЖЕНІ ПОВІДОМЛЕННЯ (Saved Messages)

**Создать:** `app/.../ui/saved/SavedMessagesActivity.kt`

**Функционал:**
- Показ сохраненных сообщений
- Закрепление сообщений
- Поиск по сохраненным

**API:**
- Использовать существующую таблицу `wo_saved_posts` или создать новую

---

#### 5. ЗАПРОСИТИ ДРУЗІВ (Invite Friends)

**Реализация через Android Share Intent:**

```kotlin
// ChatsActivity.kt: line ~805
DrawerMenuItem(
    icon = Icons.Default.Share,
    title = "Запросити друзів",
    onClick = {
        onClose()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Приєднуйся до WorldMates - найкращого месенджера! 🚀\n" +
                "Скачай тут: [App Link]"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Запросити друга"))
    }
)
```

---

#### 6. ПРО ДОДАТОК (About App)

**Создать:** `app/.../ui/components/AboutAppDialog.kt`

```kotlin
@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("WorldMates Messenger") },
        text = {
            Column {
                Text("Версія: 1.0.0", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Розроблено з ❤️ для спілкування")
                Spacer(modifier = Modifier.height(8.dp))
                Text("© 2024 WorldMates")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Функції:",
                    fontWeight = FontWeight.Bold
                )
                Text("• Безпечні чати з шифруванням")
                Text("• Групові чати та канали")
                Text("• Аудіо та відео дзвінки")
                Text("• Stories та статуси")
                Text("• Хмарне сховище")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
```

**Использование:**
```kotlin
// ChatsActivity.kt: line ~815
var showAboutDialog by remember { mutableStateOf(false) }

DrawerMenuItem(
    icon = Icons.Default.Info,
    title = "Про додаток",
    onClick = {
        onClose()
        showAboutDialog = true
    }
)

// После DrawerContent:
if (showAboutDialog) {
    AboutAppDialog(
        onDismiss = { showAboutDialog = false }
    )
}
```

---

## 🔄 СИНХРОНИЗАЦИЯ АВАТАРОК

### Как работает синхронизация:

1. **При загрузке аватара:**
   - Android вызывает `POST ?type=upload_user_avatar`
   - Сервер сохраняет файл в `upload/photos/`
   - Обновляет `avatar` и `avatar_org` в таблице `Wo_Users`
   - Возвращает URL аватара

2. **Автоматическое обновление:**
   - После загрузки Android получает новый URL
   - Обновляет UserSession.avatar
   - Все экраны автоматически обновляются через StateFlow

3. **На сайте:**
   - Сайт читает `avatar` из БД
   - Показывает тот же аватар что и в приложении

4. **В официальном приложении WoWonder:**
   - Использует те же API endpoints
   - Читает те же поля из БД
   - Автоматически синхронизируется

---

## 📝 ЧЕКЛИСТ РЕАЛИЗАЦИИ

### Аватарки:
- [ ] Добавить avatar_male.png и avatar_female.png в drawable
- [ ] Создать компонент GenderSelectionCard
- [ ] Добавить выбор пола в RegisterActivity
- [ ] Обновить RegisterViewModel для передачи gender
- [ ] Заменить d-avatar.jpg и f-avatar.jpg на сервере

### Боковое меню:
- [ ] Создать UserProfileActivity
- [ ] Интегрировать CreateGroupDialog
- [ ] Создать CallsHistoryActivity
- [ ] Создать get-calls-history.php API
- [ ] Создать SavedMessagesActivity
- [ ] Реализовать Share Intent
- [ ] Создать AboutAppDialog

---

## 🚀 ПОРЯДОК РЕАЛИЗАЦИИ

1. **Сначала:** Аватарки и регистрация (самое важное)
2. **Потом:** Простые диалоги (About, Share)
3. **Затем:** Сложные Activity (Profile, Calls, Saved)

---

*Документ создан: 2026-01-23*
*Версия: 1.0*
