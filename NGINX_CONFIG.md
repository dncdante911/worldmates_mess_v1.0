# Налаштування Nginx для WorldMates Stories

## Проблема: Ошибка 413 "Payload Too Large" при загрузке видео

При попытке загрузить видео в Stories возникает ошибка **413 Request Entity Too Large**.
Это означает, что размер загружаемого файла превышает лимит nginx `client_max_body_size`.

## Решение

### 1. Найдите конфигурационный файл nginx

Обычно расположен в одном из следующих мест:
- `/etc/nginx/nginx.conf` (основной конфиг)
- `/etc/nginx/sites-available/worldmates.club` (конфиг для конкретного сайта)
- `/etc/nginx/conf.d/worldmates.conf`

### 2. Откройте файл для редактирования

```bash
sudo nano /etc/nginx/sites-available/worldmates.club
```

или

```bash
sudo nano /etc/nginx/nginx.conf
```

### 3. Добавьте или измените параметр client_max_body_size

#### Вариант А: В блоке server {} для конкретного сайта

```nginx
server {
    listen 80;
    server_name worldmates.club;

    # Увеличиваем лимит до 550MB (для видео до 500MB + запас)
    client_max_body_size 550M;

    root /var/www/www-root/data/www/worldmates.club;
    index index.php index.html;

    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }

    # ... остальная конфигурация
}
```

#### Вариант Б: В блоке http {} (глобально для всех сайтов)

```nginx
http {
    # Глобальное увеличение лимита
    client_max_body_size 550M;

    # ... остальная конфигурация

    include /etc/nginx/sites-enabled/*;
}
```

### 4. Также нужно проверить лимиты PHP

Откройте конфигурационный файл PHP:

```bash
sudo nano /etc/php/8.1/fpm/php.ini
```

Убедитесь, что установлены следующие значения:

```ini
upload_max_filesize = 550M
post_max_size = 550M
max_execution_time = 600
memory_limit = 512M
```

### 5. Перезапустите nginx и PHP-FPM

```bash
# Проверьте конфигурацию nginx на ошибки
sudo nginx -t

# Если всё ОК, перезапустите nginx
sudo systemctl restart nginx

# Перезапустите PHP-FPM
sudo systemctl restart php8.1-fpm
```

### 6. Проверьте логи при возникновении проблем

```bash
# Логи nginx
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log

# Логи PHP
sudo tail -f /var/log/php8.1-fpm.log

# Логи Stories
tail -f /var/www/www-root/data/www/worldmates.club/api/v2/logs/create-story-debug.log
```

## Проверка лимитов

### Текущие лимиты PHP

Создайте временный файл `phpinfo.php` в корне сайта:

```php
<?php
phpinfo();
?>
```

Откройте в браузере: `https://worldmates.club/phpinfo.php`

Найдите параметры:
- `upload_max_filesize`
- `post_max_size`
- `max_execution_time`
- `memory_limit`

**ВАЖНО:** Удалите файл после проверки!

```bash
rm /var/www/www-root/data/www/worldmates.club/phpinfo.php
```

## Рекомендуемые лимиты для WorldMates Stories

| Параметр | Значение | Описание |
|----------|----------|----------|
| nginx: `client_max_body_size` | 550M | Максимальный размер тела запроса |
| PHP: `upload_max_filesize` | 550M | Максимальный размер загружаемого файла |
| PHP: `post_max_size` | 550M | Максимальный размер POST запроса |
| PHP: `max_execution_time` | 600 | Макс. время выполнения скрипта (10 минут) |
| PHP: `memory_limit` | 512M | Лимит памяти для скрипта |

## Тестирование

После настройки попробуйте загрузить:
1. **Фото** (до 15MB) - должно работать без сжатия
2. **Видео < 50MB** - должно загружаться без сжатия
3. **Видео > 50MB** - должно сжиматься через ffmpeg на сервере

## Устранение неполадок

### Проблема: Всё ещё ошибка 413

1. Убедитесь, что нет других включенных конфигов nginx с меньшим лимитом
2. Проверьте, что изменения применились: `nginx -T | grep client_max_body_size`
3. Попробуйте добавить лимит в блок `location ~ \.php$`

### Проблема: Загрузка зависает

1. Увеличьте `max_execution_time` в PHP
2. Проверьте логи на тайм-ауты
3. Убедитесь, что ffmpeg работает: `which ffmpeg`

### Проблема: Ошибка 500 при загрузке фото

1. Проверьте логи: `/var/www/www-root/data/www/worldmates.club/api/v2/logs/create-story-debug.log`
2. Убедитесь, что библиотека SimpleImage найдена
3. Проверьте права доступа к директории `upload/photos/`

```bash
sudo chmod -R 755 /var/www/www-root/data/www/worldmates.club/upload/
sudo chown -R www-data:www-data /var/www/www-root/data/www/worldmates.club/upload/
```
