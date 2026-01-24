<?php
// +------------------------------------------------------------------------+
// | API Endpoint: Upload User Avatar
// +------------------------------------------------------------------------+
// | Загрузка аватара пользователя
// +------------------------------------------------------------------------+

if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_POST['access_token']);
    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(401);
    } else {
        // Проверить наличие файла
        if (empty($_FILES)) {
            $error_code    = 5;
            $error_message = 'No file uploaded';
            http_response_code(400);
        } else {
            // Поддержка разных имен поля: file, avatar
            $file_field = null;
            if (!empty($_FILES['file']['tmp_name'])) {
                $file_field = 'file';
            } elseif (!empty($_FILES['avatar']['tmp_name'])) {
                $file_field = 'avatar';
            }

            if ($file_field === null) {
                $error_code    = 6;
                $error_message = 'No file in upload (expected "file" or "avatar" field)';
                http_response_code(400);
            } else {
                // Загрузить изображение
                $upload_result = Wo_UploadImage(
                    $_FILES[$file_field]['tmp_name'],
                    $_FILES[$file_field]['name'],
                    'avatar',
                    $_FILES[$file_field]['type'],
                    $user_id
                );

                if ($upload_result) {
                    // Получить обновленные данные пользователя
                    $user_data = Wo_UserData($user_id);

                    if ($user_data) {
                        // Очистить кэш пользователя
                        cache($user_id, 'users', 'delete');

                        $data = array(
                            'api_status' => 200,
                            'message' => 'Avatar uploaded successfully',
                            'avatar' => $user_data['avatar'],
                            'avatar_org' => $user_data['avatar_org'],
                            'avatar_full' => Wo_GetMedia($user_data['avatar'])
                        );
                    } else {
                        $error_code    = 8;
                        $error_message = 'Failed to retrieve user data after upload';
                        http_response_code(500);
                    }
                } else {
                    $error_code    = 7;
                    $error_message = 'Failed to upload image. Please check file type and size.';
                    http_response_code(500);
                }
            }
        }
    }
}
?>
