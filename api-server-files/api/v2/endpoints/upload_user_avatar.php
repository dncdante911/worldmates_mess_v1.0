<?php
// +------------------------------------------------------------------------+
// | API Endpoint: Upload User Avatar
// +------------------------------------------------------------------------+
// | Загрузка аватара пользователя
// +------------------------------------------------------------------------+

// Налаштування логування
define('AVATAR_LOG_FILE', '/var/www/www-root/data/www/worldmates.club/api/v2/logs/avatar_upload.log');

// Функція для запису логів У КОНКРЕТНИЙ файл
function log_avatar($message) {
    $timestamp = date('Y-m-d H:i:s');
    $log_message = "[{$timestamp}] {$message}\n";
    error_log($log_message, 3, AVATAR_LOG_FILE);
}

// Створюємо папку logs якщо її немає
$log_dir = dirname(AVATAR_LOG_FILE);
if (!file_exists($log_dir)) {
    mkdir($log_dir, 0755, true);
}

// Initialize response
$error_code = 0;
$error_message = '';
$data = [];

log_avatar("========== NEW AVATAR UPLOAD REQUEST ==========");

// Get access token from POST or GET (Android sends via URL, but WoWonder expects POST)
$access_token = $_POST['access_token'] ?? $_GET['access_token'] ?? '';

if (empty($access_token)) {
    log_avatar("❌ ERROR: access_token is missing");
    $error_code = 3;
    $error_message = 'access_token is missing';
    http_response_code(400);
} else {
    log_avatar("✓ access_token received: " . substr($access_token, 0, 20) . "...");
}

if ($error_code == 0) {
    // Validate access token and get user_id
    $user_id = Wo_UserIdFromAccessToken($access_token);

    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        log_avatar("❌ ERROR: Invalid access_token - user_id=" . var_export($user_id, true));
        $error_code = 4;
        $error_message = 'Invalid access_token';
        http_response_code(401);
    } else {
        log_avatar("✓ Valid user_id: {$user_id}");
        // Load user data and set $wo['user'] (CRITICAL for Wo_UploadImage)
        global $wo;
        $user_data = Wo_UserData($user_id);

        if (empty($user_data)) {
            log_avatar("❌ ERROR: User not found for user_id={$user_id}");
            $error_code = 5;
            $error_message = 'User not found';
            http_response_code(404);
        } else {
            log_avatar("✓ User data loaded: username={$user_data['username']}");
            // Update $wo['user'] with real user data (required by Wo_UploadImage)
            $wo['user'] = $user_data;
            $wo['loggedin'] = true;

            // Check if file is uploaded
            log_avatar("📂 Checking uploaded file...");
            log_avatar("📂 \$_FILES dump: " . print_r($_FILES, true));

            if (empty($_FILES['avatar']['tmp_name'])) {
                log_avatar("❌ ERROR: Avatar file is required - \$_FILES['avatar']['tmp_name'] is empty");
                $error_code = 6;
                $error_message = 'Avatar file is required';
                http_response_code(400);
            } else {
                log_avatar("✓ File uploaded to tmp: {$_FILES['avatar']['tmp_name']}");
                // Log upload attempt
                log_avatar("🖼️ Avatar upload: user_id={$user_id}, file={$_FILES['avatar']['name']}, size={$_FILES['avatar']['size']}");

                // Upload image using WoWonder's function
                // Same as update-user-data.php line 225
                $upload_image = Wo_UploadImage(
                    $_FILES["avatar"]["tmp_name"],
                    $_FILES['avatar']['name'],
                    'avatar',
                    $_FILES['avatar']['type'],
                    $user_id
                );

                log_avatar("🖼️ Upload result: " . ($upload_image === true ? 'SUCCESS' : 'FAILED'));

                if ($upload_image === true) {
                    // Get updated user data
                    $updated_user = Wo_UserData($user_id);

                    if ($updated_user) {
                        // Clear user cache
                        cache($user_id, 'users', 'delete');

                        // Log database update
                        log_avatar("✅ Avatar saved to DB: {$updated_user['avatar']}");

                        // Success response
                        // Return 'url' for compatibility with MediaUploadResponse
                        $data = array(
                            'api_status' => 200,
                            'message' => 'Avatar uploaded successfully',
                            'url' => Wo_GetMedia($updated_user['avatar']), // Full URL for Android
                            'avatar' => $updated_user['avatar'],
                            'avatar_org' => $updated_user['avatar_org']
                        );

                        log_avatar("✅ Response: url={$data['url']}, avatar={$data['avatar']}");
                    } else {
                        log_avatar("❌ ERROR: Failed to retrieve updated user data after upload");
                        $error_code = 8;
                        $error_message = 'Failed to retrieve updated user data';
                        http_response_code(500);
                    }
                } else {
                    $error_code = 7;
                    $error_message = 'Failed to upload image. Please check file type and size.';
                    log_avatar("❌ Wo_UploadImage failed for user_id={$user_id}");
                    log_avatar("❌ File details: name={$_FILES['avatar']['name']}, type={$_FILES['avatar']['type']}, size={$_FILES['avatar']['size']}");
                    log_avatar("❌ wo[loggedin]=" . ($wo['loggedin'] ? 'true' : 'false') . ", wo[user][user_id]={$wo['user']['user_id']}");
                    http_response_code(500);
                }
            }
        }
    }
}

// Send response
if ($error_code > 0) {
    log_avatar("❌ Sending error response: error_code={$error_code}, message={$error_message}");
    http_response_code($error_code >= 100 ? $error_code : 400);
    echo json_encode([
        'api_status' => $error_code >= 100 ? $error_code : 400,
        'error_code' => $error_code,
        'error_message' => $error_message
    ]);
} else if (!empty($data)) {
    log_avatar("✅ SUCCESS - Sending successful response");
    echo json_encode($data);
} else {
    // No data and no error - something went wrong
    log_avatar("❌ CRITICAL ERROR: No data and no error code!");
    http_response_code(500);
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Unknown error occurred'
    ]);
}

log_avatar("========== REQUEST COMPLETED ==========");
?>
