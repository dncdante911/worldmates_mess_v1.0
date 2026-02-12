<?php
// +------------------------------------------------------------------------+
// | WoWonder - Core Functions (functions_one.php)
// | WorldMates Messenger v1.0
// |
// | Contains critical user management functions:
// |   - Registration, Login, User Data
// |   - Password Reset, Email/Username lookup
// |   - Configuration, Language
// +------------------------------------------------------------------------+

/**
 * Get site configuration from database
 */
function Wo_GetConfig() {
    global $sqlConnect;
    $data = array();
    $query = mysqli_query($sqlConnect, "SELECT * FROM " . T_CONFIG);
    if ($query) {
        while ($row = mysqli_fetch_assoc($query)) {
            $data[$row['name']] = $row['value'];
        }
    }
    return $data;
}

/**
 * Get language names from database
 */
function Wo_LangsNamesFromDB() {
    global $sqlConnect;
    $data = array();
    $query = mysqli_query($sqlConnect, "SELECT DISTINCT `lang_name` FROM " . T_LANGS);
    if ($query) {
        while ($row = mysqli_fetch_assoc($query)) {
            $data[] = $row['lang_name'];
        }
    }
    return $data;
}

/**
 * Get language strings from database
 */
function Wo_LangsFromDB($lang = 'english') {
    global $sqlConnect;
    $data = array();
    $lang = mysqli_real_escape_string($sqlConnect, $lang);
    $query = mysqli_query($sqlConnect, "SELECT `lang_key`, `english` FROM " . T_LANGS . " WHERE 1");
    if ($query) {
        while ($row = mysqli_fetch_assoc($query)) {
            $data[$row['lang_key']] = isset($row[$lang]) ? $row[$lang] : $row['english'];
        }
    }
    // Try to get the specific language column
    $query2 = mysqli_query($sqlConnect, "SHOW COLUMNS FROM " . T_LANGS . " LIKE '{$lang}'");
    if ($query2 && mysqli_num_rows($query2) > 0) {
        $query3 = mysqli_query($sqlConnect, "SELECT `lang_key`, `{$lang}` FROM " . T_LANGS);
        if ($query3) {
            while ($row = mysqli_fetch_assoc($query3)) {
                if (!empty($row[$lang])) {
                    $data[$row['lang_key']] = $row[$lang];
                }
            }
        }
    }
    return $data;
}

/**
 * Get ISO country code (stub)
 */
function GetIso() {
    return 'US';
}

/**
 * Register a new user
 */
function Wo_RegisterUser($registration_data = array()) {
    global $sqlConnect;

    if (empty($registration_data['username']) || empty($registration_data['password'])) {
        return false;
    }

    // Hash the password
    $registration_data['password'] = password_hash($registration_data['password'], PASSWORD_DEFAULT);

    // Set defaults
    if (empty($registration_data['joined'])) {
        $registration_data['joined'] = time();
    }
    if (empty($registration_data['registered'])) {
        $registration_data['registered'] = date('m') . '/' . date('Y');
    }
    if (empty($registration_data['avatar'])) {
        $registration_data['avatar'] = 'upload/photos/d-avatar.jpg';
    }
    if (empty($registration_data['cover'])) {
        $registration_data['cover'] = 'upload/photos/d-cover.jpg';
    }
    if (empty($registration_data['ip_address'])) {
        $registration_data['ip_address'] = get_ip_address();
    }

    // Build INSERT query
    $columns = array();
    $values = array();
    foreach ($registration_data as $key => $value) {
        $columns[] = '`' . mysqli_real_escape_string($sqlConnect, $key) . '`';
        $values[] = "'" . mysqli_real_escape_string($sqlConnect, $value) . "'";
    }

    $query = "INSERT INTO " . T_USERS . " (" . implode(',', $columns) . ") VALUES (" . implode(',', $values) . ")";
    $result = mysqli_query($sqlConnect, $query);

    if ($result) {
        return true;
    }
    return false;
}

/**
 * Check if email exists in database
 */
function Wo_EmailExists($email = '') {
    global $sqlConnect;
    if (empty($email)) {
        return false;
    }
    $email = mysqli_real_escape_string($sqlConnect, $email);
    $query = mysqli_query($sqlConnect, "SELECT COUNT(*) AS count FROM " . T_USERS . " WHERE `email` = '{$email}'");
    if ($query) {
        $row = mysqli_fetch_assoc($query);
        if ($row['count'] > 0) {
            return true;
        }
    }
    return false;
}

