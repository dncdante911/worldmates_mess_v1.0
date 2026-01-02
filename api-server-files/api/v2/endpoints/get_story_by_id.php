<?php

// Enable error logging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
$log_dir = $_SERVER['DOCUMENT_ROOT'] . '/api/v2/logs';
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0777, true);
}
$log_file = $log_dir . '/get-story-by-id-debug.log';
ini_set('error_log', $log_file);

error_log("=== get_story_by_id.php START at " . date('Y-m-d H:i:s') . " ===");
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

// Manual authentication via GET access_token (same as create-story.php)
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

if (!empty($_POST['id']) && is_numeric($_POST['id']) && $_POST['id'] > 0) {
	$id = Wo_Secure($_POST['id']);
	error_log("Loading story with ID: {$id}");

	$story = $db->where('id',$id)->getOne(T_USER_STORY);

	$story_images              = Wo_GetStoryMedia($story->id, 'image');
	if (count($story_images) > 0) {
        $story->thumb  = array_shift($story_images);
        $story->images = $story_images;
    }
    $user_data = Wo_UserData($story->user_id);
    foreach ($non_allowed as $key => $value) {
       unset($user_data[$value]);
    }
    $story->user_data = $user_data;
    if (empty($story->thumbnail)) {
        $story->thumb['filename'] = $story->user_data['avatar_org'];
    } else {
        $story->thumb             = array();
        $story->thumb['filename'] = $story->thumbnail;
    }
    $story->thumb['filename'] = Wo_GetMedia($story->thumb['filename']);
    $story->videos            = Wo_GetStoryMedia($story->id, 'video');
    $story->is_owner          = ($story->user_id == $wo['user']['id'] || Wo_IsAdmin() || Wo_IsModerator()) ? true : false;

    $is_viewed = $db->where('story_id',$id)->where('user_id',$wo['user']['user_id'])->getValue(T_STORY_SEEN,'COUNT(*)');
    if ($is_viewed == 0) {
        $db->insert(T_STORY_SEEN,array('story_id' => $id,
                                          'user_id' => $wo['user']['user_id'],
                                          'time' => time()));
        if (!empty($user_data) && $user_data['user_id'] != $wo['user']['user_id']) {
            $notification_data_array = array(
                'recipient_id' => $user_data['user_id'],
                'type' => 'viewed_story',
                'story_id' => $id,
                'text' => '',
                'url' => 'index.php?link1=timeline&u=' . $wo['user']['username'] . '&story=true&story_id=' . $id
            );
            Wo_RegisterNotification($notification_data_array);
        }
    }
    $story->view_count = $db->where('story_id',$id)->where('user_id',$story->user_id,'!=')->getValue(T_STORY_SEEN,'COUNT(*)');
    $story->comment_count = $db->where('story_id',$id)->getValue(T_STORY_COMMENTS,'COUNT(*)');

    error_log("✅ Story loaded successfully: id={$story->id}, user_id={$story->user_id}");
    error_log("Story has " . count($story->images ?? []) . " images and " . count($story->videos ?? []) . " videos");

    $response_data = array(
	    'api_status' => 200,
	    'story' => $story
	);
}
else{
	error_log("ERROR: id parameter is missing or invalid");
	$error_code    = 4;
    $error_message = 'id can not be empty';
    $response_data = array(
        'api_status' => 400,
        'error_code' => $error_code,
        'error_message' => $error_message
    );
}

header('Content-Type: application/json');
echo json_encode($response_data);
error_log("=== get_story_by_id.php END ===");