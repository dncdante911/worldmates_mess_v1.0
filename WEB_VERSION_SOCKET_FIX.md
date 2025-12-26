# 🌐 WEB VERSION SOCKET.IO FIX - Complete Guide

## 📋 Overview

This guide provides the complete Socket.IO client implementation for the WoWonder web messenger to work with the fixed Node.js server.

**Target:** Web browser version (HTML/JavaScript)
**Server:** Node.js with Socket.IO on port 449
**Protocol:** WebSocket over HAproxy
**Status:** Based on server implementation analysis

---

## 🔍 Issues Found in Web Version

### Common Problems in WoWonder Web Messenger:

1. **XHR Polling Instead of WebSocket**
   - Slower performance
   - Higher server load
   - Connection drops

2. **Event Name Mismatches**
   - Server emits `user_status_change` but client expects different events
   - Multiple message formats: `private_message` vs `private_message_page`

3. **Room Type Mismatches**
   - Server uses String room IDs: `String(user_id)`
   - Client might try numeric IDs

4. **Authentication Issues**
   - Must send `access_token` (session hash) not numeric `user_id`
   - Incorrect join payload format

5. **HTML vs JSON Responses**
   - Server sends HTML wrapped responses for `private_message_page`
   - Must parse HTML or use JSON events

---

## ✅ CORRECT SOCKET.IO CLIENT IMPLEMENTATION

### 1. Load Socket.IO Library

```html
<!-- In your HTML <head> or before </body> -->
<script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>

<!-- OR if using local library -->
<script src="/assets/libraries/socket.io/socket.io.min.js"></script>
```

### 2. Initialize Socket Connection

```javascript
/**
 * Socket.IO Connection Configuration
 * CRITICAL: Must match server requirements!
 */

// Get user session token (access_token from cookies/localStorage)
const userSessionToken = getCookie('access_token') || localStorage.getItem('access_token');

// Initialize Socket.IO connection
const socket = io('https://worldmates.club:449', {
    // CRITICAL: Force WebSocket transport (avoid XHR polling)
    transports: ['websocket', 'polling'],

    // Reconnection settings
    reconnection: true,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 5000,
    reconnectionAttempts: 5,

    // SSL settings
    secure: true,
    rejectUnauthorized: false,  // For self-signed certs

    // Query parameters (optional)
    query: {
        access_token: userSessionToken
    }
});

console.log('🔌 Socket.IO initialized');
```

### 3. Connection Event Handlers

```javascript
/**
 * Socket Connection Events
 */

socket.on('connect', () => {
    console.log('✅ Socket connected:', socket.id);

    // CRITICAL: Authenticate with server
    // Send access_token (session hash), NOT numeric user_id!
    socket.emit('join', {
        user_id: userSessionToken  // This is the session hash!
    }, (response) => {
        if (response && response.status === 200) {
            console.log('✅ Joined as user:', response.user_id);
            window.currentUserId = response.user_id;  // Store numeric ID
        } else {
            console.error('❌ Join failed:', response);
        }
    });
});

socket.on('connect_error', (error) => {
    console.error('❌ Connection error:', error.message);
});

socket.on('disconnect', (reason) => {
    console.log('❌ Disconnected:', reason);
});

socket.on('reconnect', (attemptNumber) => {
    console.log('🔄 Reconnected after', attemptNumber, 'attempts');
});
```

### 4. Message Event Handlers

