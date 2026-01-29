<?php
/**
 * Test endpoint to debug API initialization
 */

header('Content-Type: application/json');

$debug = [];
$debug['step'] = 'start';

try {
    // Check if $wo is set
    $debug['wo_isset'] = isset($wo);
    $debug['wo_user_isset'] = isset($wo['user']);

    if (isset($wo['user'])) {
        $debug['user_id'] = $wo['user']['user_id'] ?? 'not set';
        $debug['username'] = $wo['user']['username'] ?? 'not set';
    }

    // Check if $sqlConnect is set
    $debug['sqlConnect_isset'] = isset($sqlConnect);

    // Check if functions exist
    $debug['Wo_Secure_exists'] = function_exists('Wo_Secure');
    $debug['Wo_UserData_exists'] = function_exists('Wo_UserData');
    $debug['Wo_ShareFile_exists'] = function_exists('Wo_ShareFile');
    $debug['Wo_GetMedia_exists'] = function_exists('Wo_GetMedia');

    // Check if table constants exist
    $debug['T_APP_SESSIONS_defined'] = defined('T_APP_SESSIONS');
    $debug['T_CHANNELS_defined'] = defined('T_CHANNELS');
    $debug['T_USERS_defined'] = defined('T_USERS');

    $debug['step'] = 'complete';
    $debug['api_status'] = 200;

} catch (Exception $e) {
    $debug['error'] = $e->getMessage();
    $debug['api_status'] = 500;
}

echo json_encode($debug, JSON_PRETTY_PRINT);
exit();
?>
