# 🔐 Серверная часть для TOTP 2FA (Google Authenticator)

## План реализации

### 1️⃣ Обновление БД

Добавить новые поля в таблицу `wo_users`:

```sql
ALTER TABLE `wo_users`
ADD COLUMN `totp_secret` VARCHAR(32) DEFAULT NULL COMMENT 'TOTP секретный ключ (Base32)',
ADD COLUMN `totp_enabled` TINYINT(1) DEFAULT 0 COMMENT 'TOTP включен',
ADD COLUMN `recovery_codes` TEXT DEFAULT NULL COMMENT 'Резервные коды (JSON)',
ADD COLUMN `recovery_codes_used` TEXT DEFAULT NULL COMMENT 'Использованные коды (JSON)';
```

### 2️⃣ PHP класс для TOTP

Создать файл `/api/v2/classes/TOTP.php`:

```php
<?php
class TOTP {
    private $secret;
    private $timeStep = 30;
    private $digits = 6;

    public function __construct($secret = null) {
        $this->secret = $secret ?: $this->generateSecret();
    }

    // Генерация секретного ключа (Base32)
    public function generateSecret($length = 20) {
        $chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
        $secret = '';
        for ($i = 0; $i < $length; $i++) {
            $secret .= $chars[random_int(0, strlen($chars) - 1)];
        }
        return $secret;
    }

    // Генерация TOTP кода
    public function generateCode($timestamp = null) {
        if ($timestamp === null) {
            $timestamp = time();
        }

        $timeCounter = floor($timestamp / $this->timeStep);

        // Декодируем Base32 в бинарные данные
        $binarySecret = $this->base32Decode($this->secret);

        // Создаем time counter в binary
        $timeBytes = pack('N*', 0) . pack('N*', $timeCounter);

        // HMAC-SHA1
        $hash = hash_hmac('sha1', $timeBytes, $binarySecret, true);

        // Dynamic truncation
        $offset = ord($hash[strlen($hash) - 1]) & 0x0F;
        $code = (
            ((ord($hash[$offset + 0]) & 0x7F) << 24) |
            ((ord($hash[$offset + 1]) & 0xFF) << 16) |
            ((ord($hash[$offset + 2]) & 0xFF) << 8) |
            (ord($hash[$offset + 3]) & 0xFF)
        ) % pow(10, $this->digits);

        return str_pad($code, $this->digits, '0', STR_PAD_LEFT);
    }

    // Верификация TOTP кода
    public function verifyCode($code, $window = 1) {
        $currentTime = time();

        for ($i = -$window; $i <= $window; $i++) {
            $time = $currentTime + ($i * $this->timeStep);
            if ($this->generateCode($time) === $code) {
                return true;
            }
        }

        return false;
    }

    // Генерация QR-кода URI для Google Authenticator
    public function getQRCodeURI($account, $issuer = 'WorldMates') {
        $params = http_build_query([
            'secret' => $this->secret,
            'issuer' => $issuer,
            'digits' => $this->digits,
            'period' => $this->timeStep
        ]);

        return "otpauth://totp/{$issuer}:{$account}?{$params}";
    }

    // Генерация резервных кодов
    public function generateRecoveryCodes($count = 10) {
        $codes = [];
        for ($i = 0; $i < $count; $i++) {
            $code = str_pad(random_int(0, 99999999), 8, '0', STR_PAD_LEFT);
            $codes[] = substr($code, 0, 4) . '-' . substr($code, 4, 4);
        }
        return $codes;
    }

    // Base32 decode
    private function base32Decode($secret) {
        $base32chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
        $base32charsFlipped = array_flip(str_split($base32chars));

        $paddingCharCount = substr_count($secret, '=');
        $allowedValues = [6, 4, 3, 1, 0];

        if (!in_array($paddingCharCount, $allowedValues)) {
            return false;
        }

        for ($i = 0; $i < 4; $i++) {
            if ($paddingCharCount == $allowedValues[$i] &&
                substr($secret, -($allowedValues[$i])) != str_repeat('=', $allowedValues[$i])) {
                return false;
            }
        }

        $secret = str_replace('=', '', $secret);
        $secret = str_split($secret);
        $binaryString = '';

        for ($i = 0; $i < count($secret); $i = $i + 8) {
            $x = '';
            if (!in_array($secret[$i], $base32charsFlipped)) {
                return false;
            }
            for ($j = 0; $j < 8; $j++) {
                $x .= str_pad(base_convert(@$base32charsFlipped[@$secret[$i + $j]], 10, 2), 5, '0', STR_PAD_LEFT);
            }
            $eightBits = str_split($x, 8);
            for ($z = 0; $z < count($eightBits); $z++) {
                $binaryString .= (($y = chr(base_convert($eightBits[$z], 2, 10))) || ord($y) == 48) ? $y : '';
            }
        }

        return $binaryString;
    }

    public function getSecret() {
        return $this->secret;
    }
}
```

