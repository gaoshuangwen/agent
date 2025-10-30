# LangGraph UI - Quick Start Guide

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Quick Start (5 minutes)

### 1. Build the Project

```bash
cd langgraph-ui
mvn clean install
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

The application will start on http://localhost:8080

### 3. Access the UI

Open your browser and navigate to:
```
http://localhost:8080
```

### 4. Try It Out

1. **Select a Graph**: Choose "Sample LangGraph" or "Complex LangGraph" from the dropdown
2. **Start Execution**: Click the "Start Execution" button
3. **Watch It Run**: 
   - Nodes will change color (gray → blue → green)
   - Timeline will show node execution progress
   - Logs will display events in real-time
4. **Interact**: 
   - Use Zoom In/Out/Fit buttons to navigate the graph
   - Click on executions in the list to view their details
   - Watch for approval requests (if any)

## Running Tests

```bash
mvn test
```

Expected output:
```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Using the REST API

### Start an Execution

```bash
curl -X POST http://localhost:8080/api/executions \
  -H "Content-Type: application/json" \
  -d '{
    "graphId": "sample-graph-1",
    "input": {"message": "Hello, LangGraph!"}
  }'
```

### Get Execution Status

```bash
# Replace {executionId} with the ID from the previous response
curl http://localhost:8080/api/executions/{executionId}
```

### List All Graphs

```bash
curl http://localhost:8080/api/graphs
```

### Get Pending Approvals

```bash
curl http://localhost:8080/api/approvals
```

## WebSocket Integration

Connect to WebSocket for real-time updates:

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    console.log('Connected to WebSocket');
    
    // Subscribe to all events
    stompClient.subscribe('/topic/events', function(message) {
        const event = JSON.parse(message.body);
        console.log('Event:', event);
    });
});
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in `application.yml`:

```yaml
server:
  port: 9090  # Or any other available port
```

### Build Failures

Make sure you have Java 17 or higher:

```bash
java -version
```

### Tests Failing

Run tests with verbose output:

```bash
mvn test -X
```

## What's Next?

- Explore the [README.md](README.md) for detailed documentation
- Check [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for architecture details
- Customize the sample graphs in `GraphTopologyService.java`
- Add your own execution logic by extending `GraphExecutionService.java`

## Example Workflow

Here's a complete example workflow:

```bash
# 1. Start the application
mvn spring-boot:run

# 2. In another terminal, start an execution
EXEC_ID=$(curl -s -X POST http://localhost:8080/api/executions \
  -H "Content-Type: application/json" \
  -d '{"graphId":"sample-graph-1","input":{}}' \
  | jq -r '.executionId')

echo "Execution ID: $EXEC_ID"

# 3. Watch the execution (run multiple times)
curl http://localhost:8080/api/executions/$EXEC_ID | jq '.status'

# 4. Get execution events
curl http://localhost:8080/api/telemetry/executions/$EXEC_ID/events | jq '.'

# 5. View in browser
echo "View in browser: http://localhost:8080"
```

## Support

For issues or questions, refer to:
- [README.md](README.md) - Main documentation
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Technical details
- Source code in `src/main/java/com/langgraph/ui/`

Happy graphing! 🚀
