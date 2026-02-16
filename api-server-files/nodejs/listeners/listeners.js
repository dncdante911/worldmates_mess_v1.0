const funcs = require('../functions/functions')
const compiledTemplates = require('../compiledTemplates/compiledTemplates')
const socketEvents = require('../events/events')
const { Sequelize, Op, DataTypes } = require("sequelize");
const striptags = require('striptags');
const moment = require("moment")
const registerCallsListeners = require('./calls-listener');
const registerChannelsListeners = require('./channels-listener');
const registerStoriesListeners = require('./stories-listener');


const { AvatarChangedController } = require('../controllers/AvatarChangedController');
const { JoinController } = require('../controllers/JoinController');
const { PingForLastseenController } = require('../controllers/PingForLastseenController');
const { CloseChatController } = require('../controllers/CloseChatController');
const { IsChatOnController } = require('../controllers/IsChatOnController');
const { PageMessageController } = require('../controllers/PageMessageController');
const { GroupMessageController } = require('../controllers/GroupMessageController');
const { GroupMessagePageController } = require('../controllers/GroupMessagePageController');
const { PrivateMessagePageController } = require('../controllers/PrivateMessagePageController');
const { ActiveMessageUserChangeController } = require('../controllers/ActiveMessageUserChangeController');
const { TypingController } = require('../controllers/TypingController');
const { RecordingController } = require('../controllers/RecordingController');
const { TypingDoneController } = require('../controllers/TypingDoneController');
const { GetReactionController } = require('../controllers/GetReactionController');
const { ColorChangeController } = require('../controllers/ColorChangeController');
const { SyncGroupsController } = require('../controllers/SyncGroupsController');
const { MuteController } = require('../controllers/MuteController');
const { PrivateMessageController } = require('../controllers/PrivateMessageController');
const { LoadmoreController } = require('../controllers/LoadmoreController');
const { LoadmorePageController } = require('../controllers/LoadmorePageController');
const { LoadmoreGroupController } = require('../controllers/LoadmoreGroupController');
const { LoadmoreGroupPageController } = require('../controllers/LoadmoreGroupPageController');
const { OnNameChangedController } = require('../controllers/OnNameChangedController');
const { OnUserLoggedinController } = require('../controllers/OnUserLoggedinController');
const { EventNotificationController } = require('../controllers/EventNotificationController');
const { GroupNotificationController } = require('../controllers/GroupNotificationController');
const { PageNotificationController } = require('../controllers/PageNotificationController');
const { UserFollowersNotificationController } = require('../controllers/UserFollowersNotificationController');
const { UpdateNewPostsController } = require('../controllers/UpdateNewPostsController');
const { DeclineCallController } = require('../controllers/DeclineCallController');
const { UserNotificationController } = require('../controllers/UserNotificationController');
const { RegisterReactionController } = require('../controllers/RegisterReactionController');
const { CheckoutNotificationController } = require('../controllers/CheckoutNotificationController');
const { MainNotificationController } = require('../controllers/MainNotificationController');
const { PostNotificationController } = require('../controllers/PostNotificationController');
const { CommentNotificationController } = require('../controllers/CommentNotificationController');
const { ReplyNotificationController } = require('../controllers/ReplyNotificationController');
const { OnUserLoggedoffController } = require('../controllers/OnUserLoggedoffController');
const { SeenMessagesController } = require('../controllers/SeenMessagesController');
const { DisconnectController } = require('../controllers/DisconnectController');
const { NotificationsController } = require('../controllers/NotificationsController');
const { ColorChangedController } = require('../controllers/ColorChangedController');
const { CreateGroupController } = require('../controllers/CreateGroupController');
const { UpdateGroupController } = require('../controllers/UpdateGroupController');
const { DeleteGroupController } = require('../controllers/DeleteGroupController');
const { AddGroupMemberController } = require('../controllers/AddGroupMemberController');
const { RemoveGroupMemberController } = require('../controllers/RemoveGroupMemberController');
const { SetGroupRoleController } = require('../controllers/SetGroupRoleController');
const { LeaveGroupController } = require('../controllers/LeaveGroupController');
const { GetGroupDetailsController } = require('../controllers/GetGroupDetailsController');

const redis = require("redis");
let redisSubscribed = false;
let redisConnecting = false;
let sub = null;

// Redis configuration - reads from config.json or uses defaults
let redisConfig;
try {
    redisConfig = require('../config.json');
} catch (e) {
    redisConfig = {};
}
const REDIS_HOST = redisConfig.redis_host || '127.0.0.1';
const REDIS_PORT = redisConfig.redis_port || 6379;
const REDIS_PASSWORD = redisConfig.redis_password || '';