```javascript
/**
 * Message Reception Events
 * IMPORTANT: Server emits BOTH formats!
 */

// Format 1: JSON message (for mobile apps and modern web)
socket.on('private_message', (data) => {
    console.log('📨 private_message:', data);

    /**
     * Data structure:
     * {
     *   id: 12345,
     *   from_id: 8,
     *   to_id: 24,
     *   text: "encrypted_message",
     *   time: 1703601234,
     *   media: "url_or_empty",
     *   user_data: {
     *     avatar: "url",
     *     name: "Username"
     *   }
     * }
     */

    // Decrypt message if needed
    const decryptedText = decryptMessage(data.text);

    // Add message to chat UI
    addMessageToChat({
        id: data.id,
        fromId: data.from_id,
        toId: data.to_id,
        text: decryptedText,
        time: data.time,
        media: data.media,
        avatar: data.user_data?.avatar,
        username: data.user_data?.name,
        isSelf: data.from_id === window.currentUserId
    });

    // Play notification sound
    playMessageSound();

    // Update unread count
    updateUnreadCount();
});

// Format 2: HTML wrapped message (for legacy web UI)
socket.on('private_message_page', (data) => {
    console.log('📨 private_message_page:', data);

    /**
     * Data structure:
     * {
     *   status: 200,
     *   id: "8",  // from_id as string
     *   message: "text",
     *   messages_html: "<div>...</div>",
     *   message_page_html: "<div>...</div>",
     *   avatar: "url",
     *   username: "Name",
     *   messageData: { full message object },
     *   self: false
     * }
     */

    // If using HTML rendering
    if (data.messages_html) {
        $('#chat-messages').append(data.messages_html);
    }

    // OR use JSON data
    if (data.messageData) {
        addMessageToChat(data.messageData);
    }

    scrollToBottom();
});

// New message notification (light event)
socket.on('new_message', (data) => {
    console.log('📨 new_message:', data);
    // Same structure as private_message
    // Can be used for notifications without full UI update
});
```

### 5. User Status Events

```javascript
/**
 * User Online/Offline Status
 * IMPORTANT: Server may send JSON OR HTML!
 */

socket.on('user_status_change', (data) => {
    console.log('👤 user_status_change:', data);

    // Check if it's JSON or HTML
    if (typeof data === 'object' && data.user_id) {
        // JSON format
        const userId = parseInt(data.user_id);
        const isOnline = data.status === "1" || data.status === 1;

        updateUserStatus(userId, isOnline);

    } else if (typeof data === 'string' || data.online_users) {
        // HTML format - parse it
        const onlineUsersHtml = data.online_users || data;
        parseOnlineUsersFromHtml(onlineUsersHtml);
    }
});

// Specific events
socket.on('on_user_loggedin', (data) => {
    console.log('✅ User logged in:', data.user_id);
    updateUserStatus(data.user_id, true);
});

socket.on('on_user_loggedoff', (data) => {
    console.log('❌ User logged off:', data.user_id);
    updateUserStatus(data.user_id, false);
});

// Helper function to parse HTML status
function parseOnlineUsersFromHtml(html) {
    // Extract user IDs from HTML like: id="online_8"
    const pattern = /id="online_(\d+)"/g;
    let match;

    while ((match = pattern.exec(html)) !== null) {
        const userId = parseInt(match[1]);
        if (userId > 0) {
            updateUserStatus(userId, true);
        }
    }
}

// Helper function to update UI
function updateUserStatus(userId, isOnline) {
    const statusIndicator = $(`[data-user-id="${userId}"] .status-indicator`);

    if (isOnline) {
        statusIndicator.addClass('online').removeClass('offline');
        statusIndicator.attr('title', 'Online');
    } else {
        statusIndicator.addClass('offline').removeClass('online');
        statusIndicator.attr('title', 'Offline');
    }
}
```

### 6. Typing Indicator Events

```javascript
/**
 * Typing Status Events
 * IMPORTANT: is_typing values are 200 (typing) or 300 (stopped)
 */

socket.on('typing', (data) => {
    console.log('⌨️ typing:', data);

    /**
     * Data structure:
     * {
     *   sender_id: 8,
     *   is_typing: 200  // or 300
     * }
     */

    const senderId = parseInt(data.sender_id);
    const isTyping = data.is_typing === 200;

    // Only show if we're in a chat with this user
    if (window.currentChatUserId === senderId) {
        if (isTyping) {
            $('#typing-indicator').show().text('Typing...');
        } else {
            $('#typing-indicator').hide();
        }
    }
});

// When current user types
let typingTimeout;

$('#message-input').on('keypress', function() {
    clearTimeout(typingTimeout);

    // Send typing event
    socket.emit('typing', {
        user_id: userSessionToken,  // session hash
        recipient_id: window.currentChatUserId,
        is_typing: 200
    });

    // Auto-stop after 3 seconds
    typingTimeout = setTimeout(() => {
        socket.emit('typing_done', {
            user_id: userSessionToken,
            recipient_id: window.currentChatUserId
        });
    }, 3000);
});
```

### 7. Message Sending

