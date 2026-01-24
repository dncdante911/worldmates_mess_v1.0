<?php
// +------------------------------------------------------------------------+
// | 🔲 GROUPS: Генерація QR коду для групи
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
        // Отримати параметр
        $group_id = (!empty($_POST['group_id']) && is_numeric($_POST['group_id'])) ? (int)$_POST['group_id'] : 0;

        if ($group_id < 1) {
            $error_code    = 5;
            $error_message = 'group_id is required';
            http_response_code(400);
        } else {
            // Перевірити чи існує група
            $group_query = mysqli_query($sqlConnect, "
                SELECT g.group_id, g.user_id, g.name, g.qr_code
                FROM " . T_GROUPS . " g
                WHERE g.group_id = {$group_id}
            ");

            if (mysqli_num_rows($group_query) == 0) {
                $error_code    = 7;
                $error_message = 'Group not found';
                http_response_code(404);
            } else {
                $group_data = mysqli_fetch_assoc($group_query);

                // Перевірити чи користувач є адміном групи
                $is_admin = ($group_data['user_id'] == $user_id);

                if (!$is_admin) {
                    // Перевірити чи користувач є модератором
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
                    $error_message = 'Only group admins can generate QR codes';
                    http_response_code(403);
                } else {
                    // Якщо QR код вже існує, повернути його
                    if (!empty($group_data['qr_code'])) {
                        $qr_code = $group_data['qr_code'];
                    } else {
                        // Генеруємо унікальний QR код
                        $qr_code = 'WMG_' . strtoupper(substr(md5(uniqid($group_id, true)), 0, 16));

                        // Зберігаємо QR код в БД
                        $update_query = mysqli_query($sqlConnect, "
                            UPDATE " . T_GROUPS . "
                            SET qr_code = '{$qr_code}'
                            WHERE group_id = {$group_id}
                        ");

                        if (!$update_query) {
                            $error_code    = 9;
                            $error_message = 'Failed to generate QR code: ' . mysqli_error($sqlConnect);
                            http_response_code(500);
                        }
                    }

                    if ($error_code == 0) {
                        // Формуємо URL для приєднання
                        $join_url = $wo['config']['site_url'] . '/join-group/' . $qr_code;

                        $data = array(
                            'api_status' => 200,
                            'message' => 'QR code generated successfully',
                            'qr_code' => $qr_code,
                            'join_url' => $join_url,
                            'group_id' => $group_id,
                            'group_name' => $group_data['name']
                        );

                        // Логування
                        error_log("🔲 Group {$group_id}: User {$user_id} generated QR code: {$qr_code}");
                    }
                }
            }
        }
    }
}
?>
