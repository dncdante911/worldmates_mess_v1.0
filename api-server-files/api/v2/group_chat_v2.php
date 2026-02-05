<?php
/**
 * ========================================
 * WORLDMATES GROUP CHAT API V2 - CONSOLIDATED
 * ========================================
 *
 * ALL group functionality in one file:
 * - CRUD groups, members, messages (with encryption)
 * - QR codes (generate, join)
 * - Join requests (for private groups)
 * - Settings (slow mode, anti-spam, permissions, privacy)
 * - Statistics
 * - Scheduled posts
 * - Subgroups (topics)
 * - Roles management
 * - Invitations
 *
 * @version 3.0
 */

// Include crypto helper for message encryption
if (file_exists(__DIR__ . '/crypto_helper.php')) {
    require_once(__DIR__ . '/crypto_helper.php');
}

// DB constants
if (!defined('DB_HOST')) define('DB_HOST', 'localhost');
if (!defined('DB_NAME')) define('DB_NAME', 'socialhub');
if (!defined('DB_USER')) define('DB_USER', 'social');
if (!defined('DB_PASS')) define('DB_PASS', '3344Frzaq0607DmC157');
if (!defined('DB_CHARSET')) define('DB_CHARSET', 'utf8mb4');

error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', '/var/www/www-root/data/www/worldmates.club/api/v2/logs/php_errors.log');

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Logging
if (!function_exists('logMessage')) {
    function logMessage($message, $level = 'INFO') {
        $log_file = '/var/www/www-root/data/www/worldmates.club/api/v2/logs/group_chat_v2.log';
        $timestamp = date('Y-m-d H:i:s');
        @file_put_contents($log_file, "[{$timestamp}] [{$level}] {$message}\n", FILE_APPEND);
    }
}

