<?php
// +------------------------------------------------------------------------+
// | 📡 CHANNELS: Завантаження аватара каналу (DEBUG VERSION)
// +------------------------------------------------------------------------+

// Налаштування логування
define('CHANNEL_AVATAR_LOG', '/var/www/www-root/data/www/worldmates.club/api/v2/logs/channel_avatar_debug.log');

function log_channel_avatar($message) {
    $timestamp = date('Y-m-d H:i:s');
    $log_message = "[{$timestamp}] {$message}\n";
    @error_log($log_message, 3, CHANNEL_AVATAR_LOG);
}

// Створюємо папку logs якщо її немає
$log_dir = dirname(CHANNEL_AVATAR_LOG);
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0755, true);
}

log_channel_avatar("========== CHANNEL AVATAR UPLOAD DEBUG ==========");
log_channel_avatar("REQUEST_METHOD: " . ($_SERVER['REQUEST_METHOD'] ?? 'unknown'));
log_channel_avatar("REQUEST_URI: " . ($_SERVER['REQUEST_URI'] ?? 'unknown'));
log_channel_avatar("CONTENT_TYPE: " . ($_SERVER['CONTENT_TYPE'] ?? 'unknown'));

// Проверка переменных окружения
log_channel_avatar("Check \$sqlConnect: " . (isset($sqlConnect) ? 'EXISTS' : 'NOT EXISTS'));
log_channel_avatar("Check \$wo: " . (isset($wo) ? 'EXISTS' : 'NOT EXISTS'));
log_channel_avatar("Check \$wo['user']: " . (isset($wo['user']) ? 'EXISTS' : 'NOT EXISTS'));
log_channel_avatar("Check \$wo['loggedin']: " . (isset($wo['loggedin']) ? ($wo['loggedin'] ? 'TRUE' : 'FALSE') : 'NOT SET'));

if (isset($wo['user'])) {
    log_channel_avatar("User ID from \$wo: " . ($wo['user']['user_id'] ?? 'NOT SET'));
    log_channel_avatar("User username: " . ($wo['user']['username'] ?? 'NOT SET'));
}

// Check constants
log_channel_avatar("T_CHANNELS defined: " . (defined('T_CHANNELS') ? T_CHANNELS : 'NOT DEFINED'));
log_channel_avatar("T_CHANNEL_ADMINS defined: " . (defined('T_CHANNEL_ADMINS') ? T_CHANNEL_ADMINS : 'NOT DEFINED'));

// Dump $_GET
log_channel_avatar("\$_GET keys: " . implode(', ', array_keys($_GET)));
log_channel_avatar("access_token in GET: " . (isset($_GET['access_token']) ? 'YES (length=' . strlen($_GET['access_token']) . ')' : 'NO'));

// Dump $_POST
log_channel_avatar("\$_POST keys: " . implode(', ', array_keys($_POST)));
log_channel_avatar("channel_id in POST: " . ($_POST['channel_id'] ?? 'NOT SET'));

// Dump $_FILES
log_channel_avatar("\$_FILES keys: " . implode(', ', array_keys($_FILES)));
if (isset($_FILES['avatar'])) {
    log_channel_avatar("avatar file name: " . $_FILES['avatar']['name']);
    log_channel_avatar("avatar file size: " . $_FILES['avatar']['size']);
    log_channel_avatar("avatar file error: " . $_FILES['avatar']['error']);
    log_channel_avatar("avatar file tmp_name: " . $_FILES['avatar']['tmp_name']);
}

// Initialize response
$error_code = 0;
$error_message = '';
$data = [];

log_channel_avatar("Initialized response variables");

