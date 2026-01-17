/**
 * TURN Credentials Helper for WorldMates
 * Generates dynamic TURN credentials using HMAC-SHA1
 *
 * Integrates with existing Node.js backend structure
 */

const crypto = require('crypto');

// ⚠️ ВАЖНО: Этот секрет ДОЛЖЕН совпадать с static-auth-secret в /etc/turnserver.conf
const TURN_SECRET = 'a7f3e9c2d8b4f6a1c5e8d9b2f4a6c8e1d3f5a7b9c2e4f6a8b1d3f5a7c9e2f4a6';

// TURN server configuration
const TURN_SERVER_URL = 'worldmates.club';
const TURN_PORT = 3478;
const TURN_TLS_PORT = 5349;

/**
 * Generate TURN credentials for a user
 * @param {string|number} userId - User ID
 * @param {number} ttl - Time to live in seconds (default: 86400 = 24 hours)
 * @returns {Object} - { username, password, expiresAt }
 */
function generateTurnCredentials(userId, ttl = 86400) {
    // Unix timestamp когда credentials истекут
    const expirationTimestamp = Math.floor(Date.now() / 1000) + ttl;

    // Username формат: timestamp:userId
    const username = `${expirationTimestamp}:${userId}`;

    // Password: base64(HMAC-SHA1(secret, username))
    const hmac = crypto.createHmac('sha1', TURN_SECRET);
    hmac.update(username);
    const password = hmac.digest('base64');

    return {
        username: username,
        password: password,
        expiresAt: new Date(expirationTimestamp * 1000)
    };
}

/**
 * Get complete ICE servers configuration for WebRTC
 * @param {string|number} userId - User ID
 * @param {number} ttl - Credential TTL in seconds
 * @returns {Array} - Array of ICE server configurations
 */
function getIceServers(userId, ttl = 86400) {
    const turnCredentials = generateTurnCredentials(userId, ttl);

    return [
        // Google STUN servers (бесплатные, для NAT traversal)
        {
            urls: 'stun:stun.l.google.com:19302'
        },
        {
            urls: 'stun:stun1.l.google.com:19302'
        },

        // WorldMates TURN server (с динамическими credentials)
        {
            urls: [
                `turn:${TURN_SERVER_URL}:${TURN_PORT}?transport=udp`,
                `turn:${TURN_SERVER_URL}:${TURN_PORT}?transport=tcp`
            ],
            username: turnCredentials.username,
            credential: turnCredentials.password
        },

        // TURN over TLS (более безопасный)
        {
            urls: `turns:${TURN_SERVER_URL}:${TURN_TLS_PORT}?transport=tcp`,
            username: turnCredentials.username,
            credential: turnCredentials.password
        }
    ];
}

/**
 * Validate TURN credentials
 * @param {string} username - Format: "timestamp:userId"
 * @param {string} password - Base64 HMAC-SHA1
 * @returns {boolean} - True if valid and not expired
 */
function validateTurnCredentials(username, password) {
    try {
        const parts = username.split(':');
        if (parts.length !== 2) return false;

        const expirationTimestamp = parseInt(parts[0]);
        const now = Math.floor(Date.now() / 1000);

        // Check expiration
        if (now > expirationTimestamp) {
            console.log('[TURN] Credentials expired');
            return false;
        }

        // Verify HMAC
        const hmac = crypto.createHmac('sha1', TURN_SECRET);
        hmac.update(username);
        const expectedPassword = hmac.digest('base64');

        return password === expectedPassword;
    } catch (error) {
        console.error('[TURN] Validation error:', error);
        return false;
    }
}

/**
 * Get ICE servers configuration in simple format for Android
 * @param {string|number} userId - User ID
 * @returns {Object} - Simplified ICE configuration
 */
function getIceConfigForAndroid(userId) {
    const iceServers = getIceServers(userId);

    return {
        success: true,
        iceServers: iceServers,
        timestamp: Date.now()
    };
}

module.exports = {
    generateTurnCredentials,
    getIceServers,
    validateTurnCredentials,
    getIceConfigForAndroid,
    TURN_SERVER_URL,
    TURN_PORT,
    TURN_TLS_PORT
};
