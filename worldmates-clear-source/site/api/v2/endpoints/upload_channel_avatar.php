<?php
// +------------------------------------------------------------------------+
// | 📡 CHANNELS: Завантаження аватара каналу
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
        // Channels are stored in T_GROUPCHAT table
        $channel_query = mysqli_query($sqlConnect, "
            SELECT g.id, g.user_id
            FROM " . T_GROUPCHAT . " g
            WHERE g.id = {$channel_id}
        ");

        if (mysqli_num_rows($channel_query) == 0) {
            $error_code    = 7;
            $error_message = 'Channel not found';
            http_response_code(404);
        } else {
            $channel_data = mysqli_fetch_assoc($channel_query);

            // Check if user is owner or admin
            $is_admin = ($channel_data['user_id'] == $user_id);

            if (!$is_admin) {
                // Check if user is admin in group chat users table
                $admin_query = mysqli_query($sqlConnect, "
                    SELECT COUNT(*) as count
                    FROM " . T_GROUPCHATUSERS . "
                    WHERE group_id = {$channel_id}
                    AND user_id = {$user_id}
                    AND role IN ('owner', 'admin')
                ");
                
                if ($admin_query) {
                    $admin_data = mysqli_fetch_assoc($admin_query);
                    $is_admin = ($admin_data['count'] > 0);
                }
            }

            if (!$is_admin) {
                $error_code    = 8;
                $error_message = 'Only channel admins can upload avatar';
                http_response_code(403);
            } else {
                // Check file size (max 5MB)
                $max_file_size = 5 * 1024 * 1024; // 5MB in bytes
                if ($_FILES['avatar']['size'] > $max_file_size) {
                    $error_code    = 9;
                    $error_message = 'File size exceeds maximum allowed (5MB)';
                    http_response_code(400);
                } else {
                    // Upload file
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

                        // Update avatar in database
                        $update_query = mysqli_query($sqlConnect, "
                            UPDATE " . T_GROUPCHAT . "
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
