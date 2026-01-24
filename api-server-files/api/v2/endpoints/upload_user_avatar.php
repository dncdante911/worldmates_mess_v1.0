<?php
// +------------------------------------------------------------------------+
// | API Endpoint: Upload User Avatar
// +------------------------------------------------------------------------+
// | Загрузка аватара пользователя
// +------------------------------------------------------------------------+

// Initialize response
$error_code = 0;
$error_message = '';
$data = [];

// Get access token from POST or GET (Android sends via URL, but WoWonder expects POST)
$access_token = $_POST['access_token'] ?? $_GET['access_token'] ?? '';

if (empty($access_token)) {
    $error_code = 3;
    $error_message = 'access_token is missing';
    http_response_code(400);
}

if ($error_code == 0) {
    // Validate access token and get user_id
    $user_id = Wo_UserIdFromAccessToken($access_token);

    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code = 4;
        $error_message = 'Invalid access_token';
        http_response_code(401);
    } else {
        // Load user data and set $wo['user'] (CRITICAL for Wo_UploadImage)
        global $wo;
        $user_data = Wo_UserData($user_id);

        if (empty($user_data)) {
            $error_code = 5;
            $error_message = 'User not found';
            http_response_code(404);
        } else {
            // Update $wo['user'] with real user data (required by Wo_UploadImage)
            $wo['user'] = $user_data;
            $wo['loggedin'] = true;

            // Check if file is uploaded
            if (empty($_FILES['avatar']['tmp_name'])) {
                $error_code = 6;
                $error_message = 'Avatar file is required';
                http_response_code(400);
            } else {
                // Upload image using WoWonder's function
                // Same as update-user-data.php line 225
                $upload_image = Wo_UploadImage(
                    $_FILES["avatar"]["tmp_name"],
                    $_FILES['avatar']['name'],
                    'avatar',
                    $_FILES['avatar']['type'],
                    $user_id
                );

                if ($upload_image === true) {
                    // Get updated user data
                    $updated_user = Wo_UserData($user_id);

                    if ($updated_user) {
                        // Clear user cache
                        cache($user_id, 'users', 'delete');

                        // Success response
                        // Return 'url' for compatibility with MediaUploadResponse
                        $data = array(
                            'api_status' => 200,
                            'message' => 'Avatar uploaded successfully',
                            'url' => Wo_GetMedia($updated_user['avatar']), // Full URL for Android
                            'avatar' => $updated_user['avatar'],
                            'avatar_org' => $updated_user['avatar_org']
                        );
                    } else {
                        $error_code = 8;
                        $error_message = 'Failed to retrieve updated user data';
                        http_response_code(500);
                    }
                } else {
                    $error_code = 7;
                    $error_message = 'Failed to upload image. Please check file type and size.';
                    http_response_code(500);
                }
            }
        }
    }
}

// Send response
if ($error_code > 0) {
    http_response_code($error_code >= 100 ? $error_code : 400);
    echo json_encode([
        'api_status' => $error_code >= 100 ? $error_code : 400,
        'error_code' => $error_code,
        'error_message' => $error_message
    ]);
} else if (!empty($data)) {
    echo json_encode($data);
} else {
    // No data and no error - something went wrong
    http_response_code(500);
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Unknown error occurred'
    ]);
}
?>
