/**
 * 📡 Main Socket.IO Listeners Registry
 *
 * Registers all socket event handlers:
 * - User authentication and room management
 * - Private messages
 * - Group messages
 * - Typing indicators
 * - Online status
 * - Channels (from channels-listener.js)
 * - Stories (from stories-listener.js)
 * - Calls (from calls-listener.js)
 */

const PrivateMessageController = require('../controllers/PrivateMessageController');
const GroupMessageController = require('../controllers/GroupMessageController');
const socketEvents = require('../events/events');
const registerChannelsListeners = require('./channels-listener');
const registerStoriesListeners = require('./stories-listener');

// Track user sockets
const userSockets = new Map(); // userId -> Set of socket IDs
const socketToUser = new Map(); // socketId -> userId

/**
 * Register all socket event listeners
 */
async function registerListeners(socket, io, ctx) {
    console.log(`📡 New socket connected: ${socket.id}`);

    // Extract user info from handshake query
    const accessToken = socket.handshake.query.access_token;
    const userId = socket.handshake.query.user_id;

    // ==================== USER AUTHENTICATION ====================

    /**
     * Handle 'join' event - user authentication
     * Client sends this after connection to register with their user ID
     */
    socket.on('join', async (data) => {
        try {
            const sessionHash = data.user_id || accessToken;

            if (!sessionHash) {
                console.log('❌ join: No session hash provided');
                return;
            }

            // Lookup user ID from session hash
            let resolvedUserId = null;
            if (ctx.wo_appssessions) {
                const session = await ctx.wo_appssessions.findOne({
                    where: { session_id: sessionHash },
                    raw: true
                });
                if (session) {
                    resolvedUserId = session.user_id;
                }
            }

            // If no session found, try to use numeric user_id directly
            if (!resolvedUserId && userId && !isNaN(parseInt(userId))) {
                resolvedUserId = parseInt(userId);
            }

            if (!resolvedUserId) {
                console.log(`❌ join: Could not resolve user ID for session: ${sessionHash?.substring(0, 20)}...`);
                return;
            }

            // Join user's personal room
            socket.join(resolvedUserId.toString());
            socket.userId = resolvedUserId;

            // Track socket
            if (!userSockets.has(resolvedUserId)) {
                userSockets.set(resolvedUserId, new Set());
            }
            userSockets.get(resolvedUserId).add(socket.id);
            socketToUser.set(socket.id, resolvedUserId);

            // Update context for backward compatibility
            if (!ctx.userIdSocket) ctx.userIdSocket = {};
            if (!ctx.userIdSocket[resolvedUserId]) ctx.userIdSocket[resolvedUserId] = [];
            ctx.userIdSocket[resolvedUserId].push(socket);

            if (!ctx.userHashUserId) ctx.userHashUserId = {};
            ctx.userHashUserId[sessionHash] = resolvedUserId;

            if (!ctx.userIdCount) ctx.userIdCount = {};
            ctx.userIdCount[resolvedUserId] = (ctx.userIdCount[resolvedUserId] || 0) + 1;

            console.log(`✅ User ${resolvedUserId} joined (socket: ${socket.id})`);

            // Notify user is online
            socket.broadcast.emit('user_online', { user_id: resolvedUserId });

        } catch (e) {
            console.log('❌ join error:', e.message);
        }
    });

    // ==================== PRIVATE MESSAGES ====================

    /**
     * Handle sending private messages
     */
    socket.on('private_message', async (data, callback) => {
        try {
            console.log('📨 private_message received:', JSON.stringify(data).substring(0, 200));

            // Call the message controller
            await PrivateMessageController(ctx, data, io, socket, callback);

        } catch (e) {
            console.log('❌ private_message error:', e.message);
            if (callback) callback({ status: 500, error: e.message });
        }
    });

    /**
     * Handle message seen
     */
    socket.on('message_seen', async (data) => {
        try {
            const { from_id, to_id, message_id } = data;

            if (!from_id || !to_id) return;

            // Notify sender that message was seen
            io.to(from_id.toString()).emit('message_seen', {
                message_id,
                seen_by: to_id,
                seen_at: Date.now()
            });

            console.log(`👁️ Message ${message_id} seen by ${to_id}`);

        } catch (e) {
            console.log('❌ message_seen error:', e.message);
        }
    });

    // ==================== GROUP MESSAGES ====================

    /**
     * Handle group messages
     */
    socket.on('group_message', async (data, callback) => {
        try {
            console.log('📨 group_message received:', JSON.stringify(data).substring(0, 200));

            await GroupMessageController(ctx, data, io, socket, callback);

        } catch (e) {
            console.log('❌ group_message error:', e.message);
            if (callback) callback({ status: 500, error: e.message });
        }
    });

    /**
     * Join group room
     */
    socket.on('join_group', async (data) => {
        try {
            const { group_id } = data;
            if (!group_id) return;

            const roomName = `group_${group_id}`;
            socket.join(roomName);
            console.log(`✅ Socket ${socket.id} joined group ${group_id}`);

        } catch (e) {
            console.log('❌ join_group error:', e.message);
        }
    });

    /**
     * Leave group room
     */
    socket.on('leave_group', async (data) => {
        try {
            const { group_id } = data;
            if (!group_id) return;

            const roomName = `group_${group_id}`;
            socket.leave(roomName);
            console.log(`👋 Socket ${socket.id} left group ${group_id}`);

        } catch (e) {
            console.log('❌ leave_group error:', e.message);
        }
    });

    // ==================== TYPING INDICATORS ====================

    /**
     * Handle typing status
     */
    socket.on('typing', async (data) => {
        try {
            const { user_id, recipient_id, is_typing, group_id } = data;

            if (group_id) {
                // Group typing
                const roomName = `group_${group_id}`;
                socket.to(roomName).emit('typing', {
                    sender_id: socket.userId || user_id,
                    is_typing: is_typing, // 200 = typing, 300 = stopped
                    group_id
                });
            } else if (recipient_id) {
                // Private typing
                io.to(recipient_id.toString()).emit('typing', {
                    sender_id: socket.userId || user_id,
                    is_typing: is_typing
                });
            }

        } catch (e) {
            console.log('❌ typing error:', e.message);
        }
    });

    // ==================== ONLINE STATUS ====================

    /**
     * Get online users
     */
    socket.on('get_online_users', async (data, callback) => {
        try {
            const onlineUserIds = Array.from(userSockets.keys());
            if (callback) {
                callback({ status: 200, online_users: onlineUserIds });
            }

        } catch (e) {
            console.log('❌ get_online_users error:', e.message);
        }
    });

    /**
     * Update last seen
     */
    socket.on('lastseen', async (data) => {
        try {
            await socketEvents.lastseen(ctx, socket, data);
        } catch (e) {
            console.log('❌ lastseen error:', e.message);
        }
    });

    // ==================== REGISTER SUB-LISTENERS ====================

    // Register channels listeners
    try {
        await registerChannelsListeners(socket, io, ctx);
    } catch (e) {
        console.log('⚠️ Could not register channels listeners:', e.message);
    }

    // Register stories listeners
    try {
        await registerStoriesListeners(socket, io, ctx);
    } catch (e) {
        console.log('⚠️ Could not register stories listeners:', e.message);
    }

    // ==================== DISCONNECT HANDLING ====================

    socket.on('disconnect', (reason) => {
        const userId = socketToUser.get(socket.id);

        if (userId) {
            // Remove from tracking
            if (userSockets.has(userId)) {
                userSockets.get(userId).delete(socket.id);
                if (userSockets.get(userId).size === 0) {
                    userSockets.delete(userId);
                    // Notify user is offline
                    socket.broadcast.emit('user_offline', { user_id: userId });
                }
            }
            socketToUser.delete(socket.id);

            // Update context
            if (ctx.userIdSocket && ctx.userIdSocket[userId]) {
                ctx.userIdSocket[userId] = ctx.userIdSocket[userId].filter(s => s.id !== socket.id);
            }
            if (ctx.userIdCount) {
                ctx.userIdCount[userId] = Math.max(0, (ctx.userIdCount[userId] || 0) - 1);
            }
        }

        console.log(`🔌 Socket ${socket.id} disconnected (user: ${userId || 'unknown'}, reason: ${reason})`);
    });
}

module.exports = {
    registerListeners
};
