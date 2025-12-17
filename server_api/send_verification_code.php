<?php
/**
 * API Endpoint: Send Verification Code
 * Отправляет код верификации на email или SMS
 *
 * POST Parameters:
 * - verification_type: "email" или "phone"
 * - contact_info: email адрес или номер телефона
 * - username: имя пользователя (опционально, для регистрации)
 * - user_id: ID пользователя (опционально, для существующих пользователей)
 */

if ($f == 'send_verification_code') {
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

    // Генерируем код верификации
    $verification_code = rand(100000, 999999);

    if ($verification_type == 'email') {
        // Проверка валидности email
        if (!filter_var($contact_info, FILTER_VALIDATE_EMAIL)) {
            $errors = $error_icon . $wo['lang']['email_invalid_characters'];
        }

        if (empty($errors)) {
            // Если указан username, ищем пользователя
            if ($username) {
                $user_id = Wo_UserIdFromUsername($username);
            }

            if ($user_id) {
                // Сохраняем код в БД
                $email_code = md5($verification_code);
                $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `email_code` = '{$email_code}' WHERE `user_id` = {$user_id}");

                if ($query) {
                    cache($user_id, 'users', 'delete');

                    // Отправляем email с кодом
                    $wo['verification_code'] = $verification_code;
                    $body = "Your verification code is: <strong>{$verification_code}</strong><br><br>This code will expire in 10 minutes.";

                    $send_message_data = array(
                        'from_email' => $wo['config']['siteEmail'],
                        'from_name' => $wo['config']['siteName'],
                        'to_email' => $contact_info,
                        'to_name' => $username ?: 'User',
                        'subject' => 'Email Verification Code',
                        'charSet' => 'utf-8',
                        'message_body' => $body,
                        'is_html' => true
                    );

                    $send = Wo_SendMessage($send_message_data);

                    if ($send) {
                        $data = array(
                            'status' => 200,
                            'message' => $success_icon . 'Verification code sent to your email',
                            'code_length' => 6,
                            'expires_in' => 600 // 10 минут
                        );
                    } else {
                        $errors = $error_icon . 'Failed to send verification email';
                    }
                } else {
                    $errors = $error_icon . 'Database error';
                }
            } else {
                $errors = $error_icon . 'User not found';
            }
        }

    } elseif ($verification_type == 'phone') {
        // Проверка валидности номера телефона
        if (!preg_match('/^\+?\d{10,15}$/', $contact_info)) {
            $errors = $error_icon . $wo['lang']['worng_phone_number'];
        }

        if (empty($errors)) {
            // Если указан username, ищем пользователя
            if ($username) {
                $user_id = Wo_UserIdFromUsername($username);
            }

            if ($user_id) {
                // Сохраняем код в БД
                $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `sms_code` = {$verification_code}, `phone_number` = '{$contact_info}' WHERE `user_id` = {$user_id}");

                if ($query) {
                    cache($user_id, 'users', 'delete');

                    // Отправляем SMS с кодом
                    $message = "Your verification code is: {$verification_code}\n\nThis code will expire in 10 minutes.";

                    if (Wo_SendSMSMessage($contact_info, $message) === true) {
                        $data = array(
                            'status' => 200,
                            'message' => $success_icon . 'Verification code sent via SMS',
                            'code_length' => 6,
                            'expires_in' => 600 // 10 минут
                        );
                    } else {
                        $errors = $error_icon . 'Failed to send SMS';
                    }
                } else {
                    $errors = $error_icon . 'Database error';
                }
            } else {
                $errors = $error_icon . 'User not found';
            }
        }

    } else {
        $errors = $error_icon . 'Invalid verification type';
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
