/**
 * 🚀 WorldMates Server - Main Entry Point
 *
 * Приклад інтеграції Socket.IO обробників для відеодзвінків
 */

const express = require('express');
const http = require('http');
const socketIO = require('socket.io');
const cors = require('cors');

// Імпорт обробників
const { initializeCallsHandler, getActiveCallsStats } = require('./callsSocketHandler');
const { getIceServers, generateTurnCredentials } = require('./generate-turn-credentials');
const { initializeBotHandler, getBotStats } = require('./botSocketHandler');

// ==================== EXPRESS SETUP ====================

const app = express();
const server = http.createServer(app);

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ==================== SOCKET.IO SETUP ====================

const io = socketIO(server, {
    cors: {
        origin: "*", // ⚠️ В продакшні обмежити до worldmates.club
        methods: ["GET", "POST"],
        credentials: true
    },
    transports: ['websocket', 'polling'],
    pingTimeout: 60000,
    pingInterval: 25000
});

// Ініціалізувати обробники відеодзвінків
initializeCallsHandler(io);

// Ініціалізувати обробники Bot API
initializeBotHandler(io);

// Логування підключень
io.on('connection', (socket) => {
    console.log(`✅ Client connected: ${socket.id}`);

    socket.on('disconnect', (reason) => {
        console.log(`❌ Client disconnected: ${socket.id}, reason: ${reason}`);
    });
});

// ==================== REST API ENDPOINTS ====================

/**
 * GET /api/health
 * Health check endpoint
 */
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
        memory: process.memoryUsage()
    });
});

/**
 * GET /api/ice-servers/:userId
 * Отримати ICE server configuration з динамічними TURN credentials
 */
app.get('/api/ice-servers/:userId', (req, res) => {
    try {
        const userId = req.params.userId;

        if (!userId) {
            return res.status(400).json({ error: 'userId is required' });
        }

        const iceServers = getIceServers(userId);

        res.json({
            success: true,
            iceServers,
            timestamp: Date.now()
        });
    } catch (error) {
        console.error('Error generating ICE servers:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to generate ICE servers'
        });
    }
});

/**
 * POST /api/turn-credentials
 * Згенерувати TURN credentials (альтернативний метод)
 */
app.post('/api/turn-credentials', (req, res) => {
    try {
        const { userId, ttl } = req.body;

        if (!userId) {
            return res.status(400).json({ error: 'userId is required' });
        }

        const credentials = generateTurnCredentials(userId, ttl || 86400);

        res.json({
            success: true,
            credentials,
            turnServers: [
                {
                    urls: [
                        'turn:worldmates.club:3478?transport=udp',
                        'turn:worldmates.club:3478?transport=tcp',
                        'turns:worldmates.club:5349?transport=tcp'
                    ],
                    username: credentials.username,
                    credential: credentials.password
                }
            ]
        });
    } catch (error) {
        console.error('Error generating TURN credentials:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to generate credentials'
        });
    }
});

/**
 * GET /api/admin/calls/stats
 * Статистика активних дзвінків (тільки для адміністраторів)
 */
app.get('/api/admin/calls/stats', (req, res) => {
    try {
        // TODO: Додати аутентифікацію адміністратора
        const stats = getActiveCallsStats();

        res.json({
            success: true,
            stats,
            timestamp: Date.now()
        });
    } catch (error) {
        console.error('Error fetching call stats:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to fetch stats'
        });
    }
});

/**
 * GET /api/bots/stats
 * Bot API статистика (кількість підключених ботів, підписок)
 */
app.get('/api/bots/stats', (req, res) => {
    try {
        const stats = getBotStats();
        res.json({
            success: true,
            stats,
            timestamp: Date.now()
        });
    } catch (error) {
        console.error('Error fetching bot stats:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to fetch bot stats'
        });
    }
});

/**
 * GET /api/calls/active-count
 * Кількість активних дзвінків (публічний endpoint)
 */
app.get('/api/calls/active-count', (req, res) => {
    try {
        const stats = getActiveCallsStats();

        res.json({
            success: true,
            activeCallsCount: stats.totalCalls,
            timestamp: Date.now()
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: 'Failed to fetch active calls count'
        });
    }
});

// 404 Handler
app.use((req, res) => {
    res.status(404).json({
        error: 'Not Found',
        path: req.path
    });
});

// Error Handler
app.use((err, req, res, next) => {
    console.error('Server Error:', err);
    res.status(500).json({
        error: 'Internal Server Error',
        message: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
});

// ==================== START SERVER ====================

const PORT = process.env.PORT || 449;
const HOST = process.env.HOST || '0.0.0.0';

server.listen(PORT, HOST, () => {
    console.log('');
    console.log('🚀 ========================================');
    console.log(`📱 WorldMates Server STARTED`);
    console.log('🚀 ========================================');
    console.log(`📡 Socket.IO Server: ws://${HOST}:${PORT}`);
    console.log(`🌐 HTTP Server: http://${HOST}:${PORT}`);
    console.log(`📞 WebRTC Calls: ENABLED`);
    console.log(`🔐 TURN Server: worldmates.club:3478`);
    console.log('');
    console.log('📋 Available Endpoints:');
    console.log(`   GET  /api/health`);
    console.log(`   GET  /api/ice-servers/:userId`);
    console.log(`   POST /api/turn-credentials`);
    console.log(`   GET  /api/admin/calls/stats`);
    console.log(`   GET  /api/calls/active-count`);
    console.log(`   GET  /api/bots/stats`);
    console.log(`   WS   /bots (Bot API namespace)`);
    console.log('');
    console.log(`🕐 Started at: ${new Date().toISOString()}`);
    console.log('🚀 ========================================');
    console.log('');
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('SIGTERM signal received: closing HTTP server');
    server.close(() => {
        console.log('HTTP server closed');
        process.exit(0);
    });
});

process.on('SIGINT', () => {
    console.log('\nSIGINT signal received: closing HTTP server');
    server.close(() => {
        console.log('HTTP server closed');
        process.exit(0);
    });
});

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
    console.error('Uncaught Exception:', error);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

module.exports = { app, server, io };