if (!function_exists('sendError')) {
    function sendError($code, $message) {
        http_response_code($code);
        echo json_encode([
            'api_status' => $code,
            'error_code' => $code,
            'error_message' => $message
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

if (!function_exists('sendResponse')) {
    function sendResponse($data) {
        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit();
    }
}

// ==============================================
// AUTH
// ==============================================

logMessage("=== NEW REQUEST ===");
logMessage("Method: {$_SERVER['REQUEST_METHOD']}, URI: {$_SERVER['REQUEST_URI']}");

$access_token = $_GET['access_token'] ?? $_POST['access_token'] ?? null;
if (empty($access_token)) {
    sendError(401, 'access_token is required');
}

try {
    $db = new PDO(
        "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4",
        DB_USER, DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC, PDO::ATTR_EMULATE_PREPARES => false]
    );
} catch (PDOException $e) {
    logMessage("DB ERROR: " . $e->getMessage(), 'ERROR');
    sendError(500, 'Database connection failed');
}

// Authenticate user
try {
    $stmt = $db->prepare("SELECT user_id, platform FROM Wo_AppsSessions WHERE session_id = ? LIMIT 1");
    $stmt->execute([$access_token]);
    $session = $stmt->fetch();
    if (!$session) {
        sendError(401, 'Invalid access_token');
    }
    $current_user_id = $session['user_id'];

    $stmt = $db->prepare("SELECT user_id, username, first_name, last_name, avatar, active FROM Wo_Users WHERE user_id = ? AND active = '1' LIMIT 1");
    $stmt->execute([$current_user_id]);
    $user = $stmt->fetch();
    if (!$user) {
        sendError(401, 'User not found or inactive');
    }
    $user['name'] = trim($user['first_name'] . ' ' . $user['last_name']);
    logMessage("Auth OK: user_id={$current_user_id}, username={$user['username']}");
} catch (PDOException $e) {
    sendError(500, 'Authentication failed');
}

// Ensure all settings columns exist
ensureGroupSettingsColumns($db);
ensureScheduledPostsTable($db);
ensureSubgroupsTable($db);
ensureJoinRequestsTable($db);

// ==============================================
// ROUTING
// ==============================================

$type = $_POST['type'] ?? $_GET['type'] ?? '';
logMessage("Action: $type");

switch ($type) {

    // ==========================================
    // CREATE GROUP
    // ==========================================
    case 'create':
        if (empty($_POST['group_name'])) sendError(400, 'group_name is required');

        $group_name = trim($_POST['group_name']);
        $group_type = $_POST['group_type'] ?? 'group';
        $parts = $_POST['parts'] ?? '';
        $is_private = ($_POST['is_private'] ?? '0') === '1' ? 1 : 0;
        $description = trim($_POST['description'] ?? '');

        $name_length = mb_strlen($group_name, 'UTF-8');
        if ($name_length < 2 || $name_length > 50) sendError(400, 'group_name must be between 2 and 50 characters');

        try {
            $time = time();
            $stmt = $db->prepare("INSERT INTO Wo_GroupChat (user_id, group_name, description, avatar, time, type, is_private) VALUES (?, ?, ?, 'upload/photos/d-group.jpg', ?, 'group', ?)");
            $stmt->execute([$current_user_id, $group_name, $description, $time, $is_private]);
            $group_id = $db->lastInsertId();

            // Add creator as owner
            $stmt = $db->prepare("INSERT INTO Wo_GroupChatUsers (user_id, group_id, role, admin, active, time) VALUES (?, ?, 'owner', '1', '1', ?)");
            $stmt->execute([$current_user_id, $group_id, $time]);

            // Add other members
            $member_ids = array_filter(array_map('trim', explode(',', $parts)));
            $stmtAdd = $db->prepare("INSERT INTO Wo_GroupChatUsers (user_id, group_id, role, admin, active, time) VALUES (?, ?, 'member', '0', '1', ?)");
            foreach ($member_ids as $mid) {
                if (!empty($mid) && is_numeric($mid) && (int)$mid != $current_user_id) {
                    $stmtAdd->execute([(int)$mid, $group_id, $time]);
                }
            }

            sendResponse(getFullGroupResponse($db, $current_user_id, $group_id));
        } catch (PDOException $e) {
            logMessage("CREATE ERROR: " . $e->getMessage(), 'ERROR');
            sendError(500, 'Failed to create group');
        }
        break;

    // ==========================================
    // GET LIST
    // ==========================================
    case 'get_list':
        $limit = (int)($_POST['limit'] ?? $_GET['limit'] ?? 50);
        $offset = (int)($_POST['offset'] ?? $_GET['offset'] ?? 0);

        try {
            $stmt = $db->prepare("
                SELECT
                    g.group_id AS id, g.group_name AS name, g.avatar, g.description,
                    g.user_id AS admin_id, g.is_private, g.time AS created_time, g.type,
                    CONCAT(u.first_name, ' ', u.last_name) AS admin_name,
                    (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND active = '1') AS members_count,
                    (SELECT role FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS user_role,
                    1 AS is_member,
                    COALESCE((SELECT muted FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?), 0) AS is_muted
                FROM Wo_GroupChat g
                LEFT JOIN Wo_Users u ON g.user_id = u.user_id
                WHERE g.group_id IN (SELECT group_id FROM Wo_GroupChatUsers WHERE user_id = ? AND active = '1')
                  AND (g.type = 'group' OR g.type IS NULL OR g.type = '')
                ORDER BY g.time DESC
                LIMIT ? OFFSET ?
            ");
            $stmt->execute([$current_user_id, $current_user_id, $current_user_id, $limit, $offset]);
            $groups = $stmt->fetchAll();

            foreach ($groups as &$g) {
                $g['is_private'] = (bool)$g['is_private'];
                $g['is_member'] = true;
                $g['is_admin'] = in_array($g['user_role'], ['owner', 'admin']);
                $g['is_moderator'] = in_array($g['user_role'], ['owner', 'admin', 'moderator']);
                $g['is_muted'] = (bool)$g['is_muted'];
                $g['members_count'] = (int)$g['members_count'];
                $g['admin_name'] = $g['admin_name'] ?? '';
                unset($g['user_role']);
            }

            sendResponse(['api_status' => 200, 'groups' => $groups]);
        } catch (PDOException $e) {
            sendError(500, 'Failed to get groups');
        }
        break;

    // ==========================================
    // GET BY ID
    // ==========================================
    case 'get_by_id':
        $group_id = (int)($_POST['id'] ?? $_GET['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        sendResponse(getFullGroupResponse($db, $current_user_id, $group_id));
        break;

    // ==========================================
    // EDIT GROUP
    // ==========================================
    case 'edit':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can edit the group');
        }

        $updates = [];
        $params = [];
        if (isset($_POST['group_name'])) { $updates[] = "group_name = ?"; $params[] = trim($_POST['group_name']); }
        if (isset($_POST['description'])) { $updates[] = "description = ?"; $params[] = trim($_POST['description']); }
        if (isset($_POST['is_private'])) { $updates[] = "is_private = ?"; $params[] = $_POST['is_private'] === '1' ? 1 : 0; }

        if (empty($updates)) sendError(400, 'Nothing to update');

        $params[] = $group_id;
        $db->prepare("UPDATE Wo_GroupChat SET " . implode(', ', $updates) . " WHERE group_id = ? AND (type = 'group' OR type IS NULL OR type = '')")->execute($params);

        sendResponse(getFullGroupResponse($db, $current_user_id, $group_id));
        break;

    // ==========================================
    // DELETE GROUP
    // ==========================================
    case 'delete':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        if (!isGroupOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only group owner can delete the group');
        }

        try {
            $db->beginTransaction();
            $db->prepare("DELETE FROM Wo_Messages WHERE group_id = ?")->execute([$group_id]);
            $db->prepare("DELETE FROM Wo_GroupChatUsers WHERE group_id = ?")->execute([$group_id]);
            $db->prepare("DELETE FROM Wo_GroupChat WHERE group_id = ? AND (type = 'group' OR type IS NULL OR type = '')")->execute([$group_id]);
            // Clean related data
            $db->prepare("DELETE FROM Wo_GroupJoinRequests WHERE group_id = ?")->execute([$group_id]);
            $db->prepare("DELETE FROM Wo_ScheduledPosts WHERE group_id = ?")->execute([$group_id]);
            $db->prepare("DELETE FROM Wo_Subgroups WHERE parent_group_id = ?")->execute([$group_id]);
            $db->commit();
            sendResponse(['api_status' => 200, 'message' => 'Group deleted successfully']);
        } catch (PDOException $e) {
            if ($db->inTransaction()) $db->rollBack();
            sendError(500, 'Failed to delete group');
        }
        break;

    // ==========================================
    // GET MEMBERS
    // ==========================================
    case 'get_members':
        $group_id = (int)($_POST['id'] ?? $_GET['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        $limit = (int)($_POST['limit'] ?? 100);
        $offset = (int)($_POST['offset'] ?? 0);

        $stmt = $db->prepare("
            SELECT gcu.user_id, u.username, CONCAT(u.first_name, ' ', u.last_name) AS name,
                   u.avatar, gcu.role, gcu.time AS joined_time
            FROM Wo_GroupChatUsers gcu
            LEFT JOIN Wo_Users u ON u.user_id = gcu.user_id
            WHERE gcu.group_id = ? AND gcu.active = '1'
            ORDER BY CASE gcu.role WHEN 'owner' THEN 1 WHEN 'admin' THEN 2 WHEN 'moderator' THEN 3 ELSE 4 END, gcu.time ASC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$group_id, $limit, $offset]);
        $members = $stmt->fetchAll();

        sendResponse(['api_status' => 200, 'members' => $members, 'total' => count($members)]);
        break;

    // ==========================================
    // ADD MEMBER
    // ==========================================
    case 'add_user':
    case 'add_member':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        $user_to_add = $_POST['parts'] ?? $_POST['user_id'] ?? '';
        if (empty($user_to_add)) sendError(400, 'parts or user_id is required');

        // Check if group allows member invites or if user is admin
        $isAdmin = isGroupAdminOrModerator($db, $group_id, $current_user_id);
        if (!$isAdmin) {
            // Check settings
            $settingsStmt = $db->prepare("SELECT allow_members_invite FROM Wo_GroupChat WHERE group_id = ?");
            $settingsStmt->execute([$group_id]);
            $settings = $settingsStmt->fetch();
            if (!$settings || !$settings['allow_members_invite']) {
                sendError(403, 'Only admins/moderators can add members');
            }
        }

        $user_ids = array_filter(array_map('intval', explode(',', $user_to_add)));
        $added = 0;
        $time = time();

        foreach ($user_ids as $uid) {
            if ($uid < 1) continue;
            $stmt = $db->prepare("SELECT id, active FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
            $stmt->execute([$group_id, $uid]);
            $existing = $stmt->fetch();

            if ($existing) {
                if ($existing['active'] != '1') {
                    $db->prepare("UPDATE Wo_GroupChatUsers SET active = '1', time = ? WHERE group_id = ? AND user_id = ?")->execute([$time, $group_id, $uid]);
                    $added++;
                }
            } else {
                $db->prepare("INSERT INTO Wo_GroupChatUsers (user_id, group_id, role, admin, active, time) VALUES (?, ?, 'member', '0', '1', ?)")->execute([$uid, $group_id, $time]);
                $added++;
            }
        }

        sendResponse(['api_status' => 200, 'message' => "Added $added members successfully"]);
        break;

    // ==========================================
    // REMOVE MEMBER
    // ==========================================
    case 'remove_user':
    case 'remove_member':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can remove members');
        }

        $user_to_remove = $_POST['parts'] ?? $_POST['user_id'] ?? '';
        if (empty($user_to_remove)) sendError(400, 'parts or user_id is required');

        $user_ids = array_filter(array_map('intval', explode(',', $user_to_remove)));
        $removed = 0;

        foreach ($user_ids as $uid) {
            // Don't remove owner
            $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
            $stmt->execute([$group_id, $uid]);
            $role = $stmt->fetchColumn();
            if ($role !== 'owner') {
                $db->prepare("UPDATE Wo_GroupChatUsers SET active = '0' WHERE group_id = ? AND user_id = ?")->execute([$group_id, $uid]);
                $removed++;
            }
        }

        sendResponse(['api_status' => 200, 'message' => "Removed $removed members"]);
        break;

    // ==========================================
    // LEAVE GROUP
    // ==========================================
    case 'leave':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
        $stmt->execute([$group_id, $current_user_id]);
        $role = $stmt->fetchColumn();

        if ($role === 'owner') sendError(400, 'Owner cannot leave. Transfer ownership or delete the group.');

        $db->prepare("UPDATE Wo_GroupChatUsers SET active = '0' WHERE group_id = ? AND user_id = ?")->execute([$group_id, $current_user_id]);
        sendResponse(['api_status' => 200, 'message' => 'Left group successfully']);
        break;

    // ==========================================
    // SET ROLE
    // ==========================================
    case 'set_admin':
    case 'set_role':
        $group_id = (int)($_POST['id'] ?? $_POST['group_id'] ?? 0);
        $target_user_id = (int)($_POST['user_id'] ?? 0);
        $new_role = $_POST['role'] ?? 'admin';

        if ($group_id < 1 || $target_user_id < 1) sendError(400, 'id and user_id are required');
        if (!in_array($new_role, ['admin', 'moderator', 'member'])) sendError(400, 'Invalid role');

        if (!isGroupOwner($db, $group_id, $current_user_id)) {
            // Admins can set moderators
            if ($new_role !== 'moderator' || !isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
                sendError(403, 'Insufficient permissions to change roles');
            }
        }

        $admin_flag = in_array($new_role, ['admin', 'owner']) ? '1' : '0';
        $db->prepare("UPDATE Wo_GroupChatUsers SET role = ?, admin = ? WHERE group_id = ? AND user_id = ? AND role != 'owner'")
            ->execute([$new_role, $admin_flag, $group_id, $target_user_id]);

        sendResponse(['api_status' => 200, 'message' => "Role updated to $new_role"]);
        break;

    // ==========================================
    // SEND MESSAGE (with encryption)
    // ==========================================
    case 'send_message':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        $text = $_POST['text'] ?? '';
        $media = $_POST['media'] ?? '';
        $topic_id = (int)($_POST['topic_id'] ?? 0);

        if (empty(trim($text)) && empty($media)) sendError(400, 'text or media is required');

        // Check membership
        $stmt = $db->prepare("SELECT id, role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ? AND active = '1'");
        $stmt->execute([$group_id, $current_user_id]);
        $membership = $stmt->fetch();
        if (!$membership) sendError(403, 'You are not a member of this group');

        // Check slow mode
        $slowMode = getGroupSetting($db, $group_id, 'slow_mode_seconds');
        if ($slowMode > 0 && !in_array($membership['role'], ['owner', 'admin', 'moderator'])) {
            $stmt = $db->prepare("SELECT MAX(time) as last_msg_time FROM Wo_Messages WHERE group_id = ? AND from_id = ?");
            $stmt->execute([$group_id, $current_user_id]);
            $lastMsg = $stmt->fetch();
            if ($lastMsg && $lastMsg['last_msg_time'] && (time() - $lastMsg['last_msg_time']) < $slowMode) {
                $remaining = $slowMode - (time() - $lastMsg['last_msg_time']);
                sendError(429, "Slow mode active. Wait {$remaining} seconds.");
            }
        }

        // Check anti-spam
        $antiSpam = getGroupSetting($db, $group_id, 'anti_spam_enabled');
        if ($antiSpam && !in_array($membership['role'], ['owner', 'admin', 'moderator'])) {
            $maxPerMinute = getGroupSetting($db, $group_id, 'max_messages_per_minute') ?: 20;
            $minuteAgo = time() - 60;
            $stmt = $db->prepare("SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ? AND from_id = ? AND time >= ?");
            $stmt->execute([$group_id, $current_user_id, $minuteAgo]);
            $msgCount = $stmt->fetchColumn();
            if ($msgCount >= $maxPerMinute) {
                sendError(429, 'Anti-spam: too many messages per minute');
            }
        }

        // Check member permissions
        $memberRole = $membership['role'];
        if (!in_array($memberRole, ['owner', 'admin', 'moderator'])) {
            // Check media permission
            if (!empty($media)) {
                $allowMedia = getGroupSetting($db, $group_id, 'allow_members_send_media');
                if ($allowMedia === '0' || $allowMedia === 0) {
                    sendError(403, 'Media sending is not allowed in this group');
                }
            }
            // Check links permission
            if (!empty($text) && preg_match('/https?:\/\//', $text)) {
                $allowLinks = getGroupSetting($db, $group_id, 'allow_members_send_links');
                if ($allowLinks === '0' || $allowLinks === 0) {
                    sendError(403, 'Links are not allowed in this group');
                }
            }
        }

        $time = time();
        $reply_to = !empty($_POST['reply_id']) ? (int)$_POST['reply_id'] : null;

        // Encrypt message
        $use_gcm = !empty($_POST['use_gcm']) && $_POST['use_gcm'] == 'true';
        $encrypted_text = $text;
        $iv = null;
        $tag = null;
        $cipher_version = 1;

        if (!empty($text)) {
            if ($use_gcm && class_exists('CryptoHelper')) {
                $encrypted_data = CryptoHelper::encryptGCM($text, $time);
                if ($encrypted_data !== false) {
                    $encrypted_text = $encrypted_data['text'];
                    $iv = $encrypted_data['iv'];
                    $tag = $encrypted_data['tag'];
                    $cipher_version = $encrypted_data['cipher_version'];
                }
            } else {
                $encrypted_text = openssl_encrypt($text, "AES-128-ECB", $time);
                $cipher_version = 1;
            }
        }

        // Check if iv/tag columns exist
        $hasIvColumn = columnExists($db, 'Wo_Messages', 'iv');

        if ($hasIvColumn) {
            $stmt = $db->prepare("INSERT INTO Wo_Messages (from_id, group_id, to_id, text, media, time, seen, iv, tag, cipher_version, message_reply_id) VALUES (?, ?, 0, ?, ?, ?, 0, ?, ?, ?, ?)");
            $stmt->execute([$current_user_id, $group_id, $encrypted_text, $media, $time, $iv, $tag, $cipher_version, $reply_to]);
        } else {
            $stmt = $db->prepare("INSERT INTO Wo_Messages (from_id, group_id, to_id, text, media, time, seen, message_reply_id) VALUES (?, ?, 0, ?, ?, ?, 0, ?)");
            $stmt->execute([$current_user_id, $group_id, $encrypted_text, $media, $time, $reply_to]);
        }

        $message_id = $db->lastInsertId();
        sendResponse(['api_status' => 200, 'message' => 'Message sent successfully', 'message_id' => $message_id]);
        break;

    // ==========================================
    // GET MESSAGES
    // ==========================================
    case 'get_messages':
        $group_id = (int)($_POST['id'] ?? $_GET['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        $limit = (int)($_POST['limit'] ?? 50);
        $before_message_id = (int)($_POST['before_message_id'] ?? 0);
        $topic_id = (int)($_POST['topic_id'] ?? 0);

        // Check membership
        $stmt = $db->prepare("SELECT id, time AS join_time FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ? AND active = '1'");
        $stmt->execute([$group_id, $current_user_id]);
        $membership = $stmt->fetch();
        if (!$membership) sendError(403, 'You are not a member of this group');

        // History visibility for new members
        $historyVisible = getGroupSetting($db, $group_id, 'history_visible_for_new_members');
        $historyCount = getGroupSetting($db, $group_id, 'history_messages_count') ?: 100;

        $sql = "SELECT m.*, u.username, u.avatar, CONCAT(u.first_name, ' ', u.last_name) AS user_name,
                CASE WHEN m.media != '' AND m.media IS NOT NULL THEN 'media' ELSE 'text' END AS type
                FROM Wo_Messages m LEFT JOIN Wo_Users u ON m.from_id = u.user_id WHERE m.group_id = ?";
        $params = [$group_id];

        if ($before_message_id > 0) {
            $sql .= " AND m.id < ?";
            $params[] = $before_message_id;
        }

        $sql .= " ORDER BY m.id DESC LIMIT ?";
        $params[] = $limit;

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $messages = array_reverse($stmt->fetchAll());

        sendResponse(['api_status' => 200, 'messages' => $messages]);
        break;

    // ==========================================
    // UPLOAD AVATAR
    // ==========================================
    case 'upload_avatar':
        $group_id = (int)($_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can change avatar');
        }

        if (!isset($_FILES['avatar']) || $_FILES['avatar']['error'] != UPLOAD_ERR_OK) {
            sendError(400, 'Avatar file is required');
        }

        $file = $_FILES['avatar'];
        $allowed_types = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
        $file_type = mime_content_type($file['tmp_name']);
        if (!in_array($file_type, $allowed_types)) sendError(400, 'Invalid file type');
        if ($file['size'] > 5 * 1024 * 1024) sendError(400, 'File too large (max 5MB)');

        $upload_dir = '/var/www/www-root/data/www/worldmates.club/upload/photos/' . date('Y/m') . '/';
        if (!file_exists($upload_dir)) mkdir($upload_dir, 0777, true);

        $ext = pathinfo($file['name'], PATHINFO_EXTENSION);
        $new_filename = 'group_' . $group_id . '_' . time() . '.' . $ext;
        $relative_path = 'upload/photos/' . date('Y/m') . '/' . $new_filename;

        if (!move_uploaded_file($file['tmp_name'], $upload_dir . $new_filename)) {
            sendError(500, 'Failed to upload file');
        }

        $db->prepare("UPDATE Wo_GroupChat SET avatar = ? WHERE group_id = ?")->execute([$relative_path, $group_id]);
        sendResponse(['api_status' => 200, 'message' => 'Avatar uploaded', 'avatarUrl' => $relative_path, 'avatar' => $relative_path]);
        break;

    // ==========================================
    // GENERATE QR CODE
    // ==========================================
    case 'generate_qr':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can generate QR codes');
        }

        // Ensure qr_code column exists
        ensureColumn($db, 'Wo_GroupChat', 'qr_code', 'VARCHAR(64) NULL');

        $stmt = $db->prepare("SELECT qr_code, group_name FROM Wo_GroupChat WHERE group_id = ?");
        $stmt->execute([$group_id]);
        $group = $stmt->fetch();
        if (!$group) sendError(404, 'Group not found');

        if (!empty($group['qr_code'])) {
            $qr_code = $group['qr_code'];
        } else {
            $qr_code = 'WMG_' . strtoupper(substr(md5(uniqid($group_id, true)), 0, 16));
            $db->prepare("UPDATE Wo_GroupChat SET qr_code = ? WHERE group_id = ?")->execute([$qr_code, $group_id]);
        }

        $join_url = 'https://worldmates.club/join-group/' . $qr_code;

        sendResponse([
            'api_status' => 200,
            'message' => 'QR code generated',
            'qr_code' => $qr_code,
            'join_url' => $join_url,
            'group_id' => $group_id,
            'group_name' => $group['group_name']
        ]);
        break;

    // ==========================================
    // JOIN BY QR CODE
    // ==========================================
    case 'join_by_qr':
        $qr_code = trim($_POST['qr_code'] ?? '');
        if (empty($qr_code)) sendError(400, 'qr_code is required');

        ensureColumn($db, 'Wo_GroupChat', 'qr_code', 'VARCHAR(64) NULL');

        $stmt = $db->prepare("SELECT group_id, group_name, description, avatar, is_private FROM Wo_GroupChat WHERE qr_code = ? AND (type = 'group' OR type IS NULL OR type = '')");
        $stmt->execute([$qr_code]);
        $group = $stmt->fetch();

        if (!$group) sendError(404, 'Invalid QR code or group not found');

        $group_id = $group['group_id'];

        // Check if already member
        $stmt = $db->prepare("SELECT id, active FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
        $stmt->execute([$group_id, $current_user_id]);
        $existing = $stmt->fetch();

        if ($existing && $existing['active'] == '1') {
            sendError(400, 'You are already a member of this group');
        }

        $time = time();

        if ($group['is_private']) {
            // Private group - create join request
            $stmt = $db->prepare("SELECT id FROM Wo_GroupJoinRequests WHERE group_id = ? AND user_id = ? AND status = 'pending'");
            $stmt->execute([$group_id, $current_user_id]);
            if ($stmt->fetch()) {
                sendError(400, 'You already have a pending join request');
            }

            $db->prepare("INSERT INTO Wo_GroupJoinRequests (group_id, user_id, status, created_time) VALUES (?, ?, 'pending', ?)")
                ->execute([$group_id, $current_user_id, $time]);

            sendResponse([
                'api_status' => 200,
                'message' => 'Join request sent. Waiting for admin approval.',
                'status' => 'pending',
                'group' => buildGroupObject($group)
            ]);
        } else {
            // Public group - join directly
            if ($existing) {
                $db->prepare("UPDATE Wo_GroupChatUsers SET active = '1', time = ? WHERE group_id = ? AND user_id = ?")->execute([$time, $group_id, $current_user_id]);
            } else {
                $db->prepare("INSERT INTO Wo_GroupChatUsers (group_id, user_id, role, admin, active, time) VALUES (?, ?, 'member', '0', '1', ?)")
                    ->execute([$group_id, $current_user_id, $time]);
            }

            sendResponse([
                'api_status' => 200,
                'message' => 'Successfully joined the group',
                'status' => 'joined',
                'group' => buildGroupObject($group)
            ]);
        }
        break;

    // ==========================================
    // JOIN PUBLIC GROUP (without QR)
    // ==========================================
    case 'join_group':
        $group_id = (int)($_POST['id'] ?? $_POST['group_id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        $stmt = $db->prepare("SELECT group_id, group_name, is_private FROM Wo_GroupChat WHERE group_id = ? AND (type = 'group' OR type IS NULL OR type = '')");
        $stmt->execute([$group_id]);
        $group = $stmt->fetch();
        if (!$group) sendError(404, 'Group not found');

        if ($group['is_private']) {
            // Create join request for private group
            $stmt = $db->prepare("SELECT id FROM Wo_GroupJoinRequests WHERE group_id = ? AND user_id = ? AND status = 'pending'");
            $stmt->execute([$group_id, $current_user_id]);
            if ($stmt->fetch()) sendError(400, 'You already have a pending join request');

            $message = trim($_POST['message'] ?? '');
            $db->prepare("INSERT INTO Wo_GroupJoinRequests (group_id, user_id, message, status, created_time) VALUES (?, ?, ?, 'pending', ?)")
                ->execute([$group_id, $current_user_id, $message, time()]);

            sendResponse(['api_status' => 200, 'message' => 'Join request sent', 'status' => 'pending']);
        } else {
            // Join public group
            $time = time();
            $stmt = $db->prepare("SELECT id, active FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
            $stmt->execute([$group_id, $current_user_id]);
            $existing = $stmt->fetch();

            if ($existing && $existing['active'] == '1') sendError(400, 'Already a member');
            if ($existing) {
                $db->prepare("UPDATE Wo_GroupChatUsers SET active = '1', time = ? WHERE group_id = ? AND user_id = ?")->execute([$time, $group_id, $current_user_id]);
            } else {
                $db->prepare("INSERT INTO Wo_GroupChatUsers (group_id, user_id, role, admin, active, time) VALUES (?, ?, 'member', '0', '1', ?)")->execute([$group_id, $current_user_id, $time]);
            }
            sendResponse(['api_status' => 200, 'message' => 'Joined successfully', 'status' => 'joined']);
        }
        break;

    // ==========================================
    // GET JOIN REQUESTS
    // ==========================================
    case 'get_join_requests':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can view join requests');
        }

        $stmt = $db->prepare("
            SELECT jr.id, jr.group_id, jr.user_id, u.username,
                   CONCAT(u.first_name, ' ', u.last_name) AS user_name,
                   u.avatar AS user_avatar, jr.message, jr.status, jr.created_time
            FROM Wo_GroupJoinRequests jr
            LEFT JOIN Wo_Users u ON u.user_id = jr.user_id
            WHERE jr.group_id = ? AND jr.status = 'pending'
            ORDER BY jr.created_time DESC
        ");
        $stmt->execute([$group_id]);
        $requests = $stmt->fetchAll();

        sendResponse(['api_status' => 200, 'join_requests' => $requests]);
        break;

    // ==========================================
    // APPROVE JOIN REQUEST
    // ==========================================
    case 'approve_join_request':
        $request_id = (int)($_POST['request_id'] ?? 0);
        if ($request_id < 1) sendError(400, 'request_id is required');

        $stmt = $db->prepare("SELECT group_id, user_id FROM Wo_GroupJoinRequests WHERE id = ? AND status = 'pending'");
        $stmt->execute([$request_id]);
        $request = $stmt->fetch();
        if (!$request) sendError(404, 'Request not found');

        if (!isGroupAdminOrOwner($db, $request['group_id'], $current_user_id)) {
            sendError(403, 'Only admins can approve requests');
        }

        $time = time();
        $db->prepare("UPDATE Wo_GroupJoinRequests SET status = 'approved', reviewed_by = ?, reviewed_time = ? WHERE id = ?")
            ->execute([$current_user_id, $time, $request_id]);

        // Add to group
        $stmt = $db->prepare("SELECT id FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ?");
        $stmt->execute([$request['group_id'], $request['user_id']]);
        if ($stmt->fetch()) {
            $db->prepare("UPDATE Wo_GroupChatUsers SET active = '1', time = ? WHERE group_id = ? AND user_id = ?")->execute([$time, $request['group_id'], $request['user_id']]);
        } else {
            $db->prepare("INSERT INTO Wo_GroupChatUsers (group_id, user_id, role, admin, active, time) VALUES (?, ?, 'member', '0', '1', ?)")
                ->execute([$request['group_id'], $request['user_id'], $time]);
        }

        sendResponse(['api_status' => 200, 'message' => 'Join request approved']);
        break;

    // ==========================================
    // REJECT JOIN REQUEST
    // ==========================================
    case 'reject_join_request':
        $request_id = (int)($_POST['request_id'] ?? 0);
        if ($request_id < 1) sendError(400, 'request_id is required');

        $stmt = $db->prepare("SELECT group_id FROM Wo_GroupJoinRequests WHERE id = ? AND status = 'pending'");
        $stmt->execute([$request_id]);
        $request = $stmt->fetch();
        if (!$request) sendError(404, 'Request not found');

        if (!isGroupAdminOrOwner($db, $request['group_id'], $current_user_id)) {
            sendError(403, 'Only admins can reject requests');
        }

        $db->prepare("UPDATE Wo_GroupJoinRequests SET status = 'rejected', reviewed_by = ?, reviewed_time = ? WHERE id = ?")
            ->execute([$current_user_id, time(), $request_id]);

        sendResponse(['api_status' => 200, 'message' => 'Join request rejected']);
        break;

    // ==========================================
    // UPDATE SETTINGS
    // ==========================================
    case 'update_settings':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can update settings');
        }

        $updates = [];
        $params = [];

        $settingsMap = [
            'is_private' => 'is_private',
            'slow_mode_seconds' => 'slow_mode_seconds',
            'history_visible' => 'history_visible_for_new_members',
            'history_messages_count' => 'history_messages_count',
            'anti_spam_enabled' => 'anti_spam_enabled',
            'max_messages_per_minute' => 'max_messages_per_minute',
            'auto_mute_spammers' => 'auto_mute_spammers',
            'block_new_users_media' => 'block_new_users_media',
            'new_user_restriction_hours' => 'new_user_restriction_hours',
            'allow_members_send_media' => 'allow_members_send_media',
            'allow_members_send_links' => 'allow_members_send_links',
            'allow_members_send_stickers' => 'allow_members_send_stickers',
            'allow_members_send_gifs' => 'allow_members_send_gifs',
            'allow_members_send_polls' => 'allow_members_send_polls',
            'allow_members_invite' => 'allow_members_invite',
            'allow_members_pin' => 'allow_members_pin',
            'allow_members_delete_messages' => 'allow_members_delete_messages',
            'allow_voice_calls' => 'allow_voice_calls',
            'allow_video_calls' => 'allow_video_calls'
        ];

        foreach ($settingsMap as $postKey => $dbCol) {
            if (isset($_POST[$postKey])) {
                $updates[] = "$dbCol = ?";
                $params[] = is_numeric($_POST[$postKey]) ? (int)$_POST[$postKey] : ($_POST[$postKey] === 'true' ? 1 : 0);
            }
        }

        if (empty($updates)) sendError(400, 'No settings to update');

        $params[] = $group_id;
        $db->prepare("UPDATE Wo_GroupChat SET " . implode(', ', $updates) . " WHERE group_id = ?")->execute($params);

        sendResponse(['api_status' => 200, 'message' => 'Settings updated successfully']);
        break;

    // ==========================================
    // GET SETTINGS
    // ==========================================
    case 'get_settings':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        $stmt = $db->prepare("SELECT * FROM Wo_GroupChat WHERE group_id = ?");
        $stmt->execute([$group_id]);
        $group = $stmt->fetch();
        if (!$group) sendError(404, 'Group not found');

        $settings = [
            'is_private' => (bool)($group['is_private'] ?? 0),
            'slow_mode_seconds' => (int)($group['slow_mode_seconds'] ?? 0),
            'history_visible_for_new_members' => (bool)($group['history_visible_for_new_members'] ?? 1),
            'history_messages_count' => (int)($group['history_messages_count'] ?? 100),
            'anti_spam_enabled' => (bool)($group['anti_spam_enabled'] ?? 0),
            'max_messages_per_minute' => (int)($group['max_messages_per_minute'] ?? 20),
            'auto_mute_spammers' => (bool)($group['auto_mute_spammers'] ?? 1),
            'block_new_users_media' => (bool)($group['block_new_users_media'] ?? 0),
            'new_user_restriction_hours' => (int)($group['new_user_restriction_hours'] ?? 24),
            'allow_members_send_media' => (bool)($group['allow_members_send_media'] ?? 1),
            'allow_members_send_links' => (bool)($group['allow_members_send_links'] ?? 1),
            'allow_members_send_stickers' => (bool)($group['allow_members_send_stickers'] ?? 1),
            'allow_members_send_gifs' => (bool)($group['allow_members_send_gifs'] ?? 1),
            'allow_members_send_polls' => (bool)($group['allow_members_send_polls'] ?? 1),
            'allow_members_invite' => (bool)($group['allow_members_invite'] ?? 0),
            'allow_members_pin' => (bool)($group['allow_members_pin'] ?? 0),
            'allow_members_delete_messages' => (bool)($group['allow_members_delete_messages'] ?? 0),
            'allow_voice_calls' => (bool)($group['allow_voice_calls'] ?? 1),
            'allow_video_calls' => (bool)($group['allow_video_calls'] ?? 1)
        ];

        sendResponse(['api_status' => 200, 'settings' => $settings]);
        break;

    // ==========================================
    // UPDATE PRIVACY
    // ==========================================
    case 'update_privacy':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        $is_private = isset($_POST['is_private']) ? ((int)$_POST['is_private']) : null;

        if ($group_id < 1) sendError(400, 'group_id is required');
        if ($is_private === null) sendError(400, 'is_private is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can change privacy');
        }

        $db->prepare("UPDATE Wo_GroupChat SET is_private = ? WHERE group_id = ?")->execute([$is_private ? 1 : 0, $group_id]);
        sendResponse(['api_status' => 200, 'message' => $is_private ? 'Group is now private' : 'Group is now public']);
        break;

    // ==========================================
    // GET STATISTICS
    // ==========================================
    case 'get_statistics':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        $today_start = strtotime('today');
        $week_start = strtotime('-7 days');
        $month_start = strtotime('-30 days');

        $membersCount = $db->prepare("SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = ? AND active = '1'");
        $membersCount->execute([$group_id]);
        $membersCount = (int)$membersCount->fetchColumn();

        $messagesCount = $db->prepare("SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ?");
        $messagesCount->execute([$group_id]);
        $messagesCount = (int)$messagesCount->fetchColumn();

        $msgToday = $db->prepare("SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ? AND time >= ?");
        $msgToday->execute([$group_id, $today_start]);
        $msgToday = (int)$msgToday->fetchColumn();

        $msgWeek = $db->prepare("SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ? AND time >= ?");
        $msgWeek->execute([$group_id, $week_start]);
        $msgWeek = (int)$msgWeek->fetchColumn();

        $msgMonth = $db->prepare("SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ? AND time >= ?");
        $msgMonth->execute([$group_id, $month_start]);
        $msgMonth = (int)$msgMonth->fetchColumn();

        $newMembersWeek = $db->prepare("SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = ? AND time >= ?");
        $newMembersWeek->execute([$group_id, $week_start]);
        $newMembersWeek = (int)$newMembersWeek->fetchColumn();

        $activeMembers24h = $db->prepare("SELECT COUNT(DISTINCT from_id) FROM Wo_Messages WHERE group_id = ? AND time >= ?");
        $activeMembers24h->execute([$group_id, time() - 86400]);
        $activeMembers24h = (int)$activeMembers24h->fetchColumn();

        $activeMembersWeek = $db->prepare("SELECT COUNT(DISTINCT from_id) FROM Wo_Messages WHERE group_id = ? AND time >= ?");
        $activeMembersWeek->execute([$group_id, $week_start]);
        $activeMembersWeek = (int)$activeMembersWeek->fetchColumn();

        // Media count
        $mediaCount = $db->prepare("SELECT COUNT(*) FROM Wo_Messages WHERE group_id = ? AND media IS NOT NULL AND media != ''");
        $mediaCount->execute([$group_id]);
        $mediaCount = (int)$mediaCount->fetchColumn();

        // Top contributors
        $topStmt = $db->prepare("
            SELECT m.from_id AS user_id, u.username, CONCAT(u.first_name, ' ', u.last_name) AS name,
                   u.avatar, COUNT(*) AS messages_count
            FROM Wo_Messages m LEFT JOIN Wo_Users u ON u.user_id = m.from_id
            WHERE m.group_id = ? AND m.time >= ?
            GROUP BY m.from_id ORDER BY messages_count DESC LIMIT 10
        ");
        $topStmt->execute([$group_id, $month_start]);
        $topContributors = $topStmt->fetchAll();

        sendResponse([
            'api_status' => 200,
            'statistics' => [
                'members_count' => $membersCount,
                'messages_count' => $messagesCount,
                'messages_today' => $msgToday,
                'messages_this_week' => $msgWeek,
                'messages_this_month' => $msgMonth,
                'new_members_week' => $newMembersWeek,
                'active_members_24h' => $activeMembers24h,
                'active_members_week' => $activeMembersWeek,
                'media_count' => $mediaCount,
                'top_contributors' => $topContributors
            ]
        ]);
        break;

    // ==========================================
    // SCHEDULED POSTS
    // ==========================================
    case 'get_scheduled_posts':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrModerator($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins/moderators can view scheduled posts');
        }

        $stmt = $db->prepare("
            SELECT sp.*, u.username AS author_username, CONCAT(u.first_name, ' ', u.last_name) AS author_name
            FROM Wo_ScheduledPosts sp
            LEFT JOIN Wo_Users u ON sp.author_id = u.user_id
            WHERE sp.group_id = ? AND sp.status IN ('scheduled', 'failed')
            ORDER BY sp.scheduled_time ASC
        ");
        $stmt->execute([$group_id]);
        $posts = $stmt->fetchAll();

        sendResponse(['api_status' => 200, 'scheduled_posts' => $posts]);
        break;

    case 'create_scheduled_post':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrModerator($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins/moderators can create scheduled posts');
        }

        $text = trim($_POST['text'] ?? '');
        $scheduled_time = (int)($_POST['scheduled_time'] ?? 0);
        $media_url = trim($_POST['media_url'] ?? '');
        $repeat_type = $_POST['repeat_type'] ?? 'none';
        $is_pinned = ($_POST['is_pinned'] ?? '0') === '1' ? 1 : 0;
        $notify_members = ($_POST['notify_members'] ?? '1') === '1' ? 1 : 0;

        if (empty($text)) sendError(400, 'text is required');
        if ($scheduled_time < time()) sendError(400, 'scheduled_time must be in the future');

        $stmt = $db->prepare("
            INSERT INTO Wo_ScheduledPosts (group_id, author_id, text, media_url, scheduled_time, created_time, status, repeat_type, is_pinned, notify_members)
            VALUES (?, ?, ?, ?, ?, ?, 'scheduled', ?, ?, ?)
        ");
        $stmt->execute([$group_id, $current_user_id, $text, $media_url ?: null, $scheduled_time, time(), $repeat_type, $is_pinned, $notify_members]);

        $postId = $db->lastInsertId();
        sendResponse(['api_status' => 200, 'message' => 'Scheduled post created', 'post_id' => $postId]);
        break;

    case 'delete_scheduled_post':
        $post_id = (int)($_POST['post_id'] ?? 0);
        if ($post_id < 1) sendError(400, 'post_id is required');

        $stmt = $db->prepare("SELECT group_id FROM Wo_ScheduledPosts WHERE id = ?");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch();
        if (!$post) sendError(404, 'Scheduled post not found');

        if (!isGroupAdminOrModerator($db, $post['group_id'], $current_user_id)) {
            sendError(403, 'Only admins/moderators can delete scheduled posts');
        }

        $db->prepare("DELETE FROM Wo_ScheduledPosts WHERE id = ?")->execute([$post_id]);
        sendResponse(['api_status' => 200, 'message' => 'Scheduled post deleted']);
        break;

    case 'publish_scheduled_post':
        $post_id = (int)($_POST['post_id'] ?? 0);
        if ($post_id < 1) sendError(400, 'post_id is required');

        $stmt = $db->prepare("SELECT * FROM Wo_ScheduledPosts WHERE id = ? AND status = 'scheduled'");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch();
        if (!$post) sendError(404, 'Scheduled post not found');

        if (!isGroupAdminOrModerator($db, $post['group_id'], $current_user_id)) {
            sendError(403, 'Only admins/moderators can publish posts');
        }

        // Send as message
        $time = time();
        $encrypted_text = openssl_encrypt($post['text'], "AES-128-ECB", $time);
        $stmt = $db->prepare("INSERT INTO Wo_Messages (from_id, group_id, to_id, text, media, time, seen) VALUES (?, ?, 0, ?, ?, ?, 0)");
        $stmt->execute([$post['author_id'], $post['group_id'], $encrypted_text, $post['media_url'], $time]);

        // Mark as published
        $db->prepare("UPDATE Wo_ScheduledPosts SET status = 'published' WHERE id = ?")->execute([$post_id]);

        sendResponse(['api_status' => 200, 'message' => 'Post published']);
        break;

    // ==========================================
    // SUBGROUPS (TOPICS)
    // ==========================================
    case 'get_subgroups':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        $stmt = $db->prepare("
            SELECT s.*, u.username AS creator_username,
                   (SELECT COUNT(*) FROM Wo_Messages WHERE group_id = s.parent_group_id AND topic_id = s.id) AS messages_count
            FROM Wo_Subgroups s
            LEFT JOIN Wo_Users u ON s.created_by = u.user_id
            WHERE s.parent_group_id = ? AND s.is_closed = 0
            ORDER BY s.created_time ASC
        ");
        $stmt->execute([$group_id]);
        $subgroups = $stmt->fetchAll();

        sendResponse(['api_status' => 200, 'subgroups' => $subgroups]);
        break;

    case 'create_subgroup':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can create subgroups');
        }

        $name = trim($_POST['name'] ?? '');
        $description = trim($_POST['description'] ?? '');
        $color = trim($_POST['color'] ?? '#2196F3');
        $icon_emoji = trim($_POST['icon_emoji'] ?? '');
        $is_private = ($_POST['is_private'] ?? '0') === '1' ? 1 : 0;

        if (empty($name)) sendError(400, 'name is required');

        $stmt = $db->prepare("INSERT INTO Wo_Subgroups (parent_group_id, name, description, icon_emoji, color, is_private, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$group_id, $name, $description, $icon_emoji ?: null, $color, $is_private, $current_user_id, time()]);

        $subgroupId = $db->lastInsertId();
        sendResponse(['api_status' => 200, 'message' => 'Subgroup created', 'subgroup_id' => $subgroupId]);
        break;

    case 'delete_subgroup':
        $subgroup_id = (int)($_POST['subgroup_id'] ?? 0);
        if ($subgroup_id < 1) sendError(400, 'subgroup_id is required');

        $stmt = $db->prepare("SELECT parent_group_id FROM Wo_Subgroups WHERE id = ?");
        $stmt->execute([$subgroup_id]);
        $subgroup = $stmt->fetch();
        if (!$subgroup) sendError(404, 'Subgroup not found');

        if (!isGroupAdminOrOwner($db, $subgroup['parent_group_id'], $current_user_id)) {
            sendError(403, 'Only admins can delete subgroups');
        }

        $db->prepare("UPDATE Wo_Subgroups SET is_closed = 1 WHERE id = ?")->execute([$subgroup_id]);
        sendResponse(['api_status' => 200, 'message' => 'Subgroup deleted']);
        break;

    // ==========================================
    // MUTE/UNMUTE GROUP
    // ==========================================
    case 'mute':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        ensureColumn($db, 'Wo_GroupChatUsers', 'muted', 'TINYINT(1) DEFAULT 0');
        $db->prepare("UPDATE Wo_GroupChatUsers SET muted = 1 WHERE group_id = ? AND user_id = ?")->execute([$group_id, $current_user_id]);
        sendResponse(['api_status' => 200, 'message' => 'Group muted']);
        break;

    case 'unmute':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        $db->prepare("UPDATE Wo_GroupChatUsers SET muted = 0 WHERE group_id = ? AND user_id = ?")->execute([$group_id, $current_user_id]);
        sendResponse(['api_status' => 200, 'message' => 'Group unmuted']);
        break;

    // ==========================================
    // PIN/UNPIN MESSAGE
    // ==========================================
    case 'pin_message':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        $message_id = (int)($_POST['message_id'] ?? 0);
        if ($group_id < 1 || $message_id < 1) sendError(400, 'group_id and message_id are required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            // Check if members can pin
            $canPin = getGroupSetting($db, $group_id, 'allow_members_pin');
            if (!$canPin) sendError(403, 'Only admins can pin messages');
        }

        ensureColumn($db, 'Wo_GroupChat', 'pinned_message_id', 'BIGINT NULL');
        $db->prepare("UPDATE Wo_GroupChat SET pinned_message_id = ? WHERE group_id = ?")->execute([$message_id, $group_id]);
        sendResponse(['api_status' => 200, 'message' => 'Message pinned']);
        break;

    case 'unpin_message':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrOwner($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins can unpin messages');
        }

        $db->prepare("UPDATE Wo_GroupChat SET pinned_message_id = NULL WHERE group_id = ?")->execute([$group_id]);
        sendResponse(['api_status' => 200, 'message' => 'Message unpinned']);
        break;

    // ==========================================
    // SEARCH GROUP MESSAGES
    // ==========================================
    case 'search_messages':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        $query = trim($_POST['query'] ?? '');
        if ($group_id < 1 || empty($query)) sendError(400, 'group_id and query are required');

        $limit = (int)($_POST['limit'] ?? 50);

        $stmt = $db->prepare("
            SELECT m.id, m.from_id, m.text, m.time, m.media, u.username,
                   CONCAT(u.first_name, ' ', u.last_name) AS sender_name, u.avatar AS sender_avatar
            FROM Wo_Messages m LEFT JOIN Wo_Users u ON m.from_id = u.user_id
            WHERE m.group_id = ? AND m.text LIKE ?
            ORDER BY m.time DESC LIMIT ?
        ");
        $stmt->execute([$group_id, "%$query%", $limit]);
        $messages = $stmt->fetchAll();

        sendResponse(['api_status' => 200, 'messages' => $messages, 'total' => count($messages)]);
        break;

    // ==========================================
    // INVITE LINK
    // ==========================================
    case 'generate_invite_link':
        $group_id = (int)($_POST['group_id'] ?? $_POST['id'] ?? 0);
        if ($group_id < 1) sendError(400, 'group_id is required');

        if (!isGroupAdminOrModerator($db, $group_id, $current_user_id)) {
            sendError(403, 'Only admins/moderators can generate invite links');
        }

        // Use QR code as invite code
        ensureColumn($db, 'Wo_GroupChat', 'qr_code', 'VARCHAR(64) NULL');
        $stmt = $db->prepare("SELECT qr_code, group_name, is_private FROM Wo_GroupChat WHERE group_id = ?");
        $stmt->execute([$group_id]);
        $group = $stmt->fetch();

        if (empty($group['qr_code'])) {
            $qr_code = 'WMG_' . strtoupper(substr(md5(uniqid($group_id, true)), 0, 16));
            $db->prepare("UPDATE Wo_GroupChat SET qr_code = ? WHERE group_id = ?")->execute([$qr_code, $group_id]);
        } else {
            $qr_code = $group['qr_code'];
        }

        $invite_url = 'https://worldmates.club/join-group/' . $qr_code;

        sendResponse([
            'api_status' => 200,
            'invite_link' => $invite_url,
            'qr_code' => $qr_code,
            'group_name' => $group['group_name'],
            'requires_approval' => (bool)$group['is_private']
        ]);
        break;

    // ==========================================
    // DEFAULT
    // ==========================================
    default:
        sendError(400, "Unknown action type: $type");
}

// ==============================================
// HELPER FUNCTIONS
// ==============================================

function getFullGroupResponse($db, $user_id, $group_id) {
    $stmt = $db->prepare("
        SELECT g.group_id AS id, g.group_name AS name, g.avatar, g.description,
               g.user_id AS admin_id, g.is_private, g.time AS created_time, g.type,
               g.pinned_message_id, g.slow_mode_seconds,
               CONCAT(u.first_name, ' ', u.last_name) AS admin_name,
               (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND active = '1') AS members_count,
               (SELECT role FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?) AS user_role,
               (SELECT COUNT(*) FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ? AND active = '1') AS is_member,
               COALESCE((SELECT muted FROM Wo_GroupChatUsers WHERE group_id = g.group_id AND user_id = ?), 0) AS is_muted
        FROM Wo_GroupChat g
        LEFT JOIN Wo_Users u ON g.user_id = u.user_id
        WHERE g.group_id = ? AND (g.type = 'group' OR g.type IS NULL OR g.type = '')
    ");
    $stmt->execute([$user_id, $user_id, $user_id, $group_id]);
    $group = $stmt->fetch();

    if (!$group) return ['api_status' => 404, 'error_message' => 'Group not found'];

    $group['is_private'] = (bool)$group['is_private'];
    $group['is_member'] = (bool)$group['is_member'];
    $group['is_muted'] = (bool)$group['is_muted'];
    $group['is_admin'] = in_array($group['user_role'], ['owner', 'admin']);
    $group['is_moderator'] = in_array($group['user_role'], ['owner', 'admin', 'moderator']);
    $group['members_count'] = (int)$group['members_count'];
    $group['admin_name'] = $group['admin_name'] ?? '';
    unset($group['user_role']);

    // Pinned message
    if (!empty($group['pinned_message_id'])) {
        $stmt = $db->prepare("SELECT m.id, m.from_id, m.text, m.time, u.username AS sender_username,
                              CONCAT(u.first_name, ' ', u.last_name) AS sender_name
                              FROM Wo_Messages m LEFT JOIN Wo_Users u ON m.from_id = u.user_id WHERE m.id = ?");
        $stmt->execute([$group['pinned_message_id']]);
        $group['pinned_message'] = $stmt->fetch() ?: null;
    }

    // Settings
    $group['settings'] = getGroupSettings($db, $group_id);

    // Members
    $stmtM = $db->prepare("SELECT gcu.user_id, u.username, CONCAT(u.first_name, ' ', u.last_name) AS name, u.avatar, gcu.role
                           FROM Wo_GroupChatUsers gcu LEFT JOIN Wo_Users u ON gcu.user_id = u.user_id
                           WHERE gcu.group_id = ? AND gcu.active = '1'");
    $stmtM->execute([$group_id]);
    $group['members'] = $stmtM->fetchAll();

    return ['api_status' => 200, 'group' => $group, 'data' => $group];
}

function buildGroupObject($group) {
    return [
        'id' => (int)$group['group_id'],
        'name' => $group['group_name'],
        'description' => $group['description'] ?? '',
        'avatar' => $group['avatar'] ?? '',
        'is_private' => (bool)$group['is_private'],
        'members_count' => 0,
        'is_member' => false,
        'is_admin' => false
    ];
}

function getGroupSettings($db, $group_id) {
    $stmt = $db->prepare("SELECT * FROM Wo_GroupChat WHERE group_id = ?");
    $stmt->execute([$group_id]);
    $g = $stmt->fetch();
    if (!$g) return null;

    return [
        'allow_members_invite' => (bool)($g['allow_members_invite'] ?? 0),
        'allow_members_pin' => (bool)($g['allow_members_pin'] ?? 0),
        'allow_members_delete_messages' => (bool)($g['allow_members_delete_messages'] ?? 0),
        'allow_voice_calls' => (bool)($g['allow_voice_calls'] ?? 1),
        'allow_video_calls' => (bool)($g['allow_video_calls'] ?? 1),
        'slow_mode_seconds' => (int)($g['slow_mode_seconds'] ?? 0),
        'history_visible_for_new_members' => (bool)($g['history_visible_for_new_members'] ?? 1),
        'history_messages_count' => (int)($g['history_messages_count'] ?? 100),
        'allow_members_send_media' => (bool)($g['allow_members_send_media'] ?? 1),
        'allow_members_send_stickers' => (bool)($g['allow_members_send_stickers'] ?? 1),
        'allow_members_send_gifs' => (bool)($g['allow_members_send_gifs'] ?? 1),
        'allow_members_send_links' => (bool)($g['allow_members_send_links'] ?? 1),
        'allow_members_send_polls' => (bool)($g['allow_members_send_polls'] ?? 1),
        'anti_spam_enabled' => (bool)($g['anti_spam_enabled'] ?? 0),
        'max_messages_per_minute' => (int)($g['max_messages_per_minute'] ?? 20),
        'auto_mute_spammers' => (bool)($g['auto_mute_spammers'] ?? 1),
        'block_new_users_media' => (bool)($g['block_new_users_media'] ?? 0),
        'new_user_restriction_hours' => (int)($g['new_user_restriction_hours'] ?? 24)
    ];
}

function getGroupSetting($db, $group_id, $column) {
    try {
        $stmt = $db->prepare("SELECT `$column` FROM Wo_GroupChat WHERE group_id = ?");
        $stmt->execute([$group_id]);
        return $stmt->fetchColumn();
    } catch (Exception $e) {
        return null;
    }
}

function isGroupOwner($db, $group_id, $user_id) {
    // Check Wo_GroupChat.user_id (creator)
    $stmt = $db->prepare("SELECT user_id FROM Wo_GroupChat WHERE group_id = ?");
    $stmt->execute([$group_id]);
    $row = $stmt->fetch();
    if ($row && $row['user_id'] == $user_id) return true;

    // Check role
    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ? AND active = '1'");
    $stmt->execute([$group_id, $user_id]);
    return $stmt->fetchColumn() === 'owner';
}

function isGroupAdminOrOwner($db, $group_id, $user_id) {
    if (isGroupOwner($db, $group_id, $user_id)) return true;

    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ? AND active = '1'");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();
    return in_array($role, ['owner', 'admin']);
}

function isGroupAdminOrModerator($db, $group_id, $user_id) {
    if (isGroupAdminOrOwner($db, $group_id, $user_id)) return true;

    $stmt = $db->prepare("SELECT role FROM Wo_GroupChatUsers WHERE group_id = ? AND user_id = ? AND active = '1'");
    $stmt->execute([$group_id, $user_id]);
    $role = $stmt->fetchColumn();
    return $role === 'moderator';
}

function columnExists($db, $table, $column) {
    try {
        $stmt = $db->prepare("SHOW COLUMNS FROM `$table` LIKE ?");
        $stmt->execute([$column]);
        return $stmt->rowCount() > 0;
    } catch (Exception $e) {
        return false;
    }
}

function ensureColumn($db, $table, $column, $definition) {
    if (!columnExists($db, $table, $column)) {
        try {
            $db->exec("ALTER TABLE `$table` ADD COLUMN `$column` $definition");
        } catch (Exception $e) {
            logMessage("Could not add column $column to $table: " . $e->getMessage(), 'WARNING');
        }
    }
}

function ensureGroupSettingsColumns($db) {
    $columns = [
        'is_private' => 'TINYINT(1) DEFAULT 0',
        'slow_mode_seconds' => 'INT DEFAULT 0',
        'history_visible_for_new_members' => 'TINYINT(1) DEFAULT 1',
        'history_messages_count' => 'INT DEFAULT 100',
        'anti_spam_enabled' => 'TINYINT(1) DEFAULT 0',
        'max_messages_per_minute' => 'INT DEFAULT 20',
        'auto_mute_spammers' => 'TINYINT(1) DEFAULT 1',
        'block_new_users_media' => 'TINYINT(1) DEFAULT 0',
        'new_user_restriction_hours' => 'INT DEFAULT 24',
        'allow_members_send_media' => 'TINYINT(1) DEFAULT 1',
        'allow_members_send_links' => 'TINYINT(1) DEFAULT 1',
        'allow_members_send_stickers' => 'TINYINT(1) DEFAULT 1',
        'allow_members_send_gifs' => 'TINYINT(1) DEFAULT 1',
        'allow_members_send_polls' => 'TINYINT(1) DEFAULT 1',
        'allow_members_invite' => 'TINYINT(1) DEFAULT 0',
        'allow_members_pin' => 'TINYINT(1) DEFAULT 0',
        'allow_members_delete_messages' => 'TINYINT(1) DEFAULT 0',
        'allow_voice_calls' => 'TINYINT(1) DEFAULT 1',
        'allow_video_calls' => 'TINYINT(1) DEFAULT 1',
        'qr_code' => 'VARCHAR(64) NULL',
        'pinned_message_id' => 'BIGINT NULL'
    ];

    foreach ($columns as $col => $def) {
        ensureColumn($db, 'Wo_GroupChat', $col, $def);
    }

    // Wo_GroupChatUsers columns
    ensureColumn($db, 'Wo_GroupChatUsers', 'role', "VARCHAR(20) DEFAULT 'member'");
    ensureColumn($db, 'Wo_GroupChatUsers', 'muted', 'TINYINT(1) DEFAULT 0');
    ensureColumn($db, 'Wo_GroupChatUsers', 'active', "TINYINT(1) DEFAULT 1");
}

function ensureScheduledPostsTable($db) {
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS Wo_ScheduledPosts (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            group_id BIGINT NULL,
            channel_id BIGINT NULL,
            author_id BIGINT NOT NULL,
            text TEXT NOT NULL,
            media_url TEXT NULL,
            media_type VARCHAR(20) NULL,
            scheduled_time BIGINT NOT NULL,
            created_time BIGINT NOT NULL,
            status ENUM('scheduled', 'published', 'failed', 'cancelled') DEFAULT 'scheduled',
            repeat_type VARCHAR(20) DEFAULT 'none',
            is_pinned TINYINT(1) DEFAULT 0,
            notify_members TINYINT(1) DEFAULT 1,
            INDEX(group_id),
            INDEX(channel_id),
            INDEX(status),
            INDEX(scheduled_time)
        )");
    } catch (Exception $e) {
        logMessage("Could not create Wo_ScheduledPosts: " . $e->getMessage(), 'WARNING');
    }
}

function ensureSubgroupsTable($db) {
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS Wo_Subgroups (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            parent_group_id BIGINT NOT NULL,
            name VARCHAR(100) NOT NULL,
            description TEXT NULL,
            icon_emoji VARCHAR(10) NULL,
            color VARCHAR(10) DEFAULT '#2196F3',
            is_private TINYINT(1) DEFAULT 0,
            is_closed TINYINT(1) DEFAULT 0,
            created_by BIGINT NOT NULL,
            created_time BIGINT NOT NULL,
            last_message_time BIGINT NULL,
            pinned_message_id BIGINT NULL,
            INDEX(parent_group_id),
            INDEX(is_closed)
        )");
    } catch (Exception $e) {
        logMessage("Could not create Wo_Subgroups: " . $e->getMessage(), 'WARNING');
    }

    // Ensure topic_id in messages
    ensureColumn($db, 'Wo_Messages', 'topic_id', 'BIGINT DEFAULT 0');
}

function ensureJoinRequestsTable($db) {
    try {
        $db->exec("CREATE TABLE IF NOT EXISTS Wo_GroupJoinRequests (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            group_id BIGINT NOT NULL,
            user_id BIGINT NOT NULL,
            message TEXT NULL,
            status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
            created_time BIGINT,
            reviewed_by BIGINT NULL,
            reviewed_time BIGINT NULL,
            INDEX(group_id),
            INDEX(user_id),
            INDEX(status)
        )");
    } catch (Exception $e) {
        logMessage("Could not create Wo_GroupJoinRequests: " . $e->getMessage(), 'WARNING');
    }
}
?>
