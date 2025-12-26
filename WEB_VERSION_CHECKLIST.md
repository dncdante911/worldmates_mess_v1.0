# 🌐 WEB VERSION IMPLEMENTATION CHECKLIST

## 📋 Overview

This checklist helps you implement and verify Socket.IO real-time messaging in the WoWonder web version.

**Current Status:** Android app ✅ FIXED | Web version ⚠️ NEEDS FIXING

---

## 🔍 STEP 1: Locate Web Socket.IO Code

### Find these files in your web codebase:

**Look for Socket.IO initialization in:**
- `assets/js/app.js`
- `assets/js/main.js`
- `assets/js/socket.js`
- `assets/js/messenger.js`
- `assets/libraries/socket.io/socket.io.min.js`
- Inline `<script>` tags in:
  - `xhr/chat.php`
  - `xhr/messages.php`
  - `index.php`
  - `header.php`

**Search patterns to look for:**
```javascript
io('https://worldmates.club:449')
io.connect(
new io(
socket.on('private_message'
socket.emit('join'
```

### How to search:

```bash
# If you have SSH access to the server
cd /var/www/www-root/data/www/worldmates.club
grep -r "socket.emit" assets/ xhr/
grep -r "io.connect\|io(" assets/ xhr/
grep -r "private_message" assets/ xhr/
```

**OR** use browser DevTools:
1. Open web messenger in browser
2. Press F12 (DevTools)
3. Go to Sources tab
4. Search (Ctrl+Shift+F) for: `socket.emit`
5. Identify the file containing Socket.IO code

---

## 🔧 STEP 2: Verify Socket.IO Configuration

### ✅ Checklist:

- [ ] **1. Socket.IO library loaded**
  ```html
  <!-- Should exist in HTML <head> or before </body> -->
  <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>
  ```

- [ ] **2. Connection uses WebSocket transport**
  ```javascript
  // MUST have this option!
  const socket = io('https://worldmates.club:449', {
      transports: ['websocket', 'polling']  // ✅ WebSocket first!
  });

  // WRONG:
  const socket = io('https://worldmates.club:449');  // ❌ Will use polling!
  ```

- [ ] **3. Correct server URL and port**
  ```javascript
  // CORRECT:
  io('https://worldmates.club:449')

  // WRONG:
  io('https://worldmates.club')         // ❌ Missing port
  io('http://worldmates.club:449')      // ❌ Should be https
  io('worldmates.club:449')             // ❌ Missing protocol
  ```

- [ ] **4. Reconnection enabled**
  ```javascript
  {
      reconnection: true,              // ✅
      reconnectionDelay: 1000,
      reconnectionAttempts: 5
  }
  ```

---

## 🔑 STEP 3: Verify Authentication (JOIN Event)

### ✅ Checklist:

- [ ] **1. JOIN event emitted on connect**
  ```javascript
  socket.on('connect', () => {
      socket.emit('join', {
          user_id: userSessionToken  // ✅ access_token hash
      }, (response) => {
          console.log('Joined:', response);
      });
  });
  ```

- [ ] **2. Using access_token (NOT numeric user_id)**
  ```javascript
  // CORRECT:
  user_id: 'd00d1617c8ab91f8e2ecd61bed65051e42e389f6'  // ✅ Session hash

  // WRONG:
  user_id: 8  // ❌ Numeric ID won't work!
  ```

- [ ] **3. Callback receives numeric user_id**
  ```javascript
  socket.emit('join', { user_id: sessionToken }, (response) => {
      if (response && response.user_id) {
          window.currentUserId = response.user_id;  // ✅ Store numeric ID
          console.log('User ID:', response.user_id); // Should be: 8
      }
  });
  ```

- [ ] **4. Session token retrieved correctly**
  ```javascript
  // From cookie:
  const sessionToken = getCookie('access_token');

  // OR from localStorage:
  const sessionToken = localStorage.getItem('access_token');

  // OR from PHP:
  const sessionToken = '<?php echo $_SESSION['access_token']; ?>';
  ```