function createRedisClient() {
    const client = redis.createClient({
        url: `redis://${REDIS_HOST}:${REDIS_PORT}`,
        password: REDIS_PASSWORD,
        socket: {
            reconnectStrategy: (retries) => {
                if (retries > 10) {
                    console.log('[Redis] Max reconnection attempts reached');
                    return new Error('Max reconnection attempts reached');
                }
                return Math.min(retries * 500, 5000);
            }
        }
    });

    client.on('error', (err) => {
        // Don't spam logs for known connection issues
        if (err.message && !err.message.includes('Socket already opened')) {
            console.log('[Redis] Error:', err.message);
        }
    });

    client.on('ready', () => {
        console.log('[Redis] Connection ready');
    });

    return client;
}

async function initRedisSub(io) {
    // Prevent concurrent initialization and re-initialization
    if (redisSubscribed || redisConnecting) return;
    redisConnecting = true;

    try {
        console.log(`=== Попытка подключения к Redis ${REDIS_HOST}:${REDIS_PORT}... ===`);

        // Create new client if needed
        if (!sub) {
            sub = createRedisClient();
        }

        // Check if already connected
        if (sub.isOpen) {
            console.log('[Redis] Already connected, skipping connect()');
            redisConnecting = false;
            return;
        }

        await sub.connect();
        console.log("=== Node.js: ПОДКЛЮЧЕНО К REDIS успешно ===");

        await sub.subscribe('messages', async (message) => {
            try {
                const decoded = JSON.parse(message);
                if (!decoded || !decoded.to_id) return;

                const targetUserId = String(decoded.to_id);
                const msgData = decoded.data;

                console.log(`=== Redis: Получено сообщение для user_${targetUserId} ===`);

                // Emit to all formats for compatibility

                // 1. For mobile apps (new_message)
                io.to(targetUserId).emit('new_message', msgData);

                // 2. Same as private_message for compatibility
                io.to(targetUserId).emit('private_message', msgData);

                // 3. Browser version with HTML (private_message_page)
                const simpleHtml = `<div class="messages-wrapper" data-message-id="${msgData.id}">
                                        <div class="message-text">${msgData.text || ""}</div>
                                    </div>`;

                io.to(targetUserId).emit('private_message_page', {
                    status: 200,
                    id: String(decoded.from_id),
                    message: msgData.text || "",
                    messages_html: simpleHtml,
                    message_page_html: simpleHtml,
                    avatar: (msgData.user_data && msgData.user_data.avatar) ? msgData.user_data.avatar : "",
                    username: (msgData.user_data && msgData.user_data.name) ? msgData.user_data.name : "User",
                    messageData: msgData,
                    self: false
                });

                console.log(`[Redis] Emitted message events to room: ${targetUserId}`);

            } catch (e) {
                console.log("[Redis] Error in subscription handler:", e.message);
            }
        });
        redisSubscribed = true;
    } catch (e) {
        console.log("[Redis] Init error:", e.message);
        // Reset state so next connection attempt can retry
        redisSubscribed = false;
    } finally {
        redisConnecting = false;
    }
}