/**
 * Check if phone number exists in database
 */
function Wo_PhoneExists($phone = '') {
    global $sqlConnect;
    if (empty($phone)) {
        return false;
    }
    $phone = mysqli_real_escape_string($sqlConnect, $phone);
    $query = mysqli_query($sqlConnect, "SELECT COUNT(*) AS count FROM " . T_USERS . " WHERE `phone_number` = '{$phone}'");
    if ($query) {
        $row = mysqli_fetch_assoc($query);
        if ($row['count'] > 0) {
            return true;
        }
    }
    return false;
}

/**
 * Get user_id from email
 */
function Wo_UserIdFromEmail($email = '') {
    global $sqlConnect;
    if (empty($email)) {
        return 0;
    }
    $email = mysqli_real_escape_string($sqlConnect, $email);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `email` = '{$email}' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        return (int)$row['user_id'];
    }
    return 0;
}

/**
 * Get user_id from username
 */
function Wo_UserIdFromUsername($username = '') {
    global $sqlConnect;
    if (empty($username)) {
        return 0;
    }
    $username = mysqli_real_escape_string($sqlConnect, $username);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `username` = '{$username}' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        return (int)$row['user_id'];
    }
    return 0;
}

/**
 * Get user_id for login (accepts both username and email)
 */
function Wo_UserIdForLogin($username = '') {
    global $sqlConnect;
    if (empty($username)) {
        return 0;
    }
    $username_esc = mysqli_real_escape_string($sqlConnect, $username);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE (`username` = '{$username_esc}' OR `email` = '{$username_esc}' OR `phone_number` = '{$username_esc}') LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        return (int)$row['user_id'];
    }
    return 0;
}

/**
 * Get full user data by user_id
 */
function Wo_UserData($user_id = 0) {
    global $sqlConnect, $wo;
    if (empty($user_id) || !is_numeric($user_id)) {
        return array();
    }

    // Try cache first
    $cached = cache($user_id, 'users', 'read');
    if (!empty($cached)) {
        return $cached;
    }

    $user_id = (int)$user_id;
    $query = mysqli_query($sqlConnect, "SELECT * FROM " . T_USERS . " WHERE `user_id` = {$user_id} LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $data = mysqli_fetch_assoc($query);

        // Process avatar and cover URLs
        if (!empty($wo['config']['site_url'])) {
            $site_url = $wo['config']['site_url'];
        } elseif (!empty($wo['site_url'])) {
            $site_url = $wo['site_url'];
        } else {
            $site_url = '';
        }

        if (!empty($data['avatar']) && strpos($data['avatar'], 'http') !== 0) {
            $data['avatar'] = $site_url . '/' . $data['avatar'];
        }
        if (!empty($data['cover']) && strpos($data['cover'], 'http') !== 0) {
            $data['cover'] = $site_url . '/' . $data['cover'];
        }

        // Add computed fields
        $data['name'] = trim($data['first_name'] . ' ' . $data['last_name']);
        if (empty($data['name'])) {
            $data['name'] = $data['username'];
        }
        $data['url'] = $site_url . '/' . $data['username'];
        $data['id'] = $data['user_id'];
        $data['is_pro'] = isset($data['is_pro']) ? $data['is_pro'] : '0';
        $data['admin'] = isset($data['admin']) ? $data['admin'] : '0';

        // Parse details JSON
        if (!empty($data['details'])) {
            $details = json_decode($data['details'], true);
            if (is_array($details)) {
                foreach ($details as $key => $value) {
                    $data[$key] = $value;
                }
            }
        }

        // Parse notification settings
        if (!empty($data['notification_settings'])) {
            $notif_settings = json_decode($data['notification_settings'], true);
            if (is_array($notif_settings)) {
                foreach ($notif_settings as $key => $value) {
                    $data[$key] = $value;
                }
            }
        }

        // Cache the data
        cache($user_id, 'users', 'write', $data);

        return $data;
    }
    return array();
}

/**
 * Verify login credentials
 */
