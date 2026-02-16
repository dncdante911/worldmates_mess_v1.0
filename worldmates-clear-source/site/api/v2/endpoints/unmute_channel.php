<?php
// +------------------------------------------------------------------------+
// | 📡 CHANNELS: Увімкнення сповіщень для каналу
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
        } else {
            // Перевірити чи користувач підписаний на канал
            $subscriber_query = mysqli_query($sqlConnect, "
                SELECT id
                FROM " . T_CHANNEL_SUBSCRIBERS . "
                WHERE channel_id = {$channel_id}
                AND user_id = {$user_id}
            ");

            if (mysqli_num_rows($subscriber_query) == 0) {
                $error_code    = 6;
                $error_message = 'You are not subscribed to this channel';
                http_response_code(400);
            } else {
                // Оновити статус is_muted
                $update_query = mysqli_query($sqlConnect, "
                    UPDATE " . T_CHANNEL_SUBSCRIBERS . "
                    SET is_muted = 0
                    WHERE channel_id = {$channel_id}
                    AND user_id = {$user_id}
                ");

                if ($update_query) {
                    $data = array(
                        'api_status' => 200,
                        'message' => 'Channel unmuted successfully'
                    );

                    error_log("📡 Channel {$channel_id}: User {$user_id} unmuted notifications");
                } else {
                    $error_code    = 7;
                    $error_message = 'Failed to unmute channel: ' . mysqli_error($sqlConnect);
                    http_response_code(500);
                }
            }
        }
    }
}
?>
