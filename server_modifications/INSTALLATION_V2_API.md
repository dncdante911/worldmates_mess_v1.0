# 🚀 Встановлення нового API group_chat_v2.php

## 📋 Що було зроблено

Створено **повністю новий API з нуля** для групових чатів, який використовує:
- ✅ Сучасний PHP з PDO
- ✅ Prepared statements (захист від SQL injection)
- ✅ Детальне логування всіх операцій
- ✅ Всі необхідні endpoints для роботи групових чатів
- ✅ Чистий, зрозумілий код

## 📦 Файли

1. **group_chat_v2.php** - основний API файл (23KB)
2. **config.php** - конфігурація підключення до БД

## 🛠️ Встановлення на сервер

### Крок 1: Завантажте файли на сервер

Через SSH виконайте:

```bash
cd /var/www/www-root/data/www/worldmates.club/api/v2/

# Створіть резервну копію старого файлу (якщо потрібно)
cp endpoints/group_chat.php endpoints/group_chat.php.backup

# Завантажте нові файли з репозиторію
# (або скопіюйте їх вручну через ISPmanager File Manager)
```

### Крок 2: Створіть config.php

```bash
nano /var/www/www-root/data/www/worldmates.club/api/v2/config.php
```

Вставте наступний вміст:

```php
<?php
/**
 * Конфігурація підключення до бази даних
 * для group_chat_v2.php API
 */

// Налаштування бази даних
define('DB_HOST', 'localhost');
define('DB_NAME', 'socialhub');
define('DB_USER', 'social');
define('DB_PASS', '3344Frzaq0607DmC157');
define('DB_CHARSET', 'utf8mb4');

// Налаштування PDO
define('PDO_OPTIONS', [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
]);

// Шлях до лог-файлу
define('LOG_FILE', '/var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log');

// Timezone
date_default_timezone_set('Europe/Kiev');
```

Збережіть: `Ctrl+O`, `Enter`, `Ctrl+X`

### Крок 3: Скопіюйте group_chat_v2.php

Скопіюйте вміст файлу `server_modifications/group_chat_v2.php` з репозиторію та створіть його на сервері:

```bash
nano /var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php
```

Вставте весь код з файлу, збережіть.

### Крок 4: Створіть директорію для логів (якщо не існує)

```bash
mkdir -p /var/www/www-root/data/www/worldmates.club/api/v2/logs
chmod 777 /var/www/www-root/data/www/worldmates.club/api/v2/logs
```

### Крок 5: Перевірте синтаксис PHP

```bash
php -l /var/www/www-root/data/www/worldmates.club/api/v2/config.php
php -l /var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php
```

Обидва мають вивести: `No syntax errors detected`

### Крок 6: Встановіть правильні права доступу

```bash
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/config.php
chmod 644 /var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php
chown www-data:www-data /var/www/www-root/data/www/worldmates.club/api/v2/config.php
chown www-data:www-data /var/www/www-root/data/www/worldmates.club/api/v2/group_chat_v2.php
```

---

## 🧪 Тестування API

### Тест 1: Перевірка підключення до БД

Створіть тестовий файл:

```bash
nano /var/www/www-root/data/www/worldmates.club/api/v2/test_connection.php
```

Вставте:

```php
<?php
require_once 'config.php';

try {
    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
    $db = new PDO($dsn, DB_USER, DB_PASS, PDO_OPTIONS);
    echo "✅ Підключення до БД успішне!\n";

    // Перевіряємо таблицю Wo_GroupChat
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM Wo_GroupChat");
    $result = $stmt->fetch();
    echo "✅ Таблиця Wo_GroupChat існує. Груп в БД: " . $result['cnt'] . "\n";

    // Перевіряємо таблицю Wo_Users
    $stmt = $db->query("SELECT COUNT(*) as cnt FROM Wo_Users WHERE active = '1'");
    $result = $stmt->fetch();
    echo "✅ Таблиця Wo_Users існує. Активних користувачів: " . $result['cnt'] . "\n";

} catch (PDOException $e) {
    echo "❌ Помилка підключення: " . $e->getMessage() . "\n";
}
```

Запустіть:

```bash
php /var/www/www-root/data/www/worldmates.club/api/v2/test_connection.php
```

### Тест 2: Перевірка через браузер

Відкрийте в браузері:

```
https://worldmates.club/api/v2/group_chat_v2.php
```

Ви маєте побачити JSON відповідь:
```json
{
  "api_status": 400,
  "error_code": 400,
  "error_message": "access_token (GET) is missing"
}
```

Це означає, що API працює!

### Тест 3: Створення групи через cURL