### 3️⃣ API Endpoints

#### A. Включение TOTP 2FA

`/api/v2/endpoints/enable-totp.php`:

```php
<?php
// Проверка авторизации
$response_data = ['api_status' => 400];

if (empty($wo['user']['user_id'])) {
    $response_data['errors'] = 'Not logged in';
    echo json_encode($response_data);
    exit;
}

require_once('classes/TOTP.php');

// Генерируем секретный ключ
$totp = new TOTP();
$secret = $totp->getSecret();

// Генерируем резервные коды
$recovery_codes = $totp->generateRecoveryCodes(10);

// Сохраняем в БД (но пока не активируем)
$user_id = $wo['user']['user_id'];
$update = $db->where('user_id', $user_id)->update(T_USERS, [
    'totp_secret' => $secret,
    'totp_enabled' => 0,  // Пока не активирован
    'recovery_codes' => json_encode($recovery_codes),
    'recovery_codes_used' => json_encode([])
]);

if ($update) {
    // Генерируем QR-код URI
    $qr_uri = $totp->getQRCodeURI($wo['user']['email'], 'WorldMates');

    $response_data = [
        'api_status' => 200,
        'secret' => $secret,
        'qr_uri' => $qr_uri,
        'recovery_codes' => $recovery_codes
    ];
} else {
    $response_data['errors'] = 'Failed to generate secret';
}

echo json_encode($response_data);
```

#### B. Верификация и активация TOTP

`/api/v2/endpoints/verify-totp.php`:

```php
<?php
$response_data = ['api_status' => 400];

if (empty($wo['user']['user_id']) || empty($_POST['code'])) {
    $response_data['errors'] = 'Missing parameters';
    echo json_encode($response_data);
    exit;
}

require_once('classes/TOTP.php');

$user_id = $wo['user']['user_id'];
$code = Wo_Secure($_POST['code']);

// Получаем секретный ключ
$user_data = Wo_UserData($user_id);

if (empty($user_data['totp_secret'])) {
    $response_data['errors'] = 'TOTP not initialized';
    echo json_encode($response_data);
    exit;
}

// Проверяем код
$totp = new TOTP($user_data['totp_secret']);

if ($totp->verifyCode($code)) {
    // Активируем TOTP
    $update = $db->where('user_id', $user_id)->update(T_USERS, [
        'totp_enabled' => 1,
        'two_factor' => 1,
        'two_factor_method' => 'google_authenticator',
        'two_factor_verified' => 1
    ]);

    cache($user_id, 'users', 'delete');

    $response_data = [
        'api_status' => 200,
        'message' => 'TOTP enabled successfully'
    ];
} else {
    $response_data['errors'] = 'Invalid code';
}

echo json_encode($response_data);
```

#### C. Вход с TOTP

Обновить `/api/v2/endpoints/auth.php`:

```php
// После строки 47, добавить проверку TOTP
if (Wo_TwoFactor($_POST['username']) != false) {
    // ... существующий код
} else {
    // Проверяем тип 2FA
    $user_data = Wo_UserData($user_id);

    if ($user_data['two_factor_method'] == 'google_authenticator' && $user_data['totp_enabled'] == 1) {
        // Требуется TOTP код
        $response_data = [
            'api_status' => 202,  // Partial success
            'message' => 'TOTP code required',
            'user_id' => $user_id,
            'requires_totp' => true
        ];
    } else {
        // Email/SMS 2FA (существующая логика)
        $response_data = [
            'api_status' => 200,
            'message' => 'Please enter your confirmation code',
            'user_id' => $user_id,
            'requires_totp' => false
        ];
    }
}
```

