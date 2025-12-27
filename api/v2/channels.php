<?php
/**
 * WorldMates Messenger - Channels API
 * Version: 2.0
 *
 * Endpoints:
 * - get_channels: Отримати список каналів
 * - get_channel_details: Деталі каналу
 * - create_channel: Створити канал
 * - update_channel: Оновити канал
 * - delete_channel: Видалити канал
 * - subscribe_channel: Підписатися на канал
 * - unsubscribe_channel: Відписатися від каналу
 * - get_channel_posts: Отримати пости каналу
 * - create_post: Створити пост
 * - update_post: Оновити пост
 * - delete_post: Видалити пост
 * - pin_post: Закріпити пост
 * - unpin_post: Відкріпити пост
 * - get_comments: Отримати коментарі
 * - add_comment: Додати коментар
 * - delete_comment: Видалити коментар
 * - add_post_reaction: Додати реакцію на пост
 * - remove_post_reaction: Видалити реакцію з поста
 * - add_comment_reaction: Додати реакцію на коментар
 * - add_channel_admin: Додати адміна
 * - remove_channel_admin: Видалити адміна
 * - update_channel_settings: Оновити налаштування
 * - get_channel_statistics: Статистика каналу
 * - get_channel_subscribers: Список підписників
 * - upload_channel_avatar: Завантажити аватар
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Якщо OPTIONS request (preflight) - просто повертаємо OK
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Підключення до БД
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../helpers/auth.php';

// Отримання параметрів
$request_body = file_get_contents('php://input');
$data = json_decode($request_body, true);

$type = $data['type'] ?? $_POST['type'] ?? $_GET['type'] ?? '';
$access_token = $data['access_token'] ?? $_POST['access_token'] ?? $_GET['access_token'] ?? '';

// Перевірка токену та отримання user_id
$user_id = validateAccessToken($access_token);

// Відповідь за замовчуванням
$response = [
    'api_status' => 400,
    'error_message' => 'Invalid request type'
];

// ========================================
// МАРШРУТИЗАЦІЯ
// ========================================

switch ($type) {
    case 'get_channels':
        $response = getChannels($user_id, $data);
        break;

    case 'get_channel_details':
        $response = getChannelDetails($user_id, $data);
        break;

    case 'create_channel':
        $response = createChannel($user_id, $data);
        break;

    case 'update_channel':
        $response = updateChannel($user_id, $data);
        break;

    case 'delete_channel':
        $response = deleteChannel($user_id, $data);
        break;

    case 'subscribe_channel':
        $response = subscribeChannel($user_id, $data);
        break;

    case 'unsubscribe_channel':
        $response = unsubscribeChannel($user_id, $data);
        break;

    case 'get_channel_posts':
        $response = getChannelPosts($user_id, $data);
        break;

    case 'create_post':
        $response = createPost($user_id, $data);
        break;

    case 'update_post':
        $response = updatePost($user_id, $data);
        break;

    case 'delete_post':
        $response = deletePost($user_id, $data);
        break;

    case 'pin_post':
        $response = pinPost($user_id, $data);
        break;

    case 'unpin_post':
        $response = unpinPost($user_id, $data);
        break;

    case 'get_comments':
        $response = getComments($user_id, $data);
        break;

    case 'add_comment':
        $response = addComment($user_id, $data);
        break;

    case 'delete_comment':
        $response = deleteComment($user_id, $data);
        break;

    case 'add_post_reaction':
        $response = addPostReaction($user_id, $data);
        break;

    case 'remove_post_reaction':
        $response = removePostReaction($user_id, $data);
        break;

    case 'add_comment_reaction':
        $response = addCommentReaction($user_id, $data);
        break;

    case 'add_channel_admin':
        $response = addChannelAdmin($user_id, $data);
        break;

    case 'remove_channel_admin':
        $response = removeChannelAdmin($user_id, $data);
        break;

    case 'update_channel_settings':
        $response = updateChannelSettings($user_id, $data);
        break;

    case 'get_channel_statistics':
        $response = getChannelStatistics($user_id, $data);
        break;

    case 'get_channel_subscribers':
        $response = getChannelSubscribers($user_id, $data);
        break;

    case 'upload_channel_avatar':
        $response = uploadChannelAvatar($user_id, $data);
        break;

    default:
        $response = [
            'api_status' => 400,
            'error_message' => 'Unknown type: ' . $type
        ];
}

// Відправка відповіді
echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

// ========================================
// ФУНКЦІЇ API
// ========================================

/**
 * Отримати список каналів
 */