function Wo_Login($username = '', $password = '') {
    global $sqlConnect;
    if (empty($username) || empty($password)) {
        return false;
    }
    $username_esc = mysqli_real_escape_string($sqlConnect, $username);
    $query = mysqli_query($sqlConnect, "SELECT `password` FROM " . T_USERS . " WHERE (`username` = '{$username_esc}' OR `email` = '{$username_esc}' OR `phone_number` = '{$username_esc}') AND `active` = '1' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        if (password_verify($password, $row['password'])) {
            return true;
        }
    }
    return false;
}

/**
 * Check two-factor authentication status
 * Returns true if 2FA is NOT enabled (user can proceed without 2FA)
 * Returns false if 2FA IS enabled (user needs to verify)
 */
function Wo_TwoFactor($username = '') {
    global $sqlConnect;
    if (empty($username)) {
        return true;
    }
    $username_esc = mysqli_real_escape_string($sqlConnect, $username);
    $query = mysqli_query($sqlConnect, "SELECT `two_factor`, `two_factor_verified` FROM " . T_USERS . " WHERE (`username` = '{$username_esc}' OR `email` = '{$username_esc}') LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        if ($row['two_factor'] == 1 && $row['two_factor_verified'] == 1) {
            return false; // 2FA enabled, needs verification
        }
    }
    return true; // No 2FA, proceed normally
}

/**
 * Check if user is banned
 */
function Wo_IsBanned($username = '') {
    global $sqlConnect;
    if (empty($username)) {
        return false;
    }
    $username_esc = mysqli_real_escape_string($sqlConnect, $username);
    $query = mysqli_query($sqlConnect, "SELECT `active` FROM " . T_USERS . " WHERE (`username` = '{$username_esc}' OR `email` = '{$username_esc}') LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        if ($row['active'] == '2') {
            return true; // User is banned/deleted
        }
    }
    return false;
}

/**
 * Check if user is active
 */
function Wo_UserActive($username = '') {
    global $sqlConnect;
    if (empty($username)) {
        return false;
    }
    $username_esc = mysqli_real_escape_string($sqlConnect, $username);
    $query = mysqli_query($sqlConnect, "SELECT `active` FROM " . T_USERS . " WHERE `username` = '{$username_esc}' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        if ($row['active'] == '1') {
            return true;
        }
    }
    return false;
}

/**
 * Check if login is allowed (brute force prevention)
 */
function WoCanLogin() {
    global $sqlConnect;
    $ip = get_ip_address();
    $ip_esc = mysqli_real_escape_string($sqlConnect, $ip);
    $time_limit = time() - 3600; // 1 hour window
    $query = mysqli_query($sqlConnect, "SELECT COUNT(*) AS count FROM " . T_BAD_LOGIN . " WHERE `ip` = '{$ip_esc}' AND `time` > {$time_limit}");
    if ($query) {
        $row = mysqli_fetch_assoc($query);
        if ($row['count'] >= 5) {
            return false; // Too many attempts
        }
    }
    return true;
}

/**
 * Log a failed login attempt
 */
function WoAddBadLoginLog() {
    global $sqlConnect;
    $ip = get_ip_address();
    $ip_esc = mysqli_real_escape_string($sqlConnect, $ip);
    $time = time();
    mysqli_query($sqlConnect, "INSERT INTO " . T_BAD_LOGIN . " (`ip`, `time`) VALUES ('{$ip_esc}', '{$time}')");
}

/**
 * Create a login session (cookie-based)
 */
function Wo_CreateLoginSession($user_id = 0) {
    global $sqlConnect;
    if (empty($user_id)) {
        return '';
    }
    $session_id = md5(rand(11111, 99999) . time() . $user_id);
    $user_id = (int)$user_id;
    $time = time();

    // Store session
    $_SESSION['user_id'] = $session_id;
    setcookie('user_id', $session_id, time() + 10 * 365 * 24 * 60 * 60);

    // Insert into database
    mysqli_query($sqlConnect, "INSERT INTO " . T_APP_SESSIONS . " (`user_id`, `session_id`, `platform`, `time`) VALUES ('{$user_id}', '{$session_id}', 'web', '{$time}')");

    return $session_id;
}

/**
 * Get user_id from session ID
 */
function Wo_GetUserFromSessionID($session_id = '') {
    global $sqlConnect;
    if (empty($session_id)) {
        return 0;
    }
    $session_id_esc = mysqli_real_escape_string($sqlConnect, $session_id);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_APP_SESSIONS . " WHERE `session_id` = '{$session_id_esc}' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        $row = mysqli_fetch_assoc($query);
        return (int)$row['user_id'];
    }
    return 0;
}

