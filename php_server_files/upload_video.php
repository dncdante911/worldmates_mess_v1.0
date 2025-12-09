<?php
/**
 * Standalone upload handler for encrypted video uploads from web version
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

    if (!isset($_FILES['video']) || $_FILES['video']['error'] !== UPLOAD_ERR_OK) {
        $response['error'] = 'No video file uploaded or upload error occurred';
        echo json_encode($response);
        exit();
    }

    $file = $_FILES['video'];
    $tmp_name = $file['tmp_name'];
    $original_name = basename($file['name']);
    $file_size = $file['size'];

    // 1GB max for videos
    $max_size = 1024 * 1024 * 1024;
    if ($file_size > $max_size) {
        $response['error'] = 'File too large. Maximum size is 1GB';
        echo json_encode($response);
        exit();
    }

    $allowed_types = array('video/mp4', 'video/webm', 'video/ogg', 'video/avi', 'video/mov', 'video/quicktime');
    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $detected_type = finfo_file($finfo, $tmp_name);
    finfo_close($finfo);

    if (!in_array($detected_type, $allowed_types)) {
        $response['error'] = 'Invalid file type. Allowed: MP4, WEBM, OGG, AVI, MOV';
        echo json_encode($response);
        exit();
    }

    $extension = pathinfo($original_name, PATHINFO_EXTENSION);
    if (empty($extension)) {
        $extension = 'mp4';
    }

    $timestamp = time();
    $random = bin2hex(random_bytes(8));
    $new_filename = "encrypted_vid_{$timestamp}_{$random}.{$extension}";

    $upload_base_dir = dirname(__DIR__) . '/upload/videos';
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

    $relative_path = 'upload/videos/' . $year_month . '/' . $new_filename;
    $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $full_url = $base_url . '/' . $relative_path;

    $is_encrypted = isset($_POST['encrypted']) && $_POST['encrypted'] == '1';

    $response = array(
        'status' => 200,
        'video' => $full_url,
        'video_src' => $relative_path,
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
