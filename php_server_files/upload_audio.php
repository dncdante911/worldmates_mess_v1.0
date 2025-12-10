<?php
/**
 * Standalone upload handler for encrypted audio uploads from web version
 */

error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$response = array('status' => 400, 'error' => 'Unknown error');

try {
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        $response['error'] = 'Method not allowed. Use POST.';
        echo json_encode($response);
        exit();
    }

    if (empty($_POST['server_key']) && empty($_GET['access_token'])) {
        $response['error'] = 'Authentication required';
        echo json_encode($response);
        exit();
    }

    if (!isset($_FILES['audio']) || $_FILES['audio']['error'] !== UPLOAD_ERR_OK) {
        $response['error'] = 'No audio file uploaded or upload error occurred';
        echo json_encode($response);
        exit();
    }

    $file = $_FILES['audio'];
    $tmp_name = $file['tmp_name'];
    $original_name = basename($file['name']);
    $file_size = $file['size'];

    // 100MB max for audio
    $max_size = 100 * 1024 * 1024;
    if ($file_size > $max_size) {
        $response['error'] = 'File too large. Maximum size is 100MB';
        echo json_encode($response);
        exit();
    }

    $allowed_types = array('audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/ogg', 'audio/webm', 'audio/aac', 'audio/m4a', 'audio/mp4', 'audio/x-m4a', 'audio/mp4a-latm', 'audio/3gpp', 'audio/3gpp2', 'video/3gpp', 'video/mp4', 'application/octet-stream');
    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $detected_type = finfo_file($finfo, $tmp_name);
    finfo_close($finfo);

    // Логування для дебагу
    error_log("Audio upload - Detected MIME type: $detected_type, Original filename: $original_name");

    if (!in_array($detected_type, $allowed_types)) {
        $response['error'] = 'Invalid file type. Detected: ' . $detected_type . '. Allowed: MP3, WAV, OGG, AAC, M4A';
        echo json_encode($response);
        exit();
    }

    $extension = pathinfo($original_name, PATHINFO_EXTENSION);
    if (empty($extension)) {
        $extension = 'mp3';
    }

    $timestamp = time();
    $random = bin2hex(random_bytes(8));
    $new_filename = "encrypted_audio_{$timestamp}_{$random}.{$extension}";

    $upload_base_dir = dirname(__DIR__) . '/upload/sounds';
    $year_month = date('Y') . '/' . date('m');
    $upload_dir = $upload_base_dir . '/' . $year_month;

    if (!file_exists($upload_dir)) {
        if (!mkdir($upload_dir, 0755, true)) {
            $response['error'] = 'Failed to create upload directory';
            echo json_encode($response);
            exit();
        }
    }

    $destination = $upload_dir . '/' . $new_filename;
    if (!move_uploaded_file($tmp_name, $destination)) {
        $response['error'] = 'Failed to move uploaded file';
        echo json_encode($response);
        exit();
    }

    $relative_path = 'upload/sounds/' . $year_month . '/' . $new_filename;
    $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $full_url = $base_url . '/' . $relative_path;

    $is_encrypted = isset($_POST['encrypted']) && $_POST['encrypted'] == '1';

    $response = array(
        'status' => 200,
        'audio' => $full_url,
        'audio_src' => $relative_path,
        'encrypted' => $is_encrypted,
        'timestamp' => $timestamp,
        'size' => $file_size,
        'type' => $detected_type
    );

    echo json_encode($response);

} catch (Exception $e) {
    $response = array('status' => 500, 'error' => 'Server error: ' . $e->getMessage());
    echo json_encode($response);
}
?>
