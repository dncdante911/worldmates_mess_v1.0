<?php
// +------------------------------------------------------------------------+
// | Quick Registration - Step 1: Send Code
// | Simplified registration: just phone or email → get verification code
// | No username/password required at this step
// +------------------------------------------------------------------------+

$response_data = array('api_status' => 400);

$email = !empty($_POST['email']) ? trim($_POST['email']) : '';
$phone_number = !empty($_POST['phone_number']) ? trim($_POST['phone_number']) : '';

// Validate: at least one contact method
if (empty($email) && empty($phone_number)) {
    $error_code = 1;
    $error_message = 'email or phone_number is required';
}

if (empty($error_code)) {
    // Validate email format
    if (!empty($email) && !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $error_code = 2;
        $error_message = 'Invalid email format';
    }

    // Check if email already registered
    if (empty($error_code) && !empty($email) && Wo_EmailExists($email) === true) {
        $email_esc = mysqli_real_escape_string($sqlConnect, $email);
        $existing_q = mysqli_query($sqlConnect, "SELECT `user_id`, `username`, `active`, `sms_code` FROM " . T_USERS . " WHERE `email` = '{$email_esc}' LIMIT 1");
        if ($existing_q && mysqli_num_rows($existing_q) > 0) {
            $existing_user = mysqli_fetch_assoc($existing_q);
            $uid_check = (int)$existing_user['user_id'];

            // Check if account has any active app sessions
            $has_sessions = false;
            $sess_q = mysqli_query($sqlConnect, "SELECT COUNT(*) as cnt FROM " . T_APP_SESSIONS . " WHERE `user_id` = {$uid_check} LIMIT 1");
            if ($sess_q) {
                $sess_row = mysqli_fetch_assoc($sess_q);
                $has_sessions = ($sess_row['cnt'] > 0);
            }

            if ($existing_user['active'] == '0') {
                // Account not yet verified - allow resending code
                $verification_code = rand(100000, 999999);
                mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `sms_code` = '{$verification_code}' WHERE `user_id` = {$uid_check}");
                $resend_mode = true;
                $user_id = $uid_check;
                $auto_username = $existing_user['username'];
                error_log("[QUICK_REG] Resend mode: unverified account user_id={$user_id}");
            } elseif (!$has_sessions && preg_match('/^u\d+$/', $existing_user['username'])) {
                // Account was activated by partial quick_verify but has no session
                // (quick_verify crashed after activating but before returning token)
                // Allow re-verification to get a session token
                $verification_code = rand(100000, 999999);
                mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `sms_code` = '{$verification_code}', `active` = '0' WHERE `user_id` = {$uid_check}");
                $resend_mode = true;
                $user_id = $uid_check;
                $auto_username = $existing_user['username'];
                error_log("[QUICK_REG] Resend mode: activated but no session, user_id={$user_id}");
            } else {
                $error_code = 3;
                $error_message = 'This email is already registered. Please log in.';
            }
        } else {
            $error_code = 3;
            $error_message = 'This email is already registered. Please log in.';
        }
    }

    // Check if phone already registered
    if (empty($error_code) && !empty($phone_number)) {
        $phone_exists = false;
        if (function_exists('Wo_PhoneExists')) {
            $phone_exists = Wo_PhoneExists($phone_number);
        } elseif (function_exists('Wo_IsPhoneExist')) {
            $phone_exists = Wo_IsPhoneExist($phone_number);
        }
        if ($phone_exists) {
            $error_code = 4;
            $error_message = 'This phone number is already registered. Please log in.';
        }
    }
}

