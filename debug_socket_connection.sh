#!/bin/bash

# 🔍 Скрипт діагностики Socket.IO підключення
# Використання: ./debug_socket_connection.sh

echo "═══════════════════════════════════════════════"
echo "🔍 ДІАГНОСТИКА Socket.IO для WorldMates Messenger"
echo "═══════════════════════════════════════════════"
echo ""

# Кольори для виводу
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Перевірка чи додаток запущений
echo "1️⃣ Перевірка чи додаток запущений..."
PACKAGE="com.worldmates.messenger"
PID=$(adb shell pidof $PACKAGE 2>/dev/null)

if [ -z "$PID" ]; then
    echo -e "${RED}❌ Додаток НЕ запущений!${NC}"
    echo "   Запустіть додаток на пристрої та спробуйте знову."
    exit 1
else
    echo -e "${GREEN}✅ Додаток запущений (PID: $PID)${NC}"
fi

echo ""

# 2. Перевірка чи є взагалі логи від додатка
echo "2️⃣ Перевірка логів додатка (5 секунд)..."
LOGS=$(timeout 5s adb logcat -v time | grep "$PACKAGE" | head -20)

if [ -z "$LOGS" ]; then
    echo -e "${RED}❌ НЕМАЄ логів від додатка!${NC}"
    echo "   Можливі причини:"
    echo "   - Додаток не активний"
    echo "   - Немає дозволів на логування"
    echo "   - ADB не підключений правильно"
else
    echo -e "${GREEN}✅ Логи від додатка є${NC}"
    echo "   Останні логи:"
    echo "$LOGS" | head -5
fi

echo ""

# 3. Очищення логів та запуск моніторингу
echo "3️⃣ Очищення старих логів..."
adb logcat -c
echo -e "${GREEN}✅ Логи очищено${NC}"

echo ""

# 4. Перевірка Constants (URL сокета)
echo "4️⃣ Інформація про підключення:"
echo "   Socket URL: https://worldmates.club:449/"
echo "   HAproxy порт: 449 (TCP mode)"
echo "   Транспорт: WebSocket -> Polling (fallback)"

echo ""

# 5. Тест підключення до сервера
echo "5️⃣ Тест підключення до сервера..."
echo "   Перевірка порту 449..."

# Перевірка через curl
CURL_TEST=$(curl -k -s -o /dev/null -w "%{http_code}" --connect-timeout 5 https://worldmates.club:449/ 2>&1)

if [[ $CURL_TEST == *"000"* ]] || [[ $CURL_TEST == "" ]]; then
    echo -e "${RED}❌ Сервер не відповідає на порту 449${NC}"
    echo "   Можливі причини:"
    echo "   - Node.js сервер не запущений"
    echo "   - HAproxy не проксує правильно"
    echo "   - Фаєрвол блокує з'єднання"
else
    echo -e "${GREEN}✅ Сервер відповідає (HTTP $CURL_TEST)${NC}"
fi

echo ""

# 6. Запуск моніторингу в реальному часі
echo "6️⃣ Запуск моніторингу Socket.IO (натисніть Ctrl+C для зупинки)..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔍 Що шукати:"
echo "   ✅ 'Socket Connected!' - успішне підключення"
echo "   ✅ '📨 private_message' - отримання повідомлення"
echo "   ✅ 'Parsed user X as ONLINE' - парсинг статусів"
echo "   ❌ 'Connection Error' - помилки підключення"
echo "   ❌ 'xhr poll error' - проблеми з транспортом"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Моніторинг з кольоровим виводом
adb logcat -v time | grep --line-buffered -E "SocketManager|MessagesViewModel|Socket\.IO" | while read line; do
    if [[ $line == *"Connected"* ]]; then
        echo -e "${GREEN}$line${NC}"
    elif [[ $line == *"Error"* ]] || [[ $line == *"error"* ]]; then
        echo -e "${RED}$line${NC}"
    elif [[ $line == *"📨"* ]]; then
        echo -e "${GREEN}$line${NC}"
    elif [[ $line == *"Parsed user"* ]]; then
        echo -e "${YELLOW}$line${NC}"
    else
        echo "$line"
    fi
done
