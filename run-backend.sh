#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# PID file to track the running process
PID_FILE="backend.pid"

echo -e "${GREEN}Starting AiDB Backend...${NC}"

# Check if backend is already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo -e "${YELLOW}Backend is already running with PID: $PID${NC}"
        echo -e "${YELLOW}To stop it, run: ./stop-backend.sh${NC}"
        exit 1
    else
        echo -e "${YELLOW}Removing stale PID file...${NC}"
        rm -f "$PID_FILE"
    fi
fi

# Run the Spring Boot application in the foreground
echo -e "${GREEN}Starting Spring Boot application...${NC}"
echo -e "${GREEN}Press Ctrl+C to stop the application${NC}"
echo -e "${YELLOW}========================================${NC}"

# Run in foreground with console output
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" 