if (empty($error_code)) {
    // Skip account creation if we're in resend mode (account already exists but unverified)
    if (empty($resend_mode)) {
        // Generate auto-username (timestamp + random)
        $auto_username = 'u' . time() . rand(1000, 9999);

        // Generate temporary password (user can change later)
        $temp_password = bin2hex(random_bytes(8));

        // Generate 6-digit verification code
        $verification_code = rand(100000, 999999);

        // Create account data
        $account_data = array(
            'username' => Wo_Secure($auto_username, 0),
            'password' => $temp_password,
            'email_code' => md5(rand(1111, 9999) . time()),
            'sms_code' => $verification_code,
            'src' => 'Phone',
            'timezone' => 'UTC',
            'gender' => 'male',
            'lastseen' => time(),
            'active' => '0'  // Inactive until code verified
        );

        if (!empty($email)) {
            $account_data['email'] = Wo_Secure($email, 0);
        }
        if (!empty($phone_number)) {
            $account_data['phone_number'] = Wo_Secure($phone_number, 0);
        }

        // Device IDs
        if (!empty($_POST['android_m_device_id'])) {
            $account_data['android_m_device_id'] = Wo_Secure($_POST['android_m_device_id']);
        }
        if (!empty($_POST['ios_m_device_id'])) {
            $account_data['ios_m_device_id'] = Wo_Secure($_POST['ios_m_device_id']);
        }

        error_log("[QUICK_REG] Attempting registration with data: " . json_encode($account_data));
        $register = Wo_RegisterUser($account_data);

        if ($register !== true) {
            error_log("[QUICK_REG] Registration failed: " . json_encode($register));
            $error_code = 5;
            $error_message = 'Registration failed. Please try again later.';
        } else {
            $user_id = Wo_UserIdFromUsername($auto_username);
            error_log("[QUICK_REG] User registered successfully: user_id={$user_id}, username={$auto_username}");
        }
    }

    if (empty($error_code)) {

        // Send verification code
        $code_sent = false;

        if (!empty($email)) {
            // Send via email
            $site_name = !empty($wo['config']['siteName']) ? $wo['config']['siteName'] : 'WorldMates';
            $body = '<html><body style="font-family: Arial, sans-serif; padding: 20px;">';
            $body .= '<div style="max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">';
            $body .= '<h2 style="color: #2196F3; text-align: center; margin-bottom: 20px;">' . htmlspecialchars($site_name) . '</h2>';
            $body .= '<p style="color: #333; font-size: 16px; text-align: center;">Your verification code:</p>';
            $body .= '<h1 style="color: #4CAF50; letter-spacing: 8px; font-size: 42px; text-align: center; background: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0;">' . $verification_code . '</h1>';
            $body .= '<p style="color: #555; font-size: 14px; text-align: center;">Enter this code in the app to complete registration.</p>';
            $body .= '<p style="color: #888; font-size: 13px; text-align: center;">The code is valid for 15 minutes.</p>';
            $body .= '<hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">';
            $body .= '<p style="color: #999; font-size: 12px; text-align: center;">If you didn\'t request this, ignore this email.</p>';
            $body .= '</div></body></html>';

            $from_email = 'support@worldmates.club';

            // First try: use Wo_SendMessage (system SMTP config)
            $send_message_data = array(
                'from_email' => $from_email,
                'from_name'  => $site_name,
                'to_email'   => $email,
                'to_name'    => '',
                'subject'    => $site_name . ' - Verification Code: ' . $verification_code,
                'charSet'    => 'utf-8',
                'message_body' => $body,
                'is_html'    => true
            );

            error_log("[QUICK_REG] Attempting to send email to: {$email}, code: {$verification_code}");

            if (function_exists('Wo_SendMessage')) {
                $code_sent = Wo_SendMessage($send_message_data);
                error_log("[QUICK_REG] Wo_SendMessage result: " . ($code_sent ? 'SUCCESS' : 'FAILED'));
            }

            // Fallback: direct PHPMailer with hardcoded SMTP credentials
            if (!$code_sent) {
                error_log("[QUICK_REG] Wo_SendMessage failed, trying direct PHPMailer...");
                try {
                    // Try to find PHPMailer autoload
                    $phpmailer_paths = array(
                        dirname(dirname(dirname(__DIR__))) . '/assets/libraries/PHPMailer-Master/vendor/autoload.php',
                        dirname(dirname(dirname(__DIR__))) . '/assets/libraries/PHPMailer/vendor/autoload.php',
                        dirname(dirname(dirname(__DIR__))) . '/vendor/autoload.php',
                    );
                    $phpmailer_loaded = false;
                    foreach ($phpmailer_paths as $path) {
                        if (file_exists($path)) {
                            require_once $path;
                            $phpmailer_loaded = true;
                            error_log("[QUICK_REG] PHPMailer loaded from: $path");
                            break;
                        }
                    }

                    if ($phpmailer_loaded && class_exists('PHPMailer\\PHPMailer\\PHPMailer')) {
                        $mail = new PHPMailer\PHPMailer\PHPMailer(true);
                        $mail->isSMTP();
                        $mail->Host       = 'mail.sthost.pro';
                        $mail->SMTPAuth   = true;
                        $mail->Username   = 'support@worldmates.club';
                        $mail->Password   = '3344Frz@q0607';
                        $mail->SMTPSecure = 'ssl';
                        $mail->Port       = 465;
                        $mail->SMTPOptions = array(
                            'ssl' => array(
                                'verify_peer' => false,
                                'verify_peer_name' => false,
                                'allow_self_signed' => true
                            )
                        );
                        $mail->CharSet = 'UTF-8';
                        $mail->IsHTML(true);
                        $mail->setFrom('support@worldmates.club', $site_name);
                        $mail->addAddress($email);
                        $mail->Subject = $site_name . ' - Verification Code: ' . $verification_code;
                        $mail->Body = $body;

                        if ($mail->send()) {
                            $code_sent = true;
                            error_log("[QUICK_REG] Direct PHPMailer send: SUCCESS");
                        } else {
                            error_log("[QUICK_REG] Direct PHPMailer send: FAILED - " . $mail->ErrorInfo);
                        }
                        $mail->ClearAddresses();
                    } else {
                        error_log("[QUICK_REG] PHPMailer class not found. Checked paths: " . implode(', ', $phpmailer_paths));
                    }
                } catch (Exception $e) {
                    error_log("[QUICK_REG] Direct PHPMailer exception: " . $e->getMessage());
                }
            }
        }
        elseif (!empty($phone_number)) {
            // Send via SMS
            $site_name = !empty($wo['config']['siteName']) ? $wo['config']['siteName'] : 'WorldMates';
            $sms_text = $site_name . ": Your verification code is " . $verification_code;

            error_log("[QUICK_REG] Attempting to send SMS to: {$phone_number}, code: {$verification_code}");
            if (function_exists('Wo_SendSMSMessage')) {
                $code_sent = Wo_SendSMSMessage($phone_number, $sms_text);
                error_log("[QUICK_REG] SMS send result: " . ($code_sent ? 'SUCCESS' : 'FAILED'));
            } else {
                error_log("[QUICK_REG] Wo_SendSMSMessage function does not exist!");
            }
        }

        if ($code_sent) {
            $response_data = array(
                'api_status' => 200,
                'message' => !empty($email)
                    ? 'Verification code sent to your email'
                    : 'Verification code sent to your phone',
                'user_id' => $user_id,
                'username' => $auto_username,
                'verification_method' => !empty($email) ? 'email' : 'phone',
                // TEMPORARY: Include code for debugging (REMOVE IN PRODUCTION!)
                'debug_verification_code' => $verification_code
            );
        } else {
            // Code not sent, but account created — allow retry
            $response_data = array(
                'api_status' => 200,
                'message' => 'Account created. Verification code could not be sent, please try resending.',
                'user_id' => $user_id,
                'username' => $auto_username,
                'verification_method' => !empty($email) ? 'email' : 'phone',
                'code_sent' => false,
                // TEMPORARY: Include code for debugging (REMOVE IN PRODUCTION!)
                'debug_verification_code' => $verification_code,
                'debug_email_config' => array(
                    'smtp_or_mail' => $wo['config']['smtp_or_mail'] ?? 'NOT SET',
                    'siteEmail' => $wo['config']['siteEmail'] ?? 'NOT SET',
                    'siteName' => $wo['config']['siteName'] ?? 'NOT SET'
                )
            );
        }
    }
}
