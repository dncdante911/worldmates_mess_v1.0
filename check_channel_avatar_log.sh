#!/bin/bash
echo "=== Checking channel avatar debug log ==="
echo ""

if [ -f "/var/www/www-root/data/www/worldmates.club/api/v2/logs/channel_avatar_debug.log" ]; then
    echo "Log file found! Last 100 lines:"
    echo ""
    tail -100 /var/www/www-root/data/www/worldmates.club/api/v2/logs/channel_avatar_debug.log
else
    echo "Log file not found at: /var/www/www-root/data/www/worldmates.club/api/v2/logs/channel_avatar_debug.log"
    echo ""
    echo "Checking if directory exists..."
    if [ -d "/var/www/www-root/data/www/worldmates.club/api/v2/logs" ]; then
        echo "Directory exists. Files in logs directory:"
        ls -la /var/www/www-root/data/www/worldmates.club/api/v2/logs/
    else
        echo "Logs directory does not exist"
    fi
fi

echo ""
echo "=== Checking PHP error log ==="
if [ -f "/var/www/www-root/data/www/worldmates.club/error.log" ]; then
    echo "PHP error log (last 50 lines):"
    tail -50 /var/www/www-root/data/www/worldmates.club/error.log | grep -i "channel\|avatar\|fatal\|error" || echo "No relevant errors found"
fi

echo ""
echo "=== Checking if endpoint file exists ===" 
if [ -f "/var/www/www-root/data/www/worldmates.club/api/v2/endpoints/upload_channel_avatar.php" ]; then
    echo "✓ Endpoint file exists"
    ls -lh /var/www/www-root/data/www/worldmates.club/api/v2/endpoints/upload_channel_avatar.php
else
    echo "✗ Endpoint file NOT FOUND"
fi
