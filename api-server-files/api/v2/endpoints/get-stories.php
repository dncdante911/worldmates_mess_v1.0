<?php
// +------------------------------------------------------------------------+
// | Get Stories Endpoint (V2 API)
// | Returns active (non-expired) stories from followed users and self
// +------------------------------------------------------------------------+

$response_data = array(
    'api_status' => 400,
);

if (empty($error_code)) {
    $logged_user_id = (int)$wo['user']['user_id'];
    $limit = 35;
    $offset = 0;

    if (!empty($_POST['limit']) && is_numeric($_POST['limit']) && $_POST['limit'] > 0 && $_POST['limit'] <= 50) {
        $limit = (int)$_POST['limit'];
    }
    if (!empty($_POST['offset']) && is_numeric($_POST['offset'])) {
        $offset = (int)$_POST['offset'];
    }

    $current_time = time();

    // Get stories from user's contacts/friends and self
    // Stories that haven't expired yet
    $query = "SELECT s.*, u.username, u.name, u.avatar, u.verified
              FROM Wo_UserStory s
              LEFT JOIN Wo_Users u ON s.user_id = u.user_id
              WHERE (s.expire > {$current_time} OR s.expire = '' OR s.expire IS NULL)
              AND (
                  s.user_id = {$logged_user_id}
                  OR s.user_id IN (
                      SELECT following_id FROM Wo_Followers WHERE follower_id = {$logged_user_id} AND active = '1'
                  )
              )
              ORDER BY s.id DESC
              LIMIT {$offset}, {$limit}";

    $sql_result = mysqli_query($sqlConnect, $query);
    $stories = array();

    if ($sql_result && mysqli_num_rows($sql_result) > 0) {
        while ($story = mysqli_fetch_assoc($sql_result)) {
            // Build media URL
            if (!empty($story['thumbnail'])) {
                $story['thumbnail'] = Wo_GetMedia($story['thumbnail']);
            }

            // Add user data
            $story['user_data'] = array(
                'user_id' => $story['user_id'],
                'username' => $story['username'],
                'name' => $story['name'],
                'avatar' => !empty($story['avatar']) ? Wo_GetMedia($story['avatar']) : '',
                'verified' => $story['verified'] ?? '0',
            );

            // Check if current user has viewed this story
            $view_check = mysqli_query($sqlConnect, "SELECT id FROM Wo_Story_Seen WHERE story_id = {$story['id']} AND user_id = {$logged_user_id} LIMIT 1");
            $story['is_viewed'] = ($view_check && mysqli_num_rows($view_check) > 0) ? true : false;

            // Clean up redundant fields
            unset($story['username'], $story['name'], $story['avatar'], $story['verified']);

            $stories[] = $story;
        }
    }

    // Group stories by user
    $grouped = array();
    foreach ($stories as $story) {
        $uid = $story['user_id'];
        if (!isset($grouped[$uid])) {
            $grouped[$uid] = array(
                'user_id' => $uid,
                'user_data' => $story['user_data'],
                'stories' => array(),
            );
        }
        unset($story['user_data']);
        $grouped[$uid]['stories'][] = $story;
    }

    // Put current user's stories first
    $result = array();
    if (isset($grouped[$logged_user_id])) {
        $result[] = $grouped[$logged_user_id];
        unset($grouped[$logged_user_id]);
    }
    foreach ($grouped as $group) {
        $result[] = $group;
    }

    $response_data = array(
        'api_status' => 200,
        'data' => $result,
    );
}
