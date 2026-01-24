<?php
// +------------------------------------------------------------------------+
// | 📡 CHANNELS: Завантаження аватара каналу
// +------------------------------------------------------------------------+

if (empty($_POST['access_token'])) {
    $error_code    = 3;
    $error_message = 'access_token is missing';
    http_response_code(400);
}

if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_POST['access_token']);
    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(400);
    } else {
        $channel_id = (!empty($_POST['channel_id']) && is_numeric($_POST['channel_id'])) ? (int)$_POST['channel_id'] : 0;

        if ($channel_id < 1) {
            $error_code    = 5;
            $error_message = 'channel_id is required';
            http_response_code(400);
        } else if (empty($_FILES['avatar'])) {
            $error_code    = 6;
            $error_message = 'avatar file is missing';
            http_response_code(400);
        } else {
            // Перевірити чи існує канал і чи є користувач власником/адміном
            $channel_query = mysqli_query($sqlConnect, "
                SELECT c.id, c.user_id
                FROM " . T_CHANNELS . " c
                WHERE c.id = {$channel_id}
            ");

            if (mysqli_num_rows($channel_query) == 0) {
                $error_code    = 7;
                $error_message = 'Channel not found';
                http_response_code(404);
            } else {
                $channel_data = mysqli_fetch_assoc($channel_query);

                // Перевірити чи користувач є власником або адміном
                $is_admin = ($channel_data['user_id'] == $user_id);

                if (!$is_admin) {
                    $admin_query = mysqli_query($sqlConnect, "
                        SELECT COUNT(*) as count
                        FROM " . T_CHANNEL_ADMINS . "
                        WHERE channel_id = {$channel_id}
                        AND user_id = {$user_id}
                    ");
                    $admin_data = mysqli_fetch_assoc($admin_query);
                    $is_admin = ($admin_data['count'] > 0);
                }

                if (!$is_admin) {
                    $error_code    = 8;
                    $error_message = 'Only channel admins can upload avatar';
                    http_response_code(403);
                } else {
                    // Перевірка розміру файлу (макс 5MB)
                    $max_file_size = 5 * 1024 * 1024; // 5MB в байтах
                    if ($_FILES['avatar']['size'] > $max_file_size) {
                        $error_code    = 9;
                        $error_message = 'File size exceeds maximum allowed (5MB)';
                        http_response_code(400);
                    } else {
                        // Завантажуємо файл
                        $file_info = array(
                            'file' => $_FILES['avatar']['tmp_name'],
                            'name' => $_FILES['avatar']['name'],
                            'size' => $_FILES['avatar']['size'],
                            'type' => $_FILES['avatar']['type'],
                            'types' => 'jpg,png,jpeg,gif'
                        );

                        $media = Wo_ShareFile($file_info);

                        if (!empty($media) && !empty($media['filename'])) {
                            $avatar_url = $media['filename'];

                            // Оновлюємо аватар в БД
                            $update_query = mysqli_query($sqlConnect, "
                                UPDATE " . T_CHANNELS . "
                                SET avatar = '{$avatar_url}'
                                WHERE id = {$channel_id}
                            ");

                            if ($update_query) {
                                $data = array(
                                    'api_status' => 200,
                                    'message' => 'Avatar uploaded successfully',
                                    'avatar_url' => $avatar_url
                                );

                                error_log("📡 Channel {$channel_id}: User {$user_id} uploaded avatar: {$avatar_url}");
                            } else {
                                $error_code    = 10;
                                $error_message = 'Failed to update avatar: ' . mysqli_error($sqlConnect);
                                http_response_code(500);
                            }
                        } else {
                            $error_code    = 11;
                            $error_message = 'Failed to upload file';
                            http_response_code(500);
                        }
                    }
                }
            }
        }
    }
}
?>
