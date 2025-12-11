<?php
require_once('./init.php');

$response_data = array(
    'api_status' => 400
);

if (!isset($_GET['access_token'])) {
    $error_code    = 1;
    $error_message = 'access_token (GET) is missing';
}

if (empty($error_code)) {
    $wo_user = Wo_UserData($_GET['access_token']);
    if (empty($wo_user)) {
        $error_code    = 2;
        $error_message = 'Invalid access_token';
    } else {
        // ВИПРАВЛЕНО: Правильно мапимо поля з user_id -> id
        $wo['loggedin'] = true;
        $wo['user']     = array(
            'id'       => isset($wo_user['user_id']) ? $wo_user['user_id'] : $wo_user['id'],
            'username' => $wo_user['username'],
            'email'    => isset($wo_user['email']) ? $wo_user['email'] : '',
            'name'     => isset($wo_user['name']) ? $wo_user['name'] : $wo_user['username'],
            'avatar'   => isset($wo_user['avatar']) ? $wo_user['avatar'] : ''
        );

        // Зберігаємо оригінальні дані також
        foreach ($wo_user as $key => $value) {
            if (!isset($wo['user'][$key])) {
                $wo['user'][$key] = $value;
            }
        }
    }
}
