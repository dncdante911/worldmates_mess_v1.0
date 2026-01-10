<?php
// Story Reactions API
// Використовує таблицю Wo_StoryReactions для реакцій на stories

$reactions_types = array_keys($wo['reactions_types']);
if (!empty($_POST['id']) && is_numeric($_POST['id']) && $_POST['id'] > 0 && !empty($_POST['reaction']) && in_array($_POST['reaction'], $reactions_types)) {
	$story_id = Wo_Secure($_POST['id']);
	$reaction_type = Wo_Secure($_POST['reaction']);

	// Перевіряємо чи існує story
	$story = $db->where('id', $story_id)->getOne(T_USER_STORY);
	if (!$story) {
		$error_code = 6;
		$error_message = 'Story not found';
	} else {
		// Перевіряємо чи вже є реакція від цього користувача
		$is_reacted = $db->where('user_id', $wo['user']['user_id'])
						 ->where('story_id', $story_id)
						 ->getValue('Wo_StoryReactions', 'COUNT(*)');

		if ($is_reacted > 0) {
			// Якщо реакція вже є - видаляємо її
			$db->where('user_id', $wo['user']['user_id'])
			   ->where('story_id', $story_id)
			   ->delete('Wo_StoryReactions');

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
			$db->insert('Wo_StoryReactions', array(
				'user_id' => $wo['user']['user_id'],
				'story_id' => $story_id,
				'reaction' => $reaction_type,
				'time' => time()
			));

			// Оновлюємо лічильник реакцій
			$db->where('id', $story_id)->update(T_USER_STORY, array(
				'reaction_count' => $db->inc(1)
			));

			$response_data = array(
				'api_status' => 200,
				'message' => 'story reacted'
			);
		}
	}
} else {
	$error_code = 5;
	$error_message = 'id, reaction can not be empty.';
}