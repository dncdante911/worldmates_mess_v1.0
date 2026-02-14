<?php
// +------------------------------------------------------------------------+
// | Test Email Configuration
// | Used to debug email sending issues
// | POST: email=your@email.com
// +------------------------------------------------------------------------+

$response_data = array('api_status' => 400);

$to_email = !empty($_POST['email']) ? trim($_POST['email']) : '';

if (empty($to_email)) {
    $error_code = 1;
    $error_message = 'Email parameter is required';
}

if (empty($error_code) && !filter_var($to_email, FILTER_VALIDATE_EMAIL)) {
    $error_code = 2;
    $error_message = 'Invalid email format';
}

if (empty($error_code)) {
    $test_code = rand(100000, 999999);
    $site_name = !empty($wo['config']['siteName']) ? $wo['config']['siteName'] : 'WorldMates';

    error_log("[TEST_EMAIL] Testing email to: {$to_email}");
    error_log("[TEST_EMAIL] Config: " . json_encode([
        'smtp_or_mail' => $wo['config']['smtp_or_mail'] ?? 'NOT SET',
        'siteEmail' => $wo['config']['siteEmail'] ?? 'NOT SET',
        'siteName' => $wo['config']['siteName'] ?? 'NOT SET',
        'smtp_host' => $wo['config']['smtp_host'] ?? 'NOT SET',
        'smtp_username' => $wo['config']['smtp_username'] ?? 'NOT SET',
        'smtp_port' => $wo['config']['smtp_port'] ?? 'NOT SET',
        'smtp_encryption' => $wo['config']['smtp_encryption'] ?? 'NOT SET',
    ]));

    $body = '<html><body style="font-family: Arial, sans-serif; padding: 20px;">';
    $body .= '<h2>Email Test - ' . htmlspecialchars($site_name) . '</h2>';
    $body .= '<p>This is a test email from API v2.</p>';
    $body .= '<p>Test code: <strong>' . $test_code . '</strong></p>';
    $body .= '<p>Time: ' . date('Y-m-d H:i:s') . '</p>';
    $body .= '<p>If you received this, email configuration is working!</p>';
    $body .= '</body></html>';

    $from_email = !empty($wo['config']['siteEmail']) ? $wo['config']['siteEmail'] : 'noreply@worldmates.club';

    $send_message_data = array(
        'from_email' => $from_email,
        'from_name'  => $site_name,
        'to_email'   => $to_email,
        'to_name'    => '',
        'subject'    => $site_name . ' - Test Email',
        'charSet'    => 'utf-8',
        'message_body' => $body,
        'is_html'    => true
    );

    error_log("[TEST_EMAIL] Calling Wo_SendMessage with: " . json_encode($send_message_data));

    $email_sent = false;
    if (function_exists('Wo_SendMessage')) {
        $email_sent = Wo_SendMessage($send_message_data);
        error_log("[TEST_EMAIL] Result: " . ($email_sent ? 'SUCCESS' : 'FAILED'));
    } else {
        error_log("[TEST_EMAIL] ERROR: Wo_SendMessage function does not exist!");
    }

    // Try PHP mail() as fallback
    $php_mail_sent = false;
    if (!$email_sent) {
        error_log("[TEST_EMAIL] Trying fallback PHP mail()...");
        $headers = "From: {$from_email}\r\n";
        $headers .= "MIME-Version: 1.0\r\n";
        $headers .= "Content-Type: text/html; charset=UTF-8\r\n";

        $php_mail_sent = mail($to_email, $site_name . ' - Test Email', $body, $headers);
        error_log("[TEST_EMAIL] PHP mail() result: " . ($php_mail_sent ? 'SUCCESS' : 'FAILED'));
    }

    if ($email_sent || $php_mail_sent) {
        $response_data = array(
            'api_status' => 200,
            'message' => 'Test email sent successfully!',
            'method' => $email_sent ? 'Wo_SendMessage' : 'PHP mail()',
            'to_email' => $to_email,
            'test_code' => $test_code
        );
    } else {
        $response_data = array(
            'api_status' => 500,
            'message' => 'Failed to send test email',
            'email_config' => array(
                'smtp_or_mail' => $wo['config']['smtp_or_mail'] ?? 'NOT SET',
                'siteEmail' => $wo['config']['siteEmail'] ?? 'NOT SET',
                'smtp_host' => $wo['config']['smtp_host'] ?? 'NOT SET',
                'smtp_username' => $wo['config']['smtp_username'] ?? 'NOT SET (show first 5 chars: ' . substr($wo['config']['smtp_username'] ?? '', 0, 5) . '***)',
                'smtp_port' => $wo['config']['smtp_port'] ?? 'NOT SET',
                'smtp_encryption' => $wo['config']['smtp_encryption'] ?? 'NOT SET',
            ),
            'function_exists' => function_exists('Wo_SendMessage') ? 'YES' : 'NO',
            'last_error' => error_get_last()
        );
    }
}
