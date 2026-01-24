<?php
// +------------------------------------------------------------------------+
// | 🔲 GROUPS: Приєднання до групи за QR кодом
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
        $qr_code = (!empty($_POST['qr_code'])) ? Wo_Secure($_POST['qr_code']) : '';

        if (empty($qr_code)) {
            $error_code    = 5;
            $error_message = 'qr_code is required';
            http_response_code(400);
        } else {
            // Знайти групу за QR кодом
            $group_query = mysqli_query($sqlConnect, "
                SELECT g.group_id, g.user_id, g.name, g.category,
                       g.about, g.avatar, g.cover, g.privacy
                FROM " . T_GROUPS . " g
                WHERE g.qr_code = '{$qr_code}'
            ");

            if (mysqli_num_rows($group_query) == 0) {
                $error_code    = 6;
                $error_message = 'Invalid QR code or group not found';
                http_response_code(404);
            } else {
                $group_data = mysqli_fetch_assoc($group_query);
                $group_id = $group_data['group_id'];

                // Перевірити чи користувач вже є членом групи
                $member_query = mysqli_query($sqlConnect, "
                    SELECT COUNT(*) as count
                    FROM " . T_GROUP_MEMBERS . "
                    WHERE group_id = {$group_id}
                    AND user_id = {$user_id}
                ");
                $member_data = mysqli_fetch_assoc($member_query);

                if ($member_data['count'] > 0) {
                    $error_code    = 7;
                    $error_message = 'You are already a member of this group';
                    http_response_code(400);
                } else {
                    // Додати користувача до групи
                    $join_query = mysqli_query($sqlConnect, "
                        INSERT INTO " . T_GROUP_MEMBERS . "
                        (group_id, user_id, role, joined_at)
                        VALUES ({$group_id}, {$user_id}, 'member', " . time() . ")
                    ");

                    if ($join_query) {
                        // Оновити лічильник членів групи
                        mysqli_query($sqlConnect, "
                            UPDATE " . T_GROUPS . "
                            SET members_count = members_count + 1
                            WHERE group_id = {$group_id}
                        ");

                        $data = array(
                            'api_status' => 200,
                            'message' => 'Successfully joined the group',
                            'group' => array(
                                'group_id' => $group_data['group_id'],
                                'user_id' => $group_data['user_id'],
                                'name' => $group_data['name'],
                                'category' => $group_data['category'],
                                'about' => $group_data['about'],
                                'avatar' => $group_data['avatar'],
                                'cover' => $group_data['cover'],
                                'privacy' => $group_data['privacy']
                            )
                        );

                        // Логування
                        error_log("🔲 Group {$group_id}: User {$user_id} joined via QR code: {$qr_code}");

                        // Відправити сповіщення адміну групи
                        $notification_data = array(
                            'recipient_id' => $group_data['user_id'],
                            'type' => 'group_join',
                            'group_id' => $group_id,
                            'user_id' => $user_id,
                            'time' => time()
                        );
                        // TODO: Implement notification system
                    } else {
                        $error_code    = 8;
                        $error_message = 'Failed to join group: ' . mysqli_error($sqlConnect);
                        http_response_code(500);
                    }
                }
            }
        }
    }
}
?>
