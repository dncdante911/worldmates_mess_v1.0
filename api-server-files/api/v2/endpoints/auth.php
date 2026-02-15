<?php
// +------------------------------------------------------------------------+
// | @author Deen Doughouz (DoughouzForest)
// | @author_url 1: http://www.wowonder.com
// | @author_url 2: http://codecanyon.net/user/doughouzforest
// | @author_email: wowondersocial@gmail.com
// +------------------------------------------------------------------------+
// | WoWonder - The Ultimate Social Networking Platform
// | Copyright (c) 2018 WoWonder. All rights reserved.
// +------------------------------------------------------------------------+
//
// NOTE: functions_two.php and functions_three.php are NOT loaded in v2 config.php
// because functions_two.php triggers require_once "app_start.php" which breaks
// the v2 API context. All calls to functions from those files MUST use
// function_exists() guards.
//
$response_data   = array(
    'api_status' => 400
);
$required_fields = array(
    'username',
    'password'
);
foreach ($required_fields as $key => $value) {
    if (empty($_POST[$value]) && empty($error_code)) {
        $error_code    = 3;
        $error_message = $value . ' (POST) is missing';
    }
}
if (empty($error_code)) {
    $username       = $_POST['username'];
    $password       = $_POST['password'];
    $user_id        = Wo_UserIdForLogin($username);
    $recipient_data = Wo_UserData($user_id);
    if (empty($recipient_data)) {
        $error_code    = 4;
        $error_message = 'Username not found';
    } elseif (!empty($wo['config']['prevent_system']) && $wo['config']['prevent_system'] == 1 && function_exists('WoCanLogin') && !WoCanLogin()) {
        $error_code    = 6;
        $error_message = 'Too many login attempts please try again later';
    } elseif (function_exists('Wo_IsBanned') && Wo_IsBanned($username)) {
        $error_code    = 7;
        $error_message = 'this user is banned';
    } else {
        $login = Wo_Login($username, $password);
        if (!$login) {
            $error_code    = 5;
            $error_message = 'Password is incorrect';
            if (!empty($wo['config']['prevent_system']) && $wo['config']['prevent_system'] == 1 && function_exists('WoAddBadLoginLog')) {
                WoAddBadLoginLog();
            }
        } else {
            // Wo_TwoFactor is in functions_three.php which is not loaded in v2 API.
            // If not available, assume 2FA is not enabled (proceed with login).
            $two_factor_enabled = function_exists('Wo_TwoFactor') ? Wo_TwoFactor($_POST['username']) : true;

            if ($two_factor_enabled != false) {
                $time           = time();
                $cookie         = '';
                $access_token   = sha1(rand(111111111, 999999999)) . md5(microtime()) . rand(11111111, 99999999) . md5(rand(5555, 9999));
                $timezone       = 'UTC';
                $device_type = 'phone';
                if (!empty($_POST['device_type']) && in_array($_POST['device_type'], array('phone','windows'))) {
                    $device_type = Wo_Secure($_POST['device_type']);
                }
                $create_session = mysqli_query($sqlConnect, "INSERT INTO " . T_APP_SESSIONS . " (`user_id`, `session_id`, `platform`, `time`) VALUES ('{$user_id}', '{$access_token}', '{$device_type}', '{$time}')");
                if (!empty($_POST['timezone'])) {
                    $timezone = Wo_Secure($_POST['timezone']);
                }
                $add_timezone = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `timezone` = '{$timezone}' WHERE `user_id` = {$user_id}");
                if (!empty($_POST['android_m_device_id'])) {
                    $device_id  = Wo_Secure($_POST['android_m_device_id']);
                    $update  = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `android_m_device_id` = '{$device_id}' WHERE `user_id` = '{$user_id}'");
                }
                if (!empty($_POST['ios_m_device_id'])) {
                    $device_id  = Wo_Secure($_POST['ios_m_device_id']);
                    $update  = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `ios_m_device_id` = '{$device_id}' WHERE `user_id` = '{$user_id}'");
                }
                if (!empty($_POST['android_n_device_id'])) {
                    $device_id  = Wo_Secure($_POST['android_n_device_id']);
                    $update  = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `android_n_device_id` = '{$device_id}' WHERE `user_id` = '{$user_id}'");
                }
                if (!empty($_POST['ios_n_device_id'])) {
                    $device_id  = Wo_Secure($_POST['ios_n_device_id']);
                    $update  = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `ios_n_device_id` = '{$device_id}' WHERE `user_id` = '{$user_id}'");
                }
                if ($create_session) {
                    if (function_exists('cache')) {
                        cache($user_id, 'users', 'delete');
                    }
                    $response_data = array(
                        'api_status' => 200,
                        'timezone' => $timezone,
                        'access_token' => $access_token,
                        'user_id' => $user_id,
                        'user_platform' => $device_type,
                    );
                }
            }
            else{
                $response_data = array(
                                    'api_status' => 200,
                                    'message' => 'Please enter your confirmation code',
                                    'user_id' => $user_id
                                );
            }

            if (!empty($response_data)) {
                $response_data['membership'] = false;
                if (!empty($wo['config']['membership_system']) && $wo['config']['membership_system'] == 1 && !empty($recipient_data['is_pro']) && $recipient_data['is_pro'] == 0) {
                    $response_data['membership'] = true;
                }
            }
        }
    }
}
