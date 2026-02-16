/**
 * WorldMates Bot Socket.IO Handler
 *
 * Handles real-time bot message delivery, bot typing indicators,
 * and callback query notifications via Socket.IO.
 *
 * Integration:
 *   const { initializeBotHandler } = require('./botSocketHandler');
 *   initializeBotHandler(io);
 */

// Active bot connections: botId -> socket
const botSockets = new Map();
// User subscriptions to bot events: userId -> Set<botId>
const userBotSubscriptions = new Map();

/**
 * Initialize bot socket handler
 * @param {SocketIO.Server} io - Socket.IO server instance
 */
function initializeBotHandler(io) {
    // Create bot namespace
    const botNamespace = io.of('/bots');

    botNamespace.on('connection', (socket) => {
        console.log(`[Bot] Socket connected: ${socket.id}`);

        // Bot authentication (bot connects with its token)
        socket.on('bot_auth', (data) => {
            const { bot_id, bot_token } = data;
            if (!bot_id || !bot_token) {
                socket.emit('auth_error', { error: 'bot_id and bot_token required' });
                return;
            }

            // Store bot connection
            botSockets.set(bot_id, socket);
            socket.botId = bot_id;
            socket.join(`bot_${bot_id}`);

            console.log(`[Bot] Authenticated: ${bot_id}`);
            socket.emit('auth_success', { bot_id, status: 'connected' });
        });

        // User subscribes to bot messages (when opening bot chat)
        socket.on('subscribe_bot', (data) => {
            const { user_id, bot_id } = data;
            if (!user_id || !bot_id) return;

            // Track subscription
            if (!userBotSubscriptions.has(user_id)) {
                userBotSubscriptions.set(user_id, new Set());
            }
            userBotSubscriptions.get(user_id).add(bot_id);

            // Join user-specific bot room
            socket.join(`user_bot_${user_id}_${bot_id}`);
            socket.userId = user_id;

            console.log(`[Bot] User ${user_id} subscribed to bot ${bot_id}`);
        });

        // User unsubscribes from bot (when leaving bot chat)
        socket.on('unsubscribe_bot', (data) => {
            const { user_id, bot_id } = data;
            if (!user_id || !bot_id) return;

            if (userBotSubscriptions.has(user_id)) {
                userBotSubscriptions.get(user_id).delete(bot_id);
            }
            socket.leave(`user_bot_${user_id}_${bot_id}`);
        });

        // Bot sends message to user (via socket for instant delivery)
        socket.on('bot_message', (data) => {
            const { bot_id, chat_id, text, message_id, reply_markup, media } = data;
            if (!bot_id || !chat_id) return;

            // Verify this is the authenticated bot
            if (socket.botId !== bot_id) {
                socket.emit('error', { error: 'Unauthorized: bot_id mismatch' });
                return;
            }

            const payload = {
                event: 'bot_message',
                bot_id,
                message_id,
                text,
                media: media || null,
                reply_markup: reply_markup || null,
                timestamp: Date.now()
            };

            // Send to the specific user's bot room
            botNamespace.to(`user_bot_${chat_id}_${bot_id}`).emit('bot_message', payload);

            // Also emit on main namespace for general listeners
            io.to(`user_${chat_id}`).emit('bot_message', payload);

            console.log(`[Bot] Message from ${bot_id} to user ${chat_id}`);
        });

        // Bot typing indicator
        socket.on('bot_typing', (data) => {
            const { bot_id, chat_id, is_typing } = data;
            if (!bot_id || !chat_id) return;

            botNamespace.to(`user_bot_${chat_id}_${bot_id}`).emit('bot_typing', {
                bot_id,
                is_typing: is_typing !== false
            });
        });

        // User sends message to bot (command or text)
        socket.on('user_to_bot', (data) => {
            const { user_id, bot_id, text, is_command, command_name, command_args, callback_data } = data;
            if (!user_id || !bot_id) return;

            const payload = {
                event: 'user_message',
                user_id,
                text,
                is_command: is_command || false,
                command_name: command_name || null,
                command_args: command_args || null,
                callback_data: callback_data || null,
                timestamp: Date.now()
            };

            // Forward to bot's socket if connected
            const botSocket = botSockets.get(bot_id);
            if (botSocket) {
                botSocket.emit('user_message', payload);
                console.log(`[Bot] User ${user_id} -> Bot ${bot_id}: ${text || callback_data}`);
            } else {
                // Bot not connected via socket, message will be picked up by polling/webhook
                console.log(`[Bot] Bot ${bot_id} offline, message queued for: ${user_id}`);
            }
        });

        // Callback query answer (bot answers inline button click)
        socket.on('callback_answer', (data) => {
            const { bot_id, callback_query_id, user_id, text, show_alert } = data;
            if (!bot_id || !user_id) return;

            botNamespace.to(`user_bot_${user_id}_${bot_id}`).emit('callback_answer', {
                callback_query_id,
                text: text || '',
                show_alert: show_alert || false
            });
        });

        // Bot sends updated keyboard (edit message markup)
        socket.on('update_markup', (data) => {
            const { bot_id, chat_id, message_id, reply_markup } = data;
            if (!bot_id || !chat_id || !message_id) return;

            botNamespace.to(`user_bot_${chat_id}_${bot_id}`).emit('update_markup', {
                bot_id,
                message_id,
                reply_markup
            });
        });

        // Disconnect handling
        socket.on('disconnect', (reason) => {
            if (socket.botId) {
                botSockets.delete(socket.botId);
                console.log(`[Bot] Bot disconnected: ${socket.botId} (${reason})`);
            }
            if (socket.userId) {
                // Clean up user subscriptions for this socket
                const subs = userBotSubscriptions.get(socket.userId);
                if (subs) {
                    subs.clear();
                    userBotSubscriptions.delete(socket.userId);
                }
            }
        });
    });

    console.log('[Bot] Bot Socket.IO handler initialized on /bots namespace');
}

/**
 * Get bot connection stats
 */
function getBotStats() {
    return {
        connectedBots: botSockets.size,
        activeSubscriptions: Array.from(userBotSubscriptions.values())
            .reduce((sum, set) => sum + set.size, 0),
        botIds: Array.from(botSockets.keys())
    };
}

/**
 * Send a message to a user from the server side
 * (used when PHP backend needs to push a bot message via Node.js)
 */
function pushBotMessage(io, botId, chatId, message) {
    const botNamespace = io.of('/bots');
    botNamespace.to(`user_bot_${chatId}_${botId}`).emit('bot_message', {
        event: 'bot_message',
        bot_id: botId,
        ...message,
        timestamp: Date.now()
    });
}

module.exports = {
    initializeBotHandler,
    getBotStats,
    pushBotMessage
};