#### D. Подтверждение TOTP при входе

`/api/v2/endpoints/confirm-totp-login.php`:

```php
<?php
$response_data = ['api_status' => 400];

if (empty($_POST['user_id']) || empty($_POST['totp_code'])) {
    $response_data['errors'] = 'Missing parameters';
    echo json_encode($response_data);
    exit;
}

require_once('classes/TOTP.php');

$user_id = Wo_Secure($_POST['user_id']);
$code = Wo_Secure($_POST['totp_code']);

$user_data = Wo_UserData($user_id);

if ($user_data['totp_enabled'] != 1) {
    $response_data['errors'] = 'TOTP not enabled';
    echo json_encode($response_data);
    exit;
}

// Проверяем TOTP код
$totp = new TOTP($user_data['totp_secret']);

if ($totp->verifyCode($code)) {
    // Создаем сессию (копируем логику из auth.php)
    $time = time();
    $access_token = sha1(rand(111111111, 999999999)) . md5(microtime()) . rand(11111111, 99999999);
    $device_type = !empty($_POST['device_type']) ? Wo_Secure($_POST['device_type']) : 'phone';

    $create_session = mysqli_query($sqlConnect,
        "INSERT INTO " . T_APP_SESSIONS . "
        (`user_id`, `session_id`, `platform`, `time`)
        VALUES ('{$user_id}', '{$access_token}', '{$device_type}', '{$time}')"
    );

    if ($create_session) {
        cache($user_id, 'users', 'delete');
        $response_data = [
            'api_status' => 200,
            'access_token' => $access_token,
            'user_id' => $user_id
        ];
    }
} else {
    $response_data['errors'] = 'Invalid TOTP code';
}

echo json_encode($response_data);
```

#### E. Использование резервного кода

`/api/v2/endpoints/use-recovery-code.php`:

```php
<?php
$response_data = ['api_status' => 400];

if (empty($_POST['user_id']) || empty($_POST['recovery_code'])) {
    $response_data['errors'] = 'Missing parameters';
    echo json_encode($response_data);
    exit;
}

$user_id = Wo_Secure($_POST['user_id']);
$code = Wo_Secure($_POST['recovery_code']);

$user_data = Wo_UserData($user_id);

$recovery_codes = json_decode($user_data['recovery_codes'], true) ?: [];
$used_codes = json_decode($user_data['recovery_codes_used'], true) ?: [];

// Проверяем код
if (in_array($code, $recovery_codes) && !in_array($code, $used_codes)) {
    // Отмечаем код как использованный
    $used_codes[] = $code;

    $update = $db->where('user_id', $user_id)->update(T_USERS, [
        'recovery_codes_used' => json_encode($used_codes)
    ]);

    // Создаем сессию (та же логика)
    $time = time();
    $access_token = sha1(rand(111111111, 999999999)) . md5(microtime()) . rand(11111111, 99999999);
    $device_type = !empty($_POST['device_type']) ? Wo_Secure($_POST['device_type']) : 'phone';

    $create_session = mysqli_query($sqlConnect,
        "INSERT INTO " . T_APP_SESSIONS . "
        (`user_id`, `session_id`, `platform`, `time`)
        VALUES ('{$user_id}', '{$access_token}', '{$device_type}', '{$time}')"
    );

    if ($create_session) {
        cache($user_id, 'users', 'delete');

        $remaining = count($recovery_codes) - count($used_codes);

        $response_data = [
            'api_status' => 200,
            'access_token' => $access_token,
            'user_id' => $user_id,
            'remaining_codes' => $remaining
        ];
    }
} else {
    $response_data['errors'] = 'Invalid or already used recovery code';
}

echo json_encode($response_data);
```

#### F. Отключение TOTP

`/api/v2/endpoints/disable-totp.php`:

