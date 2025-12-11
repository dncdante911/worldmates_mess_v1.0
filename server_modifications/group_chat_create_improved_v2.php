<?php
/**
 * ПОКРАЩЕНА СЕКЦІЯ ДЛЯ СТВОРЕННЯ ГРУПОВИХ ЧАТІВ v2
 *
 * ВИПРАВЛЕНО:
 * - Додано перевірку авторизації користувача
 * - Додано логування access_token (частково, для безпеки)
 * - Додано ранню помилку якщо користувач не авторизований
 * - Покращено обробку помилок
 */

// Шлях до лог-файлу
$log_file = '/var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_debug.log';

// Функція для логування
function log_debug($message, $log_file) {
    $timestamp = date('Y-m-d H:i:s');
    $log_entry = "[$timestamp] $message\n";
    @file_put_contents($log_file, $log_entry, FILE_APPEND);
}

if ($_POST['type'] == 'create') {
    log_debug("=== CREATE GROUP CHAT REQUEST STARTED ===", $log_file);
    log_debug("POST data: " . json_encode($_POST), $log_file);

    // Логуємо частину access_token для діагностики (перші і останні 8 символів)
    $token_preview = '';
    if (!empty($_GET['access_token'])) {
        $token = $_GET['access_token'];
        $token_preview = substr($token, 0, 8) . '...' . substr($token, -8);
    }
    log_debug("Access token preview: $token_preview", $log_file);

    // КРИТИЧНА ПЕРЕВІРКА: чи користувач авторизований
    if (empty($wo['user']['id'])) {
        log_debug("CRITICAL ERROR: User not authenticated! \$wo['user']['id'] is empty", $log_file);
        log_debug("Available \$wo keys: " . json_encode(array_keys($wo)), $log_file);

        if (isset($wo['user'])) {
            log_debug("User data exists but ID is empty: " . json_encode($wo['user']), $log_file);
        } else {
            log_debug("\$wo['user'] does not exist!", $log_file);
        }

        $error_code    = 401;
        $error_message = 'User not authenticated. Please check access_token.';
        log_debug("=== REQUEST ABORTED: Not authenticated ===\n", $log_file);

        // Повертаємо помилку авторизації
        $response_data = array(
            'api_status' => 401,
            'error_code' => 401,
            'error_message' => $error_message
        );

        header('Content-Type: application/json');
        echo json_encode($response_data);
        exit();
    }

    $user_id = $wo['user']['id'];
    log_debug("User ID: $user_id", $log_file);
    log_debug("User username: " . ($wo['user']['username'] ?? 'N/A'), $log_file);

    $required_fields = array(
        'group_name',
        'parts'
    );

    // Перевірка обов'язкових полів
    foreach ($required_fields as $key => $value) {
        if (empty($_POST[$value]) && empty($error_code)) {
            $error_code    = 4;
            $error_message = $value . ' (POST) is missing';
            log_debug("ERROR: Missing field - $value", $log_file);
        }
    }

    // Валідація довжини назви групи
    if (empty($error_code)) {
        $name_length = mb_strlen($_POST['group_name'], 'UTF-8');
        log_debug("Group name length: $name_length (name: {$_POST['group_name']})", $log_file);

        if ($name_length < 4 || $name_length > 25) {
            $error_code    = 5;
            $error_message = "group_name must be between 4 and 25 characters (current: $name_length)";
            log_debug("ERROR: Invalid name length - $name_length", $log_file);
        }
    }

    // Валідація аватару (якщо є)
    if (empty($error_code) && isset($_FILES["avatar"])) {
        log_debug("Avatar file uploaded", $log_file);
        if (file_exists($_FILES["avatar"]["tmp_name"])) {
            $image = getimagesize($_FILES["avatar"]["tmp_name"]);
            if (!in_array($image[2], array(
                IMAGETYPE_GIF,
                IMAGETYPE_JPEG,
                IMAGETYPE_PNG,
                IMAGETYPE_BMP
            ))) {
                $error_code    = 6;
                $error_message = 'Group avatar must be an image';
                log_debug("ERROR: Invalid avatar image type", $log_file);
            }
        }
    }

    // Створення групи
    if (empty($error_code)) {
        // Обробка списку учасників
        $parts_raw = Wo_Secure($_POST['parts']);
        log_debug("Parts (raw): '$parts_raw'", $log_file);

        // Якщо parts порожній, створюємо масив тільки з поточним користувачем
        if (empty($parts_raw) || trim($parts_raw) == '') {
            $users = array($user_id);
            log_debug("Parts was empty, using only current user: $user_id", $log_file);
        } else {
            $users = explode(',', $parts_raw);
            // Фільтруємо порожні елементи
            $users = array_filter($users, function($id) {
                return !empty(trim($id)) && is_numeric(trim($id));
            });
            // Додаємо поточного користувача якщо його немає
            if (!in_array($user_id, $users)) {
                $users[] = $user_id;
            }
            log_debug("Users after processing: " . json_encode($users), $log_file);
        }

        $name = Wo_Secure($_POST['group_name']);
        $type = 'group';
        if (!empty($_POST['group_type'])) {
            $type = Wo_Secure($_POST['group_type']);
        }

        log_debug("Calling Wo_CreateGChat with name='$name', type='$type', users=" . json_encode($users), $log_file);

        // Створюємо групу
        try {
            $id = Wo_CreateGChat($name, $users, $type);
            log_debug("Wo_CreateGChat returned: " . var_export($id, true) . " (type: " . gettype($id) . ")", $log_file);
        } catch (Exception $e) {
            log_debug("EXCEPTION in Wo_CreateGChat: " . $e->getMessage(), $log_file);
            log_debug("Stack trace: " . $e->getTraceAsString(), $log_file);
            $id = false;
        }

        if ($id && is_numeric($id)) {
            log_debug("SUCCESS: Group created with ID: $id", $log_file);

            // Обробка аватару
            if (isset($_FILES["avatar"])) {
                if (file_exists($_FILES["avatar"]["tmp_name"])) {
                    $fileInfo      = array(
                        'file' => $_FILES["avatar"]["tmp_name"],
                        'name' => $_FILES["avatar"]["name"],
                        'size' => $_FILES["avatar"]["size"],
                        'type' => $_FILES["avatar"]["type"],
                        'types' => 'jpg,png,jpeg,gif'
                    );
                    $media         = Wo_ShareFile($fileInfo, $id);
                    $mediaFilename = $media['filename'];
                    if (!empty($mediaFilename)) {
                        $update_data = Wo_UpdateGroupChatData($id, array(
                            'avatar' => $mediaFilename
                        ));
                        log_debug("Avatar uploaded: $mediaFilename", $log_file);
                    }
                }
            }

            // Отримуємо дані створеної групи
            $group_data = Wo_GroupTabData($id);

            if (!empty($group_data)) {
                // Видаляємо недозволені поля
                if (isset($non_allowed)) {
                    foreach ($non_allowed as $key => $value) {
                        unset($group_data['user_data'][$value]);
                        foreach ($group_data['parts'] as $key2 => $user) {
                            unset($group_data['parts'][$key2][$value]);
                        }
                    }
                }

                $response_data = array(
                    'api_status' => 200,
                    'data' => $group_data
                );
                log_debug("SUCCESS: Returning group data", $log_file);
            } else {
                $error_code    = 7;
                $error_message = 'Failed to fetch created group data';
                log_debug("ERROR: Group created but failed to fetch data", $log_file);
            }
        } else {
            $error_code    = 8;
            $error_message = 'Failed to create group chat (Wo_CreateGChat returned: ' . var_export($id, true) . ')';
            log_debug("ERROR: Wo_CreateGChat failed - returned: " . var_export($id, true), $log_file);
        }
    }

    log_debug("=== CREATE GROUP CHAT REQUEST FINISHED ===\n", $log_file);
}
