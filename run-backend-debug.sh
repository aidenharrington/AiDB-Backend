#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# PID file to track the running process
PID_FILE="backend.pid"

echo -e "${GREEN}Starting AiDB Backend in DEBUG mode...${NC}"

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

# Debug port configuration
DEBUG_PORT=5005

echo -e "${BLUE}Debug mode enabled on port: $DEBUG_PORT${NC}"
echo -e "${BLUE}To connect your IDE debugger:${NC}"
echo -e "${BLUE}  - Host: localhost${NC}"
echo -e "${BLUE}  - Port: $DEBUG_PORT${NC}"
echo -e "${BLUE}  - Transport: socket${NC}"
echo -e "${YELLOW}========================================${NC}"

# Run the Spring Boot application in debug mode
echo -e "${GREEN}Starting Spring Boot application in debug mode...${NC}"
echo -e "${GREEN}Press Ctrl+C to stop the application${NC}"
echo -e "${YELLOW}========================================${NC}"

# Run in foreground with debug enabled
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT" 