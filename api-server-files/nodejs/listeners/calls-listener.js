/**
 * WebRTC Calls Listener для WorldMates Messenger
 * Интегрируется в существующий Node.js проект
 *
 * Использует:
 * - Sequelize модели из ctx
 * - Существующие структуры ctx (userIdSocket, socketIdUserHash и т.д.)
 * - Паттерн registerListeners(socket, io, ctx)
 */

// Импорт TURN credentials helper
const turnHelper = require('../helpers/turn-credentials');

/**
 * Регистрация обработчиков звонков
 * @param {Object} socket - Socket.IO socket объект
 * @param {Object} io - Socket.IO server instance
 * @param {Object} ctx - Контекст приложения с моделями и состоянием
 */
async function registerCallsListeners(socket, io, ctx) {

    // Хранилище активных звонков (можно добавить в ctx если нужно)
    if (!ctx.activeCalls) {
        ctx.activeCalls = new Map(); // roomName -> { initiator, recipient, callType }
    }

    /**
     * Регистрация пользователя для звонков
     * Data: { userId }
     */
    socket.on('call:register', (data) => {
        const userId = data.userId || data.user_id;
        console.log(`[CALLS] 📝 User registered for calls: ${userId}, socket: ${socket.id}`);

        // Добавить в существующую структуру если нужно
        if (!ctx.userIdSocket[userId]) {
            ctx.userIdSocket[userId] = [];
        }
        if (!ctx.userIdSocket[userId].includes(socket)) {
            ctx.userIdSocket[userId].push(socket);
            console.log(`[CALLS] ✅ Added socket to user ${userId}, total sockets: ${ctx.userIdSocket[userId].length}`);
        } else {
            console.log(`[CALLS] ⚠️ Socket already registered for user ${userId}`);
        }
    });

    /**
     * Инициация звонка (1-на-1 или групповой)
     * Data: { fromId, toId?, groupId?, callType, roomName, sdpOffer }
     */
    socket.on('call:initiate', async (data) => {
        try {
            console.log('[CALLS] 📞 call:initiate received, raw data:', JSON.stringify(data).substring(0, 200));

            const { fromId, toId, groupId, callType, roomName, sdpOffer } = data;

            console.log(`[CALLS] Call initiated: ${fromId} -> ${toId || groupId} (${callType})`);

            if (toId) {
                // ===== 1-на-1 звонок =====

                // Сохранить в БД
                await ctx.wo_calls.create({
                    from_id: fromId,
                    to_id: toId,
                    call_type: callType,
                    status: 'ringing',
                    room_name: roomName,
                    sdp_offer: sdpOffer,
                    created_at: new Date()
                });

                // Получить данные инициатора
                const initiator = await ctx.wo_users.findOne({
                    where: { user_id: fromId },
                    attributes: ['user_id', 'first_name', 'last_name', 'avatar'],
                    raw: true
                });

                // Найти сокеты получателя
                const recipientSockets = ctx.userIdSocket[toId];
                console.log(`[CALLS] 🔍 Looking for recipient ${toId}, found: ${recipientSockets ? recipientSockets.length : 0} sockets`);

                if (recipientSockets && recipientSockets.length > 0) {
                    // Получить ICE servers с TURN credentials для получателя
                    const iceServers = turnHelper.getIceServers(toId);

                    // Отправить уведомление о входящем звонке на все устройства
                    const callData = {
                        fromId: fromId,
                        fromName: initiator ? `${initiator.first_name} ${initiator.last_name}` : 'Unknown',
                        fromAvatar: initiator ? initiator.avatar : '',
                        callType: callType,
                        roomName: roomName,
                        sdpOffer: sdpOffer,
                        iceServers: iceServers  // ✅ Добавлены TURN credentials
                    };

                    recipientSockets.forEach(recipientSocket => {
                        recipientSocket.emit('call:incoming', callData);
                    });

                    console.log(`[CALLS] Incoming call sent to user ${toId} with TURN credentials (${recipientSockets.length} devices)`);

                } else {
                    // Получатель оффлайн
                    await ctx.wo_calls.update(
                        { status: 'missed' },
                        { where: { room_name: roomName } }
                    );

                    socket.emit('call:error', {
                        message: 'Recipient is offline',
                        status: 'missed'
                    });

                    console.log(`[CALLS] Recipient ${toId} is offline`);
                }

            } else if (groupId) {
                // ===== Групповой звонок =====

                // Сохранить в БД
                await ctx.wo_group_calls.create({
                    group_id: groupId,
                    initiated_by: fromId,
                    call_type: callType,
                    status: 'ringing',
                    room_name: roomName,
                    created_at: new Date()
                });

                // Получить участников группы
                const members = await ctx.wo_groupchatusers.findAll({
                    where: {
                        group_id: groupId,
                        active: 1
                    },
                    attributes: ['user_id'],
                    raw: true
                });

                // Получить данные инициатора
                const initiator = await ctx.wo_users.findOne({
                    where: { user_id: fromId },
                    attributes: ['first_name', 'last_name', 'avatar'],
                    raw: true
                });

                const initiatorName = initiator ?
                    `${initiator.first_name} ${initiator.last_name}` : 'Unknown';

                // Отправить всем участникам кроме инициатора
                members.forEach(member => {
                    if (member.user_id !== fromId) {
                        const memberSockets = ctx.userIdSocket[member.user_id];
                        if (memberSockets && memberSockets.length > 0) {
                            // Получить ICE servers для каждого участника
                            const iceServers = turnHelper.getIceServers(member.user_id);

                            const callData = {
                                groupId: groupId,
                                initiatedBy: fromId,
                                initiatorName: initiatorName,
                                callType: callType,
                                roomName: roomName,
                                sdpOffer: sdpOffer,
                                iceServers: iceServers  // ✅ Добавлены TURN credentials
                            };

                            memberSockets.forEach(memberSocket => {
                                memberSocket.emit('group_call:incoming', callData);
                            });
                        }
                    }
                });

                console.log(`[CALLS] Group call initiated for group ${groupId} with TURN credentials`);
            }

        } catch (error) {
            console.error('[CALLS] Error in call:initiate:', error);
            socket.emit('call:error', {
                message: 'Failed to initiate call',
                error: error.message
            });
        }
    });

    /**
     * Принять звонок
     * Data: { roomName, userId, sdpAnswer }
     */
    socket.on('call:accept', async (data) => {
        try {
            const { roomName, userId, sdpAnswer } = data;

            console.log(`[CALLS] Call accepted: ${roomName} by user ${userId}`);

            // Обновить статус звонка
            await ctx.wo_calls.update(
                {
                    status: 'connected',
                    accepted_at: new Date(),
                    sdp_answer: sdpAnswer
                },
                { where: { room_name: roomName } }
            );

            // Получить инициатора звонка
            const callInfo = await ctx.wo_calls.findOne({
                where: { room_name: roomName },
                attributes: ['from_id', 'to_id'],
                raw: true
            });

            if (callInfo) {
                const initiatorId = callInfo.from_id;
                const initiatorSockets = ctx.userIdSocket[initiatorId];

                if (initiatorSockets && initiatorSockets.length > 0) {
                    // Получить ICE servers с TURN credentials для инициатора
                    const iceServers = turnHelper.getIceServers(initiatorId);

                    // Отправить SDP answer инициатору
                    const answerData = {
                        roomName: roomName,
                        sdpAnswer: sdpAnswer,
                        acceptedBy: userId,
                        iceServers: iceServers  // ✅ Добавлены TURN credentials
                    };

                    initiatorSockets.forEach(initiatorSocket => {
                        initiatorSocket.emit('call:answer', answerData);
                    });

                    console.log(`[CALLS] Answer sent to initiator ${initiatorId} with TURN credentials`);
                }
            }

        } catch (error) {
            console.error('[CALLS] Error in call:accept:', error);
            socket.emit('call:error', { message: 'Failed to accept call' });
        }
    });

    /**
     * Обмен ICE candidates
     * Data: { roomName, toUserId, candidate, sdpMLineIndex, sdpMid }
     */
    socket.on('ice:candidate', async (data) => {
        try {
            const { roomName, toUserId, fromUserId, candidate, sdpMLineIndex, sdpMid } = data;

            // Опционально: сохранить в БД для восстановления (может не работать если нет таблицы)
            if (ctx.wo_ice_candidates) {
                try {
                    await ctx.wo_ice_candidates.create({
                        room_name: roomName,
                        candidate: JSON.stringify(candidate),
                        sdp_mid: sdpMid,
                        sdp_m_line_index: sdpMLineIndex,
                        created_at: new Date()
                    });
                } catch (dbError) {
                    // ✅ Если таблица не существует или нет нужных колонок - игнорируем
                    console.warn('[CALLS] Could not save ICE candidate to DB (not critical):', dbError.message);
                }
            }

            if (toUserId) {
                // Отправить конкретному пользователю
                const recipientSockets = ctx.userIdSocket[toUserId];
                if (recipientSockets && recipientSockets.length > 0) {
                    const candidateData = {
                        roomName: roomName,
                        fromUserId: fromUserId,
                        candidate: candidate,
                        sdpMLineIndex: sdpMLineIndex,
                        sdpMid: sdpMid
                    };

                    recipientSockets.forEach(recipientSocket => {
                        recipientSocket.emit('ice:candidate', candidateData);
                    });
                }
            } else {
                // Broadcast в комнату (для групповых звонков)
                socket.to(roomName).emit('ice:candidate', {
                    fromUserId: fromUserId,
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
     * Data: { roomName, userId, reason }
     */
    socket.on('call:end', async (data) => {
        try {
            const { roomName, userId, reason } = data;

            // ✅ Валідація: якщо немає roomName, пропустити
            if (!roomName) {
                console.warn('[CALLS] call:end received without roomName, ignoring');
                return;
            }

            console.log(`[CALLS] Call ended: ${roomName} by ${userId} (${reason})`);

            // Обновить в БД
            const call = await ctx.wo_calls.findOne({
                where: { room_name: roomName },
                raw: true
            });

            if (call) {
                // Вычислить длительность если звонок был принят
                let duration = null;
                if (call.accepted_at) {
                    duration = Math.floor((new Date() - new Date(call.accepted_at)) / 1000);
                }

                await ctx.wo_calls.update(
                    {
                        status: 'ended',
                        ended_at: new Date(),
                        duration: duration
                    },
                    { where: { room_name: roomName } }
                );

                // Оповестить обоих участников
                const participants = [call.from_id, call.to_id];
                participants.forEach(participantId => {
                    if (participantId !== userId) {
                        const participantSockets = ctx.userIdSocket[participantId];
                        if (participantSockets && participantSockets.length > 0) {
                            participantSockets.forEach(participantSocket => {
                                participantSocket.emit('call:ended', {
                                    roomName: roomName,
                                    reason: reason,
                                    endedBy: userId
                                });
                            });
                        }
                    }
                });
            }

            // Удалить из активных звонков
            ctx.activeCalls.delete(roomName);

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

            // ✅ Валідація
            if (!roomName) {
                console.warn('[CALLS] call:reject received without roomName, ignoring');
                return;
            }

            console.log(`[CALLS] Call rejected: ${roomName} by ${userId}`);

            await ctx.wo_calls.update(
                { status: 'rejected' },
                { where: { room_name: roomName } }
            );

            // Получить информацию о звонке
            const call = await ctx.wo_calls.findOne({
                where: { room_name: roomName },
                raw: true
            });

            if (call) {
                // Уведомить инициатора
                const initiatorSockets = ctx.userIdSocket[call.from_id];
                if (initiatorSockets && initiatorSockets.length > 0) {
                    initiatorSockets.forEach(initiatorSocket => {
                        initiatorSocket.emit('call:rejected', {
                            roomName: roomName,
                            rejectedBy: userId
                        });
                    });
                }
            }

            ctx.activeCalls.delete(roomName);

        } catch (error) {
            console.error('[CALLS] Error in call:reject:', error);
        }
    });

    /**
     * Присоединиться к комнате (для Socket.IO rooms)
     * Data: { roomName, userId }
     */
    socket.on('call:join_room', (data) => {
        const { roomName, userId } = data;
        socket.join(roomName);
        console.log(`[CALLS] User ${userId} joined room: ${roomName}`);

        // Уведомить других в комнате
        socket.to(roomName).emit('user:joined_call', {
            userId: userId,
            roomName: roomName
        });
    });

    /**
     * Покинуть комнату
     * Data: { roomName, userId }
     */
    socket.on('call:leave_room', (data) => {
        const { roomName, userId } = data;
        socket.leave(roomName);
        console.log(`[CALLS] User ${userId} left room: ${roomName}`);

        // Уведомить других в комнате
        socket.to(roomName).emit('user:left_call', {
            userId: userId,
            roomName: roomName
        });
    });

    /**
     * Переключение аудио/видео (для групповых звонков)
     * Data: { roomName, userId, audio, video }
     */
    socket.on('call:toggle_media', (data) => {
        const { roomName, userId, audio, video } = data;

        // Broadcast другим участникам
        socket.to(roomName).emit('user:media_changed', {
            userId: userId,
            audio: audio,
            video: video
        });

        console.log(`[CALLS] User ${userId} toggled media: audio=${audio}, video=${video}`);
    });

    console.log(`[CALLS] Call listeners registered for socket ${socket.id}`);
}

module.exports = registerCallsListeners;
