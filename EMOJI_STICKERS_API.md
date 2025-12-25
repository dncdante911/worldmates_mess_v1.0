# Кастомні Емоджі та Стікери - Документація API

Цей документ пояснює, як підключити власні паки емоджі та стікерів до WorldMates Messenger через WoWonder API.

## 📋 Зміст

- [Огляд](#огляд)
- [Емоджі](#емоджі)
  - [Структура даних](#структура-даних-емоджі)
  - [API Endpoints](#api-endpoints-емоджі)
  - [Приклади](#приклади-емоджі)
- [Стікери](#стікери)
  - [Структура даних](#структура-даних-стікери)
  - [API Endpoints](#api-endpoints-стікери)
  - [Приклади](#приклади-стікери)
- [Формат зображень](#формат-зображень)
- [Обмеження](#обмеження)

---

## Огляд

WorldMates Messenger підтримує:
- ✅ **Стандартні паки** - вбудовані емоджі (400+) та стікери (32)
- ✅ **Кастомні паки** - власні емоджі та стікери через API
- ✅ **Множинні паки** - можна активувати кілька паків одночасно
- ✅ **Кешування** - паки зберігаються локально для швидкого доступу

---

## Емоджі

### Структура даних емоджі

#### EmojiPack (Пак емоджі)
```json
{
  "id": 1,
  "name": "Мої емоджі",
  "description": "Власний пак смайликів",
  "icon_url": "https://example.com/icon.png",
  "author": "Моє ім'я",
  "emojis": [...],
  "is_active": true,
  "created_at": "2024-01-01 12:00:00",
  "updated_at": "2024-01-01 12:00:00"
}
```

#### CustomEmoji (Кастомний емоджі)
```json
{
  "id": 1,
  "code": ":custom_smile:",
  "url": "https://example.com/emoji/smile.png",
  "pack_id": 1,
  "name": "Усміхнений",
  "keywords": ["smile", "happy", "joy"],
  "created_at": "2024-01-01 12:00:00"
}
```

### API Endpoints емоджі

#### 1. Отримати список паків
```http
GET /api/v2/?type=get_emoji_packs&access_token={TOKEN}
```

**Відповідь:**
```json
{
  "api_status": 200,
  "packs": [
    {
      "id": 1,
      "name": "Мої емоджі",
      "description": "Власний пак",
      "is_active": true,
      ...
    }
  ]
}
```

#### 2. Отримати емоджі з конкретного паку
```http
GET /api/v2/?type=get_emoji_pack&pack_id={PACK_ID}&access_token={TOKEN}
```

**Відповідь:**
```json
{
  "api_status": 200,
  "pack": {
    "id": 1,
    "name": "Мої емоджі",
    ...
  },
  "emojis": [
    {
      "id": 1,
      "code": ":smile:",
      "url": "https://example.com/smile.png",
      ...
    }
  ]
}
```

#### 3. Активувати пак
```http
POST /api/v2/?type=activate_emoji_pack&access_token={TOKEN}
Content-Type: application/x-www-form-urlencoded

pack_id=1
```

#### 4. Деактивувати пак
```http
POST /api/v2/?type=deactivate_emoji_pack&access_token={TOKEN}
Content-Type: application/x-www-form-urlencoded

pack_id=1
```

### Приклади емоджі

#### Створення паку емоджі (Backend)

```php
// database structure
CREATE TABLE emoji_packs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    description TEXT,
    icon_url VARCHAR(500),
    author VARCHAR(255),
    is_active BOOLEAN DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE custom_emojis (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pack_id INT,
    code VARCHAR(100),
    url VARCHAR(500),
    name VARCHAR(255),
    keywords JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pack_id) REFERENCES emoji_packs(id) ON DELETE CASCADE
);
```

---

## Стікери

### Структура даних стікери

#### StickerPack (Пак стікерів)
```json
{
  "id": 1,
  "name": "Мої стікери",
  "description": "Власний пак стікерів",
  "icon_url": "https://example.com/pack_icon.png",
  "thumbnail_url": "https://example.com/pack_thumb.png",
  "author": "Моє ім'я",
  "stickers": [...],
  "sticker_count": 24,
  "is_active": true,
  "is_animated": false,
  "created_at": "2024-01-01 12:00:00",
  "updated_at": "2024-01-01 12:00:00"
}
```

#### Sticker (Стікер)
```json
{
  "id": 1,
  "pack_id": 1,
  "file_url": "https://example.com/stickers/001.png",
  "thumbnail_url": "https://example.com/stickers/thumbs/001.png",
  "emoji": "😊",
  "keywords": ["smile", "happy"],
  "width": 512,
  "height": 512,
  "file_size": 45000,
  "format": "webp"
}
```

### API Endpoints стікери

#### 1. Отримати список паків стікерів
```http
GET /api/v2/?type=get_sticker_packs&access_token={TOKEN}
```

**Відповідь:**
```json
{
  "api_status": 200,
  "packs": [
    {
      "id": 1,
      "name": "Мої стікери",
      "sticker_count": 24,
      "is_active": true,
      ...
    }
  ]
}
```

#### 2. Отримати стікери з конкретного паку
```http
GET /api/v2/?type=get_sticker_pack&pack_id={PACK_ID}&access_token={TOKEN}
```

**Відповідь:**
```json
{
  "api_status": 200,
  "pack": {
    "id": 1,
    "name": "Мої стікери",
    ...
  },
  "stickers": [
    {
      "id": 1,
      "file_url": "https://example.com/sticker1.webp",
      "emoji": "😊",
      ...
    }
  ]
}
```

#### 3. Активувати пак стікерів
```http
POST /api/v2/?type=activate_sticker_pack&access_token={TOKEN}
Content-Type: application/x-www-form-urlencoded

pack_id=1
```

#### 4. Деактивувати пак стікерів
```http
POST /api/v2/?type=deactivate_sticker_pack&access_token={TOKEN}
Content-Type: application/x-www-form-urlencoded

pack_id=1
```

#### 5. Надіслати стікер
```http
POST /api/v2/?type=send_sticker&access_token={TOKEN}
Content-Type: application/x-www-form-urlencoded

user_id=123           # Для приватного чату
group_id=456          # Для групового чату
sticker_id=789
message_hash_id=unique-hash-id
```

### Приклади стікери

#### Створення паку стікерів (Backend)

```php
// database structure
CREATE TABLE sticker_packs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    description TEXT,
    icon_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    author VARCHAR(255),
    sticker_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT 0,
    is_animated BOOLEAN DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE stickers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pack_id INT,
    file_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    emoji VARCHAR(10),
    keywords JSON,
    width INT,
    height INT,
    file_size INT,
    format VARCHAR(10),
    FOREIGN KEY (pack_id) REFERENCES sticker_packs(id) ON DELETE CASCADE
);
```

---

## Формат зображень

### Емоджі
- **Розмір**: 32x32px або 64x64px (рекомендовано)
- **Формат**: PNG, SVG
- **Розмір файлу**: до 50 KB
- **Фон**: Прозорий

### Стікери
- **Розмір**: 512x512px (рекомендовано)
- **Формат**: WebP (рекомендовано), PNG
- **Розмір файлу**: до 500 KB
- **Фон**: Прозорий або білий

---

## Обмеження

### Кількість
- **Паків емоджі**: Необмежено
- **Паків стікерів**: Необмежено
- **Емоджі в паку**: до 200
- **Стікерів в паку**: до 100

### Активні паки
- Користувач може активувати до **10 паків емоджі** одночасно
- Користувач може активувати до **10 паків стікерів** одночасно
- Стандартний пак завжди активний

### Продуктивність
- Паки кешуються локально після першого завантаження
- Зображення завантажуються асинхронно через Coil
- При зміні активних паків автоматично оновлюється UI

---

## Приклади використання в додатку

### Емоджі

```kotlin
// Отримання паків емоджі
val emojiRepository = EmojiRepository.getInstance(context)
emojiRepository.fetchEmojiPacks()

// Активація паку
emojiRepository.activateEmojiPack(packId = 1)

// Отримання активних емоджі
val activeEmojis = emojiRepository.customEmojis.collectAsState()
```

### Стікери

```kotlin
// Отримання паків стікерів
val stickerRepository = StickerRepository.getInstance(context)
stickerRepository.fetchStickerPacks()

// Активація паку
stickerRepository.activateStickerPack(packId = 1)

// Надсилання стікера
viewModel.sendSticker(stickerId = 789)
```

---

## Підтримка

Якщо у вас виникли питання або проблеми з підключенням кастомних паків:
1. Перевірте формат JSON відповідей API
2. Переконайтеся, що URLs зображень доступні
3. Перевірте логи: `adb logcat | grep -i emoji` або `adb logcat | grep -i sticker`
4. Очистіть кеш додатку, якщо паки не оновлюються

---

**Версія документації**: 1.0
**Дата оновлення**: 2024-12-25
