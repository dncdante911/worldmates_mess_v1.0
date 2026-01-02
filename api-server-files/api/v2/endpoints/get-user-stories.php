<?php
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
$log_file = $log_dir . '/get-user-stories-debug.log';
ini_set('error_log', $log_file);

error_log("=== get-user-stories.php START at " . date('Y-m-d H:i:s') . " ===");
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

$stories = array();

$options['offset'] = (!empty($_POST['offset']) && is_numeric($_POST['offset']) && $_POST['offset'] > 0 ? Wo_Secure($_POST['offset']) : 0);
$options['limit'] = (!empty($_POST['limit']) && is_numeric($_POST['limit']) && $_POST['limit'] > 0 && $_POST['limit'] <= 50 ? Wo_Secure($_POST['limit']) : 20);
$options['api'] = false;

// Check if user_id is provided - if yes, get stories for specific user only
$requested_user_id = (!empty($_POST['user_id']) && is_numeric($_POST['user_id']) && $_POST['user_id'] > 0 ? Wo_Secure($_POST['user_id']) : 0);

$data_array = array();

if ($requested_user_id > 0) {
    // Get stories for specific user
    $get_stories = Wo_GetStroies(array('user' => $requested_user_id));
    foreach ($get_stories as $key => $story) {
        // Add user data to each story
        if (empty($story['user_data'])) {
            $story['user_data'] = Wo_UserData($story['user_id']);
        }

        foreach ($non_allowed as $key => $value) {
           unset($story['user_data'][$value]);
        }

        if (!empty($story['thumb']['filename'])) {
            $story['thumbnail'] = $story['thumb']['filename'];
            unset($story['thumb']);
        } else {
            $story['thumbnail'] = $story['user_data']['avatar'];
        }

        $story['time_text'] = Wo_Time_Elapsed_String($story['posted']);
        $story['view_count'] = $db->where('story_id',$story['id'])->where('user_id',$story['user_id'],'!=')->getValue(T_STORY_SEEN,'COUNT(*)');

        // Add directly to data_array
        $data_array[] = $story;
    }
} else {
    // Original behavior: Get stories from all friends and flatten them
    $get_all_stories = Wo_GetFriendsStatusAPI($options);
    foreach ($get_all_stories as $key => $one_story) {
        $is_muted = $db->where('user_id',$wo['user']['id'])->where('story_user_id',$one_story['user_id'])->getValue(T_MUTE_STORY,'COUNT(*)');
        if ($is_muted == 0) {
             $get_stories = Wo_GetStroies(array('user' => $one_story['user_id']));
            foreach ($get_stories as $key => $story) {
                // Add user data to each story
                if (empty($story['user_data'])) {
                    $story['user_data'] = Wo_UserData($story['user_id']);
                }

                foreach ($non_allowed as $key => $value) {
                   unset($story['user_data'][$value]);
                }

                if (!empty($story['thumb']['filename'])) {
                    $story['thumbnail'] = $story['thumb']['filename'];
                    unset($story['thumb']);
                } else {
                    $story['thumbnail'] = $story['user_data']['avatar'];
                }

                $story['time_text'] = Wo_Time_Elapsed_String($story['posted']);
                $story['view_count'] = $db->where('story_id',$story['id'])->where('user_id',$story['user_id'],'!=')->getValue(T_STORY_SEEN,'COUNT(*)');

                // Add directly to data_array
                $data_array[] = $story;
            }
        }
    }
}

if ($requested_user_id > 0) {
    error_log("✅ Loaded " . count($data_array) . " user stories for user_id={$requested_user_id}");
} else {
    error_log("✅ Loaded " . count($data_array) . " stories from friends");
}

$response_data = array(
    'api_status' => 200,
    'stories' => $data_array
);

header('Content-Type: application/json');
echo json_encode($response_data);
error_log("=== get-user-stories.php END ===");