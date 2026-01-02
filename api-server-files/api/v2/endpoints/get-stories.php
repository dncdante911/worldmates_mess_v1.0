<?php

// Enable error logging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
$log_dir = $_SERVER['DOCUMENT_ROOT'] . '/api/v2/logs';
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0777, true);
}
$log_file = $log_dir . '/get-stories-debug.log';
ini_set('error_log', $log_file);

error_log("=== get-stories.php START at " . date('Y-m-d H:i:s') . " ===");
error_log("POST: " . print_r($_POST, true));
error_log("GET: " . print_r($_GET, true));

// Load WoWonder initialization
$depth = '../../../';
$init_path = __DIR__ . '/' . $depth . 'assets/init.php';

if (!file_exists($init_path)) {
    error_log("ERROR: init.php not found at: " . $init_path);
    header('Content-Type: application/json');
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Server configuration error'
    ]);
    exit;
}

// Change to project root before loading init.php
$original_dir = getcwd();
$root_dir = dirname(dirname(dirname($init_path)));
chdir($root_dir);
error_log("Changed directory to: " . getcwd());

require_once($init_path);

chdir($original_dir);
error_log("Restored directory to: " . getcwd());
error_log("✅ init.php loaded successfully");

// Manual authentication via GET access_token
$access_token = !empty($_GET['access_token']) ? Wo_Secure($_GET['access_token']) : '';

if (empty($access_token)) {
    error_log("ERROR: access_token is missing");
    header('Content-Type: application/json');
    echo json_encode([
        'api_status' => 400,
        'error_message' => 'access_token is required'
    ]);
    exit;
}

// Authenticate user via access_token
$query = "SELECT user_id FROM " . T_APP_SESSIONS . " WHERE session_id = '" . $access_token . "' LIMIT 1";
$sql_query = mysqli_query($sqlConnect, $query);

if (!$sql_query || mysqli_num_rows($sql_query) == 0) {
    error_log("ERROR: Invalid access_token");
    header('Content-Type: application/json');
    echo json_encode([
        'api_status' => 400,
        'error_message' => 'Invalid access_token'
    ]);
    exit;
}

$sql_fetch = mysqli_fetch_assoc($sql_query);
$user_id = $sql_fetch['user_id'];

$wo['user'] = Wo_UserData($user_id);
$wo['loggedin'] = true;

error_log("✅ User authenticated successfully: user_id={$user_id}, username={$wo['user']['username']}");

$response_data = array('api_status' => 400);

try {
    $stories = array();

    $options['limit'] = (!empty($_POST['limit'])) ? (int) $_POST['limit'] : 35;
    $options['api'] = true;

    error_log("Fetching friends status with limit={$options['limit']}");

    $get_all_stories = Wo_GetFriendsStatus($options);

    error_log("Got " . count($get_all_stories) . " friend stories");

    foreach ($get_all_stories as $key => $one_story) {
        $is_muted = $db->where('user_id',$wo['user']['id'])->where('story_user_id',$one_story['user_id'])->getValue(T_MUTE_STORY,'COUNT(*)');
        if ($is_muted == 0 && $wo['user']['id'] != $one_story['user_id']) {
            $get_stories = Wo_GetStroies(array('id' => $one_story['id']));
            foreach ($get_stories as $key => $story) {
                foreach ($non_allowed as $key => $value) {
                   unset($story['user_data'][$value]);
                }
                if (!empty($story['thumb']['filename'])) {
                    $story['thumbnail'] = $story['thumb']['filename'];
                    unset($story['thumb']);
                } else {
                    $story['thumbnail'] = $story['user_data']['avatar'];
                }
                $stories[] = $story;
            }
        }
    }

    $response_data = array(
        'api_status' => 200,
        'stories' => $stories
    );

    error_log("✅ Successfully loaded " . count($stories) . " stories");

} catch (Exception $e) {
    error_log("EXCEPTION in get-stories.php: " . $e->getMessage());
    error_log("Stack trace: " . $e->getTraceAsString());

    $response_data = array(
        'api_status' => 400,
        'error_message' => 'Server error: ' . $e->getMessage()
    );
}

header('Content-Type: application/json');
echo json_encode($response_data);
error_log("=== get-stories.php END, api_status={$response_data['api_status']} ===");
