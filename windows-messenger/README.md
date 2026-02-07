# WorldMates Windows Messenger (from scratch)

MVP desktop client for **Windows 10/11** built from zero using Electron + React + TypeScript, reusing the current Android backend contracts (`auth`, `get_chats`, `get_user_messages`, socket events).

## Что уже реализовано

- Авторизация через API `?type=auth`
- Загрузка списка чатов через API `?type=get_chats`
- Загрузка истории сообщений через API `?type=get_user_messages`
- Отправка текстового сообщения через API `?type=send_message`
- Realtime-подписка на Socket.IO события `join`, `private_message`, `new_message`
- Desktop shell для Windows (Electron main/preload)
- Базовый современный UI (dark theme, двухпанельный layout)

## Стек

- **Electron** — desktop runtime для Windows
- **React 18 + TypeScript** — интерфейс
- **Vite** — сборка фронтенда
- **socket.io-client** — realtime сообщения

## Структура

```text
windows-messenger/
  electron/         # Main/preload процессы Electron
  src/              # React приложение (UI + API + socket)
  package.json      # Scripts и зависимости
```

## Запуск в dev-режиме

```bash
cd windows-messenger
npm install
npm run dev
```

Откроется Electron окно и подключится к локальному Vite dev server.

## Сборка

```bash
cd windows-messenger
npm run build
```

Сгенерирует production web bundle в `dist/`.

> Примечание: в этом коммите сделан рабочий MVP клиент. Для полноценного production installer (`.exe`) нужен следующий шаг с `electron-builder` (иконки, signing, auto-updater, installer pipeline).

## Что планируется в следующей итерации

1. Добавить media upload/download (фото/видео/доки)
2. Добавить группы/каналы/истории
3. Добавить звонки (WebRTC/Agora desktop integration)
4. Добавить secure session storage + PIN/biometric equivalent for Windows Hello
5. Добавить packaging pipeline для `.exe` (NSIS/MSI)
