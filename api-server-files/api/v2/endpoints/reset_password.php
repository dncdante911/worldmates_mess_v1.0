<?php
// +------------------------------------------------------------------------+
// | Password Reset Endpoint
// | Validates reset code and updates password
// +------------------------------------------------------------------------+
$response_data = array(
    'api_status' => 400,
);

if (!empty($_POST['new_password']) && !empty($_POST['email']) && !empty($_POST['code'])) {
    $code  = Wo_Secure($_POST['code']);
    $email = Wo_Secure($_POST['email']);
    $update = false;

    // Validate reset token using both methods:
    // Method 1: user_id_passwordhash format (from web reset link)
    // Method 2: email_code match (from API-generated reset code)
    if (Wo_isValidPasswordResetToken($_POST['code']) === true || Wo_isValidPasswordResetToken2($_POST['code']) === true) {
        $update = true;
    }

    // Method 3: Direct email + email_code match (for mobile API flow)
    if (!$update) {
        $email_esc = mysqli_real_escape_string($sqlConnect, $_POST['email']);
        $code_esc = mysqli_real_escape_string($sqlConnect, $_POST['code']);
        $check_query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `email` = '{$email_esc}' AND `email_code` = '{$code_esc}' AND `email_code` != '' LIMIT 1");
        if ($check_query && mysqli_num_rows($check_query) > 0) {
            $update = true;
        }
    }

    if (!$update) {
        $error_code    = 9;
        $error_message = 'Invalid reset code';
    }

    if ($update == true) {
        if (strlen($_POST['new_password']) >= 6) {
            $password = password_hash($_POST['new_password'], PASSWORD_DEFAULT);
            $email_esc = mysqli_real_escape_string($sqlConnect, $_POST['email']);
            $password_esc = mysqli_real_escape_string($sqlConnect, $password);

            // Get user data
            $user_query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `email` = '{$email_esc}' LIMIT 1");
            $getUser = ($user_query && mysqli_num_rows($user_query) > 0) ? mysqli_fetch_assoc($user_query) : null;

            if ($getUser) {
                $uid = (int)$getUser['user_id'];

                // Update password and clear reset code
                mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `password` = '{$password_esc}', `email_code` = '' WHERE `email` = '{$email_esc}'");

                // Delete all active sessions (force re-login)
                mysqli_query($sqlConnect, "DELETE FROM " . T_APP_SESSIONS . " WHERE `user_id` = {$uid}");

                cache($uid, 'users', 'delete');
                $response_data['api_status'] = 200;
                $response_data['message'] = 'Your password was updated';
            } else {
                $error_code    = 6;
                $error_message = 'User not found';
            }
        } else {
            $error_code    = 10;
            $error_message = 'Password is too short (minimum 6 characters)';
        }
    }
} else {
    $error_code    = 8;
    $error_message = 'new_password, email, and code are required';
}
