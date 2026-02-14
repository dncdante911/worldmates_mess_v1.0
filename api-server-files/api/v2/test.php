<?php
/**
 * Simple test endpoint to verify API v2 routing works
 * Test URL: https://worldmates.club/api/v2/test.php
 */

header('Content-Type: application/json; charset=UTF-8');

// Log test request
$log_dir = __DIR__ . '/logs';
if (!is_dir($log_dir)) {
    @mkdir($log_dir, 0755, true);
}
$log_file = $log_dir . '/test_' . date('Y-m-d') . '.log';
$timestamp = date('Y-m-d H:i:s');
@file_put_contents($log_file, "[{$timestamp}] TEST endpoint accessed\n", FILE_APPEND);

echo json_encode([
    'api_status' => 200,
    'message' => 'API v2 routing is working!',
    'timestamp' => time(),
    'date' => date('Y-m-d H:i:s'),
    'server_info' => [
        'php_version' => phpversion(),
        'request_method' => $_SERVER['REQUEST_METHOD'] ?? 'UNKNOWN',
        'request_uri' => $_SERVER['REQUEST_URI'] ?? 'UNKNOWN',
        'script_name' => $_SERVER['SCRIPT_NAME'] ?? 'UNKNOWN'
    ],
    'received_data' => [
        'GET' => $_GET,
        'POST' => $_POST
    ]
], JSON_PRETTY_PRINT);