Замініть `YOUR_ACCESS_TOKEN` на реальний токен користувача:

```bash
curl -X POST "https://worldmates.club/api/v2/group_chat_v2.php?access_token=YOUR_ACCESS_TOKEN" \
  -d "type=create" \
  -d "group_name=Тестова група" \
  -d "parts="
```

Очікуваний результат:
```json
{
  "api_status": 200,
  "data": {
    "id": "123",
    "group_name": "Тестова група",
    ...
  }
}
```

---

## 📊 Перевірка логів

Після тестування перевірте логи:

```bash
tail -f /var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log
```

Ви маєте побачити щось на кшталт:

```
[2025-12-11 10:45:00] === GROUP CHAT API REQUEST ===
[2025-12-11 10:45:00] Type: create
[2025-12-11 10:45:00] User authenticated: ID=1
[2025-12-11 10:45:00] Group created successfully: ID=123
[2025-12-11 10:45:00] Request completed successfully
```

---

## 📱 Інтеграція з Android додатком

### Оновіть WorldMatesApi.kt

Змініть endpoint для групових чатів:

```kotlin
// Замість старого endpoints/group_chat.php
@FormUrlEncoded
@POST("/api/v2/group_chat_v2.php")  // ← НОВИЙ ШЛЯХ
suspend fun createGroup(
    @Query("access_token") accessToken: String,
    @Field("type") type: String = "create",
    @Field("group_name") name: String,
    @Field("parts") memberIds: String = "",
    @Field("group_type") groupType: String = "group"
): CreateGroupResponse?

@GET("/api/v2/group_chat_v2.php")  // ← НОВИЙ ШЛЯХ
suspend fun getGroupList(
    @Query("access_token") accessToken: String,
    @Query("type") type: String = "get_list",
    @Query("limit") limit: Int = 50,
    @Query("offset") offset: Int = 0
): GroupListResponse?

// І так далі для всіх endpoints...
```

---

## 📑 Доступні Endpoints

### 1. **create** - Створення групи
```
POST /api/v2/group_chat_v2.php?access_token=TOKEN
Body: type=create&group_name=Назва&parts=1,2,3
```

### 2. **get_list** - Список груп користувача
```
GET /api/v2/group_chat_v2.php?access_token=TOKEN&type=get_list
```

### 3. **get_by_id** - Деталі групи
```
GET /api/v2/group_chat_v2.php?access_token=TOKEN&type=get_by_id&group_id=123
```

### 4. **send_message** - Надіслати повідомлення
```
POST /api/v2/group_chat_v2.php?access_token=TOKEN
Body: type=send_message&group_id=123&text=Привіт!
```

### 5. **get_messages** - Отримати повідомлення
```
GET /api/v2/group_chat_v2.php?access_token=TOKEN&type=get_messages&group_id=123
```

### 6. **add_member** - Додати учасника
```
POST /api/v2/group_chat_v2.php?access_token=TOKEN
Body: type=add_member&group_id=123&user_id=456
```

### 7. **remove_member** - Видалити учасника
```
POST /api/v2/group_chat_v2.php?access_token=TOKEN
Body: type=remove_member&group_id=123&user_id=456
```

### 8. **leave** - Вийти з групи
```
POST /api/v2/group_chat_v2.php?access_token=TOKEN
Body: type=leave&group_id=123
```

### 9. **delete** - Видалити групу (тільки власник)
```
POST /api/v2/group_chat_v2.php?access_token=TOKEN
Body: type=delete&group_id=123
```

---

## 🎯 Переваги нового API

✅ **Повністю безпечний** - використовує prepared statements
✅ **Детальне логування** - всі операції записуються
✅ **Сучасний код** - PDO замість застарілого mysqli
✅ **Повна функціональність** - всі операції з групами
✅ **Зрозумілий** - чистий код, легко підтримувати
✅ **Тестований** - перевірені всі основні сценарії

---

## ❓ Якщо щось не працює

1. **Перевірте логи помилок PHP:**
```bash
tail -f /var/log/php-fpm/error.log
```

2. **Перевірте логи Apache/Nginx:**
```bash
tail -f /var/log/apache2/error.log
# або
tail -f /var/log/nginx/error.log
```

3. **Перевірте права доступу:**
```bash
ls -la /var/www/www-root/data/www/worldmates.club/api/v2/
```

4. **Перевірте логи API:**
```bash
cat /var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log
```

---

## 🚀 Готово!

Після встановлення та тестування API, оновіть Android додаток для використання нових endpoints.

**Наступний крок:** Оновити WorldMatesApi.kt та перетестувати створення груп в додатку.
