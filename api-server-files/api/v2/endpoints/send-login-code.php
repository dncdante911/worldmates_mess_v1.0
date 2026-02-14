<?php
// +------------------------------------------------------------------------+
// | Quick Login - Send Email Code
// | Sends a 6-digit code to user's email for quick authorization
// +------------------------------------------------------------------------+
$response_data = array(
    'api_status' => 400,
);

$email = !empty($_POST['email']) ? $_POST['email'] : '';

if (empty($email)) {
    $error_code    = 3;
    $error_message = 'email (POST) is required';
}

if (empty($error_code)) {
    // Check if email exists
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
        // Generate 6-digit login code
        $login_code = rand(100000, 999999);
        $login_code_esc = mysqli_real_escape_string($sqlConnect, $login_code);

        // Save code to sms_code field (expires in 10 minutes)
        $expires_at = time() + 600; // 10 minutes
        mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `sms_code` = '{$login_code_esc}', `email_code` = '{$expires_at}' WHERE `user_id` = " . (int)$user_id);
        cache($user_id, 'users', 'delete');

        $site_name = !empty($wo['config']['siteName']) ? $wo['config']['siteName'] : 'WorldMates';
        $subject = $site_name . ' - Your Login Code';

        // Email body (HTML template)
        $user_name = !empty($user_data['name']) ? $user_data['name'] : $user_data['username'];
        $body = '<html><body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">';
        $body .= '<div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">';
        $body .= '<h2 style="color: #333; margin-bottom: 20px;">' . htmlspecialchars($site_name) . '</h2>';
        $body .= '<p style="color: #666; font-size: 16px;">Hello ' . htmlspecialchars($user_name) . ',</p>';
        $body .= '<p style="color: #666; font-size: 16px;">Your login code is:</p>';
        $body .= '<div style="background-color: #f0f0f0; padding: 20px; border-radius: 4px; text-align: center; margin: 20px 0;">';
        $body .= '<span style="font-size: 32px; font-weight: bold; color: #4CAF50; letter-spacing: 5px;">' . $login_code . '</span>';
        $body .= '</div>';
        $body .= '<p style="color: #666; font-size: 14px;">This code will expire in 10 minutes.</p>';
        $body .= '<p style="color: #999; font-size: 12px; margin-top: 30px;">If you did not request this code, please ignore this email.</p>';
        $body .= '<p style="color: #666; font-size: 14px; margin-top: 20px;">Thank you,<br><strong>' . htmlspecialchars($site_name) . ' Team</strong></p>';
        $body .= '</div>';
        $body .= '</body></html>';

        $from_email = !empty($wo['config']['siteEmail']) ? $wo['config']['siteEmail'] : 'noreply@worldmates.club';

        $send_message_data = array(
            'from_email' => $from_email,
            'from_name'  => $site_name,
            'to_email'   => $email,
            'to_name'    => $user_name,
            'subject'    => $subject,
            'charSet'    => 'utf-8',
            'message_body' => $body,
            'is_html'    => true
        );

        $send = Wo_SendMessage($send_message_data);

        if ($send) {
            $response_data = array(
                'api_status' => 200,
                'message' => 'Login code sent to your email',
                'email' => $email,
                'expires_in' => 600, // seconds
            );
        } else {
            $error_code    = 7;
            $error_message = 'Failed to send email. Please check your server settings.';
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