```javascript
/**
 * Send Private Message
 */

function sendMessage(recipientId, messageText, mediaUrl = '') {
    // Encrypt message if needed
    const encryptedText = encryptMessage(messageText);

    socket.emit('private_message', {
        from_id: window.currentUserId,  // Numeric ID
        to_id: recipientId,              // Numeric ID
        msg: encryptedText,
        media: mediaUrl,
        time: Math.floor(Date.now() / 1000)
    }, (response) => {
        if (response && response.status === 200) {
            console.log('✅ Message sent:', response);

            // Add to local chat immediately (optimistic update)
            addMessageToChat({
                id: response.message_id,
                fromId: window.currentUserId,
                toId: recipientId,
                text: messageText,
                time: response.time,
                media: mediaUrl,
                isSelf: true
            });
        } else {
            console.error('❌ Send failed:', response);
            showError('Failed to send message');
        }
    });
}

// Web page format (alternative)
function sendMessagePage(recipientId, messageText) {
    socket.emit('private_message_page', {
        user_id: userSessionToken,  // session hash
        recipient_id: recipientId,
        msg: messageText
    }, (response) => {
        if (response && response.messages_html) {
            $('#chat-messages').append(response.messages_html);
            scrollToBottom();
        }
    });
}
```

### 8. Chat Management Events

```javascript
/**
 * Chat Open/Close Events
 */

// When user opens a chat
function openChat(recipientId, lastMessageId = null) {
    window.currentChatUserId = recipientId;

    socket.emit('is_chat_on', {
        user_id: userSessionToken,  // session hash
        recipient_id: recipientId,
        message_id: lastMessageId,
        isGroup: false
    });

    console.log('📖 Opened chat with user:', recipientId);
}

// When user closes a chat
function closeChat(recipientId) {
    socket.emit('close_chat', {
        user_id: userSessionToken,
        recipient_id: recipientId,
        isGroup: false
    });

    window.currentChatUserId = null;
    console.log('📕 Closed chat with user:', recipientId);
}

// Last seen status
socket.on('lastseen', (data) => {
    console.log('👁️ lastseen:', data);

    /**
     * Data structure:
     * {
     *   can_seen: 1,
     *   time: "2 hours ago",
     *   seen: "2 hours ago",
     *   message_id: 12345,
     *   user_id: 8
     * }
     */

    // Update message read status
    $(`[data-message-id="${data.message_id}"]`)
        .addClass('seen')
        .find('.seen-indicator')
        .text('✓✓')
        .attr('title', `Seen ${data.seen}`);
});

// Unread message count
socket.on('messages_count', (data) => {
    console.log('📊 messages_count:', data.count);

    // Update badge
    $('#unread-badge').text(data.count).toggle(data.count > 0);
});
```

### 9. Group Chat Events

```javascript
/**
 * Group Chat Support
 */

// Send group message
function sendGroupMessage(groupId, messageText) {
    socket.emit('group_message', {
        user_id: userSessionToken,
        group_id: groupId,
        msg: encryptMessage(messageText),
        time: Math.floor(Date.now() / 1000)
    }, (response) => {
        if (response && response.status === 200) {
            console.log('✅ Group message sent');
        }
    });
}

// Receive group message
socket.on('group_message', (data) => {
    console.log('👥 group_message:', data);
    addGroupMessageToChat(data);
});
```

### 10. Notification Events

```javascript
/**
 * Various Notification Events
 */

socket.on('user_notification', (data) => {
    console.log('🔔 user_notification:', data);
    showNotification(data);
});

socket.on('main_notification', (data) => {
    console.log('🔔 main_notification:', data);
    showNotification(data);
});

socket.on('page_notification', (data) => {
    console.log('🔔 page_notification:', data);
    showNotification(data);
});
```

---

## 🔧 Helper Functions

### Session Token Management

```javascript
/**
 * Get User Session Token (access_token)
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

function getSessionToken() {
    return getCookie('access_token') ||
           localStorage.getItem('access_token') ||
           sessionStorage.getItem('access_token');
}
```

### Message Encryption/Decryption

```javascript
/**
 * Message Encryption (placeholder - implement based on your crypto)
 */
function encryptMessage(plaintext) {
    // TODO: Implement your encryption logic
    // Match the server's CryptoHelper implementation
    return plaintext;  // For now, no encryption
}

function decryptMessage(ciphertext) {
    // TODO: Implement your decryption logic
    return ciphertext;  // For now, no decryption
}
```

