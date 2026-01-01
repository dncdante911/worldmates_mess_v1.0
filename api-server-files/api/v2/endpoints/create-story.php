<?php
// +------------------------------------------------------------------------+
// | @author Deen Doughouz (DoughouzForest)
// | @author_url 1: http://www.wowonder.com
// | @author_url 2: http://codecanyon.net/user/doughouzforest
// | @author_email: wowondersocial@gmail.com
// +------------------------------------------------------------------------+
// | WoWonder - The Ultimate Social Networking Platform
// | Copyright (c) 2018 WoWonder. All rights reserved.
// +------------------------------------------------------------------------+

// Load WoWonder initialization
$depth = '../../../';
if (file_exists($depth . 'assets/init.php')) {
    require_once($depth . 'assets/init.php');
}

// Enable error logging for debugging
error_reporting(E_ALL);
ini_set('log_errors', 1);
$log_dir = '/var/www/www-root/data/www/worldmates.club/api/v2/logs';
if (!file_exists($log_dir)) {
    @mkdir($log_dir, 0755, true);
}
ini_set('error_log', $log_dir . '/create-story-debug.log');
error_log("=== create-story.php START ===");
error_log("FILES: " . print_r($_FILES, true));
error_log("POST: " . print_r($_POST, true));
error_log("GET: " . print_r($_GET, true));

$response_data = array(
    'api_status' => 400
);

