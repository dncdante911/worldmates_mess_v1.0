<?php
/**
 * WorldMates Messenger - API v2 Configuration
 * Database connection and authentication utilities
 */

// ============================================
// DATABASE CONFIGURATION
// ============================================
// TODO: Replace with actual database credentials
define('DB_HOST', 'localhost');           // Database host
define('DB_NAME', 'your_database_name');  // Database name
define('DB_USER', 'your_database_user');  // Database username
define('DB_PASS', 'your_database_pass');  // Database password
define('DB_CHARSET', 'utf8mb4');

// PDO Options
define('PDO_OPTIONS', [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
]);

// ============================================
// PDO DATABASE CONNECTION
// ============================================
try {
    $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
    $db = new PDO($dsn, DB_USER, DB_PASS, PDO_OPTIONS);
} catch (PDOException $e) {
    error_log("Database connection failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'api_status' => 500,
        'error_message' => 'Database connection failed'
    ]);
    exit;
}

// ============================================
// AUTHENTICATION FUNCTIONS
// ============================================

/**
 * Validate access token and return user_id
 *
 * @param PDO $db Database connection
 * @param string $access_token Access token from request
 * @return int|false User ID if valid, false otherwise
 */
function validateAccessToken($db, $access_token) {
    if (empty($access_token)) {
        return false;
    }

    try {
        // Query Wo_AppsSessions table for access token
        $stmt = $db->prepare("
            SELECT user_id, time
            FROM Wo_AppsSessions
            WHERE session_id = :access_token
            LIMIT 1
        ");
        $stmt->execute(['access_token' => $access_token]);
        $session = $stmt->fetch();

        if (!$session) {
            return false;
        }

        // Optional: Check if session is still valid (not expired)
        // Uncomment if you want to enforce session expiration
        /*
        $session_lifetime = 30 * 24 * 60 * 60; // 30 days in seconds
        if (time() - $session['time'] > $session_lifetime) {
            return false; // Session expired
        }
        */

        return (int)$session['user_id'];
    } catch (PDOException $e) {
        error_log("validateAccessToken error: " . $e->getMessage());
        return false;
    }
}

/**
 * Get user data by user_id
 *
 * @param PDO $db Database connection
 * @param int $user_id User ID
 * @return array|false User data array if found, false otherwise
 */
function getUserById($db, $user_id) {
    if (empty($user_id)) {
        return false;
    }

    try {
        $stmt = $db->prepare("
            SELECT user_id, username, name, avatar, verified, lastseen, lastseen_status
            FROM Wo_Users
            WHERE user_id = :user_id
            LIMIT 1
        ");
        $stmt->execute(['user_id' => $user_id]);
        return $stmt->fetch();
    } catch (PDOException $e) {
        error_log("getUserById error: " . $e->getMessage());
        return false;
    }
}

/**
 * Secure input data (prevent SQL injection)
 * Note: This is a legacy function. Use PDO prepared statements instead.
 *
 * @param string $data Input data to secure
 * @return string Secured data
 */
function secureInput($data) {
    return htmlspecialchars(strip_tags(trim($data)), ENT_QUOTES, 'UTF-8');
}

// ============================================
// ERROR HANDLING
// ============================================

/**
 * Send JSON error response
 *
 * @param string $message Error message
 * @param int $code HTTP status code
 */
function sendError($message, $code = 400) {
    http_response_code($code);
    echo json_encode([
        'api_status' => $code,
        'error_message' => $message
    ]);
    exit;
}

/**
 * Send JSON success response
 *
 * @param array $data Response data
 */
function sendSuccess($data) {
    echo json_encode(array_merge(['api_status' => 200], $data));
    exit;
}
