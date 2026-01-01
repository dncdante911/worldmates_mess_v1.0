<?php

// Enable error logging
error_reporting(E_ALL);
ini_set('log_errors', 1);
$log_dir = '/var/www/www-root/data/www/worldmates.club/api/v2/logs';
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0755, true);
}
ini_set('error_log', $log_dir . '/get-stories-debug.log');
error_log("=== get-stories.php START ===");

$response_data = array('api_status' => 400);

try {
    // Перевіряємо авторизацію
    if (empty($wo['user']) || empty($wo['user']['id'])) {
        $response_data = array(
            'api_status' => 400,
            'errors' => array(
                'error_id' => 1,
                'error_text' => 'Not authorized'
            )
        );
        header('Content-Type: application/json');
        echo json_encode($response_data);
        exit;
    }

$stories = array();

$options['limit'] = (!empty($_POST['limit'])) ? (int) $_POST['limit'] : 35;
$options['api'] = true;

$get_all_stories = Wo_GetFriendsStatus($options);

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

    error_log("Successfully loaded " . count($stories) . " stories");

} catch (Exception $e) {
    // Catch any unexpected errors
    error_log("EXCEPTION in get-stories.php: " . $e->getMessage());
    error_log("Stack trace: " . $e->getTraceAsString());

    $response_data = array(
        'api_status' => 400,
        'errors' => array(
            'error_id' => 99,
            'error_text' => 'Server error: ' . $e->getMessage()
        )
    );
}

// Return JSON response
header('Content-Type: application/json');
echo json_encode($response_data);
error_log("Final response: api_status=" . $response_data['api_status']);