function getChannels($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $query = isset($data['query']) ? trim($data['query']) : '';
    $limit = isset($data['limit']) ? (int)$data['limit'] : 50;
    $offset = isset($data['offset']) ? (int)$data['offset'] : 0;
    $subscribed_only = isset($data['subscribed_only']) ? (bool)$data['subscribed_only'] : false;

    try {
        $sql = "SELECT c.*,
                (SELECT COUNT(*) FROM channel_subscribers WHERE channel_id = c.id AND user_id = ?) as is_subscribed,
                (c.owner_id = ?) as is_owner,
                (SELECT COUNT(*) > 0 FROM channel_admins WHERE channel_id = c.id AND user_id = ?) as is_admin
                FROM channels c ";

        $params = [$user_id, $user_id, $user_id];

        if ($subscribed_only) {
            $sql .= " INNER JOIN channel_subscribers cs ON c.id = cs.channel_id AND cs.user_id = ? ";
            $params[] = $user_id;
        }

        $sql .= " WHERE 1=1 ";

        if (!empty($query)) {
            $sql .= " AND (c.name LIKE ? OR c.username LIKE ? OR c.description LIKE ?) ";
            $query_param = '%' . $query . '%';
            $params[] = $query_param;
            $params[] = $query_param;
            $params[] = $query_param;
        }

        // Тільки публічні канали або підписані приватні
        $sql .= " AND (c.is_private = 0 OR (SELECT COUNT(*) FROM channel_subscribers WHERE channel_id = c.id AND user_id = ?) > 0 OR c.owner_id = ?) ";
        $params[] = $user_id;
        $params[] = $user_id;

        $sql .= " ORDER BY c.subscribers_count DESC LIMIT ? OFFSET ? ";
        $params[] = $limit;
        $params[] = $offset;

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $channels = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Форматуємо відповідь
        foreach ($channels as &$channel) {
            $channel['is_subscribed'] = (bool)$channel['is_subscribed'];
            $channel['is_owner'] = (bool)$channel['is_owner'];
            $channel['is_admin'] = (bool)$channel['is_admin'];
        }

        return [
            'api_status' => 200,
            'channels' => $channels,
            'total_count' => count($channels)
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Database error: ' . $e->getMessage()];
    }
}

/**
 * Отримати деталі каналу
 */
function getChannelDetails($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        $stmt = $db->prepare("
            SELECT c.*,
            (SELECT COUNT(*) FROM channel_subscribers WHERE channel_id = c.id AND user_id = ?) as is_subscribed,
            (c.owner_id = ?) as is_owner,
            (SELECT COUNT(*) > 0 FROM channel_admins WHERE channel_id = c.id AND user_id = ?) as is_admin
            FROM channels c
            WHERE c.id = ?
        ");
        $stmt->execute([$user_id, $user_id, $user_id, $channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            return ['api_status' => 404, 'error_message' => 'Channel not found'];
        }

        // Перевірка доступу до приватного каналу
        if ($channel['is_private'] && !$channel['is_subscribed'] && !$channel['is_owner']) {
            return ['api_status' => 403, 'error_message' => 'Access denied to private channel'];
        }

        $channel['is_subscribed'] = (bool)$channel['is_subscribed'];
        $channel['is_owner'] = (bool)$channel['is_owner'];
        $channel['is_admin'] = (bool)$channel['is_admin'];

        // Отримуємо адмінів каналу
        $stmt_admins = $db->prepare("
            SELECT ca.*, u.username, u.avatar
            FROM channel_admins ca
            LEFT JOIN users u ON ca.user_id = u.user_id
            WHERE ca.channel_id = ?
        ");
        $stmt_admins->execute([$channel_id]);
        $admins = $stmt_admins->fetchAll(PDO::FETCH_ASSOC);

        return [
            'api_status' => 200,
            'channel' => $channel,
            'admins' => $admins
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Database error: ' . $e->getMessage()];
    }
}

/**
 * Створити канал
 */
function createChannel($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $name = trim($data['name'] ?? '');
    $description = trim($data['description'] ?? '');
    $username = trim($data['username'] ?? '');
    $is_private = isset($data['is_private']) ? (int)$data['is_private'] : 0;
    $category = trim($data['category'] ?? '');

    // Валідація
    if (empty($name)) {
        return ['api_status' => 400, 'error_message' => 'Channel name is required'];
    }

    if (!$is_private && empty($username)) {
        return ['api_status' => 400, 'error_message' => 'Username is required for public channels'];
    }

    // Перевірка унікальності username
    if (!empty($username)) {
        if (!preg_match('/^[a-zA-Z0-9_]+$/', $username)) {
            return ['api_status' => 400, 'error_message' => 'Username can only contain letters, numbers and underscore'];
        }

        $stmt = $db->prepare("SELECT id FROM channels WHERE username = ?");
        $stmt->execute([$username]);
        if ($stmt->fetch()) {
            return ['api_status' => 400, 'error_message' => 'Username already taken'];
        }
    }

    try {
        $db->beginTransaction();

        $created_time = time();

        $stmt = $db->prepare("
            INSERT INTO channels (
                name, username, description, owner_id, is_private,
                category, created_time, updated_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ");

        $stmt->execute([
            $name,
            $username ?: null,
            $description ?: null,
            $user_id,
            $is_private,
            $category ?: null,
            $created_time,
            $created_time
        ]);

        $channel_id = $db->lastInsertId();

        // Автоматично підписуємо власника на канал
        $stmt_sub = $db->prepare("
            INSERT INTO channel_subscribers (channel_id, user_id, subscribed_time)
            VALUES (?, ?, ?)
        ");
        $stmt_sub->execute([$channel_id, $user_id, $created_time]);

        $db->commit();

        // Отримуємо створений канал
        $stmt_get = $db->prepare("SELECT * FROM channels WHERE id = ?");
        $stmt_get->execute([$channel_id]);
        $channel = $stmt_get->fetch(PDO::FETCH_ASSOC);

        $channel['is_subscribed'] = true;
        $channel['is_owner'] = true;
        $channel['is_admin'] = true;

        return [
            'api_status' => 200,
            'message' => 'Channel created successfully',
            'channel' => $channel
        ];

    } catch (Exception $e) {
        $db->rollBack();
        return ['api_status' => 500, 'error_message' => 'Failed to create channel: ' . $e->getMessage()];
    }
}

/**
 * Оновити канал
 */
function updateChannel($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    // Перевіряємо права
    $stmt = $db->prepare("SELECT owner_id FROM channels WHERE id = ?");
    $stmt->execute([$channel_id]);
    $channel = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$channel || $channel['owner_id'] != $user_id) {
        return ['api_status' => 403, 'error_message' => 'Only channel owner can update channel'];
    }

    try {
        $updates = [];
        $params = [];

        if (isset($data['name'])) {
            $updates[] = "name = ?";
            $params[] = trim($data['name']);
        }

        if (isset($data['description'])) {
            $updates[] = "description = ?";
            $params[] = trim($data['description']);
        }

        if (isset($data['category'])) {
            $updates[] = "category = ?";
            $params[] = trim($data['category']);
        }

        if (empty($updates)) {
            return ['api_status' => 400, 'error_message' => 'No fields to update'];
        }

        $updates[] = "updated_time = ?";
        $params[] = time();
        $params[] = $channel_id;

        $sql = "UPDATE channels SET " . implode(', ', $updates) . " WHERE id = ?";
        $stmt = $db->prepare($sql);
        $stmt->execute($params);

        return [
            'api_status' => 200,
            'message' => 'Channel updated successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to update channel: ' . $e->getMessage()];
    }
}

/**
 * Видалити канал
 */
function deleteChannel($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    // Перевіряємо права
    $stmt = $db->prepare("SELECT owner_id FROM channels WHERE id = ?");
    $stmt->execute([$channel_id]);
    $channel = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$channel || $channel['owner_id'] != $user_id) {
        return ['api_status' => 403, 'error_message' => 'Only channel owner can delete channel'];
    }

    try {
        // Видалення cascade видалить всі пов'язані дані
        $stmt = $db->prepare("DELETE FROM channels WHERE id = ?");
        $stmt->execute([$channel_id]);

        return [
            'api_status' => 200,
            'message' => 'Channel deleted successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to delete channel: ' . $e->getMessage()];
    }
}

/**
 * Підписатися на канал
 */
function subscribeChannel($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        // Перевіряємо, чи існує канал
        $stmt = $db->prepare("SELECT id, is_private FROM channels WHERE id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            return ['api_status' => 404, 'error_message' => 'Channel not found'];
        }

        // Для приватних каналів потрібно запрошення (тут спрощена логіка)
        if ($channel['is_private']) {
            return ['api_status' => 403, 'error_message' => 'Cannot subscribe to private channel without invitation'];
        }

        // Перевіряємо, чи вже підписаний
        $stmt_check = $db->prepare("SELECT id FROM channel_subscribers WHERE channel_id = ? AND user_id = ?");
        $stmt_check->execute([$channel_id, $user_id]);

        if ($stmt_check->fetch()) {
            return ['api_status' => 200, 'message' => 'Already subscribed'];
        }

        // Підписуємось
        $stmt_sub = $db->prepare("INSERT INTO channel_subscribers (channel_id, user_id, subscribed_time) VALUES (?, ?, ?)");
        $stmt_sub->execute([$channel_id, $user_id, time()]);

        return [
            'api_status' => 200,
            'message' => 'Subscribed successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to subscribe: ' . $e->getMessage()];
    }
}

/**
 * Відписатися від каналу
 */
function unsubscribeChannel($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        $stmt = $db->prepare("DELETE FROM channel_subscribers WHERE channel_id = ? AND user_id = ?");
        $stmt->execute([$channel_id, $user_id]);

        return [
            'api_status' => 200,
            'message' => 'Unsubscribed successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to unsubscribe: ' . $e->getMessage()];
    }
}

/**
 * Отримати пости каналу
 */
function getChannelPosts($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);
    $limit = isset($data['limit']) ? (int)$data['limit'] : 30;
    $before_post_id = isset($data['before_post_id']) ? (int)$data['before_post_id'] : 0;

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        // Перевірка доступу до каналу
        $stmt_channel = $db->prepare("
            SELECT c.is_private,
            (SELECT COUNT(*) FROM channel_subscribers WHERE channel_id = c.id AND user_id = ?) as is_subscribed,
            c.owner_id
            FROM channels c
            WHERE c.id = ?
        ");
        $stmt_channel->execute([$user_id, $channel_id]);
        $channel = $stmt_channel->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            return ['api_status' => 404, 'error_message' => 'Channel not found'];
        }

        if ($channel['is_private'] && !$channel['is_subscribed'] && $channel['owner_id'] != $user_id) {
            return ['api_status' => 403, 'error_message' => 'Access denied'];
        }

        // Отримання постів
        $sql = "
            SELECT p.*, u.username as author_name, u.avatar as author_avatar,
            (SELECT JSON_ARRAYAGG(JSON_OBJECT('emoji', emoji, 'count', cnt, 'user_reacted', user_reacted))
             FROM (
                 SELECT emoji, COUNT(*) as cnt,
                 MAX(CASE WHEN user_id = ? THEN 1 ELSE 0 END) as user_reacted
                 FROM channel_post_reactions
                 WHERE post_id = p.id
                 GROUP BY emoji
             ) as reactions_data) as reactions
            FROM channel_posts p
            LEFT JOIN users u ON p.author_id = u.user_id
            WHERE p.channel_id = ?
        ";

        $params = [$user_id, $channel_id];

        if ($before_post_id > 0) {
            $sql .= " AND p.id < ? ";
            $params[] = $before_post_id;
        }

        $sql .= " ORDER BY p.is_pinned DESC, p.created_time DESC LIMIT ? ";
        $params[] = $limit;

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Декодуємо JSON поля
        foreach ($posts as &$post) {
            $post['media'] = !empty($post['media']) ? json_decode($post['media'], true) : null;
            $post['reactions'] = !empty($post['reactions']) ? json_decode($post['reactions'], true) : [];
        }

        return [
            'api_status' => 200,
            'posts' => $posts,
            'total_count' => count($posts)
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Database error: ' . $e->getMessage()];
    }
}

/**
 * Створити пост
 */
function createPost($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);
    $text = trim($data['text'] ?? '');
    $media = $data['media'] ?? null;
    $disable_comments = isset($data['disable_comments']) ? (int)$data['disable_comments'] : 0;

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    if (empty($text) && empty($media)) {
        return ['api_status' => 400, 'error_message' => 'Post must have text or media'];
    }

    try {
        // Перевірка прав (власник або адмін з правом can_post)
        $stmt_check = $db->prepare("
            SELECT c.owner_id,
            (SELECT COUNT(*) FROM channel_admins WHERE channel_id = ? AND user_id = ? AND can_post = 1) as is_admin
            FROM channels c
            WHERE c.id = ?
        ");
        $stmt_check->execute([$channel_id, $user_id, $channel_id]);
        $channel = $stmt_check->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            return ['api_status' => 404, 'error_message' => 'Channel not found'];
        }

        if ($channel['owner_id'] != $user_id && !$channel['is_admin']) {
            return ['api_status' => 403, 'error_message' => 'Only channel admins can create posts'];
        }

        $created_time = time();
        $media_json = !empty($media) ? json_encode($media, JSON_UNESCAPED_UNICODE) : null;

        $stmt = $db->prepare("
            INSERT INTO channel_posts (
                channel_id, author_id, text, media, created_time, is_comments_enabled
            ) VALUES (?, ?, ?, ?, ?, ?)
        ");

        $stmt->execute([
            $channel_id,
            $user_id,
            $text,
            $media_json,
            $created_time,
            !$disable_comments
        ]);

        $post_id = $db->lastInsertId();

        // Отримуємо створений пост
        $stmt_get = $db->prepare("SELECT * FROM channel_posts WHERE id = ?");
        $stmt_get->execute([$post_id]);
        $post = $stmt_get->fetch(PDO::FETCH_ASSOC);

        $post['media'] = !empty($post['media']) ? json_decode($post['media'], true) : null;

        return [
            'api_status' => 200,
            'message' => 'Post created successfully',
            'post' => $post
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to create post: ' . $e->getMessage()];
    }
}

/**
 * Оновити пост
 */
function updatePost($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);
    $text = trim($data['text'] ?? '');

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    try {
        // Перевірка прав
        $stmt = $db->prepare("SELECT author_id FROM channel_posts WHERE id = ?");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$post || $post['author_id'] != $user_id) {
            return ['api_status' => 403, 'error_message' => 'Only post author can edit'];
        }

        $stmt_update = $db->prepare("
            UPDATE channel_posts
            SET text = ?, edited_time = ?, is_edited = 1
            WHERE id = ?
        ");
        $stmt_update->execute([$text, time(), $post_id]);

        return [
            'api_status' => 200,
            'message' => 'Post updated successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to update post: ' . $e->getMessage()];
    }
}

/**
 * Видалити пост
 */
function deletePost($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    try {
        // Перевірка прав (автор або адмін з правом can_delete_posts)
        $stmt = $db->prepare("
            SELECT p.author_id, p.channel_id, c.owner_id,
            (SELECT COUNT(*) FROM channel_admins WHERE channel_id = p.channel_id AND user_id = ? AND can_delete_posts = 1) as is_admin
            FROM channel_posts p
            JOIN channels c ON p.channel_id = c.id
            WHERE p.id = ?
        ");
        $stmt->execute([$user_id, $post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$post) {
            return ['api_status' => 404, 'error_message' => 'Post not found'];
        }

        if ($post['author_id'] != $user_id && $post['owner_id'] != $user_id && !$post['is_admin']) {
            return ['api_status' => 403, 'error_message' => 'Access denied'];
        }

        $stmt_delete = $db->prepare("DELETE FROM channel_posts WHERE id = ?");
        $stmt_delete->execute([$post_id]);

        return [
            'api_status' => 200,
            'message' => 'Post deleted successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to delete post: ' . $e->getMessage()];
    }
}

/**
 * Закріпити пост
 */
function pinPost($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    try {
        // Перевірка прав
        $stmt = $db->prepare("
            SELECT p.channel_id, c.owner_id,
            (SELECT COUNT(*) FROM channel_admins WHERE channel_id = p.channel_id AND user_id = ? AND can_pin_posts = 1) as is_admin
            FROM channel_posts p
            JOIN channels c ON p.channel_id = c.id
            WHERE p.id = ?
        ");
        $stmt->execute([$user_id, $post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$post) {
            return ['api_status' => 404, 'error_message' => 'Post not found'];
        }

        if ($post['owner_id'] != $user_id && !$post['is_admin']) {
            return ['api_status' => 403, 'error_message' => 'Only admins can pin posts'];
        }

        $stmt_update = $db->prepare("UPDATE channel_posts SET is_pinned = 1 WHERE id = ?");
        $stmt_update->execute([$post_id]);

        return [
            'api_status' => 200,
            'message' => 'Post pinned successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to pin post: ' . $e->getMessage()];
    }
}

/**
 * Відкріпити пост
 */
function unpinPost($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    try {
        $stmt = $db->prepare("UPDATE channel_posts SET is_pinned = 0 WHERE id = ?");
        $stmt->execute([$post_id]);

        return [
            'api_status' => 200,
            'message' => 'Post unpinned successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to unpin post: ' . $e->getMessage()];
    }
}

/**
 * Отримати коментарі поста
 */
function getComments($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    try {
        $stmt = $db->prepare("
            SELECT c.*, u.username, u.avatar
            FROM channel_comments c
            LEFT JOIN users u ON c.user_id = u.user_id
            WHERE c.post_id = ?
            ORDER BY c.created_time ASC
        ");
        $stmt->execute([$post_id]);
        $comments = $stmt->fetchAll(PDO::FETCH_ASSOC);

        return [
            'api_status' => 200,
            'comments' => $comments
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Database error: ' . $e->getMessage()];
    }
}

/**
 * Додати коментар
 */
function addComment($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);
    $text = trim($data['text'] ?? '');
    $reply_to_comment_id = isset($data['reply_to_comment_id']) ? (int)$data['reply_to_comment_id'] : null;

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    if (empty($text)) {
        return ['api_status' => 400, 'error_message' => 'Comment text is required'];
    }

    try {
        // Перевірка, чи дозволені коментарі
        $stmt_check = $db->prepare("SELECT is_comments_enabled FROM channel_posts WHERE id = ?");
        $stmt_check->execute([$post_id]);
        $post = $stmt_check->fetch(PDO::FETCH_ASSOC);

        if (!$post) {
            return ['api_status' => 404, 'error_message' => 'Post not found'];
        }

        if (!$post['is_comments_enabled']) {
            return ['api_status' => 403, 'error_message' => 'Comments are disabled for this post'];
        }

        $stmt = $db->prepare("
            INSERT INTO channel_comments (post_id, user_id, text, reply_to_comment_id, created_time)
            VALUES (?, ?, ?, ?, ?)
        ");
        $stmt->execute([$post_id, $user_id, $text, $reply_to_comment_id, time()]);

        $comment_id = $db->lastInsertId();

        return [
            'api_status' => 200,
            'message' => 'Comment added successfully',
            'comment_id' => $comment_id
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to add comment: ' . $e->getMessage()];
    }
}

/**
 * Видалити коментар
 */
function deleteComment($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $comment_id = (int)($data['comment_id'] ?? 0);

    if ($comment_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid comment_id'];
    }

    try {
        // Перевірка прав (автор коментаря або адмін каналу)
        $stmt = $db->prepare("
            SELECT c.user_id, p.channel_id, ch.owner_id
            FROM channel_comments c
            JOIN channel_posts p ON c.post_id = p.id
            JOIN channels ch ON p.channel_id = ch.id
            WHERE c.id = ?
        ");
        $stmt->execute([$comment_id]);
        $comment = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$comment) {
            return ['api_status' => 404, 'error_message' => 'Comment not found'];
        }

        if ($comment['user_id'] != $user_id && $comment['owner_id'] != $user_id) {
            return ['api_status' => 403, 'error_message' => 'Access denied'];
        }

        $stmt_delete = $db->prepare("DELETE FROM channel_comments WHERE id = ?");
        $stmt_delete->execute([$comment_id]);

        return [
            'api_status' => 200,
            'message' => 'Comment deleted successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to delete comment: ' . $e->getMessage()];
    }
}

/**
 * Додати реакцію на пост
 */
function addPostReaction($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);
    $emoji = trim($data['emoji'] ?? '');

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    if (empty($emoji)) {
        return ['api_status' => 400, 'error_message' => 'Emoji is required'];
    }

    try {
        // Перевіряємо, чи вже є така реакція
        $stmt_check = $db->prepare("SELECT id FROM channel_post_reactions WHERE post_id = ? AND user_id = ? AND emoji = ?");
        $stmt_check->execute([$post_id, $user_id, $emoji]);

        if ($stmt_check->fetch()) {
            return ['api_status' => 200, 'message' => 'Reaction already exists'];
        }

        $stmt = $db->prepare("
            INSERT INTO channel_post_reactions (post_id, user_id, emoji, created_time)
            VALUES (?, ?, ?, ?)
        ");
        $stmt->execute([$post_id, $user_id, $emoji, time()]);

        // Оновлюємо лічильник
        $stmt_update = $db->prepare("UPDATE channel_posts SET reactions_count = reactions_count + 1 WHERE id = ?");
        $stmt_update->execute([$post_id]);

        return [
            'api_status' => 200,
            'message' => 'Reaction added successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to add reaction: ' . $e->getMessage()];
    }
}

/**
 * Видалити реакцію з поста
 */
function removePostReaction($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $post_id = (int)($data['post_id'] ?? 0);
    $emoji = trim($data['emoji'] ?? '');

    if ($post_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid post_id'];
    }

    try {
        $stmt = $db->prepare("DELETE FROM channel_post_reactions WHERE post_id = ? AND user_id = ? AND emoji = ?");
        $stmt->execute([$post_id, $user_id, $emoji]);

        if ($stmt->rowCount() > 0) {
            $stmt_update = $db->prepare("UPDATE channel_posts SET reactions_count = GREATEST(0, reactions_count - 1) WHERE id = ?");
            $stmt_update->execute([$post_id]);
        }

        return [
            'api_status' => 200,
            'message' => 'Reaction removed successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to remove reaction: ' . $e->getMessage()];
    }
}

/**
 * Додати реакцію на коментар
 */
function addCommentReaction($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $comment_id = (int)($data['comment_id'] ?? 0);
    $emoji = trim($data['emoji'] ?? '');

    if ($comment_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid comment_id'];
    }

    try {
        $stmt = $db->prepare("
            INSERT IGNORE INTO channel_comment_reactions (comment_id, user_id, emoji, created_time)
            VALUES (?, ?, ?, ?)
        ");
        $stmt->execute([$comment_id, $user_id, $emoji, time()]);

        return [
            'api_status' => 200,
            'message' => 'Reaction added successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to add reaction: ' . $e->getMessage()];
    }
}

/**
 * Додати адміна каналу
 */
function addChannelAdmin($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);
    $admin_user_id = (int)($data['admin_user_id'] ?? 0);
    $role = trim($data['role'] ?? 'admin');

    if ($channel_id <= 0 || $admin_user_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid parameters'];
    }

    try {
        // Перевірка прав (тільки власник)
        $stmt = $db->prepare("SELECT owner_id FROM channels WHERE id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel || $channel['owner_id'] != $user_id) {
            return ['api_status' => 403, 'error_message' => 'Only channel owner can add admins'];
        }

        $stmt_add = $db->prepare("
            INSERT INTO channel_admins (channel_id, user_id, role, added_time, added_by)
            VALUES (?, ?, ?, ?, ?)
        ");
        $stmt_add->execute([$channel_id, $admin_user_id, $role, time(), $user_id]);

        return [
            'api_status' => 200,
            'message' => 'Admin added successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to add admin: ' . $e->getMessage()];
    }
}

/**
 * Видалити адміна каналу
 */
function removeChannelAdmin($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);
    $admin_user_id = (int)($data['admin_user_id'] ?? 0);

    if ($channel_id <= 0 || $admin_user_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid parameters'];
    }

    try {
        // Перевірка прав (тільки власник)
        $stmt = $db->prepare("SELECT owner_id FROM channels WHERE id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel || $channel['owner_id'] != $user_id) {
            return ['api_status' => 403, 'error_message' => 'Only channel owner can remove admins'];
        }

        $stmt_remove = $db->prepare("DELETE FROM channel_admins WHERE channel_id = ? AND user_id = ?");
        $stmt_remove->execute([$channel_id, $admin_user_id]);

        return [
            'api_status' => 200,
            'message' => 'Admin removed successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to remove admin: ' . $e->getMessage()];
    }
}

/**
 * Оновити налаштування каналу
 */
function updateChannelSettings($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        // Перевірка прав
        $stmt = $db->prepare("SELECT owner_id FROM channels WHERE id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel || $channel['owner_id'] != $user_id) {
            return ['api_status' => 403, 'error_message' => 'Only channel owner can update settings'];
        }

        $updates = [];
        $params = [];

        $settings_fields = [
            'allow_comments', 'allow_reactions', 'allow_shares', 'show_statistics',
            'notify_subscribers_new_post', 'auto_delete_posts_days', 'signature_enabled',
            'comments_moderation', 'slow_mode_seconds'
        ];

        foreach ($settings_fields as $field) {
            if (isset($data[$field])) {
                $updates[] = "$field = ?";
                $params[] = $data[$field];
            }
        }

        if (empty($updates)) {
            return ['api_status' => 400, 'error_message' => 'No settings to update'];
        }

        $params[] = $channel_id;
        $sql = "UPDATE channels SET " . implode(', ', $updates) . " WHERE id = ?";
        $stmt_update = $db->prepare($sql);
        $stmt_update->execute($params);

        return [
            'api_status' => 200,
            'message' => 'Settings updated successfully'
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Failed to update settings: ' . $e->getMessage()];
    }
}

/**
 * Отримати статистику каналу
 */
function getChannelStatistics($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        // Базова статистика каналу
        $stmt = $db->prepare("SELECT subscribers_count, posts_count FROM channels WHERE id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            return ['api_status' => 404, 'error_message' => 'Channel not found'];
        }

        // Загальна кількість переглядів
        $stmt_views = $db->prepare("
            SELECT SUM(views_count) as total_views
            FROM channel_posts
            WHERE channel_id = ?
        ");
        $stmt_views->execute([$channel_id]);
        $views = $stmt_views->fetch(PDO::FETCH_ASSOC);

        // Середня кількість переглядів на пост
        $avg_views = $channel['posts_count'] > 0 ?
            round($views['total_views'] / $channel['posts_count']) : 0;

        // Загальна кількість реакцій
        $stmt_reactions = $db->prepare("
            SELECT COUNT(*) as total_reactions
            FROM channel_post_reactions pr
            JOIN channel_posts p ON pr.post_id = p.id
            WHERE p.channel_id = ?
        ");
        $stmt_reactions->execute([$channel_id]);
        $reactions = $stmt_reactions->fetch(PDO::FETCH_ASSOC);

        // Engagement rate (приблизний)
        $engagement_rate = $channel['subscribers_count'] > 0 ?
            round(($reactions['total_reactions'] / $channel['subscribers_count']) * 100, 2) : 0;

        $statistics = [
            'subscribers_count' => (int)$channel['subscribers_count'],
            'posts_count' => (int)$channel['posts_count'],
            'total_views' => (int)$views['total_views'],
            'average_views_per_post' => $avg_views,
            'total_reactions' => (int)$reactions['total_reactions'],
            'engagement_rate' => $engagement_rate
        ];

        return [
            'api_status' => 200,
            'statistics' => $statistics
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Database error: ' . $e->getMessage()];
    }
}

/**
 * Отримати список підписників
 */
function getChannelSubscribers($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    try {
        // Перевірка прав (тільки власник або адмін)
        $stmt_check = $db->prepare("
            SELECT owner_id,
            (SELECT COUNT(*) FROM channel_admins WHERE channel_id = ? AND user_id = ?) as is_admin
            FROM channels WHERE id = ?
        ");
        $stmt_check->execute([$channel_id, $user_id, $channel_id]);
        $channel = $stmt_check->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            return ['api_status' => 404, 'error_message' => 'Channel not found'];
        }

        if ($channel['owner_id'] != $user_id && !$channel['is_admin']) {
            return ['api_status' => 403, 'error_message' => 'Only admins can view subscribers'];
        }

        $stmt = $db->prepare("
            SELECT cs.*, u.username, u.avatar
            FROM channel_subscribers cs
            LEFT JOIN users u ON cs.user_id = u.user_id
            WHERE cs.channel_id = ?
            ORDER BY cs.subscribed_time DESC
        ");
        $stmt->execute([$channel_id]);
        $subscribers = $stmt->fetchAll(PDO::FETCH_ASSOC);

        return [
            'api_status' => 200,
            'subscribers' => $subscribers
        ];

    } catch (Exception $e) {
        return ['api_status' => 500, 'error_message' => 'Database error: ' . $e->getMessage()];
    }
}

/**
 * Завантажити аватар каналу
 */
function uploadChannelAvatar($user_id, $data) {
    global $db;

    if (!$user_id) {
        return ['api_status' => 401, 'error_message' => 'Unauthorized'];
    }

    $channel_id = (int)($data['channel_id'] ?? 0);

    if ($channel_id <= 0) {
        return ['api_status' => 400, 'error_message' => 'Invalid channel_id'];
    }

    // Тут має бути логіка завантаження файлу
    // Для простоти повертаємо заглушку

    return [
        'api_status' => 200,
        'message' => 'Avatar upload not implemented yet',
        'avatar_url' => null
    ];
}

// ========================================
// ДОПОМІЖНІ ФУНКЦІЇ
// ========================================

/**
 * Валідація access token
 */
function validateAccessToken($access_token) {
    global $db;

    if (empty($access_token)) {
        return null;
    }

    try {
        $stmt = $db->prepare("SELECT user_id FROM Wo_Sessions WHERE session_id = ?");
        $stmt->execute([$access_token]);
        $session = $stmt->fetch(PDO::FETCH_ASSOC);

        return $session ? (int)$session['user_id'] : null;
    } catch (Exception $e) {
        return null;
    }
}
