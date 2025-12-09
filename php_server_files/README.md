# PHP Server Files for Encrypted Media Uploads

This directory contains standalone PHP upload handlers for processing encrypted media uploads from the web version.

## Files

- `upload_image.php` - Handles image uploads (JPEG, PNG, GIF, WEBP) up to 15MB
- `upload_video.php` - Handles video uploads (MP4, WEBM, etc.) up to 1GB
- `upload_audio.php` - Handles audio uploads (MP3, WAV, OGG, etc.) up to 100MB
- `upload_file.php` - Handles generic file uploads up to 500MB

## Installation

1. Copy these files to your server's `/xhr/` directory:
   ```bash
   cp php_server_files/*.php /path/to/server/xhr/
   ```

2. Ensure the upload directories exist and are writable:
   ```bash
   mkdir -p /path/to/server/upload/{photos,videos,sounds,files}
   chmod 755 /path/to/server/upload/{photos,videos,sounds,files}
   ```

3. Update the paths in each PHP file if needed:
   ```php
   $upload_base_dir = dirname(__DIR__) . '/upload/photos';
   ```

## Usage

These endpoints are called automatically by the Android app through the MediaUploader class.

### Endpoints

- POST `/xhr/upload_image.php?access_token=TOKEN&f=upload_image`
- POST `/xhr/upload_video.php?access_token=TOKEN&f=upload_video`
- POST `/xhr/upload_audio.php?access_token=TOKEN&f=upload_audio`
- POST `/xhr/upload_file.php?access_token=TOKEN&f=upload_file`

### Request Parameters

- `server_key` (POST) - Server authentication key
- `access_token` (GET) - User access token
- `image/video/audio/file` (FILE) - The uploaded file
- `encrypted` (POST, optional) - Set to "1" if file is encrypted

### Response Format

Success (200):
```json
{
  "status": 200,
  "image": "https://worldmates.club/upload/photos/2025/12/encrypted_img_1733755550_abc123.jpg",
  "image_src": "upload/photos/2025/12/encrypted_img_1733755550_abc123.jpg",
  "encrypted": false,
  "timestamp": 1733755550,
  "size": 245678,
  "type": "image/jpeg"
}
```

Error (400/500):
```json
{
  "status": 400,
  "error": "Error message here"
}
```

## Security Notes

1. Files are validated by MIME type, not just extension
2. File sizes are enforced per media type
3. Filenames are sanitized and made unique
4. CORS headers are configured for web access
5. Server key and access token authentication required

## Troubleshooting

If uploads fail:

1. Check PHP upload limits in `php.ini`:
   ```ini
   upload_max_filesize = 1G
   post_max_size = 1G
   max_execution_time = 300
   ```

2. Check directory permissions:
   ```bash
   ls -la /path/to/server/upload/
   ```

3. Check PHP error logs:
   ```bash
   tail -f /var/log/php_errors.log
   ```

4. Verify the web server has write access to upload directories

## Integration with Existing System

If you need to integrate with the existing WoWonder/Worldmates system:

1. Include the main config file at the top of each upload PHP:
   ```php
   require_once('../config.php');
   ```

2. Use the existing `Wo_ShareFile()` function instead of manual file handling

3. Authenticate users with the existing session system

4. Store file metadata in the database if needed
