<?php
/**
 * API v2 Router
 * Routes requests based on 'type' parameter to appropriate endpoint files
 */

// Load WoWonder framework initialization (from phone API)
require_once(__DIR__ . '/../phone/api_init.php');

// Validate access token and set up $wo['user']
$access_token = $_GET['access_token'] ?? $_POST['access_token'] ?? '';

// List of endpoints that don't require authentication
$public_endpoints = [
    'auth',
    'send_verification_code',
    'verify_code',
    'get_site_settings'
];

// Get type early to check if it's a public endpoint
$type = $_GET['type'] ?? $_POST['type'] ?? '';

// Validate access token for protected endpoints
if (!in_array($type, $public_endpoints)) {
    if (empty($access_token)) {
        http_response_code(401);
        echo json_encode([
            'api_status' => 401,
            'error_message' => 'Missing access_token'
        ]);
        exit;
    }

    // Validate token by looking up in T_APP_SESSIONS table
    $access_token_secure = Wo_Secure($access_token);
    $session_query = mysqli_query($sqlConnect, "
        SELECT s.user_id, s.session_id, s.platform, s.time
        FROM " . T_APP_SESSIONS . " s
        WHERE s.session_id = '{$access_token_secure}'
        LIMIT 1
    ");

    if (!$session_query || mysqli_num_rows($session_query) == 0) {
        http_response_code(401);
        echo json_encode([
            'api_status' => 401,
            'error_message' => 'Invalid or expired access_token'
        ]);
        exit;
    }

    $session_data = mysqli_fetch_assoc($session_query);
    $user_id = (int)$session_data['user_id'];

    // Get full user data using WoWonder function
    $user_data = Wo_UserData($user_id);
    if (empty($user_data) || !isset($user_data['user_id'])) {
        http_response_code(401);
        echo json_encode([
            'api_status' => 401,
            'error_message' => 'User not found'
        ]);
        exit;
    }

    // Set up global $wo['user'] for endpoint files
    $wo['user'] = $user_data;
    $wo['loggedin'] = true;
}

// Type already retrieved above, check if empty
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
    'upload_group_avatar' => 'endpoints/upload_group_avatar.php',
    'generate_group_qr' => 'endpoints/generate_group_qr.php',
    'join_group_by_qr' => 'endpoints/join_group_by_qr.php',
    'mute_group' => 'endpoints/mute_group.php',
    'unmute_group' => 'endpoints/unmute_group.php',
    'pin_group_message' => 'endpoints/pin_group_message.php',
    'unpin_group_message' => 'endpoints/unpin_group_message.php',
    'get_group_members' => 'endpoints/get_group_members.php',
    'delete_group' => 'endpoints/delete_group.php',
    'delete_group_member' => 'endpoints/delete_group_member.php',
    'make_group_admin' => 'endpoints/make_group_admin.php',
    'update-group-data' => 'endpoints/update-group-data.php',
    'get-group-data' => 'endpoints/get-group-data.php',

    // Stories
    'delete_story' => 'endpoints/delete-story.php',
    'get_story_views' => 'endpoints/get_story_views.php',
    'mute_story' => 'endpoints/mute_story.php',
    'create_story_comment' => 'endpoints/create_story_comment.php',
    'get_story_comments' => 'endpoints/get_story_comments.php',
    'delete_story_comment' => 'endpoints/delete_story_comment.php',
    'get_story_reactions' => 'endpoints/get_story_reactions.php',

    // User Settings
    'get-user-data' => 'endpoints/get-user-data.php',
    'update-user-data' => 'endpoints/update-user-data.php',
    'update-privacy-settings' => 'endpoints/update-privacy-settings.php',
    'update-notification-settings' => 'endpoints/update-notification-settings.php',
    'update-profile-picture' => 'endpoints/update-user-data.php', // Handled by update-user-data
    'update-cover-picture' => 'endpoints/update-user-data.php',   // Handled by update-user-data
    'upload_user_avatar' => 'endpoints/upload_user_avatar.php',
    'get-my-groups' => 'endpoints/get-my-groups.php',

    // Cloud Backup & Media Settings
    'get_media_settings' => 'endpoints/get_media_settings.php',
    'update_media_settings' => 'endpoints/update_media_settings.php',
    'get_cloud_backup_settings' => 'endpoints/get_cloud_backup_settings.php',
    'update_cloud_backup_settings' => 'endpoints/update_cloud_backup_settings.php',

    // Channels
    'get_channel_subscribers' => 'endpoints/get_channel_subscribers.php',
    'upload_channel_avatar' => 'endpoints/upload_channel_avatar.php',
    'generate_channel_qr' => 'endpoints/generate_channel_qr.php',
    'mute_channel' => 'endpoints/mute_channel.php',
    'unmute_channel' => 'endpoints/unmute_channel.php',

    // User Rating System (Karma/Trust)
    'rate_user' => 'endpoints/rate_user.php',
    'get_user_rating' => 'endpoints/get_user_rating.php',

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
