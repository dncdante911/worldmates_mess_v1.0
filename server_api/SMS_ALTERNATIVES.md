# 📱 АЛЬТЕРНАТИВЫ TWILIO/INFOBIP - SMS БЕЗ ДОРОГИХ СЕРВИСОВ

## 🎯 Обзор

Да, можно обойтись без Twilio/Infobip! Есть несколько способов отправки SMS:

---

## 💰 ДЕШЕВЫЕ/БЕСПЛАТНЫЕ АЛЬТЕРНАТИВЫ

### 1. **Прямая интеграция с операторами связи** (самое дешевое!)

Можно интегрироваться напрямую с украинскими/европейскими операторами:

#### **Украинские провайдеры:**

**TurboSMS** (Украина) 🇺🇦
- Цена: ~0.50-1.50 грн за SMS ($0.01-$0.04)
- API: REST API
- Сайт: https://turbosms.ua/
- Плюсы: Очень дешево, украинский, поддержка Viber
- Минусы: Только Украина

```php
// Пример интеграции TurboSMS
function sendSMS_TurboSMS($phone, $message) {
    $api_key = "YOUR_API_KEY";
    $sender = "YourApp";

    $data = json_encode([
        'recipients' => [$phone],
        'sms' => [
            'sender' => $sender,
            'text' => $message
        ]
    ]);

    $ch = curl_init('https://api.turbosms.ua/message/send.json');
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $api_key,
        'Content-Type: application/json'
    ]);

    $result = curl_exec($ch);
    curl_close($ch);

    return json_decode($result, true);
}
```

**SMS Fly** (Украина) 🇺🇦
- Цена: ~0.40-1.20 грн за SMS
- API: REST API
- Сайт: https://sms-fly.ua/
- Плюсы: Дешевле TurboSMS, массовые рассылки
- Минусы: Только Украина

**SMSC.ua** (Украина)
- Цена: от 0.35 грн за SMS
- API: HTTP/HTTPS, SMPP
- Сайт: https://smsc.ua/
- Плюсы: Самый дешевый, гибкий API
- Минусы: Интерфейс не очень современный

---

#### **Европейские провайдеры:**

**MessageBird** (Нидерланды)
- Цена: от €0.015 за SMS (~$0.016)
- API: REST API
- Сайт: https://messagebird.com/
- Плюсы: Международная доставка, хорошее API
- Минусы: Дороже украинских

**Vonage (бывший Nexmo)**
- Цена: от $0.01 за SMS
- API: REST API
- Сайт: https://www.vonage.com/
- Плюсы: Стабильный, международный
- Минусы: Требует верификацию

**SMS.to**
- Цена: от €0.01 за SMS
- API: Simple REST API
- Сайт: https://sms.to/
- Плюсы: Очень простой API, нет абонплаты
- Минусы: Ограниченный функционал

---

### 2. **Собственный SMS Gateway через Android** (БЕСПЛАТНО!)

Можно развернуть свой SMS шлюз используя Android телефон:

**SMS Gateway for Android** (Open Source)
- GitHub: https://github.com/capcom6/android-sms-gateway
- Цена: БЕСПЛАТНО (только стоимость SMS по тарифу)
- Плюсы: Полный контроль, безлимит на отправку
- Минусы: Нужен Android телефон 24/7

```php
// Пример интеграции с SMS Gateway for Android
function sendSMS_AndroidGateway($phone, $message) {
    $gateway_url = "http://your-server:3000/message";
    $api_key = "YOUR_API_KEY";

    $data = json_encode([
        'phone' => $phone,
        'message' => $message
    ]);

    $ch = curl_init($gateway_url);
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $api_key,
        'Content-Type: application/json'
    ]);

    $result = curl_exec($ch);
    curl_close($ch);

    return json_decode($result, true);
}
```

**Как настроить:**
1. Купить дешевый Android телефон (~$50-100)
2. Установить безлимитный тариф на SMS
3. Установить SMS Gateway приложение
4. Подключить к вашему серверу через API
5. Готово! SMS почти бесплатные

---

### 3. **GSM Modem** (для больших объемов)

Если нужно отправлять много SMS:

**Huawei E3372** или **ZTE MF823**
- Цена: ~$30-50 за модем
- Поддержка: Linux/Windows
- Плюсы: Безлимит, полный контроль
- Минусы: Нужна настройка сервера

