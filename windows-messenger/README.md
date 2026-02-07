# WorldMates Windows Messenger

Desktop клиент под **Windows 10/11** (Electron + React + TypeScript), который использует те же backend API, что Android-приложение.

## Что реализовано в текущем состоянии

- Авторизация через `?type=auth`
- Список чатов через `?type=get_chats`
- История сообщений через `?type=get_user_messages`
- Отправка текста через `?type=send_message`
- Realtime через Socket.IO: `join`, `private_message`, `new_message`
- UI в Telegram-подобной компоновке: rail + список чатов + область диалога
- Локальное сохранение desktop-сессии (token/user)
- Подготовлен packaging для `exe/msi/portable` через `electron-builder`

## Технологии

- Electron
- React 18 + TypeScript
- Vite
- socket.io-client
- electron-builder

## Быстрый старт

```bash
cd windows-messenger
npm install
npm run dev
```

## Сборка web-части

```bash
npm run build:web
```

## Сборка инсталляторов Windows

```bash
npm run dist:win
```

Результат будет в `windows-messenger/release/`:
- `WorldMates Messenger-<version>-<arch>.exe` (NSIS installer)
- `WorldMates Messenger-<version>-<arch>.msi`
- portable вариант (если включен в target)

Главный запускаемый файл приложения внутри сборки:
- `WorldMatesMessenger.exe`

## Что нужно для 1:1 parity с Android

Чтобы получить полную идентичность (звонки, stories, каналы, backup, security flows, media pipelines), нужен отдельный этап доработки desktop-клиента с переносом всего функционала и desktop-адаптацией UI/UX. Текущая версия — рабочая MVP-база, уже совместимая с существующим API-контуром.

## Ресурсы для installer branding

Положите иконку/ресурсы в `windows-messenger/build/`:
- `icon.ico`
- optional: `installerHeader.bmp`, `installerSidebar.bmp`
