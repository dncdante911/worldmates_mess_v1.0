<?php
// +------------------------------------------------------------------------+
// | Password Reset Email Endpoint
// | Generates a reset code, saves to DB, and sends email to user
// +------------------------------------------------------------------------+
$response_data = array(
    'api_status' => 400,
);

if (empty($_POST['email'])) {
    $error_code    = 3;
    $error_message = 'email (POST) is missing';
}

if (empty($error_code)) {
    if (Wo_EmailExists($_POST['email']) === false) {
        $error_code    = 6;
        $error_message = 'Email not found';
    } else {
        $user_id = Wo_UserIdFromEmail($_POST['email']);
        $user_recover_data = Wo_UserData($user_id);

        if (empty($user_recover_data)) {
            $error_code    = 6;
            $error_message = 'User not found';
        } else {
            // Generate a unique reset code and save to email_code field
            $reset_code = md5(rand(11111, 99999) . time() . $user_id);
            $reset_code_esc = mysqli_real_escape_string($sqlConnect, $reset_code);
            mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `email_code` = '{$reset_code_esc}' WHERE `user_id` = " . (int)$user_id);
            cache($user_id, 'users', 'delete');

            // Build reset link (for web) and code (for mobile)
            $site_url = !empty($wo['config']['site_url']) ? $wo['config']['site_url'] : $wo['site_url'];
            $reset_link = $site_url . '/index.php?link1=reset-password&code=' . $user_id . '_' . $user_recover_data['password'];

            $site_name = !empty($wo['config']['siteName']) ? $wo['config']['siteName'] : 'WorldMates';
            $subject = $site_name . ' - Password Reset Request';

            // Try to load email template, fallback to simple HTML
            $body = '';
            $wo['recover'] = $user_recover_data;
            $wo['recover']['link'] = $reset_link;

            if (function_exists('Wo_LoadPage')) {
                // Save current directory and switch to site root for template loading
                $saved_dir = getcwd();
                $site_root = $_SERVER['DOCUMENT_ROOT'];
                if (is_dir($site_root) && file_exists($site_root . '/themes')) {
                    chdir($site_root);
                    $body = @Wo_LoadPage('emails/recover');
                    chdir($saved_dir);
                }
            }

            // Fallback email body
            if (empty($body)) {
                $user_name = !empty($user_recover_data['name']) ? $user_recover_data['name'] : $user_recover_data['username'];
                $body = '<html><body style="font-family: Arial, sans-serif; padding: 20px;">';
                $body .= '<h2>' . htmlspecialchars($site_name) . '</h2>';
                $body .= '<p>Hello ' . htmlspecialchars($user_name) . ',</p>';
                $body .= '<p>We received a password reset request for your account.</p>';
                $body .= '<p>Click the link below to reset your password:</p>';
                $body .= '<p><a href="' . htmlspecialchars($reset_link) . '" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;">Reset Password</a></p>';
                $body .= '<p>If you did not request this, please ignore this email.</p>';
                $body .= '<p>Thank you,<br>' . htmlspecialchars($site_name) . ' Team</p>';
                $body .= '</body></html>';
            }

            $from_email = !empty($wo['config']['siteEmail']) ? $wo['config']['siteEmail'] : 'noreply@worldmates.club';

            $send_message_data = array(
                'from_email' => $from_email,
                'from_name'  => $site_name,
                'to_email'   => $_POST['email'],
                'to_name'    => '',
                'subject'    => $subject,
                'charSet'    => 'utf-8',
                'message_body' => $body,
                'is_html'    => true
            );
            $send = Wo_SendMessage($send_message_data);
            if ($send) {
                $response_data = array(
                    'api_status' => 200,
                    'message' => 'Password reset email sent successfully',
                );
            } else {
                $error_code    = 7;
                $error_message = 'Failed to send the email, please check your server email settings.';
            }
        }
    }
}