```bash
# Установка gammu для работы с GSM модемом
apt-get install gammu gammu-smsd

# Конфигурация /etc/gammu-smsdrc
[gammu]
device = /dev/ttyUSB0
connection = at

[smsd]
service = files
logfile = syslog

# Отправка SMS через PHP
<?php
exec("echo 'Test message' | gammu --sendsms TEXT +380930000000");
?>
```

---

## 🛠️ УНИВЕРСАЛЬНЫЙ PHP КОД

Создайте файл `functions_sms.php` с поддержкой всех провайдеров:

```php
<?php
/**
 * Универсальная функция отправки SMS
 * Поддерживает: Twilio, Infobip, TurboSMS, SMS Fly, Android Gateway
 */
function Wo_SendSMS($phone, $message) {
    global $wo, $sqlConnect;

    if (empty($phone)) {
        return false;
    }

    $provider = $wo["config"]["sms_provider"]; // twilio, infobip, turbosms, smsfly, android

    switch ($provider) {
        case 'turbosms':
            return sendSMS_TurboSMS($phone, $message);

        case 'smsfly':
            return sendSMS_SMSFly($phone, $message);

        case 'smsc':
            return sendSMS_SMSC($phone, $message);

        case 'android':
            return sendSMS_AndroidGateway($phone, $message);

        case 'twilio':
            return sendSMS_Twilio($phone, $message);

        case 'infobip':
            return sendSMS_Infobip($phone, $message);

        default:
            // Mock режим для тестирования
            if ($wo["config"]["sms_mock_mode"] == 1) {
                // Сохраняем код в лог для тестирования
                file_put_contents('sms_mock.log', date('Y-m-d H:i:s') . " | $phone | $message\n", FILE_APPEND);
                return true;
            }
            return false;
    }
}

// TurboSMS
function sendSMS_TurboSMS($phone, $message) {
    global $wo;

    $api_key = $wo["config"]["turbosms_api_key"];
    $sender = $wo["config"]["turbosms_sender"];

    $data = json_encode([
        'recipients' => [$phone],
        'sms' => [
            'sender' => $sender,
            'text' => $message
        ]
    ]);

    $ch = curl_init('https://api.turbosms.ua/message/send.json');
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $api_key,
        'Content-Type: application/json'
    ]);

    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode == 200) {
        $response = json_decode($result, true);
        return isset($response['response_code']) && $response['response_code'] == 0;
    }

    return false;
}

// SMS Fly
function sendSMS_SMSFly($phone, $message) {
    global $wo;

    $login = $wo["config"]["smsfly_login"];
    $password = $wo["config"]["smsfly_password"];

    $auth = base64_encode("$login:$password");

    $xml = '<?xml version="1.0" encoding="utf-8"?>
    <request>
        <operation>SENDSMS</operation>
        <message start_time="AUTO" end_time="AUTO" lifetime="4">
            <recipient>' . htmlspecialchars($phone) . '</recipient>
            <body>' . htmlspecialchars($message) . '</body>
        </message>
    </request>';

    $ch = curl_init('https://sms-fly.com/api/api.php');
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $xml);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Basic ' . $auth,
        'Content-Type: text/xml'
    ]);

    $result = curl_exec($ch);
    curl_close($ch);

    return strpos($result, '<state>ACCEPT</state>') !== false;
}

// SMSC.ua
function sendSMS_SMSC($phone, $message) {
    global $wo;

    $login = $wo["config"]["smsc_login"];
    $password = $wo["config"]["smsc_password"];
    $sender = $wo["config"]["smsc_sender"];

    $url = "https://smsc.ua/sys/send.php?" . http_build_query([
        'login' => $login,
        'psw' => $password,
        'phones' => $phone,
        'mes' => $message,
        'sender' => $sender,
        'fmt' => 3 // JSON response
    ]);

    $result = file_get_contents($url);
    $response = json_decode($result, true);

    return isset($response['id']) && $response['id'] > 0;
}

// Android SMS Gateway
function sendSMS_AndroidGateway($phone, $message) {
    global $wo;

    $gateway_url = $wo["config"]["android_gateway_url"];
    $api_key = $wo["config"]["android_gateway_key"];

    $data = json_encode([
        'phone' => $phone,
        'message' => $message
    ]);

    $ch = curl_init($gateway_url . '/message');
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $api_key,
        'Content-Type: application/json'
    ]);

    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    return $httpCode == 200;
}

// Existing Twilio function (уже есть в проекте)
function sendSMS_Twilio($phone, $message) {
    global $wo;
    // ... existing Twilio code
}

// Existing Infobip function (уже есть в проекте)
function sendSMS_Infobip($phone, $message) {
    global $wo;
    // ... existing Infobip code
}
?>
```

