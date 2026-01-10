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

$response_data = array(
    'api_status' => 400
);

if (empty($_POST['story_id']) || !is_numeric($_POST['story_id']) || $_POST['story_id'] < 1) {
    $error_code    = 3;
    $error_message = 'story_id is missing or invalid';
}

if (empty($_POST['text']) || ctype_space($_POST['text'])) {
    $error_code    = 4;
    $error_message = 'comment text is required';
}

if (empty($error_code)) {
    $story_id = Wo_Secure($_POST['story_id']);
    $text = Wo_Secure($_POST['text']);

    // Check if story exists and is not expired
    $story = $db->where('id', $story_id)->getOne(T_USER_STORY);

    if (empty($story)) {
        $error_code    = 5;
        $error_message = 'Story not found';
    } else if ($story->expire < time()) {
        $error_code    = 6;
        $error_message = 'Story has expired';
    }
}

if (empty($error_code)) {
    $comment_data = array(
        'story_id' => $story_id,
        'user_id' => $wo['user']['id'],
        'text' => $text,
        'time' => time()
    );

    $comment_id = $db->insert(T_STORY_COMMENTS, $comment_data);

    if ($comment_id) {
        // Increment comment count
        $db->where('id', $story_id)->update(T_USER_STORY, array(
            'comment_count' => $db->inc(1)
        ));

        // Get the inserted comment with user data
        $comment = $db->where('id', $comment_id)->getOne(T_STORY_COMMENTS);

        if ($comment) {
            $user_data = Wo_UserData($comment->user_id);
            foreach ($non_allowed as $key => $value) {
                unset($user_data[$value]);
            }

            $comment->user_data = $user_data;

            // Send notification to story owner if not commenting on own story
            if ($story->user_id != $wo['user']['id']) {
                $notification_data_array = array(
                    'recipient_id' => $story->user_id,
                    'type' => 'comment_story',
                    'story_id' => $story_id,
                    'text' => '',
                    'url' => 'index.php?link1=timeline&u=' . $wo['user']['username'] . '&story=true&story_id=' . $story_id
                );
                Wo_RegisterNotification($notification_data_array);
            }

            $response_data = array(
                'api_status' => 200,
                'comment' => $comment
            );
        }
    } else {
        $error_code    = 7;
        $error_message = 'Failed to create comment';
    }
}