```php
<?php
$response_data = ['api_status' => 400];

if (empty($wo['user']['user_id']) || empty($_POST['password'])) {
    $response_data['errors'] = 'Authentication required';
    echo json_encode($response_data);
    exit;
}

$user_id = $wo['user']['user_id'];
$password = Wo_Secure($_POST['password']);

// Проверяем пароль
$user_data = Wo_UserData($user_id);
if (!password_verify($password, $user_data['password'])) {
    $response_data['errors'] = 'Invalid password';
    echo json_encode($response_data);
    exit;
}

// Отключаем TOTP
$update = $db->where('user_id', $user_id)->update(T_USERS, [
    'totp_enabled' => 0,
    'totp_secret' => NULL,
    'recovery_codes' => NULL,
    'recovery_codes_used' => NULL,
    'two_factor' => 0,
    'two_factor_method' => 'two_factor'
]);

if ($update) {
    cache($user_id, 'users', 'delete');

    $response_data = [
        'api_status' => 200,
        'message' => 'TOTP disabled successfully'
    ];
} else {
    $response_data['errors'] = 'Failed to disable TOTP';
}

echo json_encode($response_data);
```

---

## 📋 Итого: API Endpoints

1. `POST /api/v2/endpoints/enable-totp.php` - Генерация секрета и QR-кода
2. `POST /api/v2/endpoints/verify-totp.php` - Верификация и активация TOTP
3. `POST /api/v2/endpoints/confirm-totp-login.php` - Вход с TOTP кодом
4. `POST /api/v2/endpoints/use-recovery-code.php` - Использование резервного кода
5. `POST /api/v2/endpoints/disable-totp.php` - Отключение TOTP
6. Обновить `POST /api/v2/endpoints/auth.php` - Добавить проверку TOTP

---

## 🔄 Интеграция с Android приложением

### Обновить WorldMatesApi.kt:

```kotlin
interface WorldMatesApi {
    // ... существующие endpoints

    // TOTP 2FA
    @POST("/api/v2/endpoints/enable-totp.php")
    suspend fun enableTOTP(
        @Query("access_token") accessToken: String
    ): TOTPSetupResponse

    @FormUrlEncoded
    @POST("/api/v2/endpoints/verify-totp.php")
    suspend fun verifyTOTP(
        @Query("access_token") accessToken: String,
        @Field("code") code: String
    ): BaseResponse

    @FormUrlEncoded
    @POST("/api/v2/endpoints/confirm-totp-login.php")
    suspend fun confirmTOTPLogin(
        @Field("user_id") userId: Long,
        @Field("totp_code") totpCode: String,
        @Field("device_type") deviceType: String = "phone"
    ): AuthResponse

    @FormUrlEncoded
    @POST("/api/v2/endpoints/use-recovery-code.php")
    suspend fun useRecoveryCode(
        @Field("user_id") userId: Long,
        @Field("recovery_code") recoveryCode: String,
        @Field("device_type") deviceType: String = "phone"
    ): AuthResponse

    @FormUrlEncoded
    @POST("/api/v2/endpoints/disable-totp.php")
    suspend fun disableTOTP(
        @Query("access_token") accessToken: String,
        @Field("password") password: String
    ): BaseResponse
}

data class TOTPSetupResponse(
    @SerializedName("api_status") val apiStatus: Int,
    @SerializedName("secret") val secret: String?,
    @SerializedName("qr_uri") val qrUri: String?,
    @SerializedName("recovery_codes") val recoveryCodes: List<String>?,
    @SerializedName("errors") val errors: String?
)
```

---

## ✅ Преимущества серверной реализации

1. ✅ **Безопасность** - секретный ключ хранится на сервере
2. ✅ **Синхронизация** - работает на всех устройствах
3. ✅ **Восстановление** - резервные коды на сервере
4. ✅ **Централизованное управление** - админ может отключить 2FA
5. ✅ **Логирование** - можно отслеживать попытки входа
6. ✅ **Валидация** - сервер контролирует доступ

---

## 📝 Следующие шаги

1. Создать файлы на сервере
2. Обновить БД структуру
3. Интегрировать в Android приложение
4. Тестирование
5. Деплой на продакшн

**Автор:** Claude Code
**Дата:** 2025-12-27
**Статус:** План готов к реализации
