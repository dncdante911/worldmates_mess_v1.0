<?php
// +------------------------------------------------------------------------+
// | API Endpoint: Update Privacy Settings
// +------------------------------------------------------------------------+
// | Обновление настроек приватности пользователя
// | Обертка над XHR endpoint для единого API интерфейса
// +------------------------------------------------------------------------+

if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_POST['access_token']);
    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(401);
    } else {
        // Параметры приватности
        $update_data = array();

        // follow_privacy: 0 - everyone, 1 - only me
        if (isset($_POST['follow_privacy']) && in_array($_POST['follow_privacy'], array('0', '1', 0, 1))) {
            $update_data['follow_privacy'] = Wo_Secure($_POST['follow_privacy']);
        }

        // friend_privacy: 0 - everyone, 1 - people I follow, 2 - people follow me, 3 - no one
        if (isset($_POST['friend_privacy']) && in_array($_POST['friend_privacy'], array('0', '1', '2', '3', 0, 1, 2, 3))) {
            $update_data['friend_privacy'] = Wo_Secure($_POST['friend_privacy']);
        }

        // post_privacy: everyone, ifollow, nobody
        if (isset($_POST['post_privacy']) && in_array($_POST['post_privacy'], array('everyone', 'ifollow', 'nobody'))) {
            $update_data['post_privacy'] = Wo_Secure($_POST['post_privacy']);
        }

        // message_privacy: 0 - everyone, 1 - people I follow, 2 - no one
        if (isset($_POST['message_privacy']) && in_array($_POST['message_privacy'], array('0', '1', '2', 0, 1, 2))) {
            $update_data['message_privacy'] = Wo_Secure($_POST['message_privacy']);
        }

        // confirm_followers: 0 - no, 1 - yes
        if (isset($_POST['confirm_followers']) && in_array($_POST['confirm_followers'], array('0', '1', 0, 1))) {
            $update_data['confirm_followers'] = Wo_Secure($_POST['confirm_followers']);
        }

        // show_activities_privacy: 0 - hide, 1 - show
        if (isset($_POST['show_activities_privacy']) && in_array($_POST['show_activities_privacy'], array('0', '1', 0, 1))) {
            $update_data['show_activities_privacy'] = Wo_Secure($_POST['show_activities_privacy']);
        }

        // birth_privacy: 0 - everyone, 1 - people I follow, 2 - no one
        if (isset($_POST['birth_privacy']) && in_array($_POST['birth_privacy'], array('0', '1', '2', 0, 1, 2))) {
            $update_data['birth_privacy'] = Wo_Secure($_POST['birth_privacy']);
        }

        // visit_privacy: 0 - public, 1 - private
        if (isset($_POST['visit_privacy']) && in_array($_POST['visit_privacy'], array('0', '1', 0, 1))) {
            $update_data['visit_privacy'] = Wo_Secure($_POST['visit_privacy']);
        }

        // showlastseen: 0 - hide, 1 - show
        if (isset($_POST['showlastseen']) && in_array($_POST['showlastseen'], array('0', '1', 0, 1))) {
            $update_data['showlastseen'] = Wo_Secure($_POST['showlastseen']);
        }

        // status: 0 - offline, 1 - online
        if (isset($_POST['status']) && in_array($_POST['status'], array('0', '1', 0, 1))) {
            $update_data['status'] = Wo_Secure($_POST['status']);
        }

        // share_my_location: 0 - no, 1 - yes
        if (isset($_POST['share_my_location']) && in_array($_POST['share_my_location'], array('0', '1', 0, 1))) {
            $update_data['share_my_location'] = Wo_Secure($_POST['share_my_location']);
        }

        // share_my_data: 0 - no, 1 - yes
        if (isset($_POST['share_my_data']) && in_array($_POST['share_my_data'], array('0', '1', 0, 1))) {
            $update_data['share_my_data'] = Wo_Secure($_POST['share_my_data']);
        }

        if (!empty($update_data)) {
            // Обновить настройки в БД
            $update = $db->where('user_id', $user_id)->update(T_USERS, $update_data);

            if ($update) {
                // Очистить кэш пользователя
                cache($user_id, 'users', 'delete');

                $data = array(
                    'api_status' => 200,
                    'message' => 'Privacy settings updated successfully'
                );
            } else {
                $error_code    = 5;
                $error_message = 'Failed to update privacy settings';
                http_response_code(500);
            }
        } else {
            $data = array(
                'api_status' => 200,
                'message' => 'No settings to update'
            );
        }
    }
}
?>
