#!/bin/bash

echo "🔍 Перевірка всіх змін..."
echo ""

ERRORS=0

# Перевірка MessagesScreen.kt
echo "📄 Перевірка MessagesScreen.kt..."
if grep -q "var editingMessage by remember" app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt; then
    echo "   ✅ editingMessage state знайдено"
else
    echo "   ❌ editingMessage state НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "fun EditIndicator" app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt; then
    echo "   ✅ EditIndicator composable знайдено"
else
    echo "   ❌ EditIndicator composable НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "onEdit: (Message) -> Unit" app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt; then
    echo "   ✅ onEdit callback в MessageContextMenu знайдено"
else
    echo "   ❌ onEdit callback НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "wrapContentWidth()" app/src/main/java/com/worldmates/messenger/ui/messages/MessagesScreen.kt; then
    echo "   ✅ Адаптивні бульбашки (wrapContentWidth) знайдено"
else
    echo "   ❌ Адаптивні бульбашки НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

echo ""

# Перевірка MessagesViewModel.kt
echo "📄 Перевірка MessagesViewModel.kt..."
if grep -q "fun editMessage" app/src/main/java/com/worldmates/messenger/ui/messages/MessagesViewModel.kt; then
    echo "   ✅ editMessage() функція знайдено"
else
    echo "   ❌ editMessage() функція НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "fun deleteMessage" app/src/main/java/com/worldmates/messenger/ui/messages/MessagesViewModel.kt; then
    echo "   ✅ deleteMessage() функція знайдено"
else
    echo "   ❌ deleteMessage() функція НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

echo ""

# Перевірка WorldMatesApi.kt
echo "📄 Перевірка WorldMatesApi.kt..."
if grep -q 'type=edit_message' app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt; then
    echo "   ✅ editMessage API endpoint знайдено"
else
    echo "   ❌ editMessage API endpoint НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

if grep -q 'type=delete_message' app/src/main/java/com/worldmates/messenger/network/WorldMatesApi.kt; then
    echo "   ✅ deleteMessage API endpoint знайдено"
else
    echo "   ❌ deleteMessage API endpoint НЕ знайдено!"
    ERRORS=$((ERRORS + 1))
fi

echo ""

# Перевірка компонентів
echo "📄 Перевірка компонентів..."
if [ -f "app/src/main/java/com/worldmates/messenger/ui/components/EmojiPicker.kt" ]; then
    echo "   ✅ EmojiPicker.kt існує"
else
    echo "   ❌ EmojiPicker.kt НЕ існує!"
    ERRORS=$((ERRORS + 1))
fi

if [ -f "app/src/main/java/com/worldmates/messenger/ui/components/StickerPicker.kt" ]; then
    echo "   ✅ StickerPicker.kt існує"
else
    echo "   ❌ StickerPicker.kt НЕ існує!"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ]; then
    echo "✅ ВСІ ЗМІНИ НА МІСЦІ! ($ERRORS помилок)"
    echo ""
    echo "📱 Тепер зроби:"
    echo "   1. Build → Rebuild Project в Android Studio"
    echo "   2. Видали старий додаток з телефону"
    echo "   3. Встанови новий APK"
    echo "   4. Перезапусти додаток"
else
    echo "❌ ЗНАЙДЕНО $ERRORS ПОМИЛОК!"
    echo "   Щось пішло не так з файлами."
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
