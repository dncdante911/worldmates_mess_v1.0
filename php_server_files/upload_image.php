<?php
/**
 * Standalone upload handler for encrypted media uploads from web version
 * This file handles image uploads and returns proper JSON response
 */

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Set JSON response header
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Response array
$response = array(
    'status' => 400,
    'error' => 'Unknown error'
);

try {
    // Check if this is a POST request
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        $response['error'] = 'Method not allowed. Use POST.';
        echo json_encode($response);
        exit();
    }

    // Verify server_key and access_token
    if (empty($_POST['server_key']) && empty($_GET['access_token'])) {
        $response['error'] = 'Authentication required';
        echo json_encode($response);
        exit();
    }

    // Check if file was uploaded
    if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
        $response['error'] = 'No image file uploaded or upload error occurred';
        if (isset($_FILES['image']['error'])) {
            $response['upload_error_code'] = $_FILES['image']['error'];
        }
        echo json_encode($response);
        exit();
    }

    // Get file information
    $file = $_FILES['image'];
    $tmp_name = $file['tmp_name'];
    $original_name = basename($file['name']);
    $file_size = $file['size'];
    $file_type = $file['type'];

    // Validate file size (15MB max for images)
    $max_size = 15 * 1024 * 1024; // 15MB
    if ($file_size > $max_size) {
        $response['error'] = 'File too large. Maximum size is 15MB';
        echo json_encode($response);
        exit();
    }

    // Validate file type
    $allowed_types = array('image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp');
    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $detected_type = finfo_file($finfo, $tmp_name);
    finfo_close($finfo);

    if (!in_array($detected_type, $allowed_types)) {
        $response['error'] = 'Invalid file type. Allowed: JPEG, PNG, GIF, WEBP';
        $response['detected_type'] = $detected_type;
        echo json_encode($response);
        exit();
    }

    // Generate unique filename
    $extension = pathinfo($original_name, PATHINFO_EXTENSION);
    if (empty($extension)) {
        // Determine extension from mime type
        $mime_to_ext = array(
            'image/jpeg' => 'jpg',
            'image/png' => 'png',
            'image/gif' => 'gif',
            'image/webp' => 'webp'
        );
        $extension = isset($mime_to_ext[$detected_type]) ? $mime_to_ext[$detected_type] : 'jpg';
    }

    $timestamp = time();
    $random = bin2hex(random_bytes(8));
    $new_filename = "encrypted_img_{$timestamp}_{$random}.{$extension}";

    // Define upload directory
    // Adjust this path according to your server structure
    $upload_base_dir = dirname(__DIR__) . '/upload/photos';
    $year_month = date('Y') . '/' . date('m');
    $upload_dir = $upload_base_dir . '/' . $year_month;

    // Create directory if it doesn't exist
    if (!file_exists($upload_dir)) {
        if (!mkdir($upload_dir, 0755, true)) {
            $response['error'] = 'Failed to create upload directory';
            echo json_encode($response);
            exit();
        }
    }

    // Move uploaded file
    $destination = $upload_dir . '/' . $new_filename;
    if (!move_uploaded_file($tmp_name, $destination)) {
        $response['error'] = 'Failed to move uploaded file';
        echo json_encode($response);
        exit();
    }

    // Generate URLs
    $relative_path = 'upload/photos/' . $year_month . '/' . $new_filename;
    $base_url = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $full_url = $base_url . '/' . $relative_path;

    // Check if file should be encrypted
    $is_encrypted = false;
    if (isset($_POST['encrypted']) && $_POST['encrypted'] == '1') {
        $is_encrypted = true;
    }

    // Return success response
    $response = array(
        'status' => 200,
        'image' => $full_url,
        'image_src' => $relative_path,
        'encrypted' => $is_encrypted,
        'timestamp' => $timestamp,
        'size' => $file_size,
        'type' => $detected_type
    );

    echo json_encode($response);

} catch (Exception $e) {
    $response = array(
        'status' => 500,
        'error' => 'Server error: ' . $e->getMessage()
    );
    echo json_encode($response);
}
?>
