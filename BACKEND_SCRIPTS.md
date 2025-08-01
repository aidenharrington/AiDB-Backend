# Backend Management Scripts

This directory contains scripts to easily manage the AiDB backend application.

## Available Scripts

### 1. `run-backend.sh` - Start the backend
```bash
./run-backend.sh
```
- Starts the Spring Boot application in the foreground
- Uses dev profile: `-Dspring.profiles.active=dev`
- Shows real-time console output
- Press Ctrl+C to stop the application
- Prevents multiple instances from running

### 1a. `run-backend-alt.sh` - Start the backend (alternative method)
```bash
./run-backend-alt.sh
```
- Alternative method using Maven exec plugin
- Uses dev profile: `-Dspring.profiles.active=dev`
- Shows real-time console output
- Press Ctrl+C to stop the application
- Use this if the main script has issues

### 2. `stop-backend.sh` - Stop the backend
```bash
./stop-backend.sh
```
- Gracefully stops the backend application
- Waits up to 10 seconds for graceful shutdown
- Force kills if necessary
- Removes the PID file when stopped

### 3. `status-backend.sh` - Check backend status
```bash
./status-backend.sh
```
- Shows if the backend is running or not
- Displays process information
- Shows recent log entries
- Provides helpful commands

## Usage Examples

### Starting the backend:
```bash
cd aidb-backend
./run-backend.sh
```

### Checking status:
```bash
./status-backend.sh
```

### Stopping the backend:
```bash
./stop-backend.sh
```

### Viewing logs:
```bash
tail -f backend.log
```

## Benefits over Cursor's Run and Debug

1. **Better process control** - You can easily stop/start the application with Ctrl+C
2. **Real-time console output** - See all logs and output directly in your terminal
3. **Same VM arguments** - Uses the same dev profile as your VS Code configuration
4. **Simple management** - No need to deal with Cursor's Run and Debug menu issues
5. **Graceful shutdown** - Proper cleanup when stopping the application

## Troubleshooting

If the scripts don't work:
1. Make sure they're executable: `chmod +x *.sh`
2. Check if you're in the correct directory (`aidb-backend`)
3. Ensure Maven wrapper (`mvnw`) is present
4. Check the `backend.log` file for any errors

## Manual Process Management

If you need to manually manage the process:

### Find the process:
```bash
ps aux | grep java
```

### Kill by PID:
```bash
kill <PID>
```

### Force kill:
```bash
kill -9 <PID>
``` 