try {
    // Access token is already validated by api-v2.php router
    // We can get user_id from the global $wo['user']['user_id']
    $user_id = $wo['user']['user_id'] ?? 0;

    log_channel_avatar("Extracted user_id: {$user_id}");

    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(400);
        log_channel_avatar("ERROR: Invalid user_id");
    }

    if ($error_code == 0) {
        log_channel_avatar("User validation passed, processing upload...");

        $channel_id = (!empty($_POST['channel_id']) && is_numeric($_POST['channel_id'])) ? (int)$_POST['channel_id'] : 0;

        log_channel_avatar("Channel ID: {$channel_id}");

        if ($channel_id < 1) {
            $error_code    = 5;
            $error_message = 'channel_id is required';
            http_response_code(400);
            log_channel_avatar("ERROR: channel_id is missing or invalid");
        } else if (empty($_FILES['avatar'])) {
            $error_code    = 6;
            $error_message = 'avatar file is missing';
            http_response_code(400);
            log_channel_avatar("ERROR: avatar file is missing");
        } else {
            log_channel_avatar("Checking if channel exists...");

            // Перевірити чи існує канал і чи є користувач власником/адміном
            $table_name = defined('T_CHANNELS') ? T_CHANNELS : 'Wo_Channels';
            log_channel_avatar("Using table: {$table_name}");

            $channel_query = mysqli_query($sqlConnect, "
                SELECT c.id, c.user_id
                FROM {$table_name} c
                WHERE c.id = {$channel_id}
            ");

            log_channel_avatar("Query executed: " . ($channel_query ? 'SUCCESS' : 'FAILED'));
            if (!$channel_query) {
                log_channel_avatar("Query error: " . mysqli_error($sqlConnect));
            }

            if (mysqli_num_rows($channel_query) == 0) {
                $error_code    = 7;
                $error_message = 'Channel not found';
                http_response_code(404);
                log_channel_avatar("ERROR: Channel not found");
            } else {
                $channel_data = mysqli_fetch_assoc($channel_query);
                log_channel_avatar("Channel found, owner: " . $channel_data['user_id']);

                // Перевірити чи користувач є власником або адміном
                $is_admin = ($channel_data['user_id'] == $user_id);
                log_channel_avatar("Is owner: " . ($is_admin ? 'YES' : 'NO'));

                if (!$is_admin) {
                    $admin_table = defined('T_CHANNEL_ADMINS') ? T_CHANNEL_ADMINS : 'Wo_Channel_Admins';
                    log_channel_avatar("Checking admin table: {$admin_table}");

                    $admin_query = mysqli_query($sqlConnect, "
                        SELECT COUNT(*) as count
                        FROM {$admin_table}
                        WHERE channel_id = {$channel_id}
                        AND user_id = {$user_id}
                    ");

                    if ($admin_query) {
                        $admin_data = mysqli_fetch_assoc($admin_query);
                        $is_admin = ($admin_data['count'] > 0);
                        log_channel_avatar("Is admin: " . ($is_admin ? 'YES' : 'NO'));
                    } else {
                        log_channel_avatar("Admin query error: " . mysqli_error($sqlConnect));
                    }
                }

                if (!$is_admin) {
                    $error_code    = 8;
                    $error_message = 'Only channel admins can upload avatar';
                    http_response_code(403);
                    log_channel_avatar("ERROR: User is not admin");
                } else {
                    log_channel_avatar("Checking file size...");

                    // Перевірка розміру файлу (макс 5MB)
                    $max_file_size = 5 * 1024 * 1024; // 5MB в байтах
                    if ($_FILES['avatar']['size'] > $max_file_size) {
                        $error_code    = 9;
                        $error_message = 'File size exceeds maximum allowed (5MB)';
                        http_response_code(400);
                        log_channel_avatar("ERROR: File too large");
                    } else {
                        log_channel_avatar("Uploading file...");

                        // Завантажуємо файл
                        $file_info = array(
                            'file' => $_FILES['avatar']['tmp_name'],
                            'name' => $_FILES['avatar']['name'],
                            'size' => $_FILES['avatar']['size'],
                            'type' => $_FILES['avatar']['type'],
                            'types' => 'jpg,png,jpeg,gif'
                        );

                        log_channel_avatar("Calling Wo_ShareFile...");
                        log_channel_avatar("Function exists: " . (function_exists('Wo_ShareFile') ? 'YES' : 'NO'));

                        $media = Wo_ShareFile($file_info);

                        log_channel_avatar("Wo_ShareFile result: " . (empty($media) ? 'EMPTY' : 'SUCCESS'));
                        if (!empty($media)) {
                            log_channel_avatar("Media filename: " . ($media['filename'] ?? 'NOT SET'));
                        }

                        if (!empty($media) && !empty($media['filename'])) {
                            $avatar_url = $media['filename'];

                            log_channel_avatar("Updating database with avatar: {$avatar_url}");

                            // Оновлюємо аватар в БД
                            $update_query = mysqli_query($sqlConnect, "
                                UPDATE {$table_name}
                                SET avatar = '{$avatar_url}'
                                WHERE id = {$channel_id}
                            ");

                            log_channel_avatar("Update query: " . ($update_query ? 'SUCCESS' : 'FAILED'));
                            if (!$update_query) {
                                log_channel_avatar("Update error: " . mysqli_error($sqlConnect));
                            }

                            if ($update_query) {
                                $data = array(
                                    'api_status' => 200,
                                    'message' => 'Avatar uploaded successfully',
                                    'avatar_url' => $avatar_url
                                );

                                log_channel_avatar("✅ SUCCESS! Avatar uploaded");
                            } else {
                                $error_code    = 10;
                                $error_message = 'Failed to update avatar: ' . mysqli_error($sqlConnect);
                                http_response_code(500);
                                log_channel_avatar("ERROR: Database update failed");
                            }
                        } else {
                            $error_code    = 11;
                            $error_message = 'Failed to upload file';
                            http_response_code(500);
                            log_channel_avatar("ERROR: File upload failed");
                        }
                    }
                }
            }
        }
    }
} catch (Exception $e) {
    log_channel_avatar("EXCEPTION: " . $e->getMessage());
    log_channel_avatar("Stack trace: " . $e->getTraceAsString());
    $error_code = 999;
    $error_message = 'Internal server error: ' . $e->getMessage();
    http_response_code(500);
}

log_channel_avatar("Preparing response...");
log_channel_avatar("error_code: {$error_code}");
log_channel_avatar("error_message: {$error_message}");

// Send response
if ($error_code > 0) {
    $response = [
        'api_status' => http_response_code(),
        'error_code' => $error_code,
        'error_message' => $error_message
    ];
    log_channel_avatar("Sending error response: " . json_encode($response));
    echo json_encode($response);
} else if (!empty($data)) {
    log_channel_avatar("Sending success response: " . json_encode($data));
    echo json_encode($data);
} else {
    http_response_code(500);
    $response = [
        'api_status' => 500,
        'error_message' => 'Unknown error occurred'
    ];
    log_channel_avatar("Sending unknown error response: " . json_encode($response));
    echo json_encode($response);
}

log_channel_avatar("Response sent, exiting");
exit();
?>
