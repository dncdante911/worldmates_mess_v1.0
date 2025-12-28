# Stories API Documentation

## Огляд
Stories API надає функціонал для створення, перегляду, коментування та взаємодії з історіями (stories) у стилі Instagram/Telegram.

## Обмеження за типом підписки

### Безкоштовна версія:
- Максимум 2 активні stories одночасно
- Тривалість відео: до 25 секунд
- Автовидалення: через 24 години
- Реакції: ✅ (доступно)
- Коментарі: ✅ (доступно)
- Перегляди: ✅ (доступно)

### Преміум підписка (is_pro = 1):
- Максимум 15 активних stories одночасно
- Тривалість відео: до 45 секунд
- Автовидалення: через 48 годин
- Реакції: ✅ (доступно)
- Коментарі: ✅ (доступно)
- Перегляди: ✅ (доступно)

---

## API Endpoints

### 1. Створення Story

**Endpoint:** `POST /api/v2/endpoints/create-story.php`

**Параметри:**
- `file` (FILE, обов'язковий) - Медіа файл (фото або відео)
- `file_type` (STRING, обов'язковий) - Тип файлу: "image" або "video"
- `story_title` (STRING, опціонально) - Заголовок story (макс 100 символів)
- `story_description` (STRING, опціонально) - Опис story (макс 300 символів)
- `video_duration` (INT, опціонально) - Тривалість відео в секундах
- `cover` (FILE, опціонально) - Обкладинка для відео

**Приклад відповіді (успіх):**
```json
{
  "api_status": 200,
  "story_id": 123
}
```

**Можливі помилки:**
- Код 3: file is missing
- Код 4: Title is too long
- Код 5: Description is too long
- Код 6: file_type is missing
- Код 7: Incorrect value for file_type
- Код 8: Maximum stories limit reached
- Код 9: Video duration exceeds limit

---

### 2. Отримання списку Stories

**Endpoint:** `POST /api/v2/endpoints/get-stories.php`

**Параметри:**
- `limit` (INT, опціонально) - Кількість stories (за замовчуванням: 35, макс: 50)

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "stories": [
    {
      "id": 123,
      "user_id": 456,
      "title": "My Story",
      "description": "Story description",
      "posted": "1703779200",
      "expire": "1703865600",
      "thumbnail": "path/to/thumbnail.jpg",
      "user_data": { ... },
      "videos": [ ... ],
      "images": [ ... ],
      "is_owner": false,
      "is_viewed": 0,
      "view_count": 42,
      "comment_count": 5
    }
  ]
}
```

---

### 3. Отримання Story за ID

**Endpoint:** `POST /api/v2/endpoints/get_story_by_id.php`

**Параметри:**
- `id` (INT, обов'язковий) - ID story

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "story": {
    "id": 123,
    "user_id": 456,
    "title": "My Story",
    "description": "Story description",
    "posted": "1703779200",
    "expire": "1703865600",
    "thumbnail": "path/to/thumbnail.jpg",
    "user_data": { ... },
    "videos": [ ... ],
    "images": [ ... ],
    "is_owner": false,
    "view_count": 42,
    "comment_count": 5
  }
}
```

---

### 4. Видалення Story

**Endpoint:** `POST /api/v2/endpoints/delete-story.php`

**Параметри:**
- `story_id` (INT, обов'язковий) - ID story для видалення

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "message": "Story deleted successfully"
}
```

---

### 5. Перегляди Story

**Endpoint:** `POST /api/v2/endpoints/get_story_views.php`

**Параметри:**
- `story_id` (INT, обов'язковий) - ID story
- `limit` (INT, опціонально) - Кількість результатів (за замовчуванням: 20, макс: 50)
- `offset` (INT, опціонально) - Зміщення для пагінації

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "users": [
    {
      "user_id": 789,
      "username": "john_doe",
      "avatar": "path/to/avatar.jpg",
      "offset_id": 456
    }
  ]
}
```

---

### 6. Реакція на Story

**Endpoint:** `POST /api/v2/endpoints/react_story.php`

**Параметри:**
- `id` (INT, обов'язковий) - ID story
- `reaction` (STRING, обов'язковий) - Тип реакції (like, love, haha, wow, sad, angry)

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "message": "story reacted"
}
```

Повторний виклик з тією ж реакцією видалить реакцію:
```json
{
  "api_status": 200,
  "message": "reaction removed"
}
```

---

### 7. Приглушити Story користувача

**Endpoint:** `POST /api/v2/endpoints/mute_story.php`

**Параметри:**
- `user_id` (INT, обов'язковий) - ID користувача, чиї stories потрібно приглушити

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "message": "Story muted"
}
```

---

## Коментарі до Stories

### 8. Створити коментар до Story

**Endpoint:** `POST /api/v2/endpoints/create_story_comment.php`

**Параметри:**
- `story_id` (INT, обов'язковий) - ID story
- `text` (STRING, обов'язковий) - Текст коментаря

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "comment": {
    "id": 789,
    "story_id": 123,
    "user_id": 456,
    "text": "Nice story!",
    "time": 1703779200,
    "user_data": { ... }
  }
}
```

**Можливі помилки:**
- Код 3: story_id is missing or invalid
- Код 4: comment text is required
- Код 5: Story not found
- Код 6: Story has expired
- Код 7: Failed to create comment

---

### 9. Отримати коментарі до Story

**Endpoint:** `POST /api/v2/endpoints/get_story_comments.php`

**Параметри:**
- `story_id` (INT, обов'язковий) - ID story
- `limit` (INT, опціонально) - Кількість коментарів (за замовчуванням: 20, макс: 50)
- `offset` (INT, опціонально) - ID для пагінації

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "comments": [
    {
      "id": 789,
      "story_id": 123,
      "user_id": 456,
      "text": "Nice story!",
      "time": 1703779200,
      "offset_id": 789,
      "user_data": { ... }
    }
  ],
  "total": 5
}
```

---

### 10. Видалити коментар до Story

**Endpoint:** `POST /api/v2/endpoints/delete_story_comment.php`

**Параметри:**
- `comment_id` (INT, обов'язковий) - ID коментаря

**Примітка:** Видалити коментар може:
- Автор коментаря
- Власник story
- Адміністратор/модератор

**Приклад відповіді:**
```json
{
  "api_status": 200,
  "message": "Comment deleted successfully"
}
```

**Можливі помилки:**
- Код 3: comment_id is missing or invalid
- Код 4: Comment not found
- Код 5: You do not have permission to delete this comment
- Код 6: Failed to delete comment

---

## Автовидалення Stories

Stories автоматично видаляються через певний час:
- **Безкоштовна версія:** 24 години
- **Преміум підписка:** 48 годин

Для налаштування автовидалення, додайте cron job:

```bash
0 * * * * php /path/to/api-server-files/api/v2/cron/delete_expired_stories.php
```

Цей скрипт буде запускатися кожну годину і видаляти застарілі stories разом з їх медіа файлами, коментарями, переглядами та реакціями.

---

## База даних

### Таблиці:

#### Wo_UserStory
Основна таблиця для зберігання stories.

#### Wo_UserStoryMedia
Зберігає медіа файли (фото/відео) для stories з полем `duration`.

#### Wo_Story_Seen
Зберігає інформацію про перегляди stories.

#### Wo_StoryComments
Зберігає коментарі до stories.

#### Wo_Reactions
Зберігає реакції на stories (поле `story_id`).

#### Wo_Mute_Story
Зберігає інформацію про приглушені stories.

---

## Міграція бази даних

Для застосування змін до бази даних, виконайте SQL файл:

```bash
mysql -u username -p database_name < /path/to/api-server-files/sql-DB-newver/migration_story_comments.sql
```

---

## Приклади використання

### Створення відео story з обмеженнями

```javascript
const formData = new FormData();
formData.append('file', videoFile);
formData.append('file_type', 'video');
formData.append('video_duration', 20); // секунди
formData.append('story_title', 'My Video Story');
formData.append('story_description', 'Check out this cool video!');

fetch('/api/v2/endpoints/create-story.php', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

### Коментування story

```javascript
fetch('/api/v2/endpoints/create_story_comment.php', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: new URLSearchParams({
    story_id: 123,
    text: 'Great story!'
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

---

## Налаштування підписки

Для перевірки типу підписки користувача, система перевіряє поле `is_pro` в таблиці `Wo_Users`:
- `is_pro = 0` - безкоштовна версія
- `is_pro = 1` - преміум підписка

---

## Висновок

Stories API надає повний функціонал для роботи з історіями, включаючи:
- ✅ Створення з обмеженнями за підпискою
- ✅ Перегляд та список stories
- ✅ Реакції на stories
- ✅ Коментарі до stories
- ✅ Перегляди з інформацією "хто переглянув"
- ✅ Автовидалення через заданий час
- ✅ Приглушення stories окремих користувачів

Усі endpoints підтримують стандартну автентифікацію WoWonder через access token.
