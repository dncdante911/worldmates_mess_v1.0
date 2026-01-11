<?php
// Story Reactions API - Standalone endpoint
// Використовує таблицю Wo_StoryReactions для реакцій на stories

// Enable error logging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
$log_dir = $_SERVER['DOCUMENT_ROOT'] . '/api/v2/logs';
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0777, true);
}
$log_file = $log_dir . '/react-story-debug.log';
ini_set('error_log', $log_file);

error_log("=== react_story.php START at " . date('Y-m-d H:i:s') . " ===");
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

// Process reaction
$reactions_types = array_keys($wo['reactions_types']);
if (!empty($_POST['id']) && is_numeric($_POST['id']) && $_POST['id'] > 0 && !empty($_POST['reaction']) && in_array($_POST['reaction'], $reactions_types)) {
	$story_id = Wo_Secure($_POST['id']);
	$reaction_type = Wo_Secure($_POST['reaction']);

	error_log("Processing reaction: story_id={$story_id}, reaction={$reaction_type}, user_id={$wo['user']['user_id']}");

	// Перевіряємо чи існує story
	$story = $db->where('id', $story_id)->getOne(T_USER_STORY);
	if (!$story) {
		error_log("ERROR: Story not found: id={$story_id}");
		$error_code = 6;
		$error_message = 'Story not found';
	} else {
		// Перевіряємо чи вже є реакція від цього користувача
		$is_reacted = $db->where('user_id', $wo['user']['user_id'])
						 ->where('story_id', $story_id)
						 ->getValue('Wo_StoryReactions', 'COUNT(*)');

		error_log("Current reaction status: is_reacted={$is_reacted}");

		if ($is_reacted > 0) {
			// Якщо реакція вже є - видаляємо її
			$db->where('user_id', $wo['user']['user_id'])
			   ->where('story_id', $story_id)
			   ->delete('Wo_StoryReactions');

			error_log("✅ Reaction removed");

			// Оновлюємо лічильник реакцій
			$db->where('id', $story_id)->update(T_USER_STORY, array(
				'reaction_count' => $db->dec(1)
			));

			$response_data = array(
				'api_status' => 200,
				'message' => 'reaction removed'
			);
		} else {
			// Додаємо нову реакцію
			$insert_id = $db->insert('Wo_StoryReactions', array(
				'user_id' => $wo['user']['user_id'],
				'story_id' => $story_id,
				'reaction' => $reaction_type,
				'time' => time()
			));

			if ($insert_id) {
				error_log("✅ Reaction added: insert_id={$insert_id}");

				// Оновлюємо лічильник реакцій
				$db->where('id', $story_id)->update(T_USER_STORY, array(
					'reaction_count' => $db->inc(1)
				));

				$response_data = array(
					'api_status' => 200,
					'message' => 'story reacted'
				);
			} else {
				error_log("ERROR: Failed to insert reaction: " . $db->getLastError());
				$error_code = 7;
				$error_message = 'Failed to add reaction: ' . $db->getLastError();
			}
		}
	}
} else {
	error_log("ERROR: Invalid parameters");
	$error_code = 5;
	$error_message = 'id, reaction can not be empty.';
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
error_log("=== react_story.php END ===");
