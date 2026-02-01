<?php
/**
 * WorldMates Messenger - Group Chat API v2
 * Версія: 2.0 (адаптована до існуючої БД Wondertage/WO)
 *
 * ВАЖЛИВО: Цей API працює ТІЛЬКИ з групами (type='group'),
 * НЕ з каналами (type='channel'). Для каналів використовуйте channels.php
 *
 * Використовує існуючі таблиці:
 * - Wo_GroupChat (type='group')
 * - Wo_GroupChatUsers (учасники)
 * - Wo_Messages (повідомлення)
 */

require_once(__DIR__ . '/config.php');

// Налаштування логування
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', '/var/www/www-root/data/www/worldmates.club/api/v2/logs/php_errors.log');

// Функція логування для груп
if (!function_exists('logGroupMessage')) {
    function logGroupMessage($message, $level = 'INFO') {
        $log_file = '/var/www/www-root/data/www/worldmates.club/api/v2/logs/groups.log';
        $timestamp = date('Y-m-d H:i:s');
        $log_entry = "[{$timestamp}] [{$level}] {$message}\n";
        @file_put_contents($log_file, $log_entry, FILE_APPEND);
    }
}

header('Content-Type: application/json');

// Отримуємо дані запиту
$request_method = $_SERVER['REQUEST_METHOD'];

// Підтримка як JSON, так і form-encoded даних
$content_type = $_SERVER['CONTENT_TYPE'] ?? '';
if (strpos($content_type, 'application/json') !== false) {
    $data = json_decode(file_get_contents('php://input'), true) ?? [];
} else {
    $data = $_POST;
}

// Access token для автентифікації
$access_token = $data['access_token'] ?? $_GET['access_token'] ?? $_POST['access_token'] ?? null;

if (!$access_token) {
    echo json_encode(['api_status' => 400, 'error_message' => 'Access token is required']);
    exit;
}

// Валідація токену та отримання user_id
$user_id = validateAccessToken($db, $access_token);
if (!$user_id) {
    logGroupMessage("Invalid access token attempt", 'WARNING');
    echo json_encode(['api_status' => 401, 'error_message' => 'Invalid access token']);
    exit;
}

// Роутинг методів
$type = $data['type'] ?? $_GET['type'] ?? '';

logGroupMessage("User $user_id: type=$type", 'INFO');