module.exports.registerListeners = async (socket, io, ctx) => {

    console.log('🔌 User connected: socket_id=' + socket.id + " query=" + JSON.stringify(socket.handshake.query));

    initRedisSub(io);

    await compiledTemplates.DefineTemplates(ctx);
    await registerCallsListeners(socket, io, ctx);
    await registerChannelsListeners(socket, io, ctx);
    await registerStoriesListeners(socket, io, ctx);
    ctx.reactions_types = await funcs.Wo_GetReactionsTypes(ctx);

    // ==================== ОСНОВНІ ОБРОБНИКИ ====================

    socket.on("join", async (data, callback) => {
        console.log("🔥 JOIN event received:", data);
        JoinController(ctx, data, io, socket, callback);
    })

    socket.on("ping_for_lastseen", async (data) => {
        PingForLastseenController(ctx, data, io, socket);
    })

    socket.on("close_chat", async (data) => {
        CloseChatController(ctx, data, io, socket);
    })

    socket.on("is_chat_on", async (data) => {
        IsChatOnController(ctx, data, io, socket);
    })

    socket.on("page_message", async (data, callback) => {
        PageMessageController(ctx, data, io, socket, callback);
    })

    socket.on("group_message", async (data, callback) => {
        GroupMessageController(ctx, data, io, socket, callback);
    })

    socket.on("group_message_page", async (data, callback) => {
        GroupMessagePageController(ctx, data, io, socket, callback);
    })

    socket.on("private_message_page", async (data, callback) => {
        PrivateMessagePageController(ctx, data, io, socket, callback);
    })

    socket.on("active-message-user-change", async (data) => {
        ActiveMessageUserChangeController(ctx, data, io, socket);
    })

    socket.on('typing', async (data) => {
        console.log("⌨️ TYPING event:", data);
        TypingController(ctx, data, io, socket);
    })

    socket.on('recording', async (data) => {
        RecordingController(ctx, data, io, socket);
    })

    socket.on('typing_done', async (data) => {
        TypingDoneController(ctx, data, io, socket);
    })

    socket.on('get_reaction', async (data) => {
        GetReactionController(ctx, data, io, socket);
    })

    socket.on("color-change", async (data) => {
        ColorChangeController(ctx, data, io, socket);
    })

    socket.on("sync_groups", async (data) => {
        SyncGroupsController(ctx, data, io, socket);
    })

    socket.on("mute", async (data, callback) => {
        MuteController(ctx, data, io, socket, callback);
    })

    // 🔥 КРИТИЧНО: Обробник приватних повідомлень
    socket.on("private_message", async (data, callback) => {
        console.log("🔥 PRIVATE_MESSAGE event received:", {
            from_id: data.from_id,
            to_id: data.to_id,
            msg: data.msg ? data.msg.substring(0, 50) + '...' : 'empty',
            socket_id: socket.id
        });
        PrivateMessageController(ctx, data, io, socket, callback);
    })

    socket.on("loadmore", async (data, callback) => {
        LoadmoreController(ctx, data, io, socket, callback);
    })

    socket.on("loadmore_page", async (data, callback) => {
        LoadmorePageController(ctx, data, io, socket, callback);
    })

    socket.on("loadmore_group", async (data, callback) => {
        LoadmoreGroupController(ctx, data, io, socket, callback);
    })

    socket.on("loadmore_group_page", async (data, callback) => {
        LoadmoreGroupPageController(ctx, data, io, socket, callback);
    })

    socket.on("on_name_changed", async (data) => {
        OnNameChangedController(ctx, data, io, socket);
    })

    socket.on("on_avatar_changed", async (data) => {
        AvatarChangedController(ctx, data, io);
    })

    socket.on("on_user_loggedin", async (data) => {
        OnUserLoggedinController(ctx, data, io, socket);
    })

    socket.on("event_notification", async (data) => {
        EventNotificationController(ctx, data, io, socket);
    })

    socket.on("group_notification", async (data) => {
        GroupNotificationController(ctx, data, io, socket);
    })

    socket.on("page_notification", async (data) => {
        PageNotificationController(ctx, data, io, socket);
    })

    socket.on("user_followers_notification", async (data) => {
        UserFollowersNotificationController(ctx, data, io, socket);
    })

    socket.on("update_new_posts", async (data) => {
        UpdateNewPostsController(ctx, data, io, socket);
    })

    socket.on("decline_call", async (data) => {
        DeclineCallController(ctx, data, io, socket);
    })

    socket.on("user_notification", async (data) => {
        UserNotificationController(ctx, data, io, socket);
    })

    socket.on("register_reaction", async (data) => {
        RegisterReactionController(ctx, data, io, socket);
    })

    socket.on("checkout_notification", async (data) => {
        CheckoutNotificationController(ctx, data, io, socket);
    })

    socket.on("main_notification", async (data) => {
        MainNotificationController(ctx, data, io, socket);
    })

    socket.on("post_notification", async (data) => {
        PostNotificationController(ctx, data, io, socket);
    })

    socket.on("comment_notification", async (data) => {
        CommentNotificationController(ctx, data, io, socket);
    })

    socket.on("reply_notification", async (data) => {
        ReplyNotificationController(ctx, data, io, socket);
    })

    socket.on("on_user_loggedoff", async (data) => {
        OnUserLoggedoffController(ctx, data, io, socket);
    })

    socket.on('seen_messages', async (data) => {
        SeenMessagesController(ctx, data, io, socket);
    })

    socket.on('color_changed', async (data) => {
        ColorChangedController(ctx, data, io, socket);
    })

    // ==================== ГРУПОВІ ОБРОБНИКИ ====================
    // ✅ ВИПРАВЛЕНО: Винесено З disconnect!

    socket.on('create_group', async (data, callback) => {
        CreateGroupController(ctx, data, io, socket, callback);
    })

    socket.on('update_group', async (data, callback) => {
        UpdateGroupController(ctx, data, io, socket, callback);
    })

    socket.on('delete_group', async (data, callback) => {
        DeleteGroupController(ctx, data, io, socket, callback);
    })

    socket.on('add_group_member', async (data, callback) => {
        AddGroupMemberController(ctx, data, io, socket, callback);
    })

    socket.on('remove_group_member', async (data, callback) => {
        RemoveGroupMemberController(ctx, data, io, socket, callback);
    })

    socket.on('set_group_role', async (data, callback) => {
        SetGroupRoleController(ctx, data, io, socket, callback);
    })

    socket.on('leave_group', async (data, callback) => {
        LeaveGroupController(ctx, data, io, socket, callback);
    })

    socket.on('get_group_details', async (data, callback) => {
        GetGroupDetailsController(ctx, data, io, socket, callback);
    })

    // ==================== DISCONNECT ====================

    socket.on('disconnect', async (reason) => {
        console.log("❌ User disconnected: socket_id=" + socket.id + " reason=" + reason);
        DisconnectController(ctx, reason, io, socket);
    });
}