/**
 * Check user session ID validity
 */
function Wo_CheckUserSessionID($user_id = 0, $session_id = '', $platform = 'web') {
    global $sqlConnect;
    if (empty($user_id) || empty($session_id)) {
        return false;
    }
    $user_id = (int)$user_id;
    $session_id_esc = mysqli_real_escape_string($sqlConnect, $session_id);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_APP_SESSIONS . " WHERE `user_id` = '{$user_id}' AND `session_id` = '{$session_id_esc}' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        return true;
    }
    return false;
}

/**
 * Validate password reset token (user_id + password hash format)
 * Token format: user_id_passwordhash
 */
function Wo_isValidPasswordResetToken($code = '') {
    global $sqlConnect;
    if (empty($code)) {
        return false;
    }

    // Token format: user_id_passwordhash
    $parts = explode('_', $code, 2);
    if (count($parts) != 2) {
        return false;
    }

    $user_id = (int)$parts[0];
    $password_hash = $parts[1];

    if ($user_id <= 0 || empty($password_hash)) {
        return false;
    }

    $password_hash_esc = mysqli_real_escape_string($sqlConnect, $password_hash);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `user_id` = {$user_id} AND `password` = '{$password_hash_esc}' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        return true;
    }
    return false;
}

/**
 * Alternative password reset token validation (email_code based)
 */
function Wo_isValidPasswordResetToken2($code = '') {
    global $sqlConnect;
    if (empty($code)) {
        return false;
    }

    $code_esc = mysqli_real_escape_string($sqlConnect, $code);
    $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `email_code` = '{$code_esc}' AND `email_code` != '' LIMIT 1");
    if ($query && mysqli_num_rows($query) > 0) {
        return true;
    }
    return false;
}

/**
 * Auto-follow preconfigured users after registration
 */
function Wo_AutoFollow($user_id = 0) {
    global $sqlConnect, $wo;
    if (empty($user_id) || empty($wo['config']['auto_friend_users'])) {
        return false;
    }
    $auto_users = explode(',', $wo['config']['auto_friend_users']);
    foreach ($auto_users as $follow_id) {
        $follow_id = (int)trim($follow_id);
        if ($follow_id > 0 && $follow_id != $user_id) {
            $time = time();
            // Add follower relationship
            mysqli_query($sqlConnect, "INSERT IGNORE INTO " . T_FOLLOWERS . " (`following_id`, `follower_id`, `active`, `time`) VALUES ('{$follow_id}', '{$user_id}', '1', '{$time}')");
            // Add reverse relationship
            mysqli_query($sqlConnect, "INSERT IGNORE INTO " . T_FOLLOWERS . " (`following_id`, `follower_id`, `active`, `time`) VALUES ('{$user_id}', '{$follow_id}', '1', '{$time}')");
        }
    }
    return true;
}

/**
 * Auto-like pages after registration
 */
function Wo_AutoPageLike($user_id = 0) {
    global $sqlConnect, $wo;
    if (empty($user_id) || empty($wo['config']['auto_page_like'])) {
        return false;
    }
    $auto_pages = explode(',', $wo['config']['auto_page_like']);
    foreach ($auto_pages as $page_id) {
        $page_id = (int)trim($page_id);
        if ($page_id > 0) {
            $time = time();
            mysqli_query($sqlConnect, "INSERT IGNORE INTO " . T_PAGES_LIKES . " (`page_id`, `user_id`, `time`, `active`) VALUES ('{$page_id}', '{$user_id}', '{$time}', '1')");
        }
    }
    return true;
}

/**
 * Auto-join groups after registration
 */
function Wo_AutoGroupJoin($user_id = 0) {
    global $sqlConnect, $wo;
    if (empty($user_id) || empty($wo['config']['auto_group_join'])) {
        return false;
    }
    $auto_groups = explode(',', $wo['config']['auto_group_join']);
    foreach ($auto_groups as $group_id) {
        $group_id = (int)trim($group_id);
        if ($group_id > 0) {
            $time = time();
            mysqli_query($sqlConnect, "INSERT IGNORE INTO " . T_GROUP_MEMBERS . " (`group_id`, `user_id`, `time`, `active`) VALUES ('{$group_id}', '{$user_id}', '{$time}', '1')");
        }
    }
    return true;
}

