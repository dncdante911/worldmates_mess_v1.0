<?php
// Get Story Views API - Standalone endpoint
// Показує хто переглянув story
// +------------------------------------------------------------------------+
// | @author Deen Doughouz (DoughouzForest)
// | @author_url 1: http://www.wowonder.com
// | @author_url 2: http://codecanyon.net/user/doughouzforest
// | @author_email: wowondersocial@gmail.com
// +------------------------------------------------------------------------+
// | WoWonder - The Ultimate Social Networking Platform
// | Copyright (c) 2018 WoWonder. All rights reserved.
// +------------------------------------------------------------------------+

// Enable error logging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
$log_dir = $_SERVER['DOCUMENT_ROOT'] . '/api/v2/logs';
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0777, true);
}
$log_file = $log_dir . '/get-story-views-debug.log';
ini_set('error_log', $log_file);

error_log("=== get_story_views.php START at " . date('Y-m-d H:i:s') . " ===");
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
$root_dir = realpath(__DIR__ . '/' . $depth);
chdir($root_dir);
error_log("Changed directory to: " . getcwd());

require_once($init_path);

chdir($original_dir);
error_log("Restored directory to: " . getcwd());
error_log("✅ init.php loaded successfully");

// Authentication via access_token (POST or GET)
$access_token = !empty($_POST['access_token']) ? Wo_Secure($_POST['access_token']) :
                (!empty($_GET['access_token']) ? Wo_Secure($_GET['access_token']) : '');

if (empty($access_token)) {
    error_log("ERROR: access_token is missing");
    header('Content-Type: application/json');
    echo json_encode([
        'api_status' => 400,
        'error_code' => 1,
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
        'error_code' => 2,
        'error_message' => 'Invalid access_token'
    ]);
    exit;
}

$sql_fetch = mysqli_fetch_assoc($sql_query);
$user_id = $sql_fetch['user_id'];

$wo['user'] = Wo_UserData($user_id);
$wo['loggedin'] = true;

error_log("✅ User authenticated successfully: user_id={$user_id}, username={$wo['user']['username']}");

// Initialize response
$response_data = array('api_status' => 400);
$error_code = 0;
$error_message = '';

// Validate story_id parameter
if (empty($_POST['story_id']) || !is_numeric($_POST['story_id']) || $_POST['story_id'] < 1) {
    error_log("ERROR: story_id is missing or invalid");
    $error_code    = 3;
    $error_message = 'story_id is missing or invalid';
}

if (empty($error_code)) {
    $story_id = Wo_Secure($_POST['story_id']);

    error_log("Loading viewers for story_id: {$story_id}");

    // Check if story exists and belongs to current user
    $story = $db->where('id', $story_id)->getOne(T_USER_STORY);

    if (empty($story)) {
        error_log("ERROR: Story not found: id={$story_id}");
        $error_code    = 4;
        $error_message = 'Story not found';
    } else if ($story->user_id != $wo['user']['user_id']) {
        // Тільки власник story може бачити переглядів
        error_log("ERROR: Access denied. Story owner: {$story->user_id}, current user: {$wo['user']['user_id']}");
        $error_code    = 5;
        $error_message = 'You can only view your own story views';
    }
}

if (empty($error_code)) {
    // Отримуємо список переглядів з таблиці Wo_StorySeen
    $viewers_data = $db->where('story_id', $story_id)
                       ->orderBy('time', 'DESC')
                       ->get(T_STORY_SEEN);

    error_log("Found " . count($viewers_data) . " viewers");

    $viewers = array();
    foreach ($viewers_data as $view) {
        // Пропускаємо перегляди власника story
        if ($view->user_id == $wo['user']['user_id']) {
            continue;
        }

        $viewer_user = Wo_UserData($view->user_id);
        if ($viewer_user) {
            // Видаляємо непотрібні дані
            foreach ($non_allowed as $key => $value) {
                unset($viewer_user[$value]);
            }

            $viewers[] = array(
                'user_id' => $viewer_user['user_id'],
                'name' => $viewer_user['name'],
                'username' => $viewer_user['username'],
                'avatar' => $viewer_user['avatar'],
                'time' => $view->time
            );
        }
    }

    error_log("✅ Loaded " . count($viewers) . " viewers (excluding owner)");

    $response_data = array(
        'api_status' => 200,
        'viewers' => $viewers,
        'count' => count($viewers)
    );
}

// Return error if needed
if ($error_code > 0) {
    $response_data = array(
        'api_status' => 400,
        'error_code' => $error_code,
        'error_message' => $error_message
    );
}

header('Content-Type: application/json');
echo json_encode($response_data);
error_log("=== get_story_views.php END ===");
