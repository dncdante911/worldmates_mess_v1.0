<?php
// API v2 Router with server_key support
// This file handles requests from WorldMates app that include server_key
// It's a lightweight wrapper that adds quick_register/quick_verify to public endpoints

header_remove('Server');
header("Content-type: application/json");

// Load WoWonder initialization
$assets_init = __DIR__ . '/../assets/init.php';
if (file_exists($assets_init)) {
    require($assets_init);
} else {
    // Fallback: load from site directory
    $site_init = dirname(__DIR__) . '/worldmates-clear-source/site/assets/init.php';
    if (file_exists($site_init)) {
        require($site_init);
    } else {
        die(json_encode(['api_status' => '500', 'error_message' => 'init.php not found']));
    }
}

require(__DIR__ . '/api/v2/init.php');

if (function_exists('decryptConfigData')) {
    decryptConfigData();
}

$wo['loggedin'] = false;
$response_data  = array();
$error_code     = 0;
$error_message  = '';

$type = (!empty($_GET['type'])) ? Wo_Secure($_GET['type'], 0) : false;
$server_key = (!empty($_POST['server_key'])) ? Wo_Secure($_POST['server_key'], 0) : false;

if (empty($type)) {
    $response_data = array(
        'api_status' => '404',
        'errors' => array(
            'error_id' => '1',
            'error_text' => 'Error: 404 API Type not specified'
        )
    );
    echo json_encode($response_data, JSON_PRETTY_PRINT);
    exit();
}

if (empty($server_key)) {
    $response_data = array(
        'api_status' => '404',
        'errors' => array(
            'error_id' => '1',
            'error_text' => 'Error: 404 POST (server_key) not specified, Admin Panel > API Settings > Manage API Server Key'
        )
    );
    echo json_encode($response_data, JSON_PRETTY_PRINT);
    exit();
}

if ($server_key != $wo['config']['widnows_app_api_key']) {
    $response_data = array(
        'api_status' => '404',
        'errors' => array(
            'error_id' => '1',
            'error_text' => 'Error: invalid server key'
        )
    );
    echo json_encode($response_data, JSON_PRETTY_PRINT);
    exit();
}

$api = "api/v2/endpoints/$type.php";

// ✅ КРИТИЧНО: Расширенный список публичных эндпоинтов для WorldMates
$pages_without_access_token = array(
    'get-site-settings',
    'get_site_settings',
    'active_account_sms',
    'auth',
    'regsiter',
    'register',
    'send-reset-password-email',
    'send_reset_password_email',
    'create-account',
    'create_account',
    'social-login',
    'is-active',
    'two-factor',
    'reset_password',
    'validation_user',
    'check_username',
    // ✅ Добавлены эндпоинты быстрой регистрации
    'quick_register',
    'quick_verify',
    'send_verification_code',
    'verify_code',
    'check_mobile_update'
);

$pages_without_loggedin = array(
    'get-site-settings',
    'get_site_settings',
    'active_account_sms',
    'auth',
    'regsiter',
    'register',
    'send-reset-password-email',
    'send_reset_password_email',
    'create-account',
    'create_account',
    'social-login',
    'is-active',
    'two-factor',
    'reset_password',
    'validation_user',
    'check_username',
    // ✅ Добавлены эндпоинты быстрой регистрации
    'quick_register',
    'quick_verify',
    'send_verification_code',
    'verify_code',
    'check_mobile_update'
);

if (!file_exists($api)) {
    $response_data = array(
        'api_status' => '404',
        'errors' => array(
            'error_id' => '1',
            'error_text' => 'Error: 404 API Type Not Found'
        )
    );
    echo json_encode($response_data, JSON_PRETTY_PRINT);
    exit();
}

if (!in_array($type, $pages_without_access_token)) {
    if (empty($_GET['access_token'])) {
        $error_code    = 1;
        $error_message = 'Error: access_token is missing';
    }
}

if (!empty($_GET['access_token'])) {
    $get_user_id_from_access_token = Wo_ValidateAccessToken($_GET['access_token']);
    if (is_numeric($get_user_id_from_access_token) && $get_user_id_from_access_token > 0) {
        $wo['user'] = Wo_UserData($get_user_id_from_access_token);
        if (!empty($wo['user'])) {
            $wo['loggedin'] = true;
            if ($wo['user']['user_id'] < 0 || empty($wo['user']['user_id']) || !is_numeric($wo['user']['user_id']) || Wo_UserActive($wo['user']['username']) === false) {
                $wo['loggedin'] = false;
            }
        }
    }
}

if (!in_array($type, $pages_without_loggedin)) {
    if ($wo['loggedin'] == false && !empty($_GET['access_token'])) {
        $error_code    = 2;
        $error_message = 'Invalid or expired access_token';
    } else if ($wo['loggedin'] == false) {
        $error_code    = 2;
        $error_message = 'Not authorized';
    }
}

if (!empty($error_code)) {
    $response_data = array(
        'api_status' => '404',
        'errors' => array(
            'error_id' => $error_code,
            'error_text' => $error_message
        )
    );
    echo json_encode($response_data, JSON_PRETTY_PRINT);
    exit();
}

if ($wo['loggedin'] == true) {
    $wo['lang'] = Wo_LangsFromDB($wo['user']['language']);
}

require_once "api/v2/functions.php";
require_once "api/v2/endpoints/Exceptions/exceptions.php";
require_once "$api";

if (!empty($error_code)) {
    $response_data = array(
        'api_status' => '400',
        'errors' => array(
            'error_id' => $error_code,
            'error_text' => $error_message
        )
    );
}

echo json_encode($response_data, JSON_PRETTY_PRINT);
exit();
mysqli_close($sqlConnect);
unset($wo);
?>
