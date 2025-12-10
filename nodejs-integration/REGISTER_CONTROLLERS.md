# Інструкція: Реєстрація нових контролерів

## Контролери додано:
- CreateGroupController.js
- UpdateGroupController.js
- DeleteGroupController.js
- AddGroupMemberController.js
- RemoveGroupMemberController.js
- SetGroupRoleController.js
- LeaveGroupController.js
- GetGroupDetailsController.js

## Де зареєструвати:

У вашому файлі `listeners.js` (на сервері) додайте:

```javascript
// Імпорти на початку файлу
const { CreateGroupController } = require('../controllers/CreateGroupController');
const { UpdateGroupController } = require('../controllers/UpdateGroupController');
const { DeleteGroupController } = require('../controllers/DeleteGroupController');
const { AddGroupMemberController } = require('../controllers/AddGroupMemberController');
const { RemoveGroupMemberController } = require('../controllers/RemoveGroupMemberController');
const { SetGroupRoleController } = require('../controllers/SetGroupRoleController');
const { LeaveGroupController } = require('../controllers/LeaveGroupController');
const { GetGroupDetailsController } = require('../controllers/GetGroupDetailsController');

// У функції registerListeners додайте events (перед 'disconnect'):
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
```

## Модулі оновлено:
- nodejs-models/wo_groupchat.js
- nodejs-models/wo_groupchatusers.js

Скопіюйте їх на сервер у папку `models/`.

## База даних:
Виконайте SQL міграцію:
```bash
mysql -u social -p socialhub < extend-group-chat-tables.sql
```
