<?php
/**
 * 📢 Channels API Endpoint
 * Handles all channel-related operations
 *
 * Supported actions/types:
 * - get_channel_details: Get channel info
 * - get_channel_posts: Get posts in a channel
 * - create_post: Create a new post
 * - update_post: Update existing post
 * - delete_post: Delete a post
 * - pin_post/unpin_post: Pin/unpin post
 * - get_comments: Get comments on a post
 * - add_comment: Add comment to post
 * - delete_comment: Delete a comment
 * - add_post_reaction/remove_post_reaction: React to posts
 * - add_comment_reaction: React to comments
 * - register_post_view: Register view on post
 * - get_channel_statistics: Get channel stats
 * - get_channel_subscribers: Get subscriber list
 * - add_channel_admin/remove_channel_admin: Manage admins
 * - update_settings: Update channel settings
 * - subscribe_channel/unsubscribe_channel: Subscribe/unsubscribe
 */

header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Load configuration
require_once(__DIR__ . '/config.php');

// Get action/type (support both for backward compatibility)
$action = $_POST['action'] ?? $_GET['action'] ?? $_POST['type'] ?? $_GET['type'] ?? '';

// Get access token
$access_token = $_GET['access_token'] ?? $_POST['access_token'] ?? '';

if (empty($access_token)) {
    echo json_encode([
        'api_status' => 401,
        'error_message' => 'Access token is required'
    ]);
    exit;
}

// Validate token
$user_id = validateAccessToken($db, $access_token);

if (!$user_id) {
    echo json_encode([
        'api_status' => 401,
        'error_message' => 'Invalid or expired access token'
    ]);
    exit;
}

