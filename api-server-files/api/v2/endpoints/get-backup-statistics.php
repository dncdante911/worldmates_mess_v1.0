<?php
// +------------------------------------------------------------------------+
// | 📊 CLOUD BACKUP: Получение статистики облачного хранилища
// +------------------------------------------------------------------------+
// | Возвращает реальную статистику о размере бекапа пользователя:
// | - Количество сообщений
// | - Размер медиа файлов
// | - Последний бекап
// | - Общий размер storage
// +------------------------------------------------------------------------+

if (empty($_GET['access_token']) || empty($_GET['access_token'])) {
    $error_code    = 3;
    $error_message = 'access_token is missing';
    http_response_code(400);
}

if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_GET['access_token']);
    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(400);
    } else {
        // ==================== ПІДРАХУНОК ПОВІДОМЛЕНЬ ====================

        // Кількість отриманих повідомлень
        $messages_received_query = mysqli_query($sqlConnect, "
            SELECT COUNT(*) as count
            FROM " . T_MESSAGES . "
            WHERE to_id = {$user_id}
        ");
        $messages_received = mysqli_fetch_assoc($messages_received_query)['count'];

        // Кількість відправлених повідомлень
        $messages_sent_query = mysqli_query($sqlConnect, "
            SELECT COUNT(*) as count
            FROM " . T_MESSAGES . "
            WHERE from_id = {$user_id}
        ");
        $messages_sent = mysqli_fetch_assoc($messages_sent_query)['count'];

        $total_messages = $messages_received + $messages_sent;

        // ==================== ПІДРАХУНОК МЕДІА ФАЙЛІВ ====================

        // Розмір медіа у відправлених повідомленнях
        $media_sent_query = mysqli_query($sqlConnect, "
            SELECT media, media_file_names
            FROM " . T_MESSAGES . "
            WHERE from_id = {$user_id}
            AND (media != '' OR media_file_names != '')
        ");

        $media_size = 0;
        $media_count = 0;

        while ($media_row = mysqli_fetch_assoc($media_sent_query)) {
            // Якщо є media URL
            if (!empty($media_row['media'])) {
                $media_path = str_replace($wo['config']['site_url'] . '/', '', $media_row['media']);
                $full_path = __DIR__ . '/../../../' . $media_path;

                if (file_exists($full_path)) {
                    $media_size += filesize($full_path);
                    $media_count++;
                }
            }

            // Якщо є media_file_names (JSON з файлами)
            if (!empty($media_row['media_file_names'])) {
                $files = json_decode($media_row['media_file_names'], true);
                if (is_array($files)) {
                    foreach ($files as $file) {
                        $file_path = str_replace($wo['config']['site_url'] . '/', '', $file);
                        $full_path = __DIR__ . '/../../../' . $file_path;

                        if (file_exists($full_path)) {
                            $media_size += filesize($full_path);
                            $media_count++;
                        }
                    }
                }
            }
        }

        // Розмір медіа у отриманих повідомленнях
        $media_received_query = mysqli_query($sqlConnect, "
            SELECT media, media_file_names
            FROM " . T_MESSAGES . "
            WHERE to_id = {$user_id}
            AND (media != '' OR media_file_names != '')
        ");

        while ($media_row = mysqli_fetch_assoc($media_received_query)) {
            if (!empty($media_row['media'])) {
                $media_path = str_replace($wo['config']['site_url'] . '/', '', $media_row['media']);
                $full_path = __DIR__ . '/../../../' . $media_path;

                if (file_exists($full_path)) {
                    $media_size += filesize($full_path);
                    $media_count++;
                }
            }

            if (!empty($media_row['media_file_names'])) {
                $files = json_decode($media_row['media_file_names'], true);
                if (is_array($files)) {
                    foreach ($files as $file) {
                        $file_path = str_replace($wo['config']['site_url'] . '/', '', $file);
                        $full_path = __DIR__ . '/../../../' . $file_path;

                        if (file_exists($full_path)) {
                            $media_size += filesize($full_path);
                            $media_count++;
                        }
                    }
                }
            }
        }

        // ==================== ОСТАННІЙ БЕКАП ====================

        $backup_settings_query = mysqli_query($sqlConnect, "
            SELECT last_backup_time, backup_frequency
            FROM " . T_USER_CLOUD_BACKUP_SETTINGS . "
            WHERE user_id = {$user_id}
        ");

        $last_backup_time = null;
        $backup_frequency = 'daily';

        if (mysqli_num_rows($backup_settings_query) > 0) {
            $backup_settings = mysqli_fetch_assoc($backup_settings_query);
            $last_backup_time = $backup_settings['last_backup_time'];
            $backup_frequency = $backup_settings['backup_frequency'];
        }

        // ==================== ГРУПИ ТА КАНАЛИ ====================

        // Кількість груп
        $groups_query = mysqli_query($sqlConnect, "
            SELECT COUNT(*) as count
            FROM " . T_GROUP_CHAT_USERS . "
            WHERE user_id = {$user_id}
        ");
        $groups_count = mysqli_fetch_assoc($groups_query)['count'];

        // Кількість каналів
        $channels_query = mysqli_query($sqlConnect, "
            SELECT COUNT(*) as count
            FROM " . T_CHANNEL_MEMBERS . "
            WHERE user_id = {$user_id}
        ");
        $channels_count = mysqli_fetch_assoc($channels_query)['count'];

        // ==================== ЗАГАЛЬНИЙ РОЗМІР ====================

        // Примерний підрахунок: 1 текстове повідомлення ≈ 500 bytes
        $text_messages_size = $total_messages * 500;

        // Загальний розмір = текст + медіа
        $total_storage = $text_messages_size + $media_size;

        // ==================== ВІДПОВІДЬ ====================

        $data = array(
            'api_status' => 200,
            'statistics' => array(
                // Повідомлення
                'total_messages' => (int)$total_messages,
                'messages_sent' => (int)$messages_sent,
                'messages_received' => (int)$messages_received,

                // Медіа
                'media_files_count' => (int)$media_count,
                'media_size_bytes' => (int)$media_size,
                'media_size_mb' => round($media_size / 1024 / 1024, 2),

                // Групи та канали
                'groups_count' => (int)$groups_count,
                'channels_count' => (int)$channels_count,

                // Загальний розмір
                'total_storage_bytes' => (int)$total_storage,
                'total_storage_mb' => round($total_storage / 1024 / 1024, 2),
                'total_storage_gb' => round($total_storage / 1024 / 1024 / 1024, 2),

                // Бекап
                'last_backup_time' => $last_backup_time,
                'backup_frequency' => $backup_frequency,

                // Додаткова інформація
                'server_name' => 'WorldMates Server',
                'backup_provider' => 'local_server'
            )
        );
    }
}
?>
