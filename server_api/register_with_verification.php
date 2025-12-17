<?php
/**
 * API Endpoint: Register with Verification
 * Регистрация пользователя с поддержкой верификации через SMS/Email
 *
 * POST Parameters:
 * - username: имя пользователя
 * - password: пароль
 * - confirm_password: подтверждение пароля
 * - email: email (если регистрация через email)
 * - phone_number: номер телефона (если регистрация через телефон)
 * - verification_type: "email" или "phone"
 * - gender: пол (male/female)
 */

if ($f == 'register_with_verification') {
    $error_icon = '<i class="fa fa-times" aria-hidden="true"></i> ';
    $success_icon = '<i class="fa fa-check" aria-hidden="true"></i> ';

    // Очистка существующих сессий
    if (!empty($_SESSION['user_id'])) {
        $_SESSION['user_id'] = '';
        unset($_SESSION['user_id']);
    }
    if (!empty($_COOKIE['user_id'])) {
        $_COOKIE['user_id'] = '';
        unset($_COOKIE['user_id']);
        setcookie('user_id', '', -1);
        setcookie('user_id', '', -1, '/');
    }

    // Валидация входных данных
    if (empty($_POST['username']) || empty($_POST['password']) || empty($_POST['confirm_password']) || empty($_POST['verification_type'])) {
        $errors = $error_icon . $wo['lang']['please_check_details'];
    }

    $username = Wo_Secure($_POST['username']);
    $password = $_POST['password'];
    $confirm_password = $_POST['confirm_password'];
    $verification_type = Wo_Secure($_POST['verification_type']);
    $gender = !empty($_POST['gender']) ? Wo_Secure($_POST['gender']) : 'male';
    $email = !empty($_POST['email']) ? Wo_Secure($_POST['email'], 0) : '';
    $phone_number = !empty($_POST['phone_number']) ? Wo_Secure($_POST['phone_number']) : '';

    // Проверка типа верификации и соответствующих данных
    if ($verification_type == 'email' && empty($email)) {
        $errors = $error_icon . $wo['lang']['please_check_details'];
    }
    if ($verification_type == 'phone' && empty($phone_number)) {
        $errors = $error_icon . $wo['lang']['worng_phone_number'];
    }

    // Валидация username
    if (empty($errors)) {
        $is_exist = Wo_IsNameExist($username, 0);
        if (in_array(true, $is_exist)) {
            $errors = $error_icon . $wo['lang']['username_exists'];
        }
        if (Wo_IsBanned($username)) {
            $errors = $error_icon . $wo['lang']['username_is_banned'];
        }
        if (in_array($username, $wo['site_pages'])) {
            $errors = $error_icon . $wo['lang']['username_invalid_characters'];
        }
        if (strlen($username) < 5 OR strlen($username) > 32) {
            $errors = $error_icon . $wo['lang']['username_characters_length'];
        }
        if (!preg_match('/^[\w]+$/', $username)) {
            $errors = $error_icon . $wo['lang']['username_invalid_characters'];
        }
    }

    // Валидация email
    if (empty($errors) && $verification_type == 'email') {
        if (Wo_EmailExists($email) === true) {
            $errors = $error_icon . $wo['lang']['email_exists'];
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $errors = $error_icon . $wo['lang']['email_invalid_characters'];
        }
        if (Wo_IsBanned($email)) {
            $errors = $error_icon . $wo['lang']['email_is_banned'];
        }
    }

    // Валидация телефона
    if (empty($errors) && $verification_type == 'phone') {
        if (!preg_match('/^\+?\d{10,15}$/', $phone_number)) {
            $errors = $error_icon . $wo['lang']['worng_phone_number'];
        }
        if (Wo_PhoneExists($phone_number) === true) {
            $errors = $error_icon . $wo['lang']['phone_already_used'];
        }
    }

    // Валидация пароля
    if (empty($errors)) {
        if (strlen($password) < 6) {
            $errors = $error_icon . $wo['lang']['password_short'];
        }
        if ($password != $confirm_password) {
            $errors = $error_icon . $wo['lang']['password_mismatch'];
        }
    }

    // Регистрация пользователя
    if (empty($errors)) {
        // Генерируем код верификации
        $verification_code = rand(100000, 999999);
        $email_code = md5($verification_code);

        // Подготавливаем данные для регистрации
        $re_data = array(
            'username' => $username,
            'password' => $password,
            'email_code' => $email_code,
            'src' => 'app',
            'gender' => $gender,
            'lastseen' => time(),
            'active' => '0', // Требуется верификация
            'birthday' => '0000-00-00'
        );

        // Добавляем email или телефон
        if ($verification_type == 'email') {
            $re_data['email'] = $email;
        } elseif ($verification_type == 'phone') {
            $re_data['phone_number'] = $phone_number;
            $re_data['email'] = $username . '@temp.worldmates.club'; // Временный email
            $re_data['sms_code'] = $verification_code;
        }

        // Устанавливаем дефолтный аватар
        if ($gender == 'female') {
            $re_data['avatar'] = "upload/photos/f-avatar.jpg";
        } else {
            $re_data['avatar'] = "upload/photos/d-avatar.jpg";
        }

        // Создаем пользователя
        $register = Wo_RegisterUser($re_data, false);

        if ($register === true) {
            $user_id = Wo_UserIdFromUsername($username);

            if ($user_id) {
                // Отправляем код верификации
                if ($verification_type == 'email') {
                    // Отправляем Email
                    $wo['verification_code'] = $verification_code;
                    $body = "Welcome to {$wo['config']['siteName']}!<br><br>Your verification code is: <strong>{$verification_code}</strong><br><br>This code will expire in 10 minutes.";

                    $send_message_data = array(
                        'from_email' => $wo['config']['siteEmail'],
                        'from_name' => $wo['config']['siteName'],
                        'to_email' => $email,
                        'to_name' => $username,
                        'subject' => 'Email Verification Code',
                        'charSet' => 'utf-8',
                        'message_body' => $body,
                        'is_html' => true
                    );

                    $send = Wo_SendMessage($send_message_data);

                    if ($send) {
                        $data = array(
                            'api_status' => 200,
                            'message' => $success_icon . 'Registration successful! Verification code sent to your email',
                            'user_id' => $user_id,
                            'username' => $username,
                            'verification_type' => 'email',
                            'contact_info' => $email,
                            'code_length' => 6,
                            'expires_in' => 600
                        );
                    } else {
                        $errors = $error_icon . 'Failed to send verification email';
                    }

                } elseif ($verification_type == 'phone') {
                    // Отправляем SMS
                    $message = "Welcome to {$wo['config']['siteName']}!\n\nYour verification code is: {$verification_code}\n\nThis code will expire in 10 minutes.";

                    if (Wo_SendSMSMessage($phone_number, $message) === true) {
                        $data = array(
                            'api_status' => 200,
                            'message' => $success_icon . 'Registration successful! Verification code sent via SMS',
                            'user_id' => $user_id,
                            'username' => $username,
                            'verification_type' => 'phone',
                            'contact_info' => $phone_number,
                            'code_length' => 6,
                            'expires_in' => 600
                        );
                    } else {
                        $errors = $error_icon . 'Failed to send SMS';
                    }
                }
            } else {
                $errors = $error_icon . 'Registration failed';
            }
        } else {
            $errors = $error_icon . 'Registration failed';
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