---

## 📨 STEP 4: Verify Message Event Handlers

### ✅ Checklist:

- [ ] **1. Listen to ALL message event types**
  ```javascript
  // Mobile/JSON format:
  socket.on('private_message', (data) => {
      console.log('📨 private_message:', data);
      addMessageToChat(data);
  });

  // Web/HTML format:
  socket.on('private_message_page', (data) => {
      console.log('📨 private_message_page:', data);
      if (data.messages_html) {
          $('#chat-messages').append(data.messages_html);
      }
  });

  // Light notification:
  socket.on('new_message', (data) => {
      console.log('📨 new_message:', data);
      updateUnreadCount();
  });
  ```

- [ ] **2. Handle message data structure correctly**
  ```javascript
  // Expected data:
  {
      id: 12345,          // Message ID
      from_id: 8,         // Sender (numeric)
      to_id: 24,          // Recipient (numeric)
      text: "encrypted",  // Message text
      time: 1703601234,   // Unix timestamp
      media: "",          // Media URL or empty
      user_data: {
          avatar: "url",
          name: "Username"
      }
  }
  ```

- [ ] **3. Messages appear in real-time (no manual refresh)**

- [ ] **4. Both sent and received messages show up instantly**

---

## 👤 STEP 5: Verify User Status Events

### ✅ Checklist:

- [ ] **1. Listen to status events**
  ```javascript
  socket.on('user_status_change', (data) => {
      console.log('👤 status:', data);
      // May be JSON or HTML!
  });

  socket.on('on_user_loggedin', (data) => {
      updateUserStatus(data.user_id, true);
  });

  socket.on('on_user_loggedoff', (data) => {
      updateUserStatus(data.user_id, false);
  });
  ```

- [ ] **2. Handle JSON format**
  ```javascript
  if (typeof data === 'object' && data.user_id) {
      const isOnline = data.status === "1" || data.status === 1;
      updateUserStatus(data.user_id, isOnline);
  }
  ```

- [ ] **3. Handle HTML format (if needed)**
  ```javascript
  // Server may send: <div id="online_8">...</div>
  if (typeof data === 'string' || data.online_users) {
      const pattern = /id="online_(\d+)"/g;
      let match;
      while ((match = pattern.exec(data)) !== null) {
          const userId = parseInt(match[1]);
          updateUserStatus(userId, true);
      }
  }
  ```

- [ ] **4. Online/Offline indicators update in real-time**

---

## ⌨️ STEP 6: Verify Typing Indicators

### ✅ Checklist:

- [ ] **1. Send typing events**
  ```javascript
  $('#message-input').on('keypress', () => {
      socket.emit('typing', {
          user_id: sessionToken,     // Session hash
          recipient_id: recipientId, // Numeric ID
          is_typing: 200             // ✅ 200 = typing, 300 = stopped
      });
  });
  ```

- [ ] **2. Receive typing events**
  ```javascript
  socket.on('typing', (data) => {
      // data.sender_id = numeric
      // data.is_typing = 200 (typing) or 300 (stopped)

      if (data.is_typing === 200) {
          showTypingIndicator(data.sender_id);
      } else {
          hideTypingIndicator(data.sender_id);
      }
  });
  ```

- [ ] **3. Auto-stop typing after delay**
  ```javascript
  let typingTimeout;

  function sendTyping() {
      clearTimeout(typingTimeout);

      socket.emit('typing', {
          user_id: sessionToken,
          recipient_id: recipientId,
          is_typing: 200
      });

      typingTimeout = setTimeout(() => {
          socket.emit('typing_done', {
              user_id: sessionToken,
              recipient_id: recipientId
          });
      }, 3000);
  }
  ```

- [ ] **4. Typing indicator shows "Typing..." in real-time**

---

## 📤 STEP 7: Verify Message Sending

### ✅ Checklist:

- [ ] **1. Send with correct event name**
  ```javascript
  // CORRECT:
  socket.emit('private_message', {
      from_id: currentUserId,  // ✅ Numeric ID (from JOIN response)
      to_id: recipientId,       // ✅ Numeric ID
      msg: encryptedText,
      time: Math.floor(Date.now() / 1000)
  }, (response) => {
      console.log('Sent:', response);
  });

  // WRONG event names:
  socket.emit('send_message', ...)    // ❌
  socket.emit('message', ...)         // ❌
  socket.emit('sendMessage', ...)     // ❌
  ```

- [ ] **2. Use numeric user IDs (not session tokens)**
  ```javascript
  // CORRECT:
  from_id: 8,    // ✅ Numeric
  to_id: 24,     // ✅ Numeric

  // WRONG:
  from_id: 'd00d1617c8...',  // ❌ Session token
  ```

- [ ] **3. Handle callback response**
  ```javascript
  socket.emit('private_message', data, (response) => {
      if (response && response.status === 200) {
          // ✅ Message sent successfully
          console.log('Message ID:', response.message_id);
      } else {
          // ❌ Send failed
          console.error('Send failed:', response);
          showError('Failed to send message');
      }
  });
  ```

- [ ] **4. Sent messages appear immediately (optimistic UI update)**

---

## 🔍 STEP 8: Testing & Diagnostics

### Use the diagnostic tool:

1. **Open diagnostic page**
   ```
   File: WEB_DIAGNOSTICS_SCRIPT.html
   ```

2. **Run in browser**
   - Open file in browser or host on server
   - Enter your access_token
   - Click "Connect"

3. **Check diagnostics**
   - ✅ Connection status: Connected
   - ✅ Transport: websocket (not polling!)
   - ✅ User ID: numeric value
   - ✅ No issues detected

4. **Run tests**
   - Test Join Event → Should see user_id in response
   - Check Transport → Should be "websocket"
   - Send Test Message → Should receive callback
   - Test Typing → Should emit typing events

### Browser Console Tests:

```javascript
// 1. Check connection
console.log('Connected:', socket.connected);  // Should be true
console.log('Socket ID:', socket.id);         // Should be string like "abc123"
console.log('Transport:', socket.io.engine.transport.name);  // Should be "websocket"

// 2. Check authentication
console.log('User ID:', window.currentUserId);  // Should be numeric

// 3. Monitor all events
socket.onAny((event, ...args) => {
    console.log('📥', event, args);
});

// 4. Test message send
socket.emit('private_message', {
    from_id: window.currentUserId,
    to_id: 24,
    msg: 'Test',
    time: Math.floor(Date.now() / 1000)
}, (response) => {
    console.log('Response:', response);
});
```

---

## 🐛 STEP 9: Troubleshooting

### Issue: No messages received

**Check:**
- [ ] Socket connected: `socket.connected === true`
- [ ] Joined successfully: Check console for JOIN response
- [ ] Listening to correct events: `private_message`, `private_message_page`, `new_message`
- [ ] Server emitting to correct room: `String(user_id)`
- [ ] Redis subscriber working (check server logs)

**Debug:**
```javascript
socket.onAny((event, ...args) => {
    console.log('📥 Event:', event, args);
});
```

### Issue: Using XHR polling instead of WebSocket

**Check:**
```javascript
console.log(socket.io.engine.transport.name);
// Should be: "websocket"
// If "polling" → FIX NEEDED!
```

**Fix:**
```javascript
const socket = io(url, {
    transports: ['websocket', 'polling']  // ✅ WebSocket first
});
```

### Issue: Authentication fails (no user_id)

**Check:**
- [ ] Sending correct access_token (from cookie/localStorage)
- [ ] Server JoinController working (check server logs)
- [ ] Session exists in database

**Debug:**
```javascript
socket.emit('join', {
    user_id: 'd00d1617c8...'  // Your session token
}, (response) => {
    console.log('JOIN response:', response);
    // Should have: { status: 200, user_id: 8 }
});
```

