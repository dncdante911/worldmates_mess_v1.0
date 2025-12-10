# Group Chat Management API

## Overview
This document describes the new Socket.IO events for managing group chats in the messenger application.

## Database Migration

Before using these APIs, you must apply the database migration:

```bash
mysql -u social -p socialhub < migrations/001_extend_group_chat.sql
```

**Changes made:**
- Extended `Wo_GroupChat` table with: `description`, `is_private`, `settings` fields
- Increased `group_name` from VARCHAR(20) to VARCHAR(255)
- Added `role` field to `Wo_GroupChatUsers` (owner/admin/moderator/member)

## Socket.IO Events

### 1. Create Group

**Event:** `create_group`

**Request:**
```javascript
socket.emit('create_group', {
  from_id: 'user_hash',
  group_name: 'My Awesome Group',
  description: 'Group description', // optional
  is_private: false, // optional, default: false
  avatar: 'upload/photos/group.jpg', // optional
  member_ids: [123, 456, 789], // optional, array of user IDs to add
  settings: { // optional
    members_can_invite: true,
    members_can_pin: false,
    members_can_delete: false
  }
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Group created successfully",
  group: {
    group_id: 1,
    user_id: 42,
    group_name: "My Awesome Group",
    description: "Group description",
    is_private: "0",
    settings: {...},
    avatar: "https://...",
    time: "1234567890",
    type: "group",
    members_count: 4,
    members: [...]
  }
}
```

**Socket Events Emitted:**
- `group_created` - sent to all members

---

### 2. Update Group

**Event:** `update_group`

**Permissions:** Owner or Admin

**Request:**
```javascript
socket.emit('update_group', {
  from_id: 'user_hash',
  group_id: 1,
  group_name: 'Updated Name', // optional
  description: 'New description', // optional
  is_private: true, // optional, only owner can change
  avatar: 'new_avatar.jpg', // optional
  settings: {...} // optional
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Group updated successfully",
  group: {...}
}
```

**Socket Events Emitted:**
- `group_updated` - sent to all members (except requester)

---

### 3. Delete Group

**Event:** `delete_group`

**Permissions:** Owner only

**Request:**
```javascript
socket.emit('delete_group', {
  from_id: 'user_hash',
  group_id: 1
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Group deleted successfully",
  group_id: 1
}
```

**Socket Events Emitted:**
- `group_deleted` - sent to all members

**Note:** This will delete:
- The group record
- All group members
- All messages in the group

---

### 4. Add Group Member

**Event:** `add_group_member`

**Permissions:** Owner, Admin, or any member if `settings.members_can_invite` is true

**Request:**
```javascript
socket.emit('add_group_member', {
  from_id: 'user_hash',
  group_id: 1,
  user_id_to_add: 999
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Member added successfully",
  member: {
    id: 42,
    user_id: 999,
    group_id: 1,
    role: "member",
    active: "1",
    user: {
      user_id: 999,
      username: "john_doe",
      name: "John Doe",
      avatar: "..."
    }
  }
}
```

**Socket Events Emitted:**
- `group_member_added` - sent to all members

---

### 5. Remove Group Member

**Event:** `remove_group_member`

**Permissions:** Owner or Admin

**Request:**
```javascript
socket.emit('remove_group_member', {
  from_id: 'user_hash',
  group_id: 1,
  user_id_to_remove: 999
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Member removed successfully",
  group_id: 1,
  user_id: 999
}
```

**Socket Events Emitted:**
- `group_member_removed` - sent to remaining members
- `removed_from_group` - sent to removed user

**Rules:**
- Owner cannot be removed
- Admins cannot remove other admins or owner
- Only owner can remove admins

---

### 6. Set Group Role

**Event:** `set_group_role`

**Permissions:** Owner only

**Request:**
```javascript
socket.emit('set_group_role', {
  from_id: 'user_hash',
  group_id: 1,
  user_id_to_update: 999,
  new_role: 'admin' // admin, moderator, or member
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Member role updated successfully",
  member: {...}
}
```

**Socket Events Emitted:**
- `group_member_role_updated` - sent to all members

**Rules:**
- Only owner can change roles
- Owner role cannot be changed
- Cannot change your own role

---

### 7. Leave Group

**Event:** `leave_group`

**Request:**
```javascript
socket.emit('leave_group', {
  from_id: 'user_hash',
  group_id: 1
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  message: "Left group successfully",
  group_id: 1
}
```

**Socket Events Emitted:**
- `group_member_left` - sent to remaining members

**Rules:**
- Owner cannot leave (must transfer ownership or delete group)

---

### 8. Get Group Details

**Event:** `get_group_details`

**Request:**
```javascript
socket.emit('get_group_details', {
  from_id: 'user_hash',
  group_id: 1
}, (response) => {
  console.log(response);
});
```

**Response:**
```javascript
{
  status: 200,
  group: {
    group_id: 1,
    user_id: 42,
    group_name: "My Group",
    description: "...",
    is_private: "0",
    settings: {...},
    avatar: "https://...",
    time: "1234567890",
    type: "group",
    members_count: 10,
    messages_count: 1543,
    members: [
      {
        id: 1,
        user_id: 42,
        group_id: 1,
        role: "owner",
        active: "1",
        last_seen: "1234567890",
        user: {
          user_id: 42,
          username: "creator",
          name: "Group Creator",
          avatar: "...",
          last_seen: "...",
          verified: "1"
        }
      },
      // ... more members sorted by role (owner, admin, moderator, member)
    ],
    is_member: true,
    my_role: "owner"
  }
}
```

**Rules:**
- Private groups can only be viewed by members
- Public groups can be viewed by anyone

---

## Member Roles

### Owner
- Can do everything
- Cannot be removed
- Cannot leave (must delete group or transfer ownership)
- Only one owner per group

### Admin
- Can add/remove members (except owner and other admins)
- Can update group info
- Cannot change group privacy
- Cannot change member roles

### Moderator
- Can delete messages (if implemented)
- Same as member for group management

### Member
- Can send messages
- Can leave group
- Can add members if `settings.members_can_invite` is true

---

## Group Settings (JSON)

Settings are stored as JSON in the `settings` field:

```javascript
{
  "members_can_invite": true,      // Allow members to add new members
  "members_can_pin": false,        // Allow members to pin messages
  "members_can_delete": false,     // Allow members to delete messages
  "members_can_video_call": true,  // Allow members to start video calls
  "members_can_voice_call": true   // Allow members to start voice calls
}
```

---

## Error Codes

- `400` - Bad Request (missing parameters, invalid data)
- `401` - Unauthorized (user not authenticated)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found (group or user not found)
- `500` - Internal Server Error

---

## Testing

You can test these APIs using the WebSocket test client or from the Android app once integrated.

Example test script:
```javascript
const io = require('socket.io-client');
const socket = io('https://worldmates.club', {
  query: { access_token: 'your_token_here' }
});

socket.on('connect', () => {
  socket.emit('create_group', {
    from_id: 'your_hash',
    group_name: 'Test Group',
    description: 'Testing new API'
  }, (response) => {
    console.log('Group created:', response);
  });
});
```
