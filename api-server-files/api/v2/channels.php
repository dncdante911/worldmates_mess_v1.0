<?php
/**
 * WorldMates Messenger - Channels API Endpoint
 * Версія: 2.0 (адаптована до існуючої БД Wondertage/WO)
 *
 * Використовує існуючі таблиці:
 * - Wo_GroupChat (type='channel')
 * - Wo_GroupChatUsers (підписники)
 * - Wo_Messages (пости)
 * - wo_reactions (реакції)
 * - Wo_MessageComments (коментарі)
 */

require_once(__DIR__ . '/config.php');

header('Content-Type: application/json');

// Отримуємо дані запиту
$request_method = $_SERVER['REQUEST_METHOD'];

// Підтримка як JSON, так і form-encoded даних
$content_type = $_SERVER['CONTENT_TYPE'] ?? '';
if (strpos($content_type, 'application/json') !== false) {
    $data = json_decode(file_get_contents('php://input'), true) ?? [];
} else {
    // Для application/x-www-form-urlencoded використовуємо $_POST
    $data = $_POST;
}

// Access token для автентифікації
$access_token = $data['access_token'] ?? $_GET['access_token'] ?? null;

if (!$access_token) {
    echo json_encode(['api_status' => 400, 'error_message' => 'Access token is required']);
    exit;
}

// Валідація токену та отримання user_id
$user_id = validateAccessToken($db, $access_token);
if (!$user_id) {
    echo json_encode(['api_status' => 401, 'error_message' => 'Invalid access token']);
    exit;
}

// Роутинг методів
// Android клієнт надсилає 'type', веб клієнт може надсилати 'action'
$action = $data['type'] ?? $data['action'] ?? $_GET['type'] ?? $_GET['action'] ?? '';

// Мапінг старих type на нові action для сумісності з Android
$type_mapping = [
    'get_subscribed' => 'get_channels',
    'get_list' => 'get_channels',
    'get_all' => 'get_channels',
];

// Якщо є маппінг, використовуємо його
$mapped_action = $type_mapping[$action] ?? $action;

// Якщо використовується get_subscribed, додаємо filter
if ($action === 'get_subscribed') {
    $data['filter'] = 'subscribed';
} elseif ($action === 'get_list' || $action === 'get_all') {
    $data['filter'] = 'all';
}

try {
    switch ($mapped_action) {
        // ============================================
        // CRUD операції з каналами
        // ============================================
        case 'get_channels':
            echo json_encode(getChannels($db, $user_id, $data));
            break;

        case 'get_channel_details':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(getChannelDetails($db, $user_id, $channel_id));
            break;

        case 'create_channel':
            echo json_encode(createChannel($db, $user_id, $data));
            break;

        case 'update_channel':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(updateChannel($db, $user_id, $channel_id, $data));
            break;

        case 'delete_channel':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(deleteChannel($db, $user_id, $channel_id));
            break;

        // ============================================
        // Підписки
        // ============================================
        case 'subscribe_channel':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(subscribeChannel($db, $user_id, $channel_id));
            break;

        case 'unsubscribe_channel':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(unsubscribeChannel($db, $user_id, $channel_id));
            break;

        case 'get_channel_subscribers':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(getChannelSubscribers($db, $user_id, $channel_id));
            break;

        // ============================================
        // Пости
        // ============================================
        case 'get_channel_posts':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(getChannelPosts($db, $user_id, $channel_id, $data));
            break;

        case 'create_post':
            echo json_encode(createPost($db, $user_id, $data));
            break;

        case 'update_post':
            $post_id = $data['post_id'] ?? null;
            if (!$post_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
                exit;
            }
            echo json_encode(updatePost($db, $user_id, $post_id, $data));
            break;

        case 'delete_post':
            $post_id = $data['post_id'] ?? null;
            if (!$post_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
                exit;
            }
            echo json_encode(deletePost($db, $user_id, $post_id));
            break;

        case 'pin_post':
            $post_id = $data['post_id'] ?? null;
            if (!$post_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
                exit;
            }
            echo json_encode(pinPost($db, $user_id, $post_id, true));
            break;

        case 'unpin_post':
            $post_id = $data['post_id'] ?? null;
            if (!$post_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
                exit;
            }
            echo json_encode(pinPost($db, $user_id, $post_id, false));
            break;

        // ============================================
        // Коментарі
        // ============================================
        case 'get_comments':
            $post_id = $data['post_id'] ?? null;
            if (!$post_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
                exit;
            }
            echo json_encode(getComments($db, $user_id, $post_id));
            break;

        case 'add_comment':
            echo json_encode(addComment($db, $user_id, $data));
            break;

        case 'delete_comment':
            $comment_id = $data['comment_id'] ?? null;
            if (!$comment_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'comment_id is required']);
                exit;
            }
            echo json_encode(deleteComment($db, $user_id, $comment_id));
            break;

        // ============================================
        // Реакції
        // ============================================
        case 'add_post_reaction':
            echo json_encode(addPostReaction($db, $user_id, $data));
            break;

        case 'remove_post_reaction':
            echo json_encode(removePostReaction($db, $user_id, $data));
            break;

        case 'add_comment_reaction':
            echo json_encode(addCommentReaction($db, $user_id, $data));
            break;

        // ============================================
        // Адміністрування
        // ============================================
        case 'add_channel_admin':
            echo json_encode(addChannelAdmin($db, $user_id, $data));
            break;

        case 'remove_channel_admin':
            echo json_encode(removeChannelAdmin($db, $user_id, $data));
            break;

        // ============================================
        // Налаштування та статистика
        // ============================================
        case 'update_channel_settings':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(updateChannelSettings($db, $user_id, $channel_id, $data));
            break;

        case 'get_channel_statistics':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(getChannelStatistics($db, $user_id, $channel_id));
            break;

        case 'upload_channel_avatar':
            $channel_id = $data['channel_id'] ?? null;
            if (!$channel_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
                exit;
            }
            echo json_encode(uploadChannelAvatar($db, $user_id, $channel_id, $_FILES));
            break;

        default:
            echo json_encode(['api_status' => 400, 'error_message' => 'Invalid action']);
    }
} catch (Exception $e) {
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Internal server error: ' . $e->getMessage()
    ]);
}

