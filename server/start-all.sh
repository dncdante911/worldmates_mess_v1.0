#!/bin/bash

# 🚀 WorldMates Complete Start Script
# Запускає TURN сервер та Node.js backend

set -e  # Exit on error

echo "🚀 ========================================"
echo "📱 Starting WorldMates Server Components"
echo "🚀 ========================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==================== CHECK DEPENDENCIES ====================

echo "🔍 Checking dependencies..."

# Check if coturn is installed
if ! command -v turnserver &> /dev/null; then
    echo -e "${RED}❌ coturn is not installed${NC}"
    echo "Install it with: sudo apt-get install coturn"
    exit 1
fi

# Check if node is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed${NC}"
    echo "Install it with: sudo apt-get install nodejs npm"
    exit 1
fi

echo -e "${GREEN}✅ All dependencies installed${NC}"
echo ""

# ==================== CHECK CONFIGURATION ====================

echo "🔧 Checking configuration..."

# Check if turnserver.conf exists
if [ ! -f "/etc/turnserver.conf" ]; then
    echo -e "${YELLOW}⚠️  /etc/turnserver.conf not found${NC}"
    echo "Copying default config..."
    sudo cp turnserver.conf /etc/turnserver.conf
    echo -e "${YELLOW}⚠️  Please edit /etc/turnserver.conf and set your public IP${NC}"
    echo "Run: sudo nano /etc/turnserver.conf"
    echo "Find: external-ip=YOUR_PUBLIC_IP_HERE"
    echo "Replace with your actual IP (get it with: curl ifconfig.me)"
    exit 1
fi

# Check if external IP is configured
if grep -q "YOUR_PUBLIC_IP_HERE" /etc/turnserver.conf; then
    echo -e "${RED}❌ External IP not configured in /etc/turnserver.conf${NC}"
    echo "Please run: sudo nano /etc/turnserver.conf"
    echo "And set your public IP (get it with: curl ifconfig.me)"
    exit 1
fi

echo -e "${GREEN}✅ Configuration looks good${NC}"
echo ""

# ==================== START TURN SERVER ====================

echo "📞 Starting TURN server..."

# Check if coturn is already running
if sudo systemctl is-active --quiet coturn; then
    echo -e "${YELLOW}⚠️  TURN server already running${NC}"
else
    sudo systemctl start coturn
    sleep 2

    if sudo systemctl is-active --quiet coturn; then
        echo -e "${GREEN}✅ TURN server started${NC}"
    else
        echo -e "${RED}❌ Failed to start TURN server${NC}"
        echo "Check logs with: sudo journalctl -u coturn -n 50"
        exit 1
    fi
fi

echo ""

# ==================== INSTALL NPM DEPENDENCIES ====================

echo "📦 Installing Node.js dependencies..."

if [ ! -d "node_modules" ]; then
    npm install
    echo -e "${GREEN}✅ Dependencies installed${NC}"
else
    echo -e "${YELLOW}⚠️  Dependencies already installed (skipping)${NC}"
fi

echo ""

# ==================== START NODE.JS SERVER ====================

echo "🌐 Starting Node.js server..."

# Kill existing node process on port 449 if exists
EXISTING_PID=$(lsof -ti:449)
if [ ! -z "$EXISTING_PID" ]; then
    echo -e "${YELLOW}⚠️  Killing existing process on port 449 (PID: $EXISTING_PID)${NC}"
    kill -9 $EXISTING_PID || true
    sleep 1
fi

# Start Node.js server in background
nohup node server-example.js > server.log 2>&1 &
NODE_PID=$!

sleep 2

# Check if server started successfully
if ps -p $NODE_PID > /dev/null; then
    echo -e "${GREEN}✅ Node.js server started (PID: $NODE_PID)${NC}"
else
    echo -e "${RED}❌ Failed to start Node.js server${NC}"
    echo "Check logs with: cat server.log"
    exit 1
fi

echo ""

# ==================== DISPLAY STATUS ====================

echo "🚀 ========================================"
echo -e "${GREEN}✅ All services started successfully!${NC}"
echo "🚀 ========================================"
echo ""

echo "📊 Status:"
echo "  📞 TURN Server: $(sudo systemctl is-active coturn)"
echo "  🌐 Node.js Server: Running (PID: $NODE_PID)"
echo ""

echo "🔗 Endpoints:"
echo "  Socket.IO: ws://0.0.0.0:449"
echo "  Health Check: http://localhost:449/api/health"
echo "  ICE Servers: http://localhost:449/api/ice-servers/{userId}"
echo ""

echo "📝 Useful commands:"
echo "  View Node.js logs: tail -f server.log"
echo "  View TURN logs: sudo tail -f /var/log/turnserver.log"
echo "  Stop TURN: sudo systemctl stop coturn"
echo "  Stop Node.js: kill $NODE_PID"
echo ""

echo "🧪 Test TURN server:"
echo "  node generate-turn-credentials.js"
echo "  Or visit: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/"
echo ""

echo "🚀 ========================================"
echo -e "${GREEN}Happy calling! 📞📹${NC}"
echo "🚀 ========================================"
