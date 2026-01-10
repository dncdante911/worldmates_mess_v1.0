<?php
// +------------------------------------------------------------------------+
// | 📦 CLOUD BACKUP: Експорт всіх даних користувача для бекапу
// +------------------------------------------------------------------------+
// | Повертає JSON з усіма повідомленнями, контактами, групами, каналами
// | Формат: готовий для завантаження на облачні сервіси
// +------------------------------------------------------------------------+

if (empty($_GET['access_token'])) {
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
        $export_data = array();

        // ==================== МЕТАДАНІ ====================
        $export_data['manifest'] = array(
            'version' => '2.0',
            'created_at' => time() * 1000, // milliseconds
            'user_id' => $user_id,
            'app_version' => '2.0-EDIT-FIX',
            'encryption' => 'none', // TODO: додати шифрування
        );

        // ==================== КОРИСТУВАЧ ====================
        $user_query = mysqli_query($sqlConnect, "
            SELECT user_id, username, first_name, last_name, email, phone_number,
                   avatar, cover, about, gender, birthday, country_id, city, zip
            FROM " . T_USERS . "
            WHERE user_id = {$user_id}
        ");

        if (mysqli_num_rows($user_query) > 0) {
            $export_data['user'] = mysqli_fetch_assoc($user_query);
        }

        // ==================== ПОВІДОМЛЕННЯ ====================
        $messages_query = mysqli_query($sqlConnect, "
            SELECT id, from_id, to_id, text, media, media_file_names,
                   time, seen, deleted_fs1, deleted_fs2,
                   product_id, lat, lng, reply_id, story_id
            FROM " . T_MESSAGES . "
            WHERE from_id = {$user_id} OR to_id = {$user_id}
            ORDER BY time ASC
        ");

        $messages = array();
        while ($message = mysqli_fetch_assoc($messages_query)) {
            $messages[] = $message;
        }
        $export_data['messages'] = $messages;
        $export_data['manifest']['total_messages'] = count($messages);

        // ==================== КОНТАКТИ/ЧАТИ ====================
        $chats_query = mysqli_query($sqlConnect, "
            SELECT DISTINCT
                CASE
                    WHEN from_id = {$user_id} THEN to_id
                    ELSE from_id
                END as contact_id
            FROM " . T_MESSAGES . "
            WHERE from_id = {$user_id} OR to_id = {$user_id}
        ");

        $contacts = array();
        while ($chat = mysqli_fetch_assoc($chats_query)) {
            $contact_id = $chat['contact_id'];

            // Отримати дані контакту
            $contact_query = mysqli_query($sqlConnect, "
                SELECT user_id, username, first_name, last_name, avatar
                FROM " . T_USERS . "
                WHERE user_id = {$contact_id}
            ");

            if (mysqli_num_rows($contact_query) > 0) {
                $contacts[] = mysqli_fetch_assoc($contact_query);
            }
        }
        $export_data['contacts'] = $contacts;

        // ==================== ГРУПИ ====================
        $groups_query = mysqli_query($sqlConnect, "
            SELECT gc.id, gc.user_id, gc.group_name, gc.avatar, gc.time
            FROM " . T_GROUP_CHAT . " gc
            INNER JOIN " . T_GROUP_CHAT_USERS . " gcu ON gc.id = gcu.group_id
            WHERE gcu.user_id = {$user_id}
        ");

        $groups = array();
        while ($group = mysqli_fetch_assoc($groups_query)) {
            // Отримати учасників групи
            $members_query = mysqli_query($sqlConnect, "
                SELECT user_id
                FROM " . T_GROUP_CHAT_USERS . "
                WHERE group_id = {$group['id']}
            ");

            $members = array();
            while ($member = mysqli_fetch_assoc($members_query)) {
                $members[] = $member['user_id'];
            }

            $group['members'] = $members;

            // Отримати повідомлення групи
            $group_messages_query = mysqli_query($sqlConnect, "
                SELECT id, user_id, text, media, time
                FROM " . T_GROUP_CHAT_MESSAGES . "
                WHERE group_id = {$group['id']}
                ORDER BY time ASC
            ");

            $group_messages = array();
            while ($msg = mysqli_fetch_assoc($group_messages_query)) {
                $group_messages[] = $msg;
            }

            $group['messages'] = $group_messages;
            $groups[] = $group;
        }
        $export_data['groups'] = $groups;
        $export_data['manifest']['total_groups'] = count($groups);

        // ==================== КАНАЛИ ====================
        $channels_query = mysqli_query($sqlConnect, "
            SELECT c.id, c.channel_name, c.channel_description, c.channel_avatar,
                   c.owner_id, c.channel_category, c.verified
            FROM " . T_CHANNELS . " c
            INNER JOIN " . T_CHANNEL_MEMBERS . " cm ON c.id = cm.channel_id
            WHERE cm.user_id = {$user_id}
        ");

        $channels = array();
        while ($channel = mysqli_fetch_assoc($channels_query)) {
            $channels[] = $channel;
        }
        $export_data['channels'] = $channels;

        // ==================== НАЛАШТУВАННЯ ====================
        $settings_query = mysqli_query($sqlConnect, "
            SELECT *
            FROM " . T_USER_CLOUD_BACKUP_SETTINGS . "
            WHERE user_id = {$user_id}
        ");

        if (mysqli_num_rows($settings_query) > 0) {
            $export_data['settings'] = mysqli_fetch_assoc($settings_query);
        }

        // ==================== ЗАБЛОКОВАНІ КОРИСТУВАЧІ ====================
        $blocked_query = mysqli_query($sqlConnect, "
            SELECT blocked
            FROM " . T_BLOCKS . "
            WHERE blocker = {$user_id}
        ");

        $blocked = array();
        while ($block = mysqli_fetch_assoc($blocked_query)) {
            $blocked[] = $block['blocked'];
        }
        $export_data['blocked_users'] = $blocked;

        // ==================== ПІДРАХУНОК РОЗМІРУ ====================
        $export_json = json_encode($export_data);
        $export_data['manifest']['total_size'] = strlen($export_json);

        // ==================== ЗБЕРЕГТИ НА СЕРВЕРІ ====================
        $backup_dir = __DIR__ . '/../../../upload/backups/user_' . $user_id;
        if (!file_exists($backup_dir)) {
            mkdir($backup_dir, 0755, true);
        }

        $backup_filename = 'backup_' . date('Y-m-d_H-i-s') . '.json';
        $backup_path = $backup_dir . '/' . $backup_filename;

        file_put_contents($backup_path, $export_json);

        // Оновити last_backup_time
        mysqli_query($sqlConnect, "
            UPDATE " . T_USER_CLOUD_BACKUP_SETTINGS . "
            SET last_backup_time = " . (time() * 1000) . "
            WHERE user_id = {$user_id}
        ");

        // ==================== ВІДПОВІДЬ ====================
        $data = array(
            'api_status' => 200,
            'message' => 'Backup created successfully',
            'backup_file' => $backup_filename,
            'backup_url' => $wo['config']['site_url'] . '/upload/backups/user_' . $user_id . '/' . $backup_filename,
            'backup_size' => strlen($export_json),
            'export_data' => $export_data // Повертаємо також JSON для прямого завантаження
        );
    }
}
?>