// ============================================
// ФУНКЦІЇ
// ============================================
// Функція validateAccessToken оголошена в config.php

/**
 * Отримати список каналів
 */
function getChannels($db, $user_id, $data) {
    $filter = $data['filter'] ?? 'all'; // 'all', 'subscribed', 'owned'
    $limit = isset($data['limit']) ? (int)$data['limit'] : 20;
    $offset = isset($data['offset']) ? (int)$data['offset'] : 0;

    $where = "WHERE type = 'channel'";
    $params = [];

    if ($filter === 'subscribed') {
        $where .= " AND group_id IN (SELECT group_id FROM Wo_GroupChatUsers WHERE user_id = ?)";
        $params[] = $user_id;
    } elseif ($filter === 'owned') {
        $where .= " AND user_id = ?";
        $params[] = $user_id;
    }

    $stmt = $db->prepare("
        SELECT
            g.group_id AS id,
            g.group_name AS name,
            g.username,
            g.description,
            g.avatar AS avatar_url,
            g.user_id AS owner_id,
            g.is_private,
            g.is_verified,
            g.subscribers_count,
            g.posts_count,
            g.category,
            g.settings,
            g.time AS created_time,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS is_subscribed,
            (SELECT role FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS user_role
        FROM Wo_GroupChat g
        $where
        ORDER BY g.subscribers_count DESC, g.group_id DESC
        LIMIT ? OFFSET ?
    ");

    $params = array_merge([$user_id, $user_id], $params, [$limit, $offset]);
    $stmt->execute($params);
    $channels = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Парсимо settings
    foreach ($channels as &$channel) {
        $channel['is_subscribed'] = (bool)$channel['is_subscribed'];
        $channel['is_verified'] = (bool)$channel['is_verified'];
        $channel['is_admin'] = in_array($channel['user_role'], ['owner', 'admin', 'moderator']);
        $channel['settings'] = json_decode($channel['settings'] ?? '{}', true);
        unset($channel['user_role']);
    }

    return [
        'api_status' => 200,
        'channels' => $channels,
        'total' => count($channels)
    ];
}

/**
 * Отримати деталі каналу
 */
function getChannelDetails($db, $user_id, $channel_id) {
    $stmt = $db->prepare("
        SELECT
            g.group_id AS id,
            g.group_name AS name,
            g.username,
            g.description,
            g.avatar AS avatar_url,
            g.user_id AS owner_id,
            g.is_private,
            g.is_verified,
            g.subscribers_count,
            g.posts_count,
            g.category,
            g.settings,
            g.time AS created_time,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS is_subscribed,
            (SELECT role FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS user_role
        FROM Wo_GroupChat g
        WHERE g.group_id = ? AND g.type = 'channel'
    ");
    $stmt->execute([$user_id, $user_id, $channel_id]);
    $channel = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$channel) {
        return ['api_status' => 404, 'error_message' => 'Channel not found'];
    }

    // Парсимо settings
    $channel['is_subscribed'] = (bool)$channel['is_subscribed'];
    $channel['is_verified'] = (bool)$channel['is_verified'];
    $channel['is_admin'] = in_array($channel['user_role'], ['owner', 'admin', 'moderator']);
    $channel['settings'] = json_decode($channel['settings'] ?? '{}', true);
    unset($channel['user_role']);

    return [
        'api_status' => 200,
        'channel' => $channel
    ];
}

/**
 * Створити новий канал
 */
function createChannel($db, $user_id, $data) {
    $name = trim($data['name'] ?? '');
    $username = trim($data['username'] ?? '');
    $description = trim($data['description'] ?? '');
    $is_private = isset($data['is_private']) ? ($data['is_private'] ? '1' : '0') : '0';
    $category = trim($data['category'] ?? '');

    // Валідація
    if (empty($name)) {
        return ['api_status' => 400, 'error_message' => 'Channel name is required'];
    }

    if ($is_private === '0' && empty($username)) {
        return ['api_status' => 400, 'error_message' => 'Username is required for public channels'];
    }

    // Перевірка унікальності username
    if (!empty($username)) {
        if (!preg_match('/^[a-zA-Z0-9_]+$/', $username)) {
            return ['api_status' => 400, 'error_message' => 'Username can only contain letters, numbers and underscore'];
        }

        $stmt = $db->prepare("SELECT group_id FROM Wo_GroupChat WHERE username = ?");
        $stmt->execute([$username]);
        if ($stmt->fetch()) {
            return ['api_status' => 400, 'error_message' => 'Username already taken'];
        }
    }

    // Дефолтні налаштування
    $settings = json_encode([
        'allow_comments' => true,
        'allow_reactions' => true,
        'allow_shares' => true,
        'show_statistics' => true,
        'notify_subscribers_new_post' => true,
        'auto_delete_posts_days' => null,
        'signature_enabled' => false,
        'comments_moderation' => false,
        'slow_mode_seconds' => 0
    ]);

    try {
        $db->beginTransaction();

        // Створюємо канал
        $stmt = $db->prepare("
            INSERT INTO Wo_GroupChat
            (user_id, group_name, username, description, is_private, category, type, settings, time, subscribers_count, posts_count, is_verified)
            VALUES (?, ?, ?, ?, ?, ?, 'channel', ?, ?, 0, 0, 0)
        ");

        $time = time();
        $stmt->execute([
            $user_id,
            $name,
            $username ?: null,
            $description ?: null,
            $is_private,
            $category ?: null,
            $settings,
            $time
        ]);

        $channel_id = $db->lastInsertId();

        // Автоматично підписуємо власника як owner
        $stmt_sub = $db->prepare("
            INSERT INTO Wo_GroupChatUsers (user_id, group_id, role, active, last_seen)
            VALUES (?, ?, 'owner', '1', ?)
        ");
        $stmt_sub->execute([$user_id, $channel_id, $time]);

        $db->commit();

        // Повертаємо створений канал
        return getChannelDetails($db, $user_id, $channel_id);

    } catch (Exception $e) {
        $db->rollBack();
        return ['api_status' => 500, 'error_message' => 'Failed to create channel: ' . $e->getMessage()];
    }
}

/**
 * Оновити канал
 */
function updateChannel($db, $user_id, $channel_id, $data) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to edit this channel'];
    }

    $updates = [];
    $params = [];

    if (isset($data['name'])) {
        $updates[] = "group_name = ?";
        $params[] = trim($data['name']);
    }

    if (isset($data['description'])) {
        $updates[] = "description = ?";
        $params[] = trim($data['description']);
    }

    if (isset($data['username'])) {
        $username = trim($data['username']);
        if (!empty($username) && !preg_match('/^[a-zA-Z0-9_]+$/', $username)) {
            return ['api_status' => 400, 'error_message' => 'Invalid username format'];
        }

        // Перевіряємо унікальність
        $stmt = $db->prepare("SELECT group_id FROM Wo_GroupChat WHERE username = ? AND group_id != ?");
        $stmt->execute([$username, $channel_id]);
        if ($stmt->fetch()) {
            return ['api_status' => 400, 'error_message' => 'Username already taken'];
        }

        $updates[] = "username = ?";
        $params[] = $username ?: null;
    }

    if (isset($data['category'])) {
        $updates[] = "category = ?";
        $params[] = trim($data['category']);
    }

    if (empty($updates)) {
        return ['api_status' => 400, 'error_message' => 'Nothing to update'];
    }

    $params[] = $channel_id;

    $stmt = $db->prepare("UPDATE Wo_GroupChat SET " . implode(', ', $updates) . " WHERE group_id = ? AND type = 'channel'");
    $stmt->execute($params);

    return getChannelDetails($db, $user_id, $channel_id);
}

/**
 * Видалити канал
 */
function deleteChannel($db, $user_id, $channel_id) {
    // Перевіряємо права (тільки owner)
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $role = $stmt->fetchColumn();

    if ($role !== 'owner') {
        return ['api_status' => 403, 'error_message' => 'Only channel owner can delete the channel'];
    }

    try {
        $db->beginTransaction();

        // Видаляємо всі повідомлення
        $stmt = $db->prepare("DELETE FROM Wo_Messages WHERE group_id = ?");
        $stmt->execute([$channel_id]);

        // Видаляємо всіх підписників
        $stmt = $db->prepare("DELETE FROM Wo_GroupChatUsers WHERE group_id = ?");
        $stmt->execute([$channel_id]);

        // Видаляємо канал
        $stmt = $db->prepare("DELETE FROM Wo_GroupChat WHERE group_id = ? AND type = 'channel'");
        $stmt->execute([$channel_id]);

        $db->commit();

        return ['api_status' => 200, 'message' => 'Channel deleted successfully'];
    } catch (Exception $e) {
        $db->rollBack();
        return ['api_status' => 500, 'error_message' => 'Failed to delete channel: ' . $e->getMessage()];
    }
}

/**
 * Підписатися на канал
 */
function subscribeChannel($db, $user_id, $channel_id) {
    // Перевіряємо чи існує канал
    $stmt = $db->prepare("SELECT group_id, is_private FROM Wo_GroupChat WHERE group_id = ? AND type = 'channel'");
    $stmt->execute([$channel_id]);
    $channel = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$channel) {
        return ['api_status' => 404, 'error_message' => 'Channel not found'];
    }

    // Перевіряємо чи вже підписаний
    $stmt = $db->prepare("SELECT id FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    if ($stmt->fetch()) {
        return ['api_status' => 400, 'error_message' => 'Already subscribed'];
    }

    // Підписуємося
    $stmt = $db->prepare("
        INSERT INTO Wo_GroupChatUsers (user_id, group_id, role, active, last_seen)
        VALUES (?, ?, 'member', '1', ?)
    ");
    $stmt->execute([$user_id, $channel_id, time()]);

    return ['api_status' => 200, 'message' => 'Subscribed successfully'];
}

/**
 * Відписатися від каналу
 */
function unsubscribeChannel($db, $user_id, $channel_id) {
    $stmt = $db->prepare("DELETE FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ? AND role != 'owner'");
    $stmt->execute([$channel_id, $user_id]);

    if ($stmt->rowCount() === 0) {
        return ['api_status' => 400, 'error_message' => 'Not subscribed or cannot unsubscribe (you are owner)'];
    }

    return ['api_status' => 200, 'message' => 'Unsubscribed successfully'];
}

/**
 * Отримати підписників каналу
 */
function getChannelSubscribers($db, $user_id, $channel_id) {
    // Перевіряємо чи є адміном
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'Only admins can view subscribers'];
    }

    $stmt = $db->prepare("
        SELECT
            gcu.user_id AS id,
            gcu.role,
            gcu.last_seen
        FROM Wo_GroupChatUsers gcu
        WHERE gcu.group_id = ?
        ORDER BY
            CASE gcu.role
                WHEN 'owner' THEN 1
                WHEN 'admin' THEN 2
                WHEN 'moderator' THEN 3
                ELSE 4
            END,
            gcu.id ASC
    ");
    $stmt->execute([$channel_id]);
    $subscribers = $stmt->fetchAll(PDO::FETCH_ASSOC);

    return [
        'api_status' => 200,
        'subscribers' => $subscribers,
        'total' => count($subscribers)
    ];
}

/**
 * Отримати пости каналу
 */
function getChannelPosts($db, $user_id, $channel_id, $data) {
    $limit = isset($data['limit']) ? (int)$data['limit'] : 20;
    $offset = isset($data['offset']) ? (int)$data['offset'] : 0;

    // Перевіряємо чи підписаний на канал або це публічний канал
    $stmt = $db->prepare("
        SELECT is_private,
               (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?) AS is_subscribed
        FROM Wo_GroupChat
        WHERE group_id = ? AND type = 'channel'
    ");
    $stmt->execute([$channel_id, $user_id, $channel_id]);
    $channel = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$channel) {
        return ['api_status' => 404, 'error_message' => 'Channel not found'];
    }

    if ($channel['is_private'] === '1' && $channel['is_subscribed'] == 0) {
        return ['api_status' => 403, 'error_message' => 'Subscribe to view posts'];
    }

    // Отримуємо пости
    $stmt = $db->prepare("
        SELECT
            m.id,
            m.from_id AS author_id,
            m.text,
            m.media,
            m.mediaFileName,
            m.time AS created_time,
            m.type_two AS is_pinned,
            (SELECT COUNT(*) FROM wo_reactions WHERE message_id = m.id) AS reactions_count,
            (SELECT COUNT(*) FROM Wo_MessageComments WHERE message_id = m.id) AS comments_count,
            (SELECT COUNT(*) FROM Wo_MessageViews WHERE message_id = m.id) AS views_count
        FROM Wo_Messages m
        WHERE m.group_id = ?
        ORDER BY
            CASE WHEN m.type_two = 'pinned' THEN 0 ELSE 1 END,
            m.time DESC
        LIMIT ? OFFSET ?
    ");
    $stmt->execute([$channel_id, $limit, $offset]);
    $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Форматуємо пости
    foreach ($posts as &$post) {
        $post['is_pinned'] = ($post['is_pinned'] === 'pinned');
        $post['is_edited'] = false; // TODO: додати підтримку edited_time

        // Перетворюємо media у масив
        if (!empty($post['media'])) {
            $post['media'] = [[
                'url' => $post['media'],
                'type' => 'image',
                'filename' => $post['mediaFileName']
            ]];
        } else {
            $post['media'] = [];
        }
        unset($post['mediaFileName']);
    }

    return [
        'api_status' => 200,
        'posts' => $posts,
        'total' => count($posts)
    ];
}

/**
 * Створити пост у каналі
 */
function createPost($db, $user_id, $data) {
    $channel_id = $data['channel_id'] ?? null;
    $text = trim($data['text'] ?? '');
    $media_url = trim($data['media_url'] ?? '');

    if (!$channel_id) {
        return ['api_status' => 400, 'error_message' => 'channel_id is required'];
    }

    if (empty($text) && empty($media_url)) {
        return ['api_status' => 400, 'error_message' => 'Post text or media is required'];
    }

    // Перевіряємо права на публікацію
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin', 'moderator'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to post in this channel'];
    }

    // Створюємо пост
    $stmt = $db->prepare("
        INSERT INTO Wo_Messages
        (from_id, group_id, text, media, mediaFileName, time, seen, sent_push)
        VALUES (?, ?, ?, ?, ?, ?, 0, 0)
    ");

    $time = time();
    $filename = $media_url ? basename($media_url) : '';

    $stmt->execute([
        $user_id,
        $channel_id,
        $text,
        $media_url ?: '',
        $filename,
        $time
    ]);

    $post_id = $db->lastInsertId();

    // Оновлюємо лічильник постів в каналі
    $stmt = $db->prepare("UPDATE Wo_GroupChat SET posts_count = posts_count + 1 WHERE group_id = ?");
    $stmt->execute([$channel_id]);

    // Повертаємо створений пост з повними даними
    $stmt = $db->prepare("
        SELECT
            m.id,
            m.from_id AS author_id,
            m.text,
            m.media,
            m.mediaFileName,
            m.time AS created_time,
            m.type_two AS is_pinned,
            0 AS reactions_count,
            0 AS comments_count,
            0 AS views_count
        FROM Wo_Messages m
        WHERE m.id = ?
    ");
    $stmt->execute([$post_id]);
    $post = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($post) {
        $post['is_pinned'] = ($post['is_pinned'] === 'pinned');
        $post['is_edited'] = false;

        // Форматуємо media
        if (!empty($post['media'])) {
            $post['media'] = [[
                'url' => $post['media'],
                'type' => 'image',
                'filename' => $post['mediaFileName']
            ]];
        } else {
            $post['media'] = [];
        }
        unset($post['mediaFileName']);
    }

    return [
        'api_status' => 200,
        'message' => 'Post created successfully',
        'post_id' => $post_id,
        'post' => $post
    ];
}

/**
 * Оновити пост
 */
function updatePost($db, $user_id, $post_id, $data) {
    // Перевіряємо права
    $stmt = $db->prepare("
        SELECT m.from_id, m.group_id, gcu.role
        FROM Wo_Messages m
        LEFT JOIN Wo_GroupChatUsers gcu ON gcu.group_id = m.group_id AND gcu.user_id = ?
        WHERE m.id = ?
    ");
    $stmt->execute([$user_id, $post_id]);
    $post = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$post) {
        return ['api_status' => 404, 'error_message' => 'Post not found'];
    }

    if ($post['from_id'] != $user_id && !in_array($post['role'], ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to edit this post'];
    }

    $updates = [];
    $params = [];

    if (isset($data['text'])) {
        $updates[] = "text = ?";
        $params[] = trim($data['text']);
    }

    if (empty($updates)) {
        return ['api_status' => 400, 'error_message' => 'Nothing to update'];
    }

    $params[] = $post_id;

    $stmt = $db->prepare("UPDATE Wo_Messages SET " . implode(', ', $updates) . " WHERE id = ?");
    $stmt->execute($params);

    return ['api_status' => 200, 'message' => 'Post updated successfully'];
}

/**
 * Видалити пост
 */
function deletePost($db, $user_id, $post_id) {
    // Перевіряємо права
    $stmt = $db->prepare("
        SELECT m.from_id, m.group_id, gcu.role
        FROM Wo_Messages m
        LEFT JOIN Wo_GroupChatUsers gcu ON gcu.group_id = m.group_id AND gcu.user_id = ?
        WHERE m.id = ?
    ");
    $stmt->execute([$user_id, $post_id]);
    $post = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$post) {
        return ['api_status' => 404, 'error_message' => 'Post not found'];
    }

    if ($post['from_id'] != $user_id && !in_array($post['role'], ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to delete this post'];
    }

    $stmt = $db->prepare("DELETE FROM Wo_Messages WHERE id = ?");
    $stmt->execute([$post_id]);

    return ['api_status' => 200, 'message' => 'Post deleted successfully'];
}

/**
 * Закріпити/відкріпити пост
 */
function pinPost($db, $user_id, $post_id, $pin = true) {
    // Перевіряємо права
    $stmt = $db->prepare("
        SELECT m.group_id, gcu.role
        FROM Wo_Messages m
        LEFT JOIN Wo_GroupChatUsers gcu ON gcu.group_id = m.group_id AND gcu.user_id = ?
        WHERE m.id = ?
    ");
    $stmt->execute([$user_id, $post_id]);
    $post = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$post) {
        return ['api_status' => 404, 'error_message' => 'Post not found'];
    }

    if (!in_array($post['role'], ['owner', 'admin', 'moderator'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to pin posts'];
    }

    $type_two = $pin ? 'pinned' : '';

    $stmt = $db->prepare("UPDATE Wo_Messages SET type_two = ? WHERE id = ?");
    $stmt->execute([$type_two, $post_id]);

    return [
        'api_status' => 200,
        'message' => $pin ? 'Post pinned successfully' : 'Post unpinned successfully'
    ];
}

/**
 * Отримати коментарі до поста
 */
function getComments($db, $user_id, $post_id) {
    $stmt = $db->prepare("
        SELECT
            mc.id,
            mc.user_id,
            mc.text,
            mc.time,
            mc.edited_time,
            mc.reply_to_comment_id,
            (SELECT COUNT(*) FROM Wo_MessageCommentReactions WHERE comment_id = mc.id) AS reactions_count
        FROM Wo_MessageComments mc
        WHERE mc.message_id = ?
        ORDER BY mc.time ASC
    ");
    $stmt->execute([$post_id]);
    $comments = $stmt->fetchAll(PDO::FETCH_ASSOC);

    return [
        'api_status' => 200,
        'comments' => $comments,
        'total' => count($comments)
    ];
}

/**
 * Додати коментар до поста
 */
function addComment($db, $user_id, $data) {
    $post_id = $data['post_id'] ?? null;
    $text = trim($data['text'] ?? '');
    $reply_to = $data['reply_to_comment_id'] ?? null;

    if (!$post_id || empty($text)) {
        return ['api_status' => 400, 'error_message' => 'post_id and text are required'];
    }

    // Перевіряємо чи існує пост і чи дозволені коментарі
    $stmt = $db->prepare("
        SELECT m.group_id, g.settings
        FROM Wo_Messages m
        JOIN Wo_GroupChat g ON g.group_id = m.group_id
        WHERE m.id = ? AND g.type = 'channel'
    ");
    $stmt->execute([$post_id]);
    $post = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$post) {
        return ['api_status' => 404, 'error_message' => 'Post not found'];
    }

    $settings = json_decode($post['settings'] ?? '{}', true);
    if (isset($settings['allow_comments']) && !$settings['allow_comments']) {
        return ['api_status' => 403, 'error_message' => 'Comments are disabled for this channel'];
    }

    // Додаємо коментар
    $stmt = $db->prepare("
        INSERT INTO Wo_MessageComments (message_id, user_id, text, time, reply_to_comment_id)
        VALUES (?, ?, ?, ?, ?)
    ");
    $stmt->execute([$post_id, $user_id, $text, time(), $reply_to]);

    return [
        'api_status' => 200,
        'message' => 'Comment added successfully',
        'comment_id' => $db->lastInsertId()
    ];
}

/**
 * Видалити коментар
 */
function deleteComment($db, $user_id, $comment_id) {
    // Перевіряємо права
    $stmt = $db->prepare("
        SELECT mc.user_id, m.group_id, gcu.role
        FROM Wo_MessageComments mc
        JOIN Wo_Messages m ON m.id = mc.message_id
        LEFT JOIN Wo_GroupChatUsers gcu ON gcu.group_id = m.group_id AND gcu.user_id = ?
        WHERE mc.id = ?
    ");
    $stmt->execute([$user_id, $comment_id]);
    $comment = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$comment) {
        return ['api_status' => 404, 'error_message' => 'Comment not found'];
    }

    if ($comment['user_id'] != $user_id && !in_array($comment['role'], ['owner', 'admin', 'moderator'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to delete this comment'];
    }

    $stmt = $db->prepare("DELETE FROM Wo_MessageComments WHERE id = ?");
    $stmt->execute([$comment_id]);

    return ['api_status' => 200, 'message' => 'Comment deleted successfully'];
}

/**
 * Додати реакцію на пост
 */
function addPostReaction($db, $user_id, $data) {
    $post_id = $data['post_id'] ?? null;
    $reaction = trim($data['reaction'] ?? '');

    if (!$post_id || empty($reaction)) {
        return ['api_status' => 400, 'error_message' => 'post_id and reaction are required'];
    }

    // Перевіряємо чи дозволені реакції
    $stmt = $db->prepare("
        SELECT g.settings
        FROM Wo_Messages m
        JOIN Wo_GroupChat g ON g.group_id = m.group_id
        WHERE m.id = ? AND g.type = 'channel'
    ");
    $stmt->execute([$post_id]);
    $post = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$post) {
        return ['api_status' => 404, 'error_message' => 'Post not found'];
    }

    $settings = json_decode($post['settings'] ?? '{}', true);
    if (isset($settings['allow_reactions']) && !$settings['allow_reactions']) {
        return ['api_status' => 403, 'error_message' => 'Reactions are disabled for this channel'];
    }

    // Видаляємо попередню реакцію цього користувача
    $stmt = $db->prepare("DELETE FROM wo_reactions WHERE message_id = ? AND user_id = ?");
    $stmt->execute([$post_id, $user_id]);

    // Додаємо нову реакцію
    $stmt = $db->prepare("
        INSERT INTO wo_reactions (user_id, message_id, reaction)
        VALUES (?, ?, ?)
    ");
    $stmt->execute([$user_id, $post_id, $reaction]);

    return ['api_status' => 200, 'message' => 'Reaction added successfully'];
}

/**
 * Видалити реакцію з поста
 */
function removePostReaction($db, $user_id, $data) {
    $post_id = $data['post_id'] ?? null;

    if (!$post_id) {
        return ['api_status' => 400, 'error_message' => 'post_id is required'];
    }

    $stmt = $db->prepare("DELETE FROM wo_reactions WHERE message_id = ? AND user_id = ?");
    $stmt->execute([$post_id, $user_id]);

    return ['api_status' => 200, 'message' => 'Reaction removed successfully'];
}

/**
 * Додати реакцію на коментар
 */
function addCommentReaction($db, $user_id, $data) {
    $comment_id = $data['comment_id'] ?? null;
    $reaction = trim($data['reaction'] ?? '');

    if (!$comment_id || empty($reaction)) {
        return ['api_status' => 400, 'error_message' => 'comment_id and reaction are required'];
    }

    // Видаляємо попередню реакцію
    $stmt = $db->prepare("DELETE FROM Wo_MessageCommentReactions WHERE comment_id = ? AND user_id = ?");
    $stmt->execute([$comment_id, $user_id]);

    // Додаємо нову
    $stmt = $db->prepare("
        INSERT INTO Wo_MessageCommentReactions (comment_id, user_id, reaction, time)
        VALUES (?, ?, ?, ?)
    ");
    $stmt->execute([$comment_id, $user_id, $reaction, time()]);

    return ['api_status' => 200, 'message' => 'Reaction added successfully'];
}

/**
 * Додати адміністратора каналу
 */
function addChannelAdmin($db, $user_id, $data) {
    $channel_id = $data['channel_id'] ?? null;
    $admin_user_id = $data['user_id'] ?? null;
    $role = $data['role'] ?? 'admin'; // admin, moderator

    if (!$channel_id || !$admin_user_id) {
        return ['api_status' => 400, 'error_message' => 'channel_id and user_id are required'];
    }

    if (!in_array($role, ['admin', 'moderator'])) {
        return ['api_status' => 400, 'error_message' => 'Invalid role'];
    }

    // Перевіряємо права (тільки owner)
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $current_role = $stmt->fetchColumn();

    if ($current_role !== 'owner') {
        return ['api_status' => 403, 'error_message' => 'Only channel owner can add admins'];
    }

    // Оновлюємо роль користувача
    $stmt = $db->prepare("
        UPDATE Wo_GroupChatUsers
        SET role = ?
        WHERE group_id = ? AND user_id = ?
    ");
    $stmt->execute([$role, $channel_id, $admin_user_id]);

    if ($stmt->rowCount() === 0) {
        return ['api_status' => 404, 'error_message' => 'User is not a member of this channel'];
    }

    return ['api_status' => 200, 'message' => 'Admin added successfully'];
}

/**
 * Видалити адміністратора каналу
 */
function removeChannelAdmin($db, $user_id, $data) {
    $channel_id = $data['channel_id'] ?? null;
    $admin_user_id = $data['user_id'] ?? null;

    if (!$channel_id || !$admin_user_id) {
        return ['api_status' => 400, 'error_message' => 'channel_id and user_id are required'];
    }

    // Перевіряємо права (тільки owner)
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $current_role = $stmt->fetchColumn();

    if ($current_role !== 'owner') {
        return ['api_status' => 403, 'error_message' => 'Only channel owner can remove admins'];
    }

    // Змінюємо роль на member
    $stmt = $db->prepare("
        UPDATE Wo_GroupChatUsers
        SET role = 'member'
        WHERE group_id = ? AND user_id = ? AND role != 'owner'
    ");
    $stmt->execute([$channel_id, $admin_user_id]);

    if ($stmt->rowCount() === 0) {
        return ['api_status' => 404, 'error_message' => 'Admin not found or cannot remove owner'];
    }

    return ['api_status' => 200, 'message' => 'Admin removed successfully'];
}

/**
 * Оновити налаштування каналу
 */
function updateChannelSettings($db, $user_id, $channel_id, $data) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role, settings FROM Wo_GroupChatUsers gcu JOIN Wo_GroupChat g ON g.group_id = gcu.group_id WHERE gcu.group_id = ? AND gcu.user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$result || !in_array($result['role'], ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to change settings'];
    }

    // Отримуємо поточні налаштування
    $current_settings = json_decode($result['settings'] ?? '{}', true);

    // Оновлюємо налаштування
    $settings_to_update = [
        'allow_comments',
        'allow_reactions',
        'allow_shares',
        'show_statistics',
        'notify_subscribers_new_post',
        'auto_delete_posts_days',
        'signature_enabled',
        'comments_moderation',
        'slow_mode_seconds'
    ];

    foreach ($settings_to_update as $key) {
        if (isset($data[$key])) {
            $current_settings[$key] = $data[$key];
        }
    }

    $stmt = $db->prepare("UPDATE Wo_GroupChat SET settings = ? WHERE group_id = ?");
    $stmt->execute([json_encode($current_settings), $channel_id]);

    return ['api_status' => 200, 'message' => 'Settings updated successfully'];
}

/**
 * Отримати статистику каналу
 */
function getChannelStatistics($db, $user_id, $channel_id) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'Only admins can view statistics'];
    }

    // Загальна статистика
    $stmt = $db->prepare("
        SELECT
            subscribers_count,
            posts_count,
            (SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ? AND time > UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 7 DAY))) AS posts_last_week,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = ? AND last_seen > UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 1 DAY))) AS active_subscribers_24h
        FROM Wo_GroupChat
        WHERE group_id = ?
    ");
    $stmt->execute([$channel_id, $channel_id, $channel_id]);
    $stats = $stmt->fetch(PDO::FETCH_ASSOC);

    // Топ-3 поста за переглядами
    $stmt = $db->prepare("
        SELECT
            m.id,
            m.text,
            (SELECT COUNT(*) FROM Wo_MessageViews WHERE message_id = m.id) AS views
        FROM Wo_Messages m
        WHERE m.group_id = ?
        ORDER BY views DESC
        LIMIT 3
    ");
    $stmt->execute([$channel_id]);
    $top_posts = $stmt->fetchAll(PDO::FETCH_ASSOC);

    return [
        'api_status' => 200,
        'statistics' => [
            'subscribers_count' => $stats['subscribers_count'],
            'posts_count' => $stats['posts_count'],
            'posts_last_week' => $stats['posts_last_week'],
            'active_subscribers_24h' => $stats['active_subscribers_24h'],
            'top_posts' => $top_posts
        ]
    ];
}

/**
 * Завантажити аватар каналу
 */
function uploadChannelAvatar($db, $user_id, $channel_id, $files) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$channel_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to change avatar'];
    }

    // TODO: Реалізувати завантаження файлу
    return ['api_status' => 501, 'error_message' => 'Avatar upload not implemented yet'];
}

?>