/**
 * 🔐 Generate TURN Credentials for WebRTC
 *
 * Використовує static-auth-secret для генерації тимчасових credentials
 */

const crypto = require('crypto');

// ⚠️ MUST MATCH static-auth-secret in turnserver.conf
const TURN_SECRET = 'a7f3e9c2d8b4f6a1c5e8d9b2f4a6c8e1d3f5a7b9c2e4f6a8b1d3f5a7c9e2f4a6';

/**
 * Generate TURN credentials for a user
 * @param {string} username - Username (usually user ID)
 * @param {number} ttl - Time to live in seconds (default: 24 hours)
 * @returns {object} - { username, password }
 */
function generateTurnCredentials(username, ttl = 86400) {
    // TTL (Time To Live) - час дії credentials
    const unixTimeStamp = Math.floor(Date.now() / 1000) + ttl;

    // Username format: timestamp:username
    const turnUsername = `${unixTimeStamp}:${username}`;

    // Password = base64(hmac-sha1(secret, username))
    const hmac = crypto.createHmac('sha1', TURN_SECRET);
    hmac.update(turnUsername);
    const turnPassword = hmac.digest('base64');

    return {
        username: turnUsername,
        password: turnPassword,
        ttl: unixTimeStamp
    };
}

/**
 * Get full ICE server configuration
 * @param {string} userId - User ID
 * @returns {array} - Array of ICE servers
 */
function getIceServers(userId) {
    const turnCreds = generateTurnCredentials(userId);

    return [
        // STUN servers (free from Google)
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },

        // TURN server (your own)
        {
            urls: [
                'turn:worldmates.club:3478?transport=udp',
                'turn:worldmates.club:3478?transport=tcp',
                'turns:worldmates.club:5349?transport=tcp' // TLS
            ],
            username: turnCreds.username,
            credential: turnCreds.password
        }
    ];
}

// Export functions
module.exports = {
    generateTurnCredentials,
    getIceServers,
    TURN_SECRET
};

// CLI testing
if (require.main === module) {
    const testUserId = 'user123';
    const creds = generateTurnCredentials(testUserId);
    console.log('🔐 TURN Credentials Generated:');
    console.log('Username:', creds.username);
    console.log('Password:', creds.password);
    console.log('Expires:', new Date(creds.ttl * 1000).toISOString());
    console.log('\n📡 Full ICE Configuration:');
    console.log(JSON.stringify(getIceServers(testUserId), null, 2));
}
