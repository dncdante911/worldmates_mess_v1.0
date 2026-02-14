<?php
// Test SMTP configuration endpoint

$email = $_POST['email'] ?? $_GET['email'] ?? 'test@example.com';
$test_code = rand(100000, 999999);

// Include PHPMailer
require_once __DIR__ . '/../../assets/libraries/PHPMailer-Master/vendor/autoload.php';

try {
    $mail = new PHPMailer\PHPMailer\PHPMailer(true);

    // Enable debug output
    $mail->SMTPDebug = 2;
    $mail->Debugoutput = function($str, $level) {
        global $debug_output;
        $debug_output .= htmlspecialchars($str) . "\n";
    };

    $debug_output = '';

    // Get config from WoWonder
    $smtp_mode = $wo['config']['smtp_or_mail'] ?? 'mail';

    $config_info = [
        'smtp_or_mail' => $smtp_mode,
        'smtp_host' => $wo['config']['smtp_host'] ?? 'not set',
        'smtp_username' => $wo['config']['smtp_username'] ?? 'not set',
        'smtp_password_encrypted' => !empty($wo['config']['smtp_password']) ? 'YES (encrypted)' : 'NO',
        'smtp_encryption' => $wo['config']['smtp_encryption'] ?? 'not set',
        'smtp_port' => $wo['config']['smtp_port'] ?? 'not set',
        'siteEmail' => $wo['config']['siteEmail'] ?? 'not set',
        'siteName' => $wo['config']['siteName'] ?? 'WorldMates'
    ];

    if ($smtp_mode == "mail") {
        $mail->IsMail();
    } elseif ($smtp_mode == "smtp") {
        $mail->isSMTP();
        $mail->Host = $wo['config']['smtp_host'];
        $mail->SMTPAuth = true;
        $mail->Username = $wo['config']['smtp_username'];

        // Decrypt password using WoWonder function
        if (function_exists('getSMTPPassword')) {
            $mail->Password = getSMTPPassword($wo['config']['smtp_password']);
        } else {
            $mail->Password = $wo['config']['smtp_password'];
        }

        $mail->SMTPSecure = $wo['config']['smtp_encryption'];
        $mail->Port = $wo['config']['smtp_port'];
        $mail->SMTPOptions = array(
            'ssl' => array(
                'verify_peer' => false,
                'verify_peer_name' => false,
                'allow_self_signed' => true
            )
        );
    }

    // Email content
    $mail->IsHTML(true);
    $mail->CharSet = 'UTF-8';
    $mail->setFrom($wo['config']['siteEmail'] ?? 'noreply@worldmates.club', $wo['config']['siteName'] ?? 'WorldMates');
    $mail->addAddress($email);
    $mail->Subject = 'SMTP Test - WorldMates';
    $mail->Body = "
        <h2>SMTP Configuration Test</h2>
        <p>This email was sent using PHPMailer with SMTP settings.</p>
        <p><strong>Test code:</strong> <span style='font-size: 24px; color: #4CAF50;'>{$test_code}</span></p>
        <p><strong>Configuration:</strong></p>
        <ul>
            <li>Mode: {$smtp_mode}</li>
            <li>SMTP Host: " . ($wo['config']['smtp_host'] ?? 'N/A') . "</li>
            <li>SMTP Username: " . ($wo['config']['smtp_username'] ?? 'N/A') . "</li>
            <li>SMTP Encryption: " . ($wo['config']['smtp_encryption'] ?? 'N/A') . "</li>
            <li>SMTP Port: " . ($wo['config']['smtp_port'] ?? 'N/A') . "</li>
        </ul>
        <p><small>Time: " . date('Y-m-d H:i:s') . "</small></p>
    ";

    // Try to send
    $send_result = $mail->send();

    header('Content-Type: application/json');
    echo json_encode([
        'api_status' => 200,
        'message' => 'SMTP test email sent successfully!',
        'config' => $config_info,
        'to_email' => $email,
        'test_code' => $test_code,
        'smtp_debug' => $debug_output,
        'send_result' => $send_result
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    header('Content-Type: application/json');
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Failed to send SMTP test email',
        'config' => $config_info ?? [],
        'exception' => $e->getMessage(),
        'smtp_debug' => $debug_output ?? '',
        'mailer_error' => $mail->ErrorInfo ?? ''
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
}
exit;
