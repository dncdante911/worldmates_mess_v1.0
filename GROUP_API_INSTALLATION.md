# Інструкція по встановленню Group API на сервер

## Проблема

Android додаток отримує помилку **404 API Type Not Found** при спробі створити групу:

```json
{
  "api_status": "404",
  "errors": {
    "error_id": "1",
    "error_text": "Error: 404 API Type Not Found"
  }
}
```

Це відбувається тому, що ендпоінт `?type=create_group` **не реалізований** на сервері WoWonder.

## Рішення

Потрібно додати обробник групових API на сервер.

---

## Крок 1: Завантажити файл на сервер

### Файл для завантаження:
```
php_server_files/group_api.php
```

### Куди завантажувати:

**Варіант А (рекомендується)**: Включити в main API файл

```bash
# Підключитися до сервера
ssh user@worldmates.club

# Знайти main API файл WoWonder
cd /var/www/html/worldmates.club/api/v2

# Файли які можуть бути:
ls -la
# Шукайте: index.php, endpoints.php, api.php
```

**Варіант Б**: Створити окремий файл

```bash
# Завантажити файл
scp php_server_files/group_api.php user@worldmates.club:/var/www/html/worldmates.club/api/v2/

# Встановити права
ssh user@worldmates.club
cd /var/www/html/worldmates.club/api/v2
chmod 644 group_api.php
chown www-data:www-data group_api.php  # або apache:apache
```

---

## Крок 2: Інтеграція з WoWonder API

### Знайдіть main API файл

Відкрийте `/var/www/html/worldmates.club/api/v2/index.php` або подібний файл.

### Додайте включення group_api.php

Знайдіть секцію, де обробляються різні типи API (switch або if-else з $_GET['type']).

**Додайте** наступний код:

```php
<?php
// В main API file (index.php або endpoints.php)

// ... існуючий код ...

// Константа для безпеки
define('IN_WO_API', true);

// Отримати тип запиту
$type = isset($_GET['type']) ? Wo_Secure($_GET['type']) : '';

// Групові операції
$groupTypes = [
    'create_group',
    'update_group',
    'delete_group',
    'get_group_details',
    'get_group_members',
    'add_group_member',
    'remove_group_member',
    'set_group_admin',
    'leave_group'
];

if (in_array($type, $groupTypes)) {
    require_once('group_api.php');
    exit; // Важливо!
}

// ... решта існуючого коду для інших типів API ...
?>
```

---

## Крок 3: Перевірка роботи

### Через curl (з терміналу):

```bash
curl -X POST \
  "https://worldmates.club/api/v2/?type=create_group&access_token=YOUR_TOKEN" \
  -d "server_key=YOUR_SERVER_KEY" \
  -d "name=Test Group" \
  -d "description=Test description" \
  -d "is_private=0" \
  -d "member_ids="
```

**Очікуваний результат:**

```json
{
  "api_status": 200,
  "group_id": 123,
  "group": {
    "id": "123",
    "group_name": "Test Group",
    "description": "Test description",
    "members_count": "1",
    ...
  },
  "message": "Group created successfully"
}
```

### З Android додатку:

1. Відкрийте додаток
2. Перейдіть: **Повідомлення → Групи**
3. Натисніть синю кнопку "**Створити групу**"
4. Заповніть форму:
   - Назва групи
   - Опис (опціонально)
   - Виберіть учасників
   - Приватність
5. Натисніть "**Створити**"

**Перевірте logcat:**

```bash
adb logcat | grep -E "API_LOG|GroupsViewModel"
```

Має бути:

```
API_LOG: <-- 200 https://worldmates.club/api/v2/?type=create_group...
GroupsViewModel: Група створена успішно
```

---

## Альтернатива: Швидке тестування без інтеграції

Якщо ви хочете **швидко протестувати** без інтеграції в main API file:

### 1. Створіть standalone файл

```bash
# На сервері
cd /var/www/html/worldmates.club/api/v2

# Створіть test_groups.php
nano test_groups.php
```

### 2. Вставте наступний код:

```php
<?php
// test_groups.php - Standalone group API endpoint

// Load WoWonder config and functions
require_once('../../config.php');  // Adjust path if needed

// Define security constant
define('IN_WO_API', true);

// Include group API handlers
require_once('group_api.php');
?>
```

### 3. Тимчасово змініть Android код

В `WorldMatesApi.kt`, змініть ендпоінт:

```kotlin
@FormUrlEncoded
@POST("/api/v2/test_groups.php?type=create_group")  // Тимчасово!
suspend fun createGroup(...)
```

### 4. Rebuild і тестуйте

Після тесту поверніть назад:

```kotlin
@POST("?type=create_group")  // Нормальний ендпоінт
```

І додайте `group_api.php` в main API file як описано вище.

---

## Troubleshooting

### Помилка: "Call to undefined function Wo_Secure()"

**Причина**: Не підключено WoWonder функції

**Рішення**: В main API file переконайтеся що є:

```php
require_once('../../config.php');
// або
require_once('../../includes/functions.php');
```

### Помилка: "Direct access forbidden"

**Причина**: Константа `IN_WO_API` не визначена

**Рішення**: Додайте в main API file перед require:

```php
define('IN_WO_API', true);
require_once('group_api.php');
```

### Помилка: "Table 'Wo_GroupChatUsers' doesn't exist"

**Причина**: SQL міграція не виконана

**Рішення**: Виконайте міграцію:

```bash
mysql -u root -p wowonder < extend-group-chat-tables.sql
```

### Помилка: 500 Internal Server Error

**Перевірте логи:**

```bash
tail -f /var/log/php_errors.log
tail -f /var/log/nginx/error.log
tail -f /var/log/apache2/error.log
```

---

## Структура файлів після встановлення

```
/var/www/html/worldmates.club/
├── api/
│   └── v2/
│       ├── index.php           # Main API file (змінений)
│       └── group_api.php       # ← Новий файл
├── config.php
├── includes/
│   └── functions.php
└── ...
```

---

## Що далі?

Після успішного встановлення, всі групові операції працюватимуть:

✅ `?type=create_group` - створення групи
✅ `?type=update_group` - оновлення групи
✅ `?type=delete_group` - видалення групи
✅ `?type=add_group_member` - додавання учасників
✅ `?type=remove_group_member` - видалення учасників
✅ `?type=set_group_admin` - зміна ролей
✅ `?type=leave_group` - вихід з групи
✅ `?type=get_group_details` - деталі групи
✅ `?type=get_group_members` - список учасників

**Готово!** 🎉
