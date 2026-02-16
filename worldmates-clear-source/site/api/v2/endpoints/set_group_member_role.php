<?php
// +------------------------------------------------------------------------+
// | WorldMates - Set Group Member Role API
// | Endpoint: /api/v2/endpoints/set_group_member_role.php
// +------------------------------------------------------------------------+

$response_data = array(
    'api_status' => 400
);

// Validate required parameters
if (empty($_POST['group_id']) || !is_numeric($_POST['group_id']) || $_POST['group_id'] < 1) {
    $error_code = 1;
    $error_message = 'group_id must be numeric and greater than 0';
}
elseif (empty($_POST['user_id']) || !is_numeric($_POST['user_id']) || $_POST['user_id'] < 1) {
    $error_code = 2;
    $error_message = 'user_id must be numeric and greater than 0';
}
elseif (empty($_POST['role']) || !in_array($_POST['role'], array('admin', 'moderator', 'member', 'user'))) {
    $error_code = 3;
    $error_message = 'role must be one of: admin, moderator, member, user';
}

if (empty($error_code)) {
    $group_id = Wo_Secure($_POST['group_id']);
    $target_user_id = Wo_Secure($_POST['user_id']);
    $new_role = Wo_Secure($_POST['role']);

    // Get group data
    $group_tab = Wo_GroupTabData($group_id);

    if (empty($group_tab)) {
        $error_code = 4;
        $error_message = 'Group not found';
    }
    // Check if current user is the group owner
    elseif ($group_tab['user_id'] != $wo['user']['id']) {
        // Allow admins to change moderator/member roles, but not other admin roles
        $current_user_role = Wo_GetGroupUserRole($group_id, $wo['user']['id']);

        if ($current_user_role != 'admin' && $current_user_role != 'owner') {
            $error_code = 5;
            $error_message = 'Only group owner or admins can change member roles';
        }
        elseif ($current_user_role == 'admin' && $new_role == 'admin') {
            $error_code = 6;
            $error_message = 'Only group owner can set admin role';
        }
    }

    // Check if target user is a member
    if (empty($error_code)) {
        if (!Wo_IsGChatMemeberExists($group_id, $target_user_id) && $group_tab['user_id'] != $target_user_id) {
            $error_code = 7;
            $error_message = 'User is not a member of this group';
        }
    }

    // Update role
    if (empty($error_code)) {
        // Check if role column exists, if not add it dynamically (migration)
        try {
            // Try to update the role
            $db->where('user_id', $target_user_id)
               ->where('group_id', $group_id)
               ->update(T_GROUP_CHAT_USERS, array('role' => $new_role));

            $response_data = array(
                'api_status' => 200,
                'message' => 'Role updated successfully',
                'user_id' => (int)$target_user_id,
                'role' => $new_role
            );
        } catch (Exception $e) {
            // Column might not exist - try to add it
            try {
                @mysqli_query($sqlConnect, "ALTER TABLE " . T_GROUP_CHAT_USERS . " ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'member'");

                // Retry update
                $db->where('user_id', $target_user_id)
                   ->where('group_id', $group_id)
                   ->update(T_GROUP_CHAT_USERS, array('role' => $new_role));

                $response_data = array(
                    'api_status' => 200,
                    'message' => 'Role updated successfully',
                    'user_id' => (int)$target_user_id,
                    'role' => $new_role
                );
            } catch (Exception $e2) {
                $error_code = 8;
                $error_message = 'Failed to update role: ' . $e2->getMessage();
            }
        }
    }
}

// Return error if any
if (!empty($error_code)) {
    $response_data = array(
        'api_status' => 400,
        'error_code' => $error_code,
        'error_message' => $error_message
    );
}

// Helper function to get user's role in group
function Wo_GetGroupUserRole($group_id, $user_id) {
    global $db, $sqlConnect;

    // Check if user is owner
    $group = Wo_GroupTabData($group_id);
    if ($group && $group['user_id'] == $user_id) {
        return 'owner';
    }

    // Get role from group_chat_users
    try {
        $member = $db->where('user_id', $user_id)
                     ->where('group_id', $group_id)
                     ->getOne(T_GROUP_CHAT_USERS);

        if ($member && isset($member->role)) {
            return $member->role;
        }
    } catch (Exception $e) {
        // Role column might not exist
    }

    return 'member';
}
