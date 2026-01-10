<?php
// +------------------------------------------------------------------------+
// | 📦 CLOUD BACKUP: Імпорт даних користувача з бекапу
// +------------------------------------------------------------------------+
// | Відновлює дані з JSON бекапу при вході з нового пристрою
// +------------------------------------------------------------------------+

if (empty($_POST['access_token'])) {
    $error_code    = 3;
    $error_message = 'access_token is missing';
    http_response_code(400);
}

if (empty($_POST['backup_data'])) {
    $error_code    = 3;
    $error_message = 'backup_data is missing';
    http_response_code(400);
}

if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_POST['access_token']);
    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(400);
    } else {
        // Декодувати JSON
        $backup_data = json_decode($_POST['backup_data'], true);

        if (!$backup_data) {
            $error_code    = 5;
            $error_message = 'Invalid backup data format';
            http_response_code(400);
        } else {
            $imported_stats = array(
                'messages' => 0,
                'groups' => 0,
                'channels' => 0,
                'settings' => false
            );

            // ==================== ІМПОРТ ПОВІДОМЛЕНЬ ====================
            if (!empty($backup_data['messages'])) {
                foreach ($backup_data['messages'] as $message) {
                    // Перевірити чи повідомлення вже існує
                    $check_query = mysqli_query($sqlConnect, "
                        SELECT id FROM " . T_MESSAGES . "
                        WHERE id = {$message['id']}
                    ");

                    if (mysqli_num_rows($check_query) == 0) {
                        // Додати повідомлення
                        $text = Wo_Secure($message['text']);
                        $media = Wo_Secure($message['media']);
                        $media_file_names = Wo_Secure($message['media_file_names']);

                        $insert_query = "
                            INSERT INTO " . T_MESSAGES . "
                            (id, from_id, to_id, text, media, media_file_names, time, seen, reply_id)
                            VALUES (
                                {$message['id']},
                                {$message['from_id']},
                                {$message['to_id']},
                                '{$text}',
                                '{$media}',
                                '{$media_file_names}',
                                {$message['time']},
                                {$message['seen']},
                                " . ($message['reply_id'] ? $message['reply_id'] : 'NULL') . "
                            )
                        ";

                        if (mysqli_query($sqlConnect, $insert_query)) {
                            $imported_stats['messages']++;
                        }
                    }
                }
            }

            // ==================== ІМПОРТ ГРУП ====================
            if (!empty($backup_data['groups'])) {
                foreach ($backup_data['groups'] as $group) {
                    // Перевірити чи група вже існує
                    $check_query = mysqli_query($sqlConnect, "
                        SELECT id FROM " . T_GROUP_CHAT . "
                        WHERE id = {$group['id']}
                    ");

                    if (mysqli_num_rows($check_query) == 0) {
                        // Створити групу
                        $group_name = Wo_Secure($group['group_name']);
                        $avatar = Wo_Secure($group['avatar']);

                        $insert_query = "
                            INSERT INTO " . T_GROUP_CHAT . "
                            (id, user_id, group_name, avatar, time)
                            VALUES (
                                {$group['id']},
                                {$group['user_id']},
                                '{$group_name}',
                                '{$avatar}',
                                {$group['time']}
                            )
                        ";

                        if (mysqli_query($sqlConnect, $insert_query)) {
                            // Додати учасників
                            if (!empty($group['members'])) {
                                foreach ($group['members'] as $member_id) {
                                    mysqli_query($sqlConnect, "
                                        INSERT IGNORE INTO " . T_GROUP_CHAT_USERS . "
                                        (user_id, group_id, active, time_added)
                                        VALUES ({$member_id}, {$group['id']}, 1, " . time() . ")
                                    ");
                                }
                            }

                            // Імпорт повідомлень групи
                            if (!empty($group['messages'])) {
                                foreach ($group['messages'] as $msg) {
                                    $text = Wo_Secure($msg['text']);
                                    $media = Wo_Secure($msg['media']);

                                    mysqli_query($sqlConnect, "
                                        INSERT INTO " . T_GROUP_CHAT_MESSAGES . "
                                        (group_id, user_id, text, media, time)
                                        VALUES (
                                            {$group['id']},
                                            {$msg['user_id']},
                                            '{$text}',
                                            '{$media}',
                                            {$msg['time']}
                                        )
                                    ");
                                }
                            }

                            $imported_stats['groups']++;
                        }
                    }
                }
            }

            // ==================== ІМПОРТ НАЛАШТУВАНЬ ====================
            if (!empty($backup_data['settings'])) {
                $settings = $backup_data['settings'];

                // Перевірити чи існують налаштування
                $check_query = mysqli_query($sqlConnect, "
                    SELECT id FROM " . T_USER_CLOUD_BACKUP_SETTINGS . "
                    WHERE user_id = {$user_id}
                ");

                if (mysqli_num_rows($check_query) > 0) {
                    // Оновити
                    mysqli_query($sqlConnect, "
                        UPDATE " . T_USER_CLOUD_BACKUP_SETTINGS . "
                        SET backup_provider = '{$settings['backup_provider']}',
                            backup_frequency = '{$settings['backup_frequency']}',
                            backup_enabled = {$settings['backup_enabled']}
                        WHERE user_id = {$user_id}
                    ");
                } else {
                    // Створити
                    mysqli_query($sqlConnect, "
                        INSERT INTO " . T_USER_CLOUD_BACKUP_SETTINGS . "
                        (user_id, backup_provider, backup_frequency, backup_enabled)
                        VALUES ({$user_id}, '{$settings['backup_provider']}', '{$settings['backup_frequency']}', {$settings['backup_enabled']})
                    ");
                }

                $imported_stats['settings'] = true;
            }

            // ==================== ВІДПОВІДЬ ====================
            $data = array(
                'api_status' => 200,
                'message' => 'Backup restored successfully',
                'imported' => $imported_stats
            );
        }
    }
}
?>
