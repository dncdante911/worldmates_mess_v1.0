/**
 * WebRTC Calls Listener для WorldMates Messenger
 * Обработчики Socket.IO событий для аудио/видео звонков
 *
 * Интеграция в существующий проект
 * Поместить в: /var/www/www-root/data/www/worldmates.club/nodejs/listeners/
 */

module.exports = function(io, db) {
    // Хранилище активных пользователей и звонков
    const users = new Map(); // userId -> { socket, status }
    const calls = new Map(); // roomName -> { initiator, recipient, callType }
    const groupCalls = new Map(); // roomName -> { groupId, participants }

    io.on('connection', (socket) => {
        console.log(`[CALLS] User connected: ${socket.id}`);

        /**
         * Регистрация пользователя для звонков
         */
        socket.on('call:register', (data) => {
            const userId = data.userId || data.user_id;
            users.set(userId, {
                socket: socket,
                status: 'online'
            });
            console.log(`[CALLS] User registered: ${userId}`);
        });

        /**
         * Инициация звонка (1-на-1)
         * Data: { fromId, toId, callType, roomName, sdpOffer }
         */
        socket.on('call:initiate', async (data) => {
            try {
                const { fromId, toId, groupId, callType, roomName, sdpOffer } = data;

                console.log(`[CALLS] Call initiated: ${fromId} -> ${toId || groupId}`);

                if (toId) {
                    // 1-на-1 звонок
                    await db.query(
                        `INSERT INTO wo_calls (from_id, to_id, call_type, status, room_name, created_at)
                         VALUES (?, ?, ?, 'ringing', ?, NOW())`,
                        [fromId, toId, callType, roomName]
                    );

                    // Найти получателя
                    const recipientUser = users.get(toId);
                    if (recipientUser) {
                        // Получить данные инициатора
                        const [initiator] = await db.query(
                            'SELECT user_id, first_name, last_name, avatar FROM wo_users WHERE user_id = ?',
                            [fromId]
                        );

                        if (initiator && initiator.length > 0) {
                            const user = initiator[0];
                            // Отправить уведомление о входящем звонке
                            recipientUser.socket.emit('call:incoming', {
                                fromId: fromId,
                                fromName: `${user.first_name} ${user.last_name}`,
                                fromAvatar: user.avatar,
                                callType: callType,
                                roomName: roomName,
                                sdpOffer: sdpOffer
                            });

                            console.log(`[CALLS] Incoming call sent to user ${toId}`);
                        }
                    } else {
                        // Получатель оффлайн
                        await db.query(
                            `UPDATE wo_calls SET status = 'missed' WHERE room_name = ?`,
                            [roomName]
                        );
                        console.log(`[CALLS] Recipient ${toId} is offline`);
                    }

                } else if (groupId) {
                    // Групповой звонок
                    await db.query(
                        `INSERT INTO wo_group_calls (group_id, initiated_by, call_type, status, room_name, created_at)
                         VALUES (?, ?, ?, 'ringing', ?, NOW())`,
                        [groupId, fromId, callType, roomName]
                    );

                    // Получить участников группы
                    const [members] = await db.query(
                        'SELECT user_id FROM wo_group_members WHERE group_id = ? AND active = 1',
                        [groupId]
                    );

                    const [initiator] = await db.query(
                        'SELECT first_name, last_name FROM wo_users WHERE user_id = ?',
                        [fromId]
                    );

                    const initiatorName = initiator[0] ?
                        `${initiator[0].first_name} ${initiator[0].last_name}` : 'Unknown';

                    // Отправить всем участникам кроме инициатора
                    members.forEach(member => {
                        if (member.user_id !== fromId) {
                            const memberUser = users.get(member.user_id);
                            if (memberUser) {
                                memberUser.socket.emit('group_call:incoming', {
                                    groupId: groupId,
                                    initiatedBy: fromId,
                                    initiatorName: initiatorName,
                                    callType: callType,
                                    roomName: roomName,
                                    sdpOffer: sdpOffer
                                });
                            }
                        }
                    });
                }

            } catch (error) {
                console.error('[CALLS] Error in call:initiate:', error);
                socket.emit('call:error', { message: 'Failed to initiate call' });
            }
        });

        /**
         * Принять звонок
         * Data: { roomName, fromId, sdpAnswer }
         */
        socket.on('call:accept', async (data) => {
            try {
                const { roomName, fromId, sdpAnswer } = data;

                console.log(`[CALLS] Call accepted in room ${roomName}`);

                // Обновить статус
                await db.query(
                    `UPDATE wo_calls SET status = 'connected', accepted_at = NOW() WHERE room_name = ?`,
                    [roomName]
                );

                // Получить инициатора
                const [callInfo] = await db.query(
                    'SELECT from_id FROM wo_calls WHERE room_name = ? LIMIT 1',
                    [roomName]
                );

                if (callInfo && callInfo.length > 0) {
                    const initiatorId = callInfo[0].from_id;
                    const initiatorUser = users.get(initiatorId);

                    if (initiatorUser) {
                        // Отправить answer инициатору
                        initiatorUser.socket.emit('call:answer', {
                            roomName: roomName,
                            sdpAnswer: sdpAnswer
                        });
                    }
                }

            } catch (error) {
                console.error('[CALLS] Error in call:accept:', error);
            }
        });

        /**
         * Обмен ICE candidates
         * Data: { roomName, toUserId, candidate, sdpMLineIndex, sdpMid }
         */
        socket.on('ice:candidate', (data) => {
            try {
                const { roomName, toUserId, candidate, sdpMLineIndex, sdpMid } = data;

                if (toUserId) {
                    const recipientUser = users.get(toUserId);
                    if (recipientUser) {
                        recipientUser.socket.emit('ice:candidate', {
                            roomName: roomName,
                            candidate: candidate,
                            sdpMLineIndex: sdpMLineIndex,
                            sdpMid: sdpMid
                        });
                    }
                } else {
                    // Broadcast всем в комнате
                    socket.broadcast.to(roomName).emit('ice:candidate', {
                        candidate: candidate,
                        sdpMLineIndex: sdpMLineIndex,
                        sdpMid: sdpMid
                    });
                }

            } catch (error) {
                console.error('[CALLS] Error in ice:candidate:', error);
            }
        });

        /**
         * Завершить звонок
         * Data: { roomName, reason }
         */
        socket.on('call:end', async (data) => {
            try {
                const { roomName, reason } = data;

                console.log(`[CALLS] Call ended: ${roomName} (${reason})`);

                // Обновить в БД
                await db.query(
                    `UPDATE wo_calls SET status = 'ended', ended_at = NOW() WHERE room_name = ?`,
                    [roomName]
                );

                // Оповестить других
                io.to(roomName).emit('call:ended', {
                    roomName: roomName,
                    reason: reason
                });

                calls.delete(roomName);

            } catch (error) {
                console.error('[CALLS] Error in call:end:', error);
            }
        });

        /**
         * Отклонить звонок
         * Data: { roomName, userId }
         */
        socket.on('call:reject', async (data) => {
            try {
                const { roomName, userId } = data;

                console.log(`[CALLS] Call rejected: ${roomName}`);

                await db.query(
                    `UPDATE wo_calls SET status = 'rejected' WHERE room_name = ?`,
                    [roomName]
                );

                io.to(roomName).emit('call:rejected', {
                    roomName: roomName
                });

                calls.delete(roomName);

            } catch (error) {
                console.error('[CALLS] Error in call:reject:', error);
            }
        });

        /**
         * Присоединиться к комнате
         */
        socket.on('call:join_room', (data) => {
            const { roomName } = data;
            socket.join(roomName);
            console.log(`[CALLS] User joined room: ${roomName}`);
        });

        /**
         * Покинуть комнату
         */
        socket.on('call:leave_room', (data) => {
            const { roomName } = data;
            socket.leave(roomName);
            console.log(`[CALLS] User left room: ${roomName}`);
        });

        /**
         * Отключение
         */
        socket.on('disconnect', () => {
            // Удалить пользователя из активных
            for (const [userId, userData] of users.entries()) {
                if (userData.socket.id === socket.id) {
                    users.delete(userId);
                    console.log(`[CALLS] User ${userId} disconnected`);
                    break;
                }
            }
        });
    });

    console.log('[CALLS] WebRTC Calls listener initialized');
};
