<?php
/**
 * API Endpoint: Resend Verification Code
 * Повторно отправляет код верификации
 *
 * POST Parameters:
 * - verification_type: "email" или "phone"
 * - contact_info: email адрес или номер телефона
 * - username: имя пользователя (опционально)
 * - user_id: ID пользователя (опционально)
 */

if ($f == 'resend_verification_code') {
    $error_icon = '<i class="fa fa-times" aria-hidden="true"></i> ';
    $success_icon = '<i class="fa fa-check" aria-hidden="true"></i> ';

    // Валидация входных данных
    if (empty($_POST['verification_type']) || empty($_POST['contact_info'])) {
        $errors = $error_icon . $wo['lang']['please_check_details'];
    }

    $verification_type = Wo_Secure($_POST['verification_type']);
    $contact_info = Wo_Secure($_POST['contact_info']);
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

    // Генерируем новый код
    $verification_code = rand(100000, 999999);

    if (empty($errors)) {
        if ($verification_type == 'email') {
            // Проверка валидности email
            if (!filter_var($contact_info, FILTER_VALIDATE_EMAIL)) {
                $errors = $error_icon . $wo['lang']['email_invalid_characters'];
            }

            if (empty($errors)) {
                // Сохраняем новый код в БД
                $email_code = md5($verification_code);
                $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `email_code` = '{$email_code}' WHERE `user_id` = {$user_id}");

                if ($query) {
                    cache($user_id, 'users', 'delete');

                    // Получаем данные пользователя
                    $user_data = Wo_UserData($user_id);

                    // Отправляем email с кодом
                    $wo['verification_code'] = $verification_code;
                    $body = "Your new verification code is: <strong>{$verification_code}</strong><br><br>This code will expire in 10 minutes.";

                    $send_message_data = array(
                        'from_email' => $wo['config']['siteEmail'],
                        'from_name' => $wo['config']['siteName'],
                        'to_email' => $contact_info,
                        'to_name' => $user_data['username'],
                        'subject' => 'Email Verification Code - Resend',
                        'charSet' => 'utf-8',
                        'message_body' => $body,
                        'is_html' => true
                    );

                    $send = Wo_SendMessage($send_message_data);

                    if ($send) {
                        $data = array(
                            'api_status' => 200,
                            'message' => $success_icon . 'Verification code resent to your email',
                            'code_length' => 6,
                            'expires_in' => 600
                        );
                    } else {
                        $errors = $error_icon . 'Failed to resend verification email';
                    }
                } else {
                    $errors = $error_icon . 'Database error';
                }
            }

        } elseif ($verification_type == 'phone') {
            // Проверка валидности номера телефона
            if (!preg_match('/^\+?\d{10,15}$/', $contact_info)) {
                $errors = $error_icon . $wo['lang']['worng_phone_number'];
            }

            if (empty($errors)) {
                // Сохраняем новый код в БД
                $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `sms_code` = {$verification_code} WHERE `user_id` = {$user_id}");

                if ($query) {
                    cache($user_id, 'users', 'delete');

                    // Отправляем SMS с кодом
                    $message = "Your new verification code is: {$verification_code}\n\nThis code will expire in 10 minutes.";

                    if (Wo_SendSMSMessage($contact_info, $message) === true) {
                        $data = array(
                            'api_status' => 200,
                            'message' => $success_icon . 'Verification code resent via SMS',
                            'code_length' => 6,
                            'expires_in' => 600
                        );
                    } else {
                        $errors = $error_icon . 'Failed to resend SMS';
                    }
                } else {
                    $errors = $error_icon . 'Database error';
                }
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
