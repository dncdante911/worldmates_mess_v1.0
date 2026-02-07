# Отчет по обновлению Windows-клиента

## Что изменено относительно прошлой версии

1. UI переработан в более Telegram-подобный layout:
   - левая navigation rail
   - панель чатов с табами (All / Groups / Channels)
   - поиск по чатам
   - правый диалог с message composer
2. Добавлено сохранение desktop-сессии в localStorage (автовход после перезапуска).
3. Добавлен Logout.
4. Подключен packaging pipeline под Windows:
   - NSIS installer (`.exe`)
   - MSI installer (`.msi`)
   - Portable build
5. Настроено имя запускаемого файла приложения: `WorldMatesMessenger.exe`.

## Подключенные API/события

- `POST /api/v2/?type=auth`
- `POST /api/v2/?type=get_chats&access_token=...`
- `POST /api/v2/?type=get_user_messages&access_token=...`
- `POST /api/v2/?type=send_message&access_token=...`
- Socket.IO: `join`, `private_message`, `new_message`

## Команды для сборки Windows installer

```bash
cd windows-messenger
npm install
npm run dist:win
```

Артефакты будут в папке `release/`.

## Важно по цели "1:1 с Android"

Текущий этап дает рабочую desktop основу (auth/chats/messages/realtime + installer).
Для полного 1:1 parity необходимо поэтапно перенести весь функционал Android (calls, stories, channels moderation, media pipeline, security/2FA, backups, settings parity, etc.) и адаптировать его к desktop UX.
