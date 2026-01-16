<?php
/**
 * API v2 Router
 * Routes requests based on 'type' parameter to appropriate endpoint files
 */

// Load configuration
require_once(__DIR__ . '/config.php');

// Get request type
$type = $_GET['type'] ?? $_POST['type'] ?? '';

if (empty($type)) {
    http_response_code(400);
    echo json_encode([
        'api_status' => 400,
        'error_message' => 'Missing required parameter: type'
    ]);
    exit;
}

// Route map: type => endpoint file
$routes = [
    // Search endpoints
    'search' => 'endpoints/search.php',
    'recent_search' => 'endpoints/recent_search.php',
    'search_for_posts' => 'endpoints/search_for_posts.php',
    'search_group_messages' => 'endpoints/search_group_messages.php',

    // Authentication
    'auth' => 'endpoints/auth.php',
    'get_chats' => 'endpoints/get_chats.php',
    'get_user_messages' => 'endpoints/get_messages.php',

    // Verification
    'send_verification_code' => 'endpoints/send_verification_code.php',
    'verify_code' => 'endpoints/verify_code.php',

    // Messaging
    'send_message' => 'endpoints/send-message.php',
    'delete_message' => 'endpoints/delete_message.php',
    'read_chats' => 'endpoints/read_chats.php',

    // Groups
    'join_group' => 'endpoints/join-group.php',

    // Stories
    'delete_story' => 'endpoints/delete-story.php',
    'get_story_views' => 'endpoints/get_story_views.php',

    // Other
    'get_invites' => 'endpoints/get_invites.php',
    'get_live_friends' => 'endpoints/get_live_friends.php',
    'get_site_settings' => 'endpoints/get-site-settings.php',
    'subscribe_channel_by_qr' => 'endpoints/subscribe_channel_by_qr.php',
];

// Check if route exists
if (!isset($routes[$type])) {
    http_response_code(404);
    echo json_encode([
        'api_status' => 404,
        'error_message' => "Unknown API type: $type"
    ]);
    exit;
}

$endpoint_file = __DIR__ . '/' . $routes[$type];

// Check if endpoint file exists
if (!file_exists($endpoint_file)) {
    http_response_code(500);
    echo json_encode([
        'api_status' => 500,
        'error_message' => "Endpoint file not found: {$routes[$type]}"
    ]);
    exit;
}

// Include and execute endpoint
require_once($endpoint_file);
