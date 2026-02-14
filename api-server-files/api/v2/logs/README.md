# API v2 Logs Directory

This directory contains debug and error logs for the API v2 router.

## Log Files

- `api_v2_YYYY-MM-DD.log` - Daily log files with all API requests and errors

## Reading Logs

### On Server (via SSH):
```bash
# View today's log
tail -f /var/www/www-root/data/www/worldmates.club/api/v2/logs/api_v2_$(date +%Y-%m-%d).log

# Search for errors
grep ERROR /var/www/www-root/data/www/worldmates.club/api/v2/logs/api_v2_*.log

# Search for specific endpoint
grep "type=quick_register" /var/www/www-root/data/www/worldmates.club/api/v2/logs/api_v2_*.log
```

### Log Format:
```
[YYYY-MM-DD HH:MM:SS] [LEVEL] Message
```

Levels: `DEBUG`, `INFO`, `ERROR`

## Debug Information Logged

For each request:
1. Request type and server_key (first 20 chars)
2. Config server_key from database
3. Server key validation result
4. Public endpoint check
5. Authentication flow decision
6. Access token validation (if applicable)

## Troubleshooting

If registration fails with "Not authorized":
1. Check if `server_key` is being received
2. Verify it matches the database config
3. Confirm endpoint is in public_endpoints list
4. Check authentication flow logic

## Permissions

Make sure this directory is writable by the web server:
```bash
chmod 755 /var/www/www-root/data/www/worldmates.club/api/v2/logs
chown www-data:www-data /var/www/www-root/data/www/worldmates.club/api/v2/logs
```
