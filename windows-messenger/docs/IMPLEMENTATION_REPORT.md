# Отчет по реализации Windows клиента

## Цель
С нуля сделать рабочую базу Windows 10/11 клиента, совместимую с текущим Android backend API.

## Подключенные API/события

- `POST /api/v2/?type=auth`
- `POST /api/v2/?type=get_chats&access_token=...`
- `POST /api/v2/?type=get_user_messages&access_token=...`
- `POST /api/v2/?type=send_message&access_token=...`
- Socket.IO: `join`, `private_message`, `new_message`

## Что работает сейчас

- Логин по username/password
- Загрузка чатов после входа
- Открытие чата и загрузка сообщений
- Отправка текстовых сообщений
- Получение realtime входящих сообщений
- UI с левой панелью чатов и правой панелью диалога

## Ограничения MVP

- Без регистрации, 2FA, backup, групп/каналов/stories
- Без мультимедиа отправки
- Хранение сессии только в оперативной памяти
- Не добавлен Windows installer packaging

## Технические заметки

- Архитектура разделена на `api.ts`, `socket.ts`, `App.tsx`.
- Все URL и endpoint вынесены в `config.ts`.
- Electron preload добавлен с `contextIsolation=true` и без `nodeIntegration` для безопасной базовой конфигурации.