// Helper function to get user info
function getChannelUserInfo($db, $userId) {
    $stmt = $db->prepare("SELECT user_id, username, first_name, last_name, avatar FROM Wo_Users WHERE user_id = ?");
    $stmt->execute([$userId]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    if ($user) {
        return [
            'id' => (int)$user['user_id'],
            'username' => $user['username'],
            'name' => trim($user['first_name'] . ' ' . $user['last_name']),
            'avatar' => $user['avatar']
        ];
    }
    return null;
}

// Helper function to check if user is channel admin
function isChannelAdmin($db, $userId, $channelId) {
    // Check if user is owner
    $stmt = $db->prepare("SELECT user_id FROM Wo_Pages WHERE page_id = ?");
    $stmt->execute([$channelId]);
    $channel = $stmt->fetch(PDO::FETCH_ASSOC);
    if ($channel && (int)$channel['user_id'] === (int)$userId) {
        return 'owner';
    }

    // Check if user is admin
    $stmt = $db->prepare("SELECT role FROM page_admins WHERE page_id = ? AND user_id = ?");
    $stmt->execute([$channelId, $userId]);
    $admin = $stmt->fetch(PDO::FETCH_ASSOC);
    if ($admin) {
        return $admin['role'] ?? 'admin';
    }

    return false;
}

// Helper: Format channel post
function formatChannelPost($row, $db, $currentUserId) {
    $media = [];
    if (!empty($row['postFile'])) {
        $media[] = [
            'url' => $row['postFile'],
            'type' => strpos($row['postFile'], '.mp4') !== false || strpos($row['postFile'], '.webm') !== false ? 'video' : 'image'
        ];
    }
    if (!empty($row['postPhoto'])) {
        $media[] = [
            'url' => $row['postPhoto'],
            'type' => 'image'
        ];
    }

    // Get user info
    $user = getChannelUserInfo($db, $row['user_id']);

    return [
        'id' => (int)$row['id'],
        'channel_id' => (int)$row['page_id'],
        'author_id' => (int)$row['user_id'],
        'author_username' => $user ? $user['username'] : 'Unknown',
        'author_name' => $user ? $user['name'] : 'Unknown',
        'author_avatar' => $user ? $user['avatar'] : '',
        'text' => $row['postText'] ?? '',
        'media' => $media,
        'created_time' => (int)($row['time'] ?? time()),
        'is_pinned' => (bool)($row['pinned'] ?? false),
        'views_count' => (int)($row['post_views'] ?? 0),
        'comments_count' => (int)($row['comments'] ?? 0),
        'reactions_count' => (int)($row['reaction'] ?? 0),
        'shares_count' => (int)($row['shares'] ?? 0)
    ];
}

// Handle different actions
switch ($action) {
    // ======================== GET CHANNEL DETAILS ========================
    case 'get_channel_details':
        $channel_id = (int)($_POST['channel_id'] ?? $_GET['channel_id'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        $stmt = $db->prepare("SELECT * FROM Wo_Pages WHERE page_id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$channel) {
            echo json_encode(['api_status' => 404, 'error_message' => 'Channel not found']);
            exit;
        }

        // Get subscriber count
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM Wo_Pages_Likes WHERE page_id = ?");
        $stmt->execute([$channel_id]);
        $subCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'] ?? 0;

        // Get posts count
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM Wo_Posts WHERE page_id = ? AND active = 1");
        $stmt->execute([$channel_id]);
        $postsCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'] ?? 0;

        // Check if user is subscribed
        $stmt = $db->prepare("SELECT * FROM Wo_Pages_Likes WHERE page_id = ? AND user_id = ?");
        $stmt->execute([$channel_id, $user_id]);
        $isSubscribed = $stmt->fetch() !== false;

        // Check if user is admin
        $adminRole = isChannelAdmin($db, $user_id, $channel_id);

        // Get admins list
        $stmt = $db->prepare("
            SELECT u.user_id, u.username, u.first_name, u.last_name, u.avatar, pa.role
            FROM page_admins pa
            JOIN Wo_Users u ON pa.user_id = u.user_id
            WHERE pa.page_id = ?
        ");
        $stmt->execute([$channel_id]);
        $admins = [];
        while ($admin = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $admins[] = [
                'id' => (int)$admin['user_id'],
                'username' => $admin['username'],
                'name' => trim($admin['first_name'] . ' ' . $admin['last_name']),
                'avatar' => $admin['avatar'],
                'role' => $admin['role']
            ];
        }

        // Add owner to admins
        $owner = getChannelUserInfo($db, $channel['user_id']);
        if ($owner) {
            array_unshift($admins, [
                'id' => $owner['id'],
                'username' => $owner['username'],
                'name' => $owner['name'],
                'avatar' => $owner['avatar'],
                'role' => 'owner'
            ]);
        }

        echo json_encode([
            'api_status' => 200,
            'channel' => [
                'id' => (int)$channel['page_id'],
                'name' => $channel['page_name'],
                'username' => $channel['page_name'],
                'description' => $channel['page_description'] ?? '',
                'avatar_url' => $channel['avatar'] ?? '',
                'cover_url' => $channel['cover'] ?? '',
                'subscribers_count' => (int)$subCount,
                'posts_count' => (int)$postsCount,
                'is_subscribed' => $isSubscribed,
                'is_admin' => $adminRole !== false,
                'admin_role' => $adminRole ?: null,
                'created_time' => (int)($channel['registered'] ?? time()),
                'settings' => [
                    'allow_comments' => true,
                    'allow_reactions' => true,
                    'notify_on_post' => true
                ]
            ],
            'admins' => $admins
        ]);
        break;

    // ======================== GET CHANNEL POSTS ========================
    case 'get_channel_posts':
        $channel_id = (int)($_POST['channel_id'] ?? $_GET['channel_id'] ?? 0);
        $limit = (int)($_POST['limit'] ?? $_GET['limit'] ?? 20);
        $before_post_id = (int)($_POST['before_post_id'] ?? $_GET['before_post_id'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        $limit = min(max($limit, 1), 50);

        $sql = "SELECT * FROM Wo_Posts WHERE page_id = ? AND active = 1";
        $params = [$channel_id];

        if ($before_post_id > 0) {
            $sql .= " AND id < ?";
            $params[] = $before_post_id;
        }

        $sql .= " ORDER BY pinned DESC, time DESC LIMIT ?";
        $params[] = $limit;

        $stmt = $db->prepare($sql);
        $stmt->execute($params);

        $posts = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $posts[] = formatChannelPost($row, $db, $user_id);
        }

        echo json_encode([
            'api_status' => 200,
            'posts' => $posts
        ]);
        break;

    // ======================== CREATE POST ========================
    case 'create_post':
        $channel_id = (int)($_POST['channel_id'] ?? 0);
        $text = trim($_POST['text'] ?? '');
        $media_urls = $_POST['media_urls'] ?? null;
        $disable_comments = (int)($_POST['disable_comments'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        // Check admin rights
        if (!isChannelAdmin($db, $user_id, $channel_id)) {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only admins can create posts']);
            exit;
        }

        if (empty($text) && empty($media_urls)) {
            echo json_encode(['api_status' => 400, 'error_message' => 'Post must have text or media']);
            exit;
        }

        // Parse media
        $postFile = '';
        $postPhoto = '';
        if ($media_urls) {
            $mediaArray = json_decode($media_urls, true);
            if (is_array($mediaArray) && !empty($mediaArray)) {
                foreach ($mediaArray as $media) {
                    $url = $media['url'] ?? '';
                    $type = $media['type'] ?? 'image';
                    if ($type === 'video') {
                        $postFile = $url;
                    } else {
                        $postPhoto = $url;
                    }
                }
            }
        }

        // Generate unique post hash
        $postHash = md5($user_id . time() . rand(1000, 9999));

        // Insert post
        $stmt = $db->prepare("
            INSERT INTO Wo_Posts (user_id, page_id, postText, postFile, postPhoto, time, active, post_type, postHash)
            VALUES (?, ?, ?, ?, ?, ?, 1, 'page', ?)
        ");
        $stmt->execute([$user_id, $channel_id, $text, $postFile, $postPhoto, time(), $postHash]);

        $postId = $db->lastInsertId();

        // Get created post
        $stmt = $db->prepare("SELECT * FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$postId]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        echo json_encode([
            'api_status' => 200,
            'message' => 'Post created successfully',
            'post' => formatChannelPost($post, $db, $user_id)
        ]);
        break;

    // ======================== UPDATE POST ========================
    case 'update_post':
        $post_id = (int)($_POST['post_id'] ?? 0);
        $text = trim($_POST['text'] ?? '');

        if (!$post_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
            exit;
        }

        // Get post and check permissions
        $stmt = $db->prepare("SELECT * FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$post) {
            echo json_encode(['api_status' => 404, 'error_message' => 'Post not found']);
            exit;
        }

        $channel_id = (int)$post['page_id'];
        if (!isChannelAdmin($db, $user_id, $channel_id) && (int)$post['user_id'] !== $user_id) {
            echo json_encode(['api_status' => 403, 'error_message' => 'No permission to edit this post']);
            exit;
        }

        $stmt = $db->prepare("UPDATE Wo_Posts SET postText = ? WHERE id = ?");
        $stmt->execute([$text, $post_id]);

        // Get updated post
        $stmt = $db->prepare("SELECT * FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        echo json_encode([
            'api_status' => 200,
            'message' => 'Post updated',
            'post' => formatChannelPost($post, $db, $user_id)
        ]);
        break;

    // ======================== DELETE POST ========================
    case 'delete_post':
        $post_id = (int)($_POST['post_id'] ?? 0);

        if (!$post_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
            exit;
        }

        // Get post
        $stmt = $db->prepare("SELECT * FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$post) {
            echo json_encode(['api_status' => 404, 'error_message' => 'Post not found']);
            exit;
        }

        $channel_id = (int)$post['page_id'];
        if (!isChannelAdmin($db, $user_id, $channel_id) && (int)$post['user_id'] !== $user_id) {
            echo json_encode(['api_status' => 403, 'error_message' => 'No permission to delete this post']);
            exit;
        }

        $stmt = $db->prepare("DELETE FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$post_id]);

        echo json_encode([
            'api_status' => 200,
            'message' => 'Post deleted'
        ]);
        break;

    // ======================== PIN/UNPIN POST ========================
    case 'pin_post':
    case 'unpin_post':
        $post_id = (int)($_POST['post_id'] ?? 0);
        $isPinning = $action === 'pin_post';

        if (!$post_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
            exit;
        }

        $stmt = $db->prepare("SELECT page_id FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$post_id]);
        $post = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$post) {
            echo json_encode(['api_status' => 404, 'error_message' => 'Post not found']);
            exit;
        }

        if (!isChannelAdmin($db, $user_id, $post['page_id'])) {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only admins can pin/unpin posts']);
            exit;
        }

        // If pinning, first unpin all other posts
        if ($isPinning) {
            $stmt = $db->prepare("UPDATE Wo_Posts SET pinned = 0 WHERE page_id = ?");
            $stmt->execute([$post['page_id']]);
        }

        $stmt = $db->prepare("UPDATE Wo_Posts SET pinned = ? WHERE id = ?");
        $stmt->execute([$isPinning ? 1 : 0, $post_id]);

        echo json_encode([
            'api_status' => 200,
            'message' => $isPinning ? 'Post pinned' : 'Post unpinned'
        ]);
        break;

    // ======================== GET COMMENTS ========================
    case 'get_comments':
        $post_id = (int)($_POST['post_id'] ?? $_GET['post_id'] ?? 0);
        $limit = (int)($_POST['limit'] ?? $_GET['limit'] ?? 50);
        $offset = (int)($_POST['offset'] ?? $_GET['offset'] ?? 0);

        if (!$post_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
            exit;
        }

        $stmt = $db->prepare("
            SELECT c.*, u.username, u.first_name, u.last_name, u.avatar
            FROM Wo_Comments c
            JOIN Wo_Users u ON c.user_id = u.user_id
            WHERE c.post_id = ?
            ORDER BY c.time ASC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$post_id, $limit, $offset]);

        $comments = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $comments[] = [
                'id' => (int)$row['id'],
                'post_id' => (int)$row['post_id'],
                'user_id' => (int)$row['user_id'],
                'username' => $row['username'],
                'user_name' => trim($row['first_name'] . ' ' . $row['last_name']),
                'user_avatar' => $row['avatar'],
                'text' => $row['text'],
                'created_time' => (int)$row['time'],
                'reactions_count' => (int)($row['reaction'] ?? 0)
            ];
        }

        echo json_encode([
            'api_status' => 200,
            'comments' => $comments
        ]);
        break;

    // ======================== ADD COMMENT ========================
    case 'add_comment':
        $post_id = (int)($_POST['post_id'] ?? 0);
        $text = trim($_POST['text'] ?? '');
        $reply_to_id = (int)($_POST['reply_to_id'] ?? 0);

        if (!$post_id || empty($text)) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id and text are required']);
            exit;
        }

        // Check if post exists
        $stmt = $db->prepare("SELECT * FROM Wo_Posts WHERE id = ?");
        $stmt->execute([$post_id]);
        if (!$stmt->fetch()) {
            echo json_encode(['api_status' => 404, 'error_message' => 'Post not found']);
            exit;
        }

        $stmt = $db->prepare("
            INSERT INTO Wo_Comments (user_id, post_id, text, time)
            VALUES (?, ?, ?, ?)
        ");
        $stmt->execute([$user_id, $post_id, $text, time()]);

        // Update comments count
        $stmt = $db->prepare("UPDATE Wo_Posts SET comments = comments + 1 WHERE id = ?");
        $stmt->execute([$post_id]);

        echo json_encode([
            'api_status' => 200,
            'message' => 'Comment added'
        ]);
        break;

    // ======================== DELETE COMMENT ========================
    case 'delete_comment':
        $comment_id = (int)($_POST['comment_id'] ?? 0);

        if (!$comment_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'comment_id is required']);
            exit;
        }

        // Get comment
        $stmt = $db->prepare("SELECT c.*, p.page_id FROM Wo_Comments c JOIN Wo_Posts p ON c.post_id = p.id WHERE c.id = ?");
        $stmt->execute([$comment_id]);
        $comment = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$comment) {
            echo json_encode(['api_status' => 404, 'error_message' => 'Comment not found']);
            exit;
        }

        // Check permission (comment owner or channel admin)
        if ((int)$comment['user_id'] !== $user_id && !isChannelAdmin($db, $user_id, $comment['page_id'])) {
            echo json_encode(['api_status' => 403, 'error_message' => 'No permission to delete this comment']);
            exit;
        }

        $stmt = $db->prepare("DELETE FROM Wo_Comments WHERE id = ?");
        $stmt->execute([$comment_id]);

        // Update comments count
        $stmt = $db->prepare("UPDATE Wo_Posts SET comments = comments - 1 WHERE id = ? AND comments > 0");
        $stmt->execute([$comment['post_id']]);

        echo json_encode([
            'api_status' => 200,
            'message' => 'Comment deleted'
        ]);
        break;

    // ======================== REACTIONS ========================
    case 'add_post_reaction':
    case 'remove_post_reaction':
        $post_id = (int)($_POST['post_id'] ?? 0);
        $emoji = trim($_POST['reaction'] ?? $_POST['emoji'] ?? '👍');

        if (!$post_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
            exit;
        }

        // Check if reaction exists
        $stmt = $db->prepare("SELECT * FROM Wo_Post_Reactions WHERE post_id = ? AND user_id = ?");
        $stmt->execute([$post_id, $user_id]);
        $existing = $stmt->fetch();

        if ($action === 'add_post_reaction') {
            if ($existing) {
                // Update reaction
                $stmt = $db->prepare("UPDATE Wo_Post_Reactions SET reaction = ? WHERE post_id = ? AND user_id = ?");
                $stmt->execute([$emoji, $post_id, $user_id]);
            } else {
                // Insert new reaction
                $stmt = $db->prepare("INSERT INTO Wo_Post_Reactions (post_id, user_id, reaction, time) VALUES (?, ?, ?, ?)");
                $stmt->execute([$post_id, $user_id, $emoji, time()]);

                // Update reaction count
                $stmt = $db->prepare("UPDATE Wo_Posts SET reaction = reaction + 1 WHERE id = ?");
                $stmt->execute([$post_id]);
            }
            echo json_encode(['api_status' => 200, 'message' => 'Reaction added']);
        } else {
            if ($existing) {
                $stmt = $db->prepare("DELETE FROM Wo_Post_Reactions WHERE post_id = ? AND user_id = ?");
                $stmt->execute([$post_id, $user_id]);

                $stmt = $db->prepare("UPDATE Wo_Posts SET reaction = reaction - 1 WHERE id = ? AND reaction > 0");
                $stmt->execute([$post_id]);
            }
            echo json_encode(['api_status' => 200, 'message' => 'Reaction removed']);
        }
        break;

    case 'add_comment_reaction':
        $comment_id = (int)($_POST['comment_id'] ?? 0);
        $emoji = trim($_POST['reaction'] ?? '👍');

        if (!$comment_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'comment_id is required']);
            exit;
        }

        // Simple implementation - just update reaction count
        $stmt = $db->prepare("UPDATE Wo_Comments SET reaction = reaction + 1 WHERE id = ?");
        $stmt->execute([$comment_id]);

        echo json_encode(['api_status' => 200, 'message' => 'Reaction added']);
        break;

    // ======================== REGISTER POST VIEW ========================
    case 'register_post_view':
        $post_id = (int)($_POST['post_id'] ?? 0);

        if (!$post_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'post_id is required']);
            exit;
        }

        $stmt = $db->prepare("UPDATE Wo_Posts SET post_views = post_views + 1 WHERE id = ?");
        $stmt->execute([$post_id]);

        echo json_encode(['api_status' => 200, 'message' => 'View registered']);
        break;

    // ======================== GET CHANNEL STATISTICS ========================
    case 'get_channel_statistics':
        $channel_id = (int)($_POST['channel_id'] ?? $_GET['channel_id'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        if (!isChannelAdmin($db, $user_id, $channel_id)) {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only admins can view statistics']);
            exit;
        }

        // Get subscribers count
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM Wo_Pages_Likes WHERE page_id = ?");
        $stmt->execute([$channel_id]);
        $subscribersCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'] ?? 0;

        // Get total posts
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM Wo_Posts WHERE page_id = ? AND active = 1");
        $stmt->execute([$channel_id]);
        $postsCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'] ?? 0;

        // Get total views
        $stmt = $db->prepare("SELECT SUM(post_views) as total FROM Wo_Posts WHERE page_id = ?");
        $stmt->execute([$channel_id]);
        $totalViews = $stmt->fetch(PDO::FETCH_ASSOC)['total'] ?? 0;

        // Get total reactions
        $stmt = $db->prepare("SELECT SUM(reaction) as total FROM Wo_Posts WHERE page_id = ?");
        $stmt->execute([$channel_id]);
        $totalReactions = $stmt->fetch(PDO::FETCH_ASSOC)['total'] ?? 0;

        echo json_encode([
            'api_status' => 200,
            'statistics' => [
                'subscribers_count' => (int)$subscribersCount,
                'posts_count' => (int)$postsCount,
                'total_views' => (int)$totalViews,
                'total_reactions' => (int)$totalReactions,
                'avg_views_per_post' => $postsCount > 0 ? round($totalViews / $postsCount, 1) : 0
            ]
        ]);
        break;

    // ======================== GET CHANNEL SUBSCRIBERS ========================
    case 'get_channel_subscribers':
        $channel_id = (int)($_POST['channel_id'] ?? $_GET['channel_id'] ?? 0);
        $limit = (int)($_POST['limit'] ?? $_GET['limit'] ?? 100);
        $offset = (int)($_POST['offset'] ?? $_GET['offset'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        $stmt = $db->prepare("
            SELECT u.user_id, u.username, u.first_name, u.last_name, u.avatar
            FROM Wo_Pages_Likes pl
            JOIN Wo_Users u ON pl.user_id = u.user_id
            WHERE pl.page_id = ?
            ORDER BY pl.id DESC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$channel_id, $limit, $offset]);

        $subscribers = [];
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $subscribers[] = [
                'id' => (int)$row['user_id'],
                'user_id' => (int)$row['user_id'],
                'username' => $row['username'],
                'name' => trim($row['first_name'] . ' ' . $row['last_name']),
                'avatar_url' => $row['avatar']
            ];
        }

        echo json_encode([
            'api_status' => 200,
            'subscribers' => $subscribers
        ]);
        break;

    // ======================== SUBSCRIBE/UNSUBSCRIBE ========================
    case 'subscribe_channel':
        $channel_id = (int)($_POST['channel_id'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        // Check if already subscribed
        $stmt = $db->prepare("SELECT * FROM Wo_Pages_Likes WHERE page_id = ? AND user_id = ?");
        $stmt->execute([$channel_id, $user_id]);
        if ($stmt->fetch()) {
            echo json_encode(['api_status' => 200, 'message' => 'Already subscribed']);
            exit;
        }

        $stmt = $db->prepare("INSERT INTO Wo_Pages_Likes (page_id, user_id, time) VALUES (?, ?, ?)");
        $stmt->execute([$channel_id, $user_id, time()]);

        echo json_encode(['api_status' => 200, 'message' => 'Subscribed successfully']);
        break;

    case 'unsubscribe_channel':
        $channel_id = (int)($_POST['channel_id'] ?? 0);

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        $stmt = $db->prepare("DELETE FROM Wo_Pages_Likes WHERE page_id = ? AND user_id = ?");
        $stmt->execute([$channel_id, $user_id]);

        echo json_encode(['api_status' => 200, 'message' => 'Unsubscribed successfully']);
        break;

    // ======================== UPDATE CHANNEL ========================
    case 'update_channel':
        $channel_id = (int)($_POST['channel_id'] ?? 0);
        $name = trim($_POST['name'] ?? '');
        $description = trim($_POST['description'] ?? '');
        $username = trim($_POST['username'] ?? '');

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        if (!isChannelAdmin($db, $user_id, $channel_id)) {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only admins can update channel']);
            exit;
        }

        $updates = [];
        $params = [];

        if (!empty($name)) {
            $updates[] = "page_name = ?";
            $params[] = $name;
        }
        if (!empty($description)) {
            $updates[] = "page_description = ?";
            $params[] = $description;
        }

        if (!empty($updates)) {
            $params[] = $channel_id;
            $stmt = $db->prepare("UPDATE Wo_Pages SET " . implode(', ', $updates) . " WHERE page_id = ?");
            $stmt->execute($params);
        }

        // Get updated channel
        $stmt = $db->prepare("SELECT * FROM Wo_Pages WHERE page_id = ?");
        $stmt->execute([$channel_id]);
        $channel = $stmt->fetch(PDO::FETCH_ASSOC);

        echo json_encode([
            'api_status' => 200,
            'message' => 'Channel updated',
            'channel' => [
                'id' => (int)$channel['page_id'],
                'name' => $channel['page_name'],
                'description' => $channel['page_description'] ?? '',
                'avatar_url' => $channel['avatar'] ?? ''
            ]
        ]);
        break;

    // ======================== UPDATE SETTINGS ========================
    case 'update_settings':
        $channel_id = (int)($_POST['channel_id'] ?? 0);
        $settings_json = $_POST['settings_json'] ?? '{}';

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        if (!isChannelAdmin($db, $user_id, $channel_id)) {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only admins can update settings']);
            exit;
        }

        // For now, just acknowledge - settings can be stored in a separate table if needed
        echo json_encode(['api_status' => 200, 'message' => 'Settings updated']);
        break;

    // ======================== ADMIN MANAGEMENT ========================
    case 'add_channel_admin':
        $channel_id = (int)($_POST['channel_id'] ?? 0);
        $target_user_id = (int)($_POST['user_id'] ?? 0);
        $user_search = trim($_POST['user_search'] ?? '');
        $role = trim($_POST['role'] ?? 'admin');

        if (!$channel_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id is required']);
            exit;
        }

        // Only owner can add admins
        if (isChannelAdmin($db, $user_id, $channel_id) !== 'owner') {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only owner can add admins']);
            exit;
        }

        // Find user by search
        if (empty($target_user_id) && !empty($user_search)) {
            $stmt = $db->prepare("SELECT user_id FROM Wo_Users WHERE username = ? OR email = ?");
            $stmt->execute([$user_search, $user_search]);
            $found = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($found) {
                $target_user_id = (int)$found['user_id'];
            }
        }

        if (!$target_user_id) {
            echo json_encode(['api_status' => 404, 'error_message' => 'User not found']);
            exit;
        }

        // Check if already admin
        $stmt = $db->prepare("SELECT * FROM page_admins WHERE page_id = ? AND user_id = ?");
        $stmt->execute([$channel_id, $target_user_id]);
        if ($stmt->fetch()) {
            echo json_encode(['api_status' => 400, 'error_message' => 'User is already an admin']);
            exit;
        }

        $stmt = $db->prepare("INSERT INTO page_admins (page_id, user_id, role) VALUES (?, ?, ?)");
        $stmt->execute([$channel_id, $target_user_id, $role]);

        echo json_encode(['api_status' => 200, 'message' => 'Admin added']);
        break;

    case 'remove_channel_admin':
        $channel_id = (int)($_POST['channel_id'] ?? 0);
        $target_user_id = (int)($_POST['user_id'] ?? 0);

        if (!$channel_id || !$target_user_id) {
            echo json_encode(['api_status' => 400, 'error_message' => 'channel_id and user_id are required']);
            exit;
        }

        if (isChannelAdmin($db, $user_id, $channel_id) !== 'owner') {
            echo json_encode(['api_status' => 403, 'error_message' => 'Only owner can remove admins']);
            exit;
        }

        $stmt = $db->prepare("DELETE FROM page_admins WHERE page_id = ? AND user_id = ?");
        $stmt->execute([$channel_id, $target_user_id]);

        echo json_encode(['api_status' => 200, 'message' => 'Admin removed']);
        break;

    // ======================== DEFAULT / UNKNOWN ACTION ========================
    default:
        echo json_encode([
            'api_status' => 404,
            'errors' => [
                'error_id' => '1',
                'error_text' => 'Error: 404 API Type Not Found'
            ],
            'error_message' => "Unknown action: $action"
        ]);
        break;
}
?>