### UI Update Functions

```javascript
/**
 * Add Message to Chat UI
 */
function addMessageToChat(message) {
    const messageHtml = `
        <div class="message ${message.isSelf ? 'self' : 'other'}"
             data-message-id="${message.id}">
            <div class="message-avatar">
                <img src="${message.avatar}" alt="${message.username}">
            </div>
            <div class="message-content">
                <div class="message-username">${message.username}</div>
                <div class="message-text">${escapeHtml(message.text)}</div>
                ${message.media ? `<img src="${message.media}" class="message-media">` : ''}
                <div class="message-time">${formatTime(message.time)}</div>
            </div>
        </div>
    `;

    $('#chat-messages').append(messageHtml);
    scrollToBottom();
}

function scrollToBottom() {
    const chatContainer = $('#chat-messages');
    chatContainer.animate({
        scrollTop: chatContainer[0].scrollHeight
    }, 300);
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

function formatTime(timestamp) {
    const date = new Date(timestamp * 1000);
    const now = new Date();

    if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else {
        return date.toLocaleDateString();
    }
}
```

---

## 🧪 Testing the Web Implementation

### 1. Open Browser Console

```javascript
// Check Socket connection
console.log('Socket connected:', socket.connected);
console.log('Socket ID:', socket.id);

// Check user authentication
console.log('Current user ID:', window.currentUserId);
```

### 2. Test Message Sending

```javascript
// Send test message
sendMessage(24, 'Hello from web!');

// Check in console for:
// ✅ Message sent: {status: 200, ...}
```

### 3. Monitor Events

```javascript
// Log all Socket events
const originalEmit = socket.emit;
socket.emit = function(...args) {
    console.log('📤 Emit:', args[0], args.slice(1));
    return originalEmit.apply(socket, args);
};

const originalOn = socket.on;
socket.on = function(event, handler) {
    return originalOn.call(socket, event, function(...args) {
        console.log('📥 Received:', event, args);
        return handler.apply(this, args);
    });
};
```

---

## 📝 Integration Checklist

- [ ] Load Socket.IO library (CDN or local)
- [ ] Initialize connection with correct URL and transports
- [ ] Implement `join` event on connect with session token
- [ ] Add `private_message` and `private_message_page` handlers
- [ ] Add `user_status_change` handler with HTML parsing
- [ ] Add `typing` event handler
- [ ] Implement message sending with `private_message` emit
- [ ] Add `lastseen` and `messages_count` handlers
- [ ] Implement chat open/close with `is_chat_on` and `close_chat`
- [ ] Add error handling and reconnection logic
- [ ] Test with browser console
- [ ] Verify real-time message reception
- [ ] Verify typing indicators work
- [ ] Verify online/offline status updates

---

## 🐛 Troubleshooting

### Messages Not Received

**Check:**
1. Socket connected: `socket.connected === true`
2. Joined successfully: Check console for "Joined as user"
3. Room type: Server uses String rooms `String(user_id)`
4. Event names: `private_message`, `private_message_page`, `new_message`

**Debug:**
```javascript
// Monitor all events
socket.onAny((event, ...args) => {
    console.log('📥 Event:', event, args);
});
```

### Status Not Updating

**Check:**
1. Listening to `user_status_change` event
2. Parsing HTML if server sends HTML format
3. User IDs match (numeric vs string)

### Typing Not Working

**Check:**
1. Sending `is_typing: 200` (not `true`)
2. Sending `typing_done` or `is_typing: 300` to stop
3. Recipient listening to `typing` event

### Connection Drops

**Check:**
1. Using WebSocket transport: `transports: ['websocket', 'polling']`
2. HAproxy configured for WebSocket (timeout client/server set)
3. Reconnection enabled in Socket.IO options

---

## 📚 Resources

- [Socket.IO Client Documentation](https://socket.io/docs/v4/client-api/)
- Server implementation: `server_modifications/nodejs_listeners_FIXED.js`
- Android implementation: `app/src/main/java/com/worldmates/messenger/network/SocketManager.kt`

---

**Created:** 2025-12-26
**Author:** Claude Code Agent
**Status:** Ready for implementation
**Priority:** HIGH - Required for web messenger real-time features
