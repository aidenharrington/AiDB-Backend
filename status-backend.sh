#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# PID file to track the running process
PID_FILE="backend.pid"

echo -e "${BLUE}=== AiDB Backend Status ===${NC}"

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo -e "${RED}Status: Not running${NC}"
    echo -e "${YELLOW}No PID file found. Backend is not running.${NC}"
    exit 0
fi

# Read PID from file
PID=$(cat "$PID_FILE")

# Check if process is running
if ps -p $PID > /dev/null 2>&1; then
    echo -e "${GREEN}Status: Running${NC}"
    echo -e "${GREEN}Process ID: $PID${NC}"
    
    # Get process details
    PROCESS_INFO=$(ps -p $PID -o pid,ppid,cmd --no-headers 2>/dev/null)
    if [ ! -z "$PROCESS_INFO" ]; then
        echo -e "${BLUE}Process Info: $PROCESS_INFO${NC}"
    fi
    
    # Check if log file exists
    if [ -f "backend.log" ]; then
        echo -e "${BLUE}Log file: backend.log${NC}"
        echo -e "${YELLOW}Last few log lines:${NC}"
        tail -5 backend.log 2>/dev/null || echo "No recent logs"
    fi
    
    echo ""
    echo -e "${YELLOW}To stop the backend: ./stop-backend.sh${NC}"
    echo -e "${YELLOW}To view logs: tail -f backend.log${NC}"
else
    echo -e "${RED}Status: Not running${NC}"
    echo -e "${YELLOW}Process $PID is not running. Removing stale PID file.${NC}"
    rm -f "$PID_FILE"
fi 