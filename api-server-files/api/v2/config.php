<?php
/**
 * API v2 Configuration
 * Initializes WoWonder framework and validates access_token
 */

// Prevent direct access
if (!defined('API_V2_LOADED')) {
    define('API_V2_LOADED', true);
}

// Error reporting
ini_set('display_errors', 0);
ini_set('log_errors', 1);
error_reporting(E_ALL);

// Set timezone
date_default_timezone_set('Europe/Kyiv');

// Headers
header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Session configuration
@ini_set('session.cookie_httponly', 1);
@ini_set('session.use_only_cookies', 1);
if (session_status() === PHP_SESSION_NONE) {
    @session_start();
}

// Load main WoWonder config from site root
$site_root = $_SERVER['DOCUMENT_ROOT'];
$config_path = $site_root . '/config.php';

if (!file_exists($config_path)) {
    http_response_code(500);
    die(json_encode([
        'api_status' => 500,
        'errors' => ['error_text' => 'Server configuration error: main config.php not found']
    ]));
}

require_once($config_path);

// Change to site root for WoWonder functions
if (getcwd() !== $site_root) {
    chdir($site_root);
}

// Verify database credentials
if (!isset($sql_db_host) || !isset($sql_db_user) || !isset($sql_db_pass) || !isset($sql_db_name)) {
    http_response_code(500);
    die(json_encode([
        'api_status' => 500,
        'errors' => ['error_text' => 'Database credentials not configured']
    ]));
}

// Connect to database
$sqlConnect = @mysqli_connect($sql_db_host, $sql_db_user, $sql_db_pass, $sql_db_name);
if (!$sqlConnect || mysqli_connect_error()) {
    error_log("API v2: Database connection failed: " . mysqli_connect_error());
    http_response_code(500);
    die(json_encode([
        'api_status' => 500,
        'errors' => ['error_text' => 'Database connection failed']
    ]));
}

// Set charset
mysqli_set_charset($sqlConnect, 'utf8mb4');
mysqli_query($sqlConnect, "SET NAMES utf8mb4");

// Initialize global $wo array
if (!isset($wo)) {
    $wo = [];
}
$wo['sqlConnect'] = $sqlConnect;
$wo['site_url'] = $site_url ?? 'https://worldmates.club';

// Load MySQL-Maria Database class
$mysql_maria_path = 'assets/libraries/DB/vendor/joshcam/mysqli-database-class/MySQL-Maria.php';
if (file_exists($mysql_maria_path)) {
    require_once($mysql_maria_path);
    if (!isset($mysqlMaria)) {
        $mysqlMaria = new Mysql;
    }
}

// Load database table constants
$tabels_path = 'assets/includes/tabels.php';
if (file_exists($tabels_path)) {
    require_once($tabels_path);
}

// Load WoWonder core functions
$required_files = [
    'assets/includes/cache.php',
    'assets/includes/functions_general.php',
    'assets/includes/functions_one.php',
    'assets/includes/functions_two.php',
    'assets/includes/functions_three.php'
];

foreach ($required_files as $file) {
    if (file_exists($file)) {
        require_once($file);
    }
}

// Load phone API functions
$phone_functions = $site_root . '/api/phone/core/functions.php';
if (file_exists($phone_functions)) {
    require_once($phone_functions);
}

// Load site config from database
if (function_exists('Wo_GetConfig')) {
    $config = Wo_GetConfig();
    $wo['config'] = $config;
}

// Non-allowed fields filter
$non_allowed = array(
    'password', 'email_code', 'sms_code', 'pro_time',
    'background_image_status', 'type', 'start_up', 'start_up_info',
    'startup_follow', 'startup_image', 'id', 'cover_full', 'cover_org',
    'avatar_org', 'app_session', 'last_email_sent', 'css_file', 'src'
);

// =========================================
// ACCESS TOKEN VALIDATION
// =========================================

$access_token = $_GET['access_token'] ?? $_POST['access_token'] ?? '';
$s = $_GET['s'] ?? $_POST['s'] ?? '';

// Use access_token or s parameter
$token_to_validate = !empty($access_token) ? $access_token : $s;

if (!empty($token_to_validate) && function_exists('Wo_Secure')) {
    $token_secure = Wo_Secure($token_to_validate);

    // Check in app_sessions table
    $session_query = mysqli_query($sqlConnect, "
        SELECT user_id, session_id, platform, time
        FROM " . T_APP_SESSIONS . "
        WHERE session_id = '{$token_secure}'
        LIMIT 1
    ");

    if ($session_query && mysqli_num_rows($session_query) > 0) {
        $session_data = mysqli_fetch_assoc($session_query);
        $user_id = (int)$session_data['user_id'];

        // Get user data
        if (function_exists('Wo_UserData')) {
            $user_data = Wo_UserData($user_id);
            if (!empty($user_data)) {
                $wo['user'] = $user_data;
                $wo['loggedin'] = true;
            }
        }
    }
}

// Set API version
$api_version = '2.0';

// API v2 config loaded successfully
?>
