<?php
// +------------------------------------------------------------------------+
// | WoWonder/Wondertage Database Configuration
// | WorldMates Messenger Project
// +------------------------------------------------------------------------+

// Database Credentials
$sql_db_host = 'localhost';
$sql_db_user = 'social';
$sql_db_pass = '3344Frzaq0607DmC157';
$sql_db_name = 'socialhub';

// Site URL (без слешу в кінці)
$site_url = 'https://worldmates.club';

// API Version
if (!isset($api_version)) {
    $api_version = '1.3.1';
}

// Security Settings
$wo['session_mode'] = 2; // 1 = cookie, 2 = token
$wo['ssl'] = 1; // 1 = enabled, 0 = disabled

// IMPORTANT: This config file is required by app_start.php and init.php
// All phone API files expect these variables to be defined
