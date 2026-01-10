<?php
// +------------------------------------------------------------------------+
// | 📦 CLOUD BACKUP: Список всіх бекапів користувача на сервері
// +------------------------------------------------------------------------+

if (empty($_GET['access_token'])) {
    $error_code    = 3;
    $error_message = 'access_token is missing';
    http_response_code(400);
}

if ($error_code == 0) {
    $user_id = Wo_UserIdFromAccessToken($_GET['access_token']);
    if (empty($user_id) || !is_numeric($user_id) || $user_id < 1) {
        $error_code    = 4;
        $error_message = 'Invalid access_token';
        http_response_code(400);
    } else {
        $backup_dir = __DIR__ . '/../../../upload/backups/user_' . $user_id;
        $backups = array();

        if (file_exists($backup_dir)) {
            $files = scandir($backup_dir);

            foreach ($files as $file) {
                if ($file != '.' && $file != '..' && pathinfo($file, PATHINFO_EXTENSION) == 'json') {
                    $file_path = $backup_dir . '/' . $file;
                    $file_size = filesize($file_path);
                    $file_time = filemtime($file_path);

                    $backups[] = array(
                        'filename' => $file,
                        'url' => $wo['config']['site_url'] . '/upload/backups/user_' . $user_id . '/' . $file,
                        'size' => $file_size,
                        'size_mb' => round($file_size / 1024 / 1024, 2),
                        'created_at' => $file_time * 1000, // milliseconds
                        'provider' => 'local_server'
                    );
                }
            }

            // Сортувати за датою (новіші спочатку)
            usort($backups, function($a, $b) {
                return $b['created_at'] - $a['created_at'];
            });
        }

        $data = array(
            'api_status' => 200,
            'backups' => $backups,
            'total_backups' => count($backups)
        );
    }
}
?>