### Issue: Connection drops frequently

**Check:**
- [ ] HAproxy timeout settings (should be 300000ms+)
- [ ] Reconnection enabled in Socket.IO options
- [ ] WebSocket transport (polling is less stable)

**Fix HAproxy:**
```
timeout client 300000
timeout server 300000
timeout connect 5000
```

### Issue: Status not updating

**Check:**
- [ ] Listening to `user_status_change` event
- [ ] Handling both JSON and HTML formats
- [ ] Parsing HTML correctly if needed

**Debug:**
```javascript
socket.on('user_status_change', (data) => {
    console.log('Status type:', typeof data);
    console.log('Status data:', data);
});
```

---

## 📊 STEP 10: Server Logs Verification

### On the server (Node.js):

```bash
# View Node.js logs
pm2 logs messenger-main --lines 100

# Should see:
✅ Socket joined room: "8" (type: string)
✅ Socket joined room: 8 (type: number)
>>> Emitted new_message to room: 24
>>> Emitted private_message to room: 24
```

### What to look for:

- [ ] `✅ User X joined room: "Y"` - Room join with STRING type
- [ ] `>>> Emitted new_message to room: X` - Redis emits to rooms
- [ ] `🔥 PRIVATE_MESSAGE event received` - Server receives messages
- [ ] `✅ Redis: Всі емити виконані успішно` - Redis completes

### If you DON'T see these logs:

1. **Update server files:**
   - Replace `listeners/listeners.js` with `server_modifications/nodejs_listeners_FIXED.js`
   - Replace `controllers/JoinController.js` with `server_modifications/JoinController_FIXED.js`
   - Replace `controllers/IsChatOnController.js` with `server_modifications/IsChatOnController_FIXED.js`

2. **Restart Node.js:**
   ```bash
   pm2 restart messenger-main --update-env
   ```

---

## ✅ FINAL VERIFICATION

### All systems working:

- [ ] ✅ Socket connects via WebSocket transport
- [ ] ✅ JOIN event authenticates successfully
- [ ] ✅ Real-time messages received instantly
- [ ] ✅ Sent messages appear immediately
- [ ] ✅ Online/Offline status updates in real-time
- [ ] ✅ Typing indicator works both ways
- [ ] ✅ No connection drops
- [ ] ✅ Browser console shows no errors
- [ ] ✅ Server logs show successful emissions

### Performance check:

- [ ] Messages appear in <1 second
- [ ] Typing indicator shows <500ms delay
- [ ] Status updates appear instantly
- [ ] No lag or delays

---

## 📚 Reference Files

**Implementation guide:**
- `WEB_VERSION_SOCKET_FIX.md` - Complete Socket.IO client code

**Diagnostic tools:**
- `WEB_DIAGNOSTICS_SCRIPT.html` - Browser-based diagnostic tool

**Server files:**
- `server_modifications/nodejs_listeners_FIXED.js` - Event listeners
- `server_modifications/JoinController_FIXED.js` - Authentication
- `server_modifications/IsChatOnController_FIXED.js` - Chat status

**Documentation:**
- `SOCKET_IO_REALTIME_FIX.md` - Android + Web fixes overview
- `CRITICAL_FIX_ROOMS.md` - Room type issues explained
- `SERVER_DIAGNOSTICS.md` - Server troubleshooting

---

## 🎯 Quick Start

1. **Locate Socket.IO code** in web files
2. **Compare** with `WEB_VERSION_SOCKET_FIX.md`
3. **Fix** any mismatches
4. **Test** with `WEB_DIAGNOSTICS_SCRIPT.html`
5. **Verify** with browser console
6. **Check** server logs
7. **Done!** 🎉

---

**Created:** 2025-12-26
**Author:** Claude Code Agent
**Status:** Ready to use
**Priority:** HIGH - Complete web messenger real-time functionality
