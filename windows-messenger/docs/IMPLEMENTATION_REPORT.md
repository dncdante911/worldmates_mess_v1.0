# Отчет по итерации: media/groups/channels/stories/calls/auth/encryption

## Что добавлено

1. **Auth расширен**
   - Login по username/password
   - Login по phone/password
   - Registration flow

2. **Messaging + AES-256-GCM**
   - Текстовые сообщения могут отправляться в AES-256-GCM формате
   - Добавлена попытка расшифровки входящих/исторических сообщений

3. **Media upload/download**
   - Attach file при отправке сообщения
   - Поддержка image/video/docs
   - Отрисовка ссылки на скачивание медиа

4. **Groups/Channels/Stories**
   - Загрузка списков
   - Создание группы
   - Создание канала
   - Загрузка stories
   - Upload story

5. **WebRTC calls (desktop foundation)**
   - Интеграция peer connection
   - Получение ICE servers из backend
   - Fallback STUN/TURN конфиг из серверных исходников
   - Отправка offer через socket `call_signal`

6. **UI/UX update**
   - Многораздельная rail-навигация: chats/groups/channels/stories/calls
   - Единый интерфейс для всех основных разделов

7. **Packaging**
   - `.exe` + `.msi` + portable через electron-builder
   - executableName: `WorldMatesMessenger`

## Ключевые файлы

- `src/App.tsx`
- `src/api.ts`
- `src/crypto.ts`
- `src/webrtc.ts`
- `src/config.ts`
- `src/styles.css`

## Сборка

```bash
cd windows-messenger
npm install
npm run dist:win
```
