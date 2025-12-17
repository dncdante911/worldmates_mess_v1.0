<?php
/**
 * API Endpoint: Verify Code
 * Проверяет код верификации
 *
 * POST Parameters:
 * - verification_type: "email" или "phone"
 * - contact_info: email адрес или номер телефона
 * - code: код верификации (6 цифр)
 * - username: имя пользователя (опционально)
 * - user_id: ID пользователя (опционально)
 */

if ($f == 'verify_code') {
    $error_icon = '<i class="fa fa-times" aria-hidden="true"></i> ';
    $success_icon = '<i class="fa fa-check" aria-hidden="true"></i> ';

    // Валидация входных данных
    if (empty($_POST['verification_type']) || empty($_POST['contact_info']) || empty($_POST['code'])) {
        $errors = $error_icon . $wo['lang']['please_check_details'];
    }

    $verification_type = Wo_Secure($_POST['verification_type']);
    $contact_info = Wo_Secure($_POST['contact_info']);
    $code = Wo_Secure($_POST['code']);
    $username = !empty($_POST['username']) ? Wo_Secure($_POST['username']) : null;
    $user_id = !empty($_POST['user_id']) && is_numeric($_POST['user_id']) ? (int)$_POST['user_id'] : null;

    // Находим пользователя
    if ($username) {
        $user_id = Wo_UserIdFromUsername($username);
    }

    if (!$user_id) {
        if ($verification_type == 'email') {
            // Ищем по email
            $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `email` = '{$contact_info}' LIMIT 1");
            if ($query && mysqli_num_rows($query) > 0) {
                $user_data = mysqli_fetch_assoc($query);
                $user_id = $user_data['user_id'];
            }
        } elseif ($verification_type == 'phone') {
            // Ищем по телефону
            $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `phone_number` = '{$contact_info}' LIMIT 1");
            if ($query && mysqli_num_rows($query) > 0) {
                $user_data = mysqli_fetch_assoc($query);
                $user_id = $user_data['user_id'];
            }
        }
    }

    if (!$user_id) {
        $errors = $error_icon . 'User not found';
    }

    if (empty($errors)) {
        $user_data = Wo_UserData($user_id);

        if ($verification_type == 'email') {
            // Проверяем email код
            $email_code = md5($code);

            if ($user_data['email_code'] == $email_code) {
                // Код верный - активируем аккаунт
                $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `active` = '1', `email_code` = '' WHERE `user_id` = {$user_id}");

                if ($query) {
                    cache($user_id, 'users', 'delete');

                    // Автоматический логин после верификации
                    $session = Wo_CreateLoginSession($user_id);

                    $data = array(
                        'api_status' => 200,
                        'message' => $success_icon . 'Email verified successfully',
                        'user_id' => $user_id,
                        'access_token' => $session,
                        'timezone' => 'UTC'
                    );
                } else {
                    $errors = $error_icon . 'Database error';
                }
            } else {
                $errors = $error_icon . $wo['lang']['wrong_confirmation_code'];
            }

        } elseif ($verification_type == 'phone') {
            // Проверяем SMS код
            if ($user_data['sms_code'] == $code) {
                // Код верный - активируем аккаунт
                $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `active` = '1', `sms_code` = '0' WHERE `user_id` = {$user_id}");

                if ($query) {
                    cache($user_id, 'users', 'delete');

                    // Автоматический логин после верификации
                    $session = Wo_CreateLoginSession($user_id);

                    $data = array(
                        'api_status' => 200,
                        'message' => $success_icon . 'Phone verified successfully',
                        'user_id' => $user_id,
                        'access_token' => $session,
                        'timezone' => 'UTC'
                    );
                } else {
                    $errors = $error_icon . 'Database error';
                }
            } else {
                $errors = $error_icon . $wo['lang']['wrong_confirmation_code'];
            }

        } else {
            $errors = $error_icon . 'Invalid verification type';
        }
    }

    // Возвращаем результат
    header("Content-type: application/json");
    if (!empty($errors)) {
        echo json_encode(array(
            'api_status' => 400,
            'errors' => $errors
        ));
    } else {
        echo json_encode($data);
    }
    exit();
}
?>