try {

// Check authentication
if (empty($wo['user']) || empty($wo['user']['id'])) {
    $response_data = array(
        'api_status' => 400,
        'errors' => array(
            'error_id' => 1,
            'error_text' => 'Not authorized'
        )
    );
    header('Content-Type: application/json');
    echo json_encode($response_data);
    error_log("ERROR: User not authorized");
    exit;
}

error_log("User authenticated: ID=" . $wo['user']['id'] . ", username=" . ($wo['user']['username'] ?? 'unknown'));

if (empty($_FILES["file"]["tmp_name"])) {
    $error_code    = 3;
    $error_message = 'file (STREAM FILE) is missing';
}
if (isset($_POST['story_title']) && strlen($_POST['story_title']) > 100) {
    $error_code    = 4;
    $error_message = 'Title is so long';
}
if (isset($_POST['story_description']) && strlen($_POST['story_description']) > 300) {
    $error_code    = 5;
    $error_message = 'Description is so long';
}
if (empty($_POST['file_type'])) {
    $error_code    = 6;
    $error_message = 'file_type (POST) is missing';
} else if (!in_array($_POST['file_type'], array(
        'video',
        'image'
    ))) {
    $error_code    = 7;
    $error_message = 'Incorrect value for (file_type), allowed: video|image';
}

if (empty($error_code)) {
    // Check user subscription limits
    $is_pro = ($wo['user']['is_pro'] == 1);
    $max_stories = $is_pro ? 15 : 2;
    $max_video_duration = $is_pro ? 45 : 25;
    $expire_hours = $is_pro ? 48 : 24;

    error_log("User subscription: is_pro=" . ($is_pro ? 'yes' : 'no') . ", max_stories=$max_stories, expire_hours=$expire_hours");

    // Count existing active stories
    $existing_stories = $db->where('user_id', $wo['user']['id'])
                          ->where('expire', time(), '>')
                          ->getValue(T_USER_STORY, 'COUNT(*)');

    error_log("Existing active stories: $existing_stories");

    if ($existing_stories >= $max_stories) {
        $error_code    = 8;
        $error_message = $is_pro ? 'You have reached the maximum limit of 15 stories' : 'Free users can only post up to 2 stories. Upgrade to premium for up to 15 stories.';
    }

    // Check video duration if it's a video
    if (empty($error_code) && $_POST['file_type'] == 'video' && !empty($_POST['video_duration'])) {
        $video_duration = (int)$_POST['video_duration'];
        if ($video_duration > $max_video_duration) {
            $error_code    = 9;
            $error_message = $is_pro ? 'Video duration cannot exceed 45 seconds' : 'Free users can only upload videos up to 25 seconds. Upgrade to premium for videos up to 45 seconds.';
        }
    }
}

if (empty($error_code)) {
    $amazone_s3                   = $wo['config']['amazone_s3'];
    $wasabi_storage                   = $wo['config']['wasabi_storage'];
    $backblaze_storage                   = $wo['config']['backblaze_storage'];
    $ftp_upload                   = $wo['config']['ftp_upload'];
    $spaces                       = $wo['config']['spaces'];
    $cloud_upload                 = $wo['config']['cloud_upload'];
    $story_title       = (!empty($_POST['story_title'])) ? Wo_Secure($_POST['story_title']) : '';
    $story_description = (!empty($_POST['story_description'])) ? Wo_Secure($_POST['story_description']) : '';
    $file_type         = Wo_Secure($_POST['file_type']);

    // Calculate expire time based on subscription
    $is_pro = ($wo['user']['is_pro'] == 1);
    $expire_hours = $is_pro ? 48 : 24;

    $story_data        = array(
        'user_id' => $wo['user']['id'],
        'posted' => time(),
        'expire' => time()+($expire_hours*60*60),
        'title' => $story_title,
        'description' => $story_description
    );
    $last_id           = Wo_InsertUserStory($story_data);
    if ($last_id && is_numeric($last_id) && !empty($_FILES["file"]["tmp_name"])) {
        $true     = false;
        $sources  = array();
        $fileInfo = array(
            'file' => $_FILES["file"]["tmp_name"],
            'name' => $_FILES['file']['name'],
            'size' => $_FILES["file"]["size"],
            'type' => $_FILES["file"]["type"],
            'types' => 'jpg,png,mp4,gif,jpeg,mov,webm'
        );
        error_log("Attempting to upload file: " . print_r($fileInfo, true));
        $media    = Wo_ShareFile($fileInfo);
        error_log("Wo_ShareFile result: " . print_r($media, true));
        $filename = '';
        if (!empty($media) && !empty($media['filename'])) {
            $filename = $media['filename'];
            error_log("File uploaded successfully: " . $filename);

            // Стиснення відео якщо розмір > 50MB та ffmpeg увімкнено
            if ($file_type == 'video' && $wo['config']['ffmpeg_system'] == 'on') {
                $video_path = $filename;
                if (file_exists($video_path)) {
                    $file_size_mb = filesize($video_path) / (1024 * 1024);
                    error_log("Video file size: {$file_size_mb}MB");

                    // Стискаємо відео якщо > 50MB
                    if ($file_size_mb > 50) {
                        $ffmpeg_b = $wo['config']['ffmpeg_binary_file'];
                        $compressed_path = str_replace('.', '_compressed.', $video_path);

                        // FFmpeg команда для стиснення з високою якістю
                        // -crf 23 - хороша якість (18-28, де 18=найкраща)
                        // -preset medium - баланс швидкість/якість
                        // -movflags +faststart - швидкий старт відео
                        $ffmpeg_command = "$ffmpeg_b -i $video_path -c:v libx264 -crf 23 -preset medium -c:a aac -b:a 128k -movflags +faststart $compressed_path 2>&1";

                        error_log("Compressing video with ffmpeg: $ffmpeg_command");
                        $ffmpeg_output = shell_exec($ffmpeg_command);
                        error_log("FFmpeg output: " . $ffmpeg_output);

                        if (file_exists($compressed_path)) {
                            $compressed_size_mb = filesize($compressed_path) / (1024 * 1024);
                            error_log("Compressed video size: {$compressed_size_mb}MB");

                            // Використовуємо стиснуте відео тільки якщо воно менше
                            if ($compressed_size_mb < $file_size_mb) {
                                // Видаляємо оригінал та перейменовуємо стиснуте
                                @unlink($video_path);
                                @rename($compressed_path, $video_path);
                                error_log("Video compressed successfully: {$file_size_mb}MB → {$compressed_size_mb}MB");
                            } else {
                                // Стиснене більше - видаляємо його
                                @unlink($compressed_path);
                                error_log("Compressed video is larger, keeping original");
                            }
                        } else {
                            error_log("ERROR: FFmpeg compression failed");
                        }
                    } else {
                        error_log("Video < 50MB, compression not needed");
                    }
                }
            }
        } else {
            $error_code    = 10;
            $error_message = 'Failed to upload file. Please check file size and format.';
            error_log("ERROR: File upload failed - media result: " . print_r($media, true));
        }
        if (!empty($filename) && empty($error_code)) {
            $video_duration = (!empty($_POST['video_duration']) && is_numeric($_POST['video_duration'])) ? (int)$_POST['video_duration'] : 0;

            $sources[] = array(
                'story_id' => $last_id,
                'type' => Wo_Secure($file_type),
                'filename' => $filename,
                'expire' => time()+($expire_hours*60*60),
                'duration' => $video_duration
            );
            $img_types     = array(
                'image/png',
                'image/jpeg',
                'image/jpg',
                'image/gif'
            );
            $thumb     = '';
            if (empty($thumb)) {
                if (in_array(strtolower(pathinfo($media['filename'], PATHINFO_EXTENSION)), array(
                                    "m4v",
                                    "avi",
                                    "mpg",
                                    'mp4'
                                )) && !empty($_FILES["cover"]) && in_array($_FILES["cover"]["type"], $img_types)) {
                    $fileInfo = array(
                        'file' => $_FILES["cover"]["tmp_name"],
                        'name' => $_FILES['cover']['name'],
                        'size' => $_FILES["cover"]["size"],
                        'type' => $_FILES["cover"]["type"]
                    );
                    $media            = Wo_ShareFile($fileInfo);
                    $file_type        = explode('/', $fileInfo['type']);
                    if (empty($thumb)) {
                        if (in_array(strtolower(pathinfo($media['filename'], PATHINFO_EXTENSION)), array(
                            "gif",
                            "jpg",
                            "png",
                            'jpeg'
                        ))) {
                            $thumb             = $media['filename'];
                            $explode2          = @end(explode('.', $thumb));
                            $explode3          = @explode('.', $thumb);
                            $last_file         = $explode3[0] . '_small.' . $explode2;
                            $arrContextOptions = array(
                                "ssl" => array(
                                    "verify_peer" => false,
                                    "verify_peer_name" => false
                                )
                            );
                            $fileget           = file_get_contents(Wo_GetMedia($thumb), false, stream_context_create($arrContextOptions));
                            if (!empty($fileget)) {
                                $importImage = @file_put_contents($thumb, $fileget);
                            }
                            $crop_image = Wo_Resize_Crop_Image(400, 400, $thumb, $last_file, $wo['config']['images_quality']);
                            $upload_s3  = Wo_UploadToS3($last_file);
                            $thumb      = $last_file;
                        }
                    }
                }
            }
        }
        if (count($sources) > 0) {
            foreach ($sources as $registration_data) {
                Wo_InsertUserStoryMedia($registration_data);
            }
            if (empty($thumb) && $wo['config']['ffmpeg_system'] == 'on' && $file_type == 'video') {
                $ffmpeg_b         = $wo['config']['ffmpeg_binary_file'];
                $total_seconds    = ffmpeg_duration($media['filename']);
                $thumb_1_duration = (int) ($total_seconds > 10) ? 11 : 1;
                $dir              = "upload/photos/" . date('Y') . '/' . date('m');
                $image_thumb      = $dir . '/' . Wo_GenerateKey() . '_' . date('d') . '_' . md5(time()) . "_image.jpeg";
                $output_thumb     = shell_exec("$ffmpeg_b -ss \"$thumb_1_duration\" -i " . $media['filename'] . " -vframes 1 -f mjpeg $image_thumb 2<&1");
                if (file_exists($image_thumb) && !empty(getimagesize($image_thumb))) {
                    $crop_image                   = Wo_Resize_Crop_Image(400, 400, $image_thumb, $image_thumb, $wo['config']['images_quality']);
                    $wo['config']['amazone_s3']   = $amazone_s3;
                    $wo['config']['wasabi_storage']   = $wasabi_storage;
                    $wo['config']['backblaze_storage']   = $backblaze_storage;
                    $wo['config']['ftp_upload']   = $ftp_upload;
                    $wo['config']['spaces']       = $spaces;
                    $wo['config']['cloud_upload'] = $cloud_upload;
                    Wo_UploadToS3($image_thumb);
                    $thumb = $image_thumb;
                } else {
                    @unlink($image_thumb);
                }
                $wo['config']['amazone_s3']   = $amazone_s3;
                $wo['config']['wasabi_storage']   = $wasabi_storage;
                $wo['config']['backblaze_storage']   = $backblaze_storage;
                $wo['config']['ftp_upload']   = $ftp_upload;
                $wo['config']['spaces']       = $spaces;
                $wo['config']['cloud_upload'] = $cloud_upload;
                Wo_UploadToS3($media['filename']);
            }
            if (!empty($thumb)) {
                $thumb        = Wo_Secure($thumb);
                $mysqli_query = mysqli_query($sqlConnect, "UPDATE " . T_USER_STORY . " SET thumbnail = '$thumb' WHERE id = $last_id");
            }
            $response_data = array(
                'api_status' => 200,
                'story_id' => $last_id
            );
        } else {
            // No sources were added
            if (empty($error_code)) {
                $error_code    = 11;
                $error_message = 'Failed to process media file';
            }
        }
    } else {
        // Story creation failed
        if (empty($error_code)) {
            $error_code    = 12;
            $error_message = 'Failed to create story';
        }
    }
}

} catch (Exception $e) {
    // Catch any unexpected errors
    error_log("EXCEPTION in create-story.php: " . $e->getMessage());
    error_log("Stack trace: " . $e->getTraceAsString());
    $error_code = 99;
    $error_message = 'Server error: ' . $e->getMessage();
}

// Return error if any
if (!empty($error_code)) {
    $response_data = array(
        'api_status' => 400,
        'errors' => array(
            'error_id' => $error_code,
            'error_text' => $error_message
        )
    );
}

error_log("Final response: " . print_r($response_data, true));

// Return JSON response
header('Content-Type: application/json');
echo json_encode($response_data);