<?php
// +------------------------------------------------------------------------+
// | Quick Login - Verify Code and Login
// | Verifies the 6-digit code and returns auth token
// +------------------------------------------------------------------------+
$response_data = array(
    'api_status' => 400,
);

$email = !empty($_POST['email']) ? $_POST['email'] : '';
$code = !empty($_POST['code']) ? $_POST['code'] : '';

if (empty($email)) {
    $error_code    = 3;
    $error_message = 'email (POST) is required';
}

if (empty($code)) {
    $error_code    = 4;
    $error_message = 'code (POST) is required';
}

if (empty($error_code)) {
    // Find user by email
    if (Wo_EmailExists($email) === false) {
        $error_code    = 6;
        $error_message = 'Email not found';
    } else {
        $user_id = Wo_UserIdFromEmail($email);
        $user_data = Wo_UserData($user_id);

        if (empty($user_data)) {
            $error_code    = 6;
            $error_message = 'User not found';
        }
    }

    if (empty($error_code)) {
        // Check if code matches and not expired
        $saved_code = $user_data['sms_code'];
        $expires_at = $user_data['email_code']; // Using email_code as timestamp

        if ($saved_code != $code) {
            $error_code    = 8;
            $error_message = 'Invalid code';
        } elseif (!empty($expires_at) && is_numeric($expires_at) && time() > $expires_at) {
            $error_code    = 9;
            $error_message = 'Code expired. Please request a new one.';
        } else {
            // Code is valid! Generate session token

            // Clear the code (one-time use)
            mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `sms_code` = '', `email_code` = '' WHERE `user_id` = " . (int)$user_id);
            cache($user_id, 'users', 'delete');

            // Generate access token (same as normal login)
            $access_token = sha1(rand(111111111, 999999999)) . md5(microtime()) . rand(11111111, 99999999) . md5(rand(5555, 9999));
            $access_token_esc = mysqli_real_escape_string($sqlConnect, $access_token);

            // Save token to database
            mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `access_token` = '{$access_token_esc}' WHERE `user_id` = " . (int)$user_id);
            cache($user_id, 'users', 'delete');

            // Get fresh user data
            $user_data = Wo_UserData($user_id);

            $response_data = array(
                'api_status' => 200,
                'message' => 'Login successful',
                'access_token' => $access_token,
                'user_id' => $user_id,
                'user_data' => array(
                    'user_id' => $user_data['user_id'],
                    'username' => $user_data['username'],
                    'email' => $user_data['email'],
                    'name' => $user_data['name'],
                    'avatar' => $user_data['avatar'],
                    'cover' => $user_data['cover'],
                    'verified' => $user_data['verified'],
                )
            );
        }
    }
}

if (!empty($error_code)) {
    $response_data = array(
        'api_status' => 400,
        'error_code' => $error_code,
        'errors' => array(
            'error_id' => $error_code,
            'error_text' => $error_message
        )
    );
}