/**
 * Update user balance (for affiliate system)
 */
function Wo_UpdateBalance($user_id = 0, $amount = 0) {
    global $sqlConnect;
    if (empty($user_id) || empty($amount)) {
        return false;
    }
    $user_id = (int)$user_id;
    $amount = floatval($amount);
    $query = mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `balance` = `balance` + {$amount} WHERE `user_id` = {$user_id}");
    if ($query) {
        cache($user_id, 'users', 'delete');
        return true;
    }
    return false;
}

/**
 * Add affiliate reference
 */
function AddNewRef($ref_user_id, $user_id, $amount) {
    global $sqlConnect;
    $ref_user_id = (int)$ref_user_id;
    $user_id = (int)$user_id;
    $amount = floatval($amount);
    $time = time();
    mysqli_query($sqlConnect, "UPDATE " . T_USERS . " SET `balance` = `balance` + {$amount} WHERE `user_id` = {$ref_user_id}");
    cache($ref_user_id, 'users', 'delete');
    return true;
}

/**
 * Check if user is admin
 */
function Wo_IsAdmin() {
    global $wo;
    if (!empty($wo['user']['admin']) && $wo['user']['admin'] != '0') {
        return true;
    }
    return false;
}

/**
 * Get banned IPs/users
 */
function Wo_GetBanned($type = '') {
    global $sqlConnect;
    $data = array();
    $query = mysqli_query($sqlConnect, "SELECT `ip` FROM " . T_BANNED_IP);
    if ($query) {
        while ($row = mysqli_fetch_assoc($query)) {
            $data[] = $row['ip'];
        }
    }
    return $data;
}

/**
 * Check if user is logged in (web session)
 */
function Wo_IsLogged() {
    if (!empty($_SESSION['user_id']) || !empty($_COOKIE['user_id'])) {
        return true;
    }
    return false;
}

/**
 * Validate access token for API
 */
if (!function_exists('Wo_ValidateAccessToken')) {
    function Wo_ValidateAccessToken($access_token = '') {
        global $sqlConnect;
        if (empty($access_token)) {
            return false;
        }
        $access_token_esc = mysqli_real_escape_string($sqlConnect, $access_token);
        $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_APP_SESSIONS . " WHERE `session_id` = '{$access_token_esc}' LIMIT 1");
        if ($query && mysqli_num_rows($query) > 0) {
            $row = mysqli_fetch_assoc($query);
            return (int)$row['user_id'];
        }
        return false;
    }
}

/**
 * Check if name/username exists
 * Returns array with check results
 */
if (!function_exists('Wo_IsNameExist')) {
    function Wo_IsNameExist($username, $active = 0) {
        global $sqlConnect;
        $result = array(false, false, false);
        if (empty($username)) {
            return $result;
        }
        $username_esc = mysqli_real_escape_string($sqlConnect, $username);

        // Check in users
        $query = mysqli_query($sqlConnect, "SELECT `user_id` FROM " . T_USERS . " WHERE `username` = '{$username_esc}' LIMIT 1");
        if ($query && mysqli_num_rows($query) > 0) {
            $result[0] = true;
        }

        // Check in pages
        $query2 = mysqli_query($sqlConnect, "SELECT `page_id` FROM " . T_PAGES . " WHERE `page_name` = '{$username_esc}' LIMIT 1");
        if ($query2 && mysqli_num_rows($query2) > 0) {
            $result[1] = true;
        }

        // Check in groups
        $query3 = mysqli_query($sqlConnect, "SELECT `id` FROM " . T_GROUPS . " WHERE `group_name` = '{$username_esc}' LIMIT 1");
        if ($query3 && mysqli_num_rows($query3) > 0) {
            $result[2] = true;
        }

        return $result;
    }
}

/**
 * Site pages array (reserved URLs)
 */
if (!isset($wo['site_pages'])) {
    $wo['site_pages'] = array(
        'admin', 'terms', 'about', 'privacy', 'contact',
        'blog', 'events', 'games', 'funding', 'forum',
        'movies', 'jobs', 'market', 'offers', 'nearby',
        'app', 'api', 'login', 'logout', 'register',
        'settings', 'messages', 'notifications', 'search',
        'timeline', 'groups', 'pages', 'create-page', 'create-group',
        'wallet', 'points', 'affiliates'
    );
}