try {
    switch ($type) {
        // ============================================
        // CRUD операції з групами
        // ============================================
        case 'get_list':
            echo json_encode(getGroups($db, $user_id, $data));
            break;

        case 'get_by_id':
            $group_id = $data['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(getGroupDetails($db, $user_id, $group_id));
            break;

        case 'create':
            echo json_encode(createGroup($db, $user_id, $data));
            break;

        case 'edit':
            $group_id = $data['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(updateGroup($db, $user_id, $group_id, $data));
            break;

        case 'delete':
            $group_id = $data['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(deleteGroup($db, $user_id, $group_id));
            break;

        // ============================================
        // Учасники
        // ============================================
        case 'get_members':
            $group_id = $data['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(getGroupMembers($db, $user_id, $group_id, $data));
            break;

        case 'add_user':
            $group_id = $data['id'] ?? null;
            $parts = $data['parts'] ?? '';
            if (!$group_id || !$parts) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id and parts are required']);
                exit;
            }
            echo json_encode(addGroupMembers($db, $user_id, $group_id, $parts));
            break;

        case 'remove_user':
            $group_id = $data['id'] ?? null;
            $parts = $data['parts'] ?? '';
            if (!$group_id || !$parts) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id and parts are required']);
                exit;
            }
            echo json_encode(removeGroupMembers($db, $user_id, $group_id, $parts));
            break;

        case 'set_admin':
            $group_id = $data['id'] ?? null;
            $target_user_id = $data['user_id'] ?? null;
            $role = $data['role'] ?? 'admin';
            if (!$group_id || !$target_user_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id and user_id are required']);
                exit;
            }
            echo json_encode(setGroupRole($db, $user_id, $group_id, $target_user_id, $role));
            break;

        case 'leave':
            $group_id = $data['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(leaveGroup($db, $user_id, $group_id));
            break;

        // ============================================
        // Повідомлення
        // ============================================
        case 'get_messages':
            $group_id = $data['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(getGroupMessages($db, $user_id, $group_id, $data));
            break;

        case 'send_message':
            $group_id = $data['id'] ?? null;
            $text = $data['text'] ?? '';
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(sendGroupMessage($db, $user_id, $group_id, $text, $data));
            break;

        case 'upload_avatar':
            $group_id = $data['id'] ?? $_POST['id'] ?? null;
            if (!$group_id) {
                echo json_encode(['api_status' => 400, 'error_message' => 'id is required']);
                exit;
            }
            echo json_encode(uploadGroupAvatar($db, $user_id, $group_id, $_FILES));
            break;

        default:
            echo json_encode(['api_status' => 400, 'error_message' => 'Invalid type: ' . $type]);
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

/**
 * Отримати список груп користувача
 * ВАЖЛИВО: Фільтруємо ТІЛЬКИ групи (type='group'), НЕ канали
 */
function getGroups($db, $user_id, $data) {
    $limit = isset($data['limit']) ? (int)$data['limit'] : 50;
    $offset = isset($data['offset']) ? (int)$data['offset'] : 0;

    // КРИТИЧНО: Додаємо фільтр type='group' або type IS NULL (для старих записів)
    $stmt = $db->prepare("
        SELECT
            g.group_id AS id,
            g.group_name AS name,
            g.description,
            g.avatar AS avatarUrl,
            g.user_id AS adminId,
            g.is_private AS isPrivate,
            g.time AS createdTime,
            g.type,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id) AS membersCount,
            (SELECT role FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS userRole,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS isMember
        FROM Wo_GroupChat g
        WHERE g.group_id IN (SELECT group_id FROM Wo_GroupChatUsers WHERE user_id = ?)
          AND (g.type = 'group' OR g.type IS NULL OR g.type = '')
        ORDER BY g.time DESC
        LIMIT ? OFFSET ?
    ");

    $stmt->execute([$user_id, $user_id, $user_id, $limit, $offset]);
    $groups = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Форматуємо результат
    foreach ($groups as &$group) {
        $group['isPrivate'] = (bool)$group['isPrivate'];
        $group['isMember'] = (bool)$group['isMember'];
        $group['isAdmin'] = in_array($group['userRole'], ['owner', 'admin']);
        $group['isModerator'] = in_array($group['userRole'], ['owner', 'admin', 'moderator']);
        $group['membersCount'] = (int)$group['membersCount'];
        unset($group['userRole']);
    }

    return [
        'api_status' => 200,
        'groups' => $groups,
        'total' => count($groups)
    ];
}

/**
 * Отримати деталі групи
 */
function getGroupDetails($db, $user_id, $group_id) {
    $stmt = $db->prepare("
        SELECT
            g.group_id AS id,
            g.group_name AS name,
            g.description,
            g.avatar AS avatarUrl,
            g.user_id AS adminId,
            g.is_private AS isPrivate,
            g.time AS createdTime,
            g.type,
            g.pinned_message_id AS pinnedMessageId,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id) AS membersCount,
            (SELECT role FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS userRole,
            (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS isMember,
            (SELECT muted FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS isMuted
        FROM Wo_GroupChat g
        WHERE g.group_id = ?
          AND (g.type = 'group' OR g.type IS NULL OR g.type = '')
    ");
    $stmt->execute([$user_id, $user_id, $user_id, $group_id]);
    $group = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$group) {
        return ['api_status' => 404, 'error_message' => 'Group not found'];
    }

    // Форматуємо
    $group['isPrivate'] = (bool)$group['isPrivate'];
    $group['isMember'] = (bool)$group['isMember'];
    $group['isMuted'] = (bool)$group['isMuted'];
    $group['isAdmin'] = in_array($group['userRole'], ['owner', 'admin']);
    $group['isModerator'] = in_array($group['userRole'], ['owner', 'admin', 'moderator']);
    $group['membersCount'] = (int)$group['membersCount'];
    unset($group['userRole']);

    // Отримуємо закріплене повідомлення якщо є
    if (!empty($group['pinnedMessageId'])) {
        $stmt = $db->prepare("SELECT id, text, time FROM Wo_Messages WHERE id = ?");
        $stmt->execute([$group['pinnedMessageId']]);
        $pinnedMessage = $stmt->fetch(PDO::FETCH_ASSOC);
        $group['pinnedMessage'] = $pinnedMessage ?: null;
    }

    return [
        'api_status' => 200,
        'group' => $group
    ];
}

/**
 * Створити нову групу
 * ВАЖЛИВО: Встановлюємо type='group' явно
 */
function createGroup($db, $user_id, $data) {
    $name = trim($data['group_name'] ?? '');
    $parts = $data['parts'] ?? ''; // comma-separated user IDs
    $group_type = $data['group_type'] ?? 'group'; // 'group' only, not 'channel'

    if (empty($name)) {
        return ['api_status' => 400, 'error_message' => 'Group name is required'];
    }

    // Примусово встановлюємо type='group' для уникнення конфліктів з каналами
    if ($group_type !== 'group') {
        $group_type = 'group';
    }

    try {
        $db->beginTransaction();

        // Створюємо групу з type='group'
        $stmt = $db->prepare("
            INSERT INTO Wo_GroupChat
            (user_id, group_name, type, time)
            VALUES (?, ?, 'group', ?)
        ");
        $time = time();
        $stmt->execute([$user_id, $name, $time]);
        $group_id = $db->lastInsertId();

        // Додаємо власника як owner
        $stmt = $db->prepare("
            INSERT INTO Wo_GroupChatUsers (user_id, group_id, role)
            VALUES (?, ?, 'owner')
        ");
        $stmt->execute([$user_id, $group_id]);

        // Додаємо інших учасників
        if (!empty($parts)) {
            $member_ids = array_filter(array_map('intval', explode(',', $parts)));
            foreach ($member_ids as $member_id) {
                if ($member_id != $user_id) {
                    $stmt = $db->prepare("
                        INSERT INTO Wo_GroupChatUsers (user_id, group_id, role)
                        VALUES (?, ?, 'member')
                    ");
                    $stmt->execute([$member_id, $group_id]);
                }
            }
        }

        $db->commit();

        logGroupMessage("User $user_id created group $group_id: $name (type=group)", 'INFO');

        return getGroupDetails($db, $user_id, $group_id);

    } catch (Exception $e) {
        $db->rollBack();
        logGroupMessage("Failed to create group for user $user_id: " . $e->getMessage(), 'ERROR');
        return ['api_status' => 500, 'error_message' => 'Failed to create group: ' . $e->getMessage()];
    }
}

/**
 * Оновити групу
 */
function updateGroup($db, $user_id, $group_id, $data) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to edit this group'];
    }

    // Перевіряємо що це група, не канал
    $stmt = $db->prepare("SELECT type FROM Wo_GroupChat WHERE group_id = ?");
    $stmt->execute([$group_id]);
    $type = $stmt->fetchColumn();

    if ($type === 'channel') {
        return ['api_status' => 400, 'error_message' => 'This is a channel, use channels.php API'];
    }

    $updates = [];
    $params = [];

    if (isset($data['group_name'])) {
        $updates[] = "group_name = ?";
        $params[] = trim($data['group_name']);
    }

    if (isset($data['description'])) {
        $updates[] = "description = ?";
        $params[] = trim($data['description']);
    }

    if (empty($updates)) {
        return ['api_status' => 400, 'error_message' => 'Nothing to update'];
    }

    $params[] = $group_id;

    $stmt = $db->prepare("UPDATE Wo_GroupChat SET " . implode(', ', $updates) . " WHERE group_id = ?");
    $stmt->execute($params);

    logGroupMessage("User $user_id updated group $group_id", 'INFO');

    return getGroupDetails($db, $user_id, $group_id);
}

/**
 * Видалити групу
 * ВАЖЛИВО: Видаляємо ТІЛЬКИ групи (type='group'), НЕ канали
 */
function deleteGroup($db, $user_id, $group_id) {
    // Перевіряємо права (тільки owner)
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();

    if ($role !== 'owner') {
        return ['api_status' => 403, 'error_message' => 'Only group owner can delete the group'];
    }

    // КРИТИЧНО: Перевіряємо що це група, НЕ канал
    $stmt = $db->prepare("SELECT type, group_name FROM Wo_GroupChat WHERE group_id = ?");
    $stmt->execute([$group_id]);
    $group = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$group) {
        return ['api_status' => 404, 'error_message' => 'Group not found'];
    }

    // Якщо це канал - забороняємо видалення через цей API
    if ($group['type'] === 'channel') {
        return ['api_status' => 400, 'error_message' => 'This is a channel, use channels.php API to delete it'];
    }

    try {
        $db->beginTransaction();

        // Видаляємо всі повідомлення
        $stmt = $db->prepare("DELETE FROM Wo_Messages WHERE group_id = ?");
        $stmt->execute([$group_id]);

        // Видаляємо всіх учасників
        $stmt = $db->prepare("DELETE FROM Wo_GroupChatUsers WHERE group_id = ?");
        $stmt->execute([$group_id]);

        // Видаляємо групу (ТІЛЬКИ якщо type != 'channel')
        $stmt = $db->prepare("DELETE FROM Wo_GroupChat WHERE group_id = ? AND (type = 'group' OR type IS NULL OR type = '')");
        $stmt->execute([$group_id]);

        $db->commit();

        logGroupMessage("User $user_id deleted group $group_id: {$group['group_name']}", 'INFO');

        return ['api_status' => 200, 'message' => 'Group deleted successfully'];
    } catch (Exception $e) {
        $db->rollBack();
        return ['api_status' => 500, 'error_message' => 'Failed to delete group: ' . $e->getMessage()];
    }
}

/**
 * Отримати учасників групи
 */
function getGroupMembers($db, $user_id, $group_id, $data) {
    $limit = isset($data['limit']) ? (int)$data['limit'] : 100;
    $offset = isset($data['offset']) ? (int)$data['offset'] : 0;

    $stmt = $db->prepare("
        SELECT
            gcu.user_id AS id,
            u.username,
            CONCAT(u.first_name, ' ', u.last_name) AS name,
            u.avatar,
            gcu.role,
            gcu.time AS joinedTime
        FROM Wo_GroupChatUsers gcu
        LEFT JOIN Wo_Users u ON u.user_id = gcu.user_id
        WHERE gcu.group_id = ?
        ORDER BY
            CASE gcu.role
                WHEN 'owner' THEN 1
                WHEN 'admin' THEN 2
                WHEN 'moderator' THEN 3
                ELSE 4
            END,
            gcu.time ASC
        LIMIT ? OFFSET ?
    ");
    $stmt->execute([$group_id, $limit, $offset]);
    $members = $stmt->fetchAll(PDO::FETCH_ASSOC);

    return [
        'api_status' => 200,
        'members' => $members,
        'total' => count($members)
    ];
}

/**
 * Додати учасників до групи
 */
function addGroupMembers($db, $user_id, $group_id, $parts) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin', 'moderator'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to add members'];
    }

    $member_ids = array_filter(array_map('intval', explode(',', $parts)));
    $added = 0;
    $time = time();

    foreach ($member_ids as $member_id) {
        // Перевіряємо чи вже є учасником
        $stmt = $db->prepare("SELECT id FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
        $stmt->execute([$group_id, $member_id]);

        if (!$stmt->fetch()) {
            $stmt = $db->prepare("
                INSERT INTO Wo_GroupChatUsers (user_id, group_id, role)
                VALUES (?, ?, 'member')
            ");
            $stmt->execute([$member_id, $group_id]);
            $added++;
        }
    }

    logGroupMessage("User $user_id added $added members to group $group_id", 'INFO');

    return ['api_status' => 200, 'message' => "Added $added members successfully"];
}

/**
 * Видалити учасників з групи
 */
function removeGroupMembers($db, $user_id, $group_id, $parts) {
    // Перевіряємо права
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();

    if (!in_array($role, ['owner', 'admin'])) {
        return ['api_status' => 403, 'error_message' => 'You do not have permission to remove members'];
    }

    $member_ids = array_filter(array_map('intval', explode(',', $parts)));
    $removed = 0;

    foreach ($member_ids as $member_id) {
        // Не можна видалити власника
        $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
        $stmt->execute([$group_id, $member_id]);
        $target_role = $stmt->fetchColumn();

        if ($target_role !== 'owner') {
            $stmt = $db->prepare("DELETE FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
            $stmt->execute([$group_id, $member_id]);
            $removed++;
        }
    }

    return ['api_status' => 200, 'message' => "Removed $removed members successfully"];
}

/**
 * Встановити роль учасника
 */
function setGroupRole($db, $user_id, $group_id, $target_user_id, $role) {
    // Перевіряємо права (тільки owner)
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    $current_role = $stmt->fetchColumn();

    if ($current_role !== 'owner') {
        return ['api_status' => 403, 'error_message' => 'Only group owner can change roles'];
    }

    if (!in_array($role, ['admin', 'moderator', 'member'])) {
        return ['api_status' => 400, 'error_message' => 'Invalid role'];
    }

    $stmt = $db->prepare("
        UPDATE Wo_GroupChatUsers
        SET role = ?
        WHERE group_id = ? AND user_id = ? AND role != 'owner'
    ");
    $stmt->execute([$role, $group_id, $target_user_id]);

    return ['api_status' => 200, 'message' => 'Role updated successfully'];
}

/**
 * Покинути групу
 */
function leaveGroup($db, $user_id, $group_id) {
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();

    if ($role === 'owner') {
        return ['api_status' => 400, 'error_message' => 'Owner cannot leave. Transfer ownership or delete the group.'];
    }

    $stmt = $db->prepare("DELETE FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);

    return ['api_status' => 200, 'message' => 'Left group successfully'];
}

/**
 * Отримати повідомлення групи
 */
function getGroupMessages($db, $user_id, $group_id, $data) {
    $limit = isset($data['limit']) ? (int)$data['limit'] : 50;
    $before_message_id = isset($data['before_message_id']) ? (int)$data['before_message_id'] : 0;

    // Перевіряємо чи користувач є учасником
    $stmt = $db->prepare("SELECT id FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    if (!$stmt->fetch()) {
        return ['api_status' => 403, 'error_message' => 'You are not a member of this group'];
    }

    $where_clause = $before_message_id > 0 ? "AND m.id < ?" : "";
    $params = $before_message_id > 0 ? [$group_id, $before_message_id, $limit] : [$group_id, $limit];

    $stmt = $db->prepare("
        SELECT
            m.id,
            m.from_id AS fromId,
            u.username,
            CONCAT(u.first_name, ' ', u.last_name) AS senderName,
            u.avatar AS senderAvatar,
            m.text,
            m.media,
            m.mediaFileName,
            m.time,
            m.seen
        FROM Wo_Messages m
        LEFT JOIN Wo_Users u ON u.user_id = m.from_id
        WHERE m.group_id = ? $where_clause
        ORDER BY m.time DESC
        LIMIT ?
    ");
    $stmt->execute($params);
    $messages = $stmt->fetchAll(PDO::FETCH_ASSOC);

    return [
        'api_status' => 200,
        'messages' => $messages,
        'total' => count($messages)
    ];
}

/**
 * Надіслати повідомлення в групу
 */
function sendGroupMessage($db, $user_id, $group_id, $text, $data) {
    // Перевіряємо чи користувач є учасником
    $stmt = $db->prepare("SELECT id FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
    $stmt->execute([$group_id, $user_id]);
    if (!$stmt->fetch()) {
        return ['api_status' => 403, 'error_message' => 'You are not a member of this group'];
    }

    if (empty(trim($text))) {
        return ['api_status' => 400, 'error_message' => 'Message text is required'];
    }

    $time = time();
    $reply_to = $data['message_reply_id'] ?? null;

    $stmt = $db->prepare("
        INSERT INTO Wo_Messages (from_id, group_id, text, time, seen, sent_push, message_reply_id)
        VALUES (?, ?, ?, ?, 0, 0, ?)
    ");
    $stmt->execute([$user_id, $group_id, trim($text), $time, $reply_to]);
    $message_id = $db->lastInsertId();

    // Отримуємо створене повідомлення
    $stmt = $db->prepare("
        SELECT
            m.id,
            m.from_id AS fromId,
            u.username,
            CONCAT(u.first_name, ' ', u.last_name) AS senderName,
            u.avatar AS senderAvatar,
            m.text,
            m.time
        FROM Wo_Messages m
        LEFT JOIN Wo_Users u ON u.user_id = m.from_id
        WHERE m.id = ?
    ");
    $stmt->execute([$message_id]);
    $message = $stmt->fetch(PDO::FETCH_ASSOC);

    return [
        'api_status' => 200,
        'message' => $message
    ];
}

/**
 * Завантажити аватар групи
 */
function uploadGroupAvatar($db, $user_id, $group_id, $files) {
    global $wo;

    try {
        logGroupMessage("Upload avatar request: group_id=$group_id, user_id=$user_id", 'INFO');

        // Перевіряємо права
        $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
        $stmt->execute([$group_id, $user_id]);
        $role = $stmt->fetchColumn();

        if (!in_array($role, ['owner', 'admin'])) {
            return ['api_status' => 403, 'error_message' => 'You do not have permission to change avatar'];
        }

        // Перевіряємо що це група, не канал
        $stmt = $db->prepare("SELECT type FROM Wo_GroupChat WHERE group_id = ?");
        $stmt->execute([$group_id]);
        $type = $stmt->fetchColumn();

        if ($type === 'channel') {
            return ['api_status' => 400, 'error_message' => 'This is a channel, use channels.php API'];
        }

        // Перевіряємо чи є файл
        if (empty($files['avatar']) || !isset($files['avatar']['tmp_name'])) {
            return ['api_status' => 400, 'error_message' => 'Avatar file is required'];
        }

        // Визначаємо MIME type
        $real_mime_type = $files['avatar']['type'];
        if (function_exists('mime_content_type')) {
            $detected_mime = mime_content_type($files['avatar']['tmp_name']);
            if ($detected_mime) {
                $real_mime_type = $detected_mime;
            }
        }

        // Set $wo['user'] for Wo_ShareFile()
        $wo['user'] = [
            'user_id' => $user_id,
            'username' => 'group_admin',
            'active' => '1',
            'admin' => 0
        ];

        // Підготовка файлу для завантаження
        $file_info = array(
            'file' => $files['avatar']['tmp_name'],
            'name' => $files['avatar']['name'],
            'size' => $files['avatar']['size'],
            'type' => $real_mime_type,
            'types' => 'jpg,png,jpeg,gif,webp'
        );

        if (!function_exists('Wo_ShareFile')) {
            return ['api_status' => 500, 'error_message' => 'Upload function not available'];
        }

        $upload = Wo_ShareFile($file_info);

        if (empty($upload) || empty($upload['filename'])) {
            return ['api_status' => 500, 'error_message' => 'Failed to upload avatar'];
        }

        $avatar_url = $upload['filename'];

        // Оновлюємо аватар в БД
        $stmt = $db->prepare("UPDATE Wo_GroupChat SET avatar = ? WHERE group_id = ?");
        $stmt->execute([$avatar_url, $group_id]);

        logGroupMessage("User $user_id uploaded avatar for group $group_id: $avatar_url", 'INFO');

        return [
            'api_status' => 200,
            'message' => 'Avatar uploaded successfully',
            'avatarUrl' => $avatar_url
        ];
    } catch (Exception $e) {
        logGroupMessage("Exception in uploadGroupAvatar: " . $e->getMessage(), 'ERROR');
        return [
            'api_status' => 500,
            'error_message' => 'Server error: ' . $e->getMessage()
        ];
    }
}

?>
