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

$limit = (!empty($_POST['limit']) && is_numeric($_POST['limit']) && $_POST['limit'] > 0 && $_POST['limit'] <= 50 ? Wo_Secure($_POST['limit']) : 20);
$offset = (!empty($_POST['offset']) && is_numeric($_POST['offset']) && $_POST['offset'] > 0 ? Wo_Secure($_POST['offset']) : 0);

if (empty($_POST['story_id']) || !is_numeric($_POST['story_id']) || $_POST['story_id'] < 1) {
    $error_code    = 3;
    $error_message = 'story_id is missing or invalid';
}

if (empty($error_code)) {
    $story_id = Wo_Secure($_POST['story_id']);

    // Check if story exists
    $story = $db->where('id', $story_id)->getOne(T_USER_STORY);

    if (empty($story)) {
        $error_code    = 4;
        $error_message = 'Story not found';
    }
}

if (empty($error_code)) {
    $comments_data = array();

    $db->where('story_id', $story_id);
    $db->orderBy('time', 'DESC');

    if ($offset > 0) {
        $db->where('id', $offset, '<');
    }

    $comments = $db->get(T_STORY_COMMENTS, $limit);

    if (!empty($comments)) {
        foreach ($comments as $comment) {
            $user_data = Wo_UserData($comment->user_id);
            foreach ($non_allowed as $key => $value) {
                unset($user_data[$value]);
            }

            $comment->user_data = $user_data;
            $comment->offset_id = $comment->id;
            $comments_data[] = $comment;
        }
    }

    $response_data = array(
        'api_status' => 200,
        'comments' => $comments_data,
        'total' => $db->where('story_id', $story_id)->getValue(T_STORY_COMMENTS, 'COUNT(*)')
    );
}
