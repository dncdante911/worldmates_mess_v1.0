<?php
// +------------------------------------------------------------------------+
// | 📸 GROUPS: Загрузка аватара группы
// +------------------------------------------------------------------------+

// Initialize response
$error_code = 0;
$error_message = '';
$data = [];

// Access token is already validated by api-v2.php router
// We can get user_id from the global $wo['user']['user_id']
$user_id = $wo['user']['user_id'] ?? 0;

if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
    $error_code    = 4;
    $error_message = 'Invalid access_token';
    http_response_code(400);
}

if ($error_code == 0) {
    // Получить параметр
    $group_id = (!empty($_POST['group_id']) && is_numeric($_POST['group_id'])) ? (int)$_POST['group_id'] : 0;

    if ($group_id < 1) {
        $error_code    = 5;
        $error_message = 'group_id is required';
        http_response_code(400);
    } else if (empty($_FILES['avatar']['tmp_name'])) {
        $error_code    = 6;
        $error_message = 'Avatar file is required';
        http_response_code(400);
    } else {
        // Проверить существует ли группа
        $group_query = mysqli_query($sqlConnect, "
            SELECT g.group_id, g.user_id
            FROM " . T_GROUPS . " g
            WHERE g.group_id = {$group_id}
        ");

        if (mysqli_num_rows($group_query) == 0) {
            $error_code    = 7;
            $error_message = 'Group not found';
            http_response_code(404);
        } else {
            $group_data = mysqli_fetch_assoc($group_query);

            // Проверить является ли пользователь админом группы
            $is_admin = ($group_data['user_id'] == $user_id);

            if (!$is_admin) {
                // Проверить является ли пользователь модератором
                $mod_query = mysqli_query($sqlConnect, "
                    SELECT COUNT(*) as count
                    FROM " . T_GROUP_MEMBERS . "
                    WHERE group_id = {$group_id}
                    AND user_id = {$user_id}
                    AND role = 'admin'
                ");
                $mod_data = mysqli_fetch_assoc($mod_query);
                $is_admin = ($mod_data['count'] > 0);
            }

            if (!$is_admin) {
                $error_code    = 8;
                $error_message = 'Only group admins can change avatar';
                http_response_code(403);
            } else {
                // Загрузить файл
                $file = $_FILES['avatar'];
                $file_info = array(
                    'file' => $file['tmp_name'],
                    'name' => $file['name'],
                    'size' => $file['size'],
                    'type' => $file['type'],
                    'types' => 'jpg,png,jpeg,gif'
                );

                // Проверка размера файла (макс 5MB)
                if ($file_info['size'] > 5242880) {
                    $error_code    = 9;
                    $error_message = 'File size too large (max 5MB)';
                    http_response_code(400);
                } else {
                    // Загрузить изображение
                    $media = Wo_ShareFile($file_info);

                    if (!empty($media) && !empty($media['filename'])) {
                        $avatar_url = $media['filename'];

                        // Обновить аватар группы
                        $update_query = mysqli_query($sqlConnect, "
                            UPDATE " . T_GROUPS . "
                            SET avatar = '{$avatar_url}'
                            WHERE group_id = {$group_id}
                        ");

                        if ($update_query) {
                            $data = array(
                                'api_status' => 200,
                                'message' => 'Group avatar uploaded successfully',
                                'avatar_url' => $avatar_url
                            );

                            // Логирование
                            error_log("📸 Group {$group_id}: User {$user_id} uploaded new avatar");
                        } else {
                            $error_code    = 10;
                            $error_message = 'Failed to update group avatar: ' . mysqli_error($sqlConnect);
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

// Send response
if ($error_code > 0) {
    echo json_encode([
        'api_status' => http_response_code(),
        'error_code' => $error_code,
        'error_message' => $error_message
    ]);
} else if (!empty($data)) {
    echo json_encode($data);
} else {
    http_response_code(500);
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Unknown error occurred'
    ]);
}

exit();
?>
