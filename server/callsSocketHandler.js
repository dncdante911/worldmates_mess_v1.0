/**
 * 📞 WebRTC Video/Audio Calls Socket.IO Handler
 *
 * Обробляє всі події відеодзвінків через Socket.IO
 */

const { getIceServers } = require('./generate-turn-credentials');

// In-memory storage для активних дзвінків (в продакшні використовуйте Redis)
const activeCalls = new Map(); // roomName -> { participants: [], callType, startTime }

/**
 *Ініціалізувати Socket.IO обробники для дзвінків
 * @param {object} io - Socket.IO instance
 */
function initializeCallsHandler(io) {
    io.on('connection', (socket) => {
        console.log(`📱 User connected: ${socket.id}`);

        // User joins with their ID
        socket.on('user:join', (userId) => {
            socket.userId = userId;
            socket.join(`user_${userId}`);
            console.log(`✅ User ${userId} joined their room`);
        });

        // ========== ЛИЧНЫЙ ДЗВІНОК ==========

        /**
         * call:initiate - Ініціювати дзвінок до іншого користувача
         * Payload: { fromId, toId, callType, roomName, fromName, sdpOffer }
         */
        socket.on('call:initiate', async (data) => {
            console.log(`📞 Call initiated from ${data.fromId} to ${data.toId} (${data.callType})`);

            const { fromId, toId, callType, roomName, fromName, sdpOffer } = data;

            // Створити запис дзвінка
            activeCalls.set(roomName, {
                participants: [fromId, toId],
                callType,
                startTime: Date.now(),
                initiator: fromId
            });

            // Приєднати ініціатора до кімнати
            socket.join(roomName);

            // Згенерувати TURN credentials для отримувача
            const iceServers = getIceServers(toId.toString());

            // Надіслати вхідний дзвінок отримувачу
            io.to(`user_${toId}`).emit('call:incoming', {
                callId: generateCallId(),
                fromId,
                fromName,
                fromAvatar: data.fromAvatar || '',
                toId,
                callType,
                roomName,
                sdpOffer,
                iceServers // Передаємо ICE конфігурацію з TURN credentials
            });

            console.log(`✅ Incoming call sent to user ${toId}`);
        });

        /**
         * call:accept - Прийняти вхідний дзвінок
         * Payload: { roomName, fromId, sdpAnswer }
         */
        socket.on('call:accept', (data) => {
            console.log(`✅ Call accepted in room ${data.roomName}`);

            const { roomName, fromId, sdpAnswer } = data;

            // Приєднати отримувача до кімнати
            socket.join(roomName);

            // Згенерувати TURN credentials для ініціатора
            const iceServers = getIceServers(fromId.toString());

            // Надіслати answer ініціатору
            io.to(roomName).emit('call:answer', {
                sdpAnswer,
                iceServers // Передаємо ICE конфігурацію
            });

            console.log(`✅ Answer sent to room ${roomName}`);
        });

        /**
         * call:reject - Відхилити вхідний дзвінок
         * Payload: { roomName }
         */
        socket.on('call:reject', (data) => {
            console.log(`❌ Call rejected in room ${data.roomName}`);

            const { roomName } = data;

            // Повідомити ініціатора про відхилення
            io.to(roomName).emit('call:rejected', {
                reason: 'User rejected the call'
            });

            // Видалити дзвінок
            activeCalls.delete(roomName);
        });

        /**
         * ice:candidate - Обмін ICE candidates для NAT traversal
         * Payload: { roomName, candidate, sdpMLineIndex, sdpMid }
         */
        socket.on('ice:candidate', (data) => {
            const { roomName, candidate, sdpMLineIndex, sdpMid } = data;

            // Broadcast ICE candidate всім в кімнаті (крім відправника)
            socket.to(roomName).emit('ice:candidate', {
                candidate,
                sdpMLineIndex,
                sdpMid
            });

            // console.log(`🧊 ICE candidate sent to room ${roomName}`);
        });

        /**
         * call:end - Завершити дзвінок
         * Payload: { roomName, reason }
         */
        socket.on('call:end', (data) => {
            console.log(`🔚 Call ended in room ${data.roomName}`);

            const { roomName, reason } = data;

            // Повідомити всіх учасників
            io.to(roomName).emit('call:ended', {
                reason: reason || 'Call ended'
            });

            // Видалити учасників з кімнати
            io.in(roomName).socketsLeave(roomName);

            // Видалити дзвінок з активних
            activeCalls.delete(roomName);
        });

        // ========== ГРУПОВИЙ ДЗВІНОК ==========

        /**
         * group_call:initiate - Ініціювати груповий дзвінок
         * Payload: { groupId, initiatedBy, callType, roomName, sdpOffer, memberIds }
         */
        socket.on('group_call:initiate', async (data) => {
            console.log(`📞 Group call initiated by ${data.initiatedBy} in group ${data.groupId}`);

            const { groupId, initiatedBy, callType, roomName, sdpOffer, memberIds } = data;

            // Створити запис групового дзвінка
            activeCalls.set(roomName, {
                participants: [initiatedBy, ...memberIds],
                callType,
                groupId,
                startTime: Date.now(),
                initiator: initiatedBy
            });

            // Приєднати ініціатора до кімнати
            socket.join(roomName);

            // Надіслати вхідний дзвінок всім учасникам групи
            memberIds.forEach((memberId) => {
                const iceServers = getIceServers(memberId.toString());

                io.to(`user_${memberId}`).emit('call:incoming', {
                    callId: generateCallId(),
                    fromId: initiatedBy,
                    groupId,
                    callType,
                    roomName,
                    sdpOffer,
                    iceServers,
                    isGroupCall: true
                });
            });

            console.log(`✅ Group call notifications sent to ${memberIds.length} members`);
        });

        /**
         * call:toggle_audio - Вимкнути/увімкнути мікрофон
         * Payload: { roomName, userId, audioEnabled }
         */
        socket.on('call:toggle_audio', (data) => {
            const { roomName, userId, audioEnabled } = data;

            socket.to(roomName).emit('participant:audio_changed', {
                userId,
                audioEnabled
            });
        });

        /**
         * call:toggle_video - Вимкнути/увімкнути відео
         * Payload: { roomName, userId, videoEnabled }
         */
        socket.on('call:toggle_video', (data) => {
            const { roomName, userId, videoEnabled } = data;

            socket.to(roomName).emit('participant:video_changed', {
                userId,
                videoEnabled
            });
        });

        // ========== DISCONNECT ==========

        socket.on('disconnect', () => {
            console.log(`👋 User disconnected: ${socket.id}`);

            // Знайти всі кімнати користувача і завершити дзвінки
            const rooms = Array.from(socket.rooms);
            rooms.forEach((room) => {
                if (room.startsWith('room_')) {
                    io.to(room).emit('call:ended', {
                        reason: 'Participant disconnected'
                    });
                    activeCalls.delete(room);
                }
            });
        });
    });

    console.log('📞 Calls Socket.IO handlers initialized');
}

/**
 * Генерувати унікальний ID дзвінка
 */
function generateCallId() {
    return `call_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Отримати статистику активних дзвінків
 */
function getActiveCallsStats() {
    return {
        totalCalls: activeCalls.size,
        calls: Array.from(activeCalls.entries()).map(([roomName, data]) => ({
            roomName,
            ...data,
            duration: Math.floor((Date.now() - data.startTime) / 1000)
        }))
    };
}

module.exports = {
    initializeCallsHandler,
    getActiveCallsStats
};
