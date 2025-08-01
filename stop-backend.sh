#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# PID file to track the running process
PID_FILE="backend.pid"

echo -e "${YELLOW}Stopping AiDB Backend...${NC}"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo -e "${RED}No PID file found. Backend might not be running.${NC}"
    exit 1
fi

# Read PID from file
PID=$(cat "$PID_FILE")

# Check if process is still running
if ! ps -p $PID > /dev/null 2>&1; then
    echo -e "${YELLOW}Process $PID is not running. Removing stale PID file.${NC}"
    rm -f "$PID_FILE"
    exit 0
fi

echo -e "${YELLOW}Stopping process $PID...${NC}"

# Try graceful shutdown first
kill $PID

# Wait for graceful shutdown (up to 10 seconds)
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo -e "${GREEN}Backend stopped gracefully!${NC}"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Force kill if still running
echo -e "${YELLOW}Force killing process $PID...${NC}"
kill -9 $PID

# Wait a moment and check
sleep 2
if ! ps -p $PID > /dev/null 2>&1; then
    echo -e "${GREEN}Backend stopped successfully!${NC}"
    rm -f "$PID_FILE"
else
    echo -e "${RED}Failed to stop backend process $PID${NC}"
    exit 1
fi 