---

## 🎮 MOCK РЕЖИМ ДЛЯ ТЕСТИРОВАНИЯ

Для разработки без реальной отправки SMS:

```php
// В конфиге админ-панели добавить:
$wo['config']['sms_mock_mode'] = 1; // 1 = включен, 0 = выключен

// Коды будут сохраняться в файл sms_mock.log:
// 2025-12-17 15:30:45 | +380930000000 | Your code is: 123456
```

---

## 💵 СРАВНЕНИЕ ЦЕН (на 1000 SMS)

| Провайдер | Цена за 1000 SMS | Страны | Особенности |
|-----------|------------------|--------|-------------|
| **TurboSMS** | $10-40 | Украина | Viber + SMS |
| **SMS Fly** | $8-30 | Украина | Массовые рассылки |
| **SMSC.ua** | $7-25 | Украина + СНГ | SMPP поддержка |
| **Android Gateway** | $0-5* | Любые | *только тариф оператора |
| **GSM Modem** | $0-5* | Любые | *только тариф оператора |
| **MessageBird** | $15-50 | Весь мир | Международный |
| **Twilio** | $75+ | Весь мир | Дорого |
| **Infobip** | $50+ | Весь мир | Дорого |

---

## 📋 РЕКОМЕНДАЦИИ

### Для стартапов/малого бизнеса:
✅ **TurboSMS** или **SMS Fly** - дешево и надежно для Украины

### Для личных проектов/тестирования:
✅ **Android SMS Gateway** - почти бесплатно!

### Для среднего бизнеса:
✅ **SMSC.ua** + **GSM Modem** - масштабируемо и дешево

### Для международных проектов:
✅ **MessageBird** - баланс цены и качества

### Для enterprise:
✅ **Twilio** / **Infobip** - дорого, но очень надежно

---

## 🔧 КАК ИНТЕГРИРОВАТЬ

### Шаг 1: Выберите провайдера
Зарегистрируйтесь на одном из сервисов выше

### Шаг 2: Добавьте настройки в админ-панель
```sql
INSERT INTO `wo_config` (`name`, `value`) VALUES
('sms_provider', 'turbosms'),
('turbosms_api_key', 'your_api_key_here'),
('turbosms_sender', 'YourApp');
```

### Шаг 3: Обновите `functions_two.php`
Замените функцию `Wo_SendSMSMessage` на универсальную `Wo_SendSMS` из примера выше

### Шаг 4: Протестируйте
```php
// Тест отправки
$result = Wo_SendSMS('+380930000000', 'Test message');
if ($result) {
    echo "SMS sent!";
} else {
    echo "SMS failed!";
}
```

---

## 🎯 ВЫВОДЫ

**ДА, можно обойтись без Twilio/Infobip!**

- 💰 **Дешевле в 5-10 раз** - украинские провайдеры
- 🆓 **Почти бесплатно** - Android Gateway или GSM Modem
- ✅ **Просто интегрировать** - все через REST API
- 🚀 **Масштабируемо** - от 100 до миллионов SMS

---

**Рекомендация для WorldMates:**
Используйте **TurboSMS** (Украина) + **Mock режим** (для разработки).
Цена: ~$10-15 за 1000 SMS вместо $75+ у Twilio!

---

**Файлы для интеграции:**
- `server_api/functions_sms.php` - Универсальная функция SMS
- `server_api/sms_providers_comparison.md` - Детальное сравнение

**Документация провайдеров:**
- TurboSMS: https://turbosms.ua/api.html
- SMS Fly: https://sms-fly.ua/api/
- SMSC: https://smsc.ua/api/
- Android Gateway: https://github.com/capcom6/android-sms-gateway

---

✅ **Готово! Теперь вы знаете все альтернативы Twilio!** 🚀
