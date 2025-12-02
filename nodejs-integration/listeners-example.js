/**
 * ПРИМЕР файла listeners/listeners.js
 * Показывает, как интегрировать calls-listener в существующую структуру
 *
 * ИНСТРУКЦИЯ:
 * 1. Если у вас уже есть listeners/listeners.js, добавьте только импорт и вызов registerCallsListeners
 * 2. Если файла нет, создайте его по этому образцу
 */

const registerCallsListeners = require('./calls-listener');

/**
 * Регистрация всех обработчиков событий Socket.IO
 * @param {Object} socket - Socket.IO socket
 * @param {Object} io - Socket.IO server instance
 * @param {Object} ctx - Контекст приложения
 */
async function registerListeners(socket, io, ctx) {

    // ==========================================
    // ВАШИ СУЩЕСТВУЮЩИЕ LISTENERS
    // ==========================================

    // Пример: обработчик сообщений
    socket.on('message', async (data) => {
        // ваш код обработки сообщений
        console.log('Message received:', data);
    });

    // Пример: обработчик typing
    socket.on('typing', (data) => {
        // ваш код
        socket.broadcast.emit('user_typing', data);
    });

    // Пример: user connected
    socket.on('user:connected', async (data) => {
        const userId = data.userId || data.user_id;

        // Добавить в существующие структуры
        ctx.socketIdUserHash[socket.id] = userId;
        ctx.userHashUserId[userId] = userId;

        if (!ctx.userIdSocket[userId]) {
            ctx.userIdSocket[userId] = [];
        }
        ctx.userIdSocket[userId].push(socket);

        console.log(`User ${userId} connected`);
    });

    // Пример: disconnect
    socket.on('disconnect', () => {
        const userId = ctx.socketIdUserHash[socket.id];

        if (userId && ctx.userIdSocket[userId]) {
            // Удалить сокет из массива
            const index = ctx.userIdSocket[userId].indexOf(socket);
            if (index > -1) {
                ctx.userIdSocket[userId].splice(index, 1);
            }

            // Если больше нет сокетов для этого пользователя
            if (ctx.userIdSocket[userId].length === 0) {
                delete ctx.userIdSocket[userId];
                delete ctx.userHashUserId[userId];
            }
        }

        delete ctx.socketIdUserHash[socket.id];

        console.log(`Socket disconnected: ${socket.id}`);
    });

    // ==========================================
    // НОВЫЙ LISTENER ДЛЯ ЗВОНКОВ
    // ==========================================

    await registerCallsListeners(socket, io, ctx);

    console.log(`All listeners registered for socket ${socket.id}`);
}

module.exports = { registerListeners };
