# LangGraph UI

Web-based visualization and management UI for LangGraph executions, built with Spring Boot and React.

## Features

### Backend (Spring Boot)
- **REST APIs** for managing graph executions, accessing topology, and handling human approval tasks
- **WebSocket APIs** for real-time execution monitoring and event streaming
- **Execution Tracking** with comprehensive telemetry and state management
- **Human Task Management** for approval workflows and user input

### Frontend (React + Cytoscape.js)
- **Interactive Graph Visualization** with pan/zoom capabilities
- **Real-time Node Status** indicators (pending, running, completed, failed)
- **Event Timeline** showing execution events as they occur
- **State Viewer** displaying current execution state
- **Human Task UI** for approvals and user input
- **Execution List** with filtering and selection

## REST API Endpoints

### Executions
- `POST /api/executions` - Start a new graph execution
- `GET /api/executions` - List all executions
- `GET /api/executions/{id}` - Get execution details
- `POST /api/executions/{id}/cancel` - Cancel an execution

### Graphs
- `GET /api/graphs` - List all registered graphs
- `GET /api/graphs/{id}` - Get graph topology

### Human Tasks
- `GET /api/tasks/pending` - List pending human tasks
- `GET /api/tasks/execution/{id}` - Get tasks for an execution
- `GET /api/tasks/{id}` - Get task details
- `POST /api/tasks/{id}/approve` - Approve a task
- `POST /api/tasks/{id}/reject` - Reject a task
- `POST /api/tasks/{id}/respond` - Respond to an input task

## WebSocket API

### Connection
Connect to: `ws://localhost:8080/ws/executions`

### Subscribe to Execution
```json
{
  "action": "subscribe",
  "executionId": "execution-id-here"
}
```

### Unsubscribe
```json
{
  "action": "unsubscribe",
  "executionId": "execution-id-here"
}
```

### Event Messages
The server sends execution events as they occur:
```json
{
  "type": "NodeExecutionStarted",
  "executionId": "...",
  "timestamp": "2024-01-01T12:00:00Z",
  "metadata": {}
}
```

## Running the Application

### Start the Server
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access the UI
Open your browser and navigate to:
```
http://localhost:8080/index.html
```

## Sample Graphs

The application includes three sample graphs that are automatically registered on startup:

1. **Simple Sequential Workflow** (`simple-sequential`)
   - Three-step linear workflow
   - Demonstrates basic node execution

2. **Conditional Workflow** (`conditional-workflow`)
   - Branching logic based on state values
   - Shows conditional edges and path selection

3. **Human Approval Workflow** (`human-approval`)
   - Requires human approval to proceed
   - Demonstrates human-in-the-loop patterns

## Starting an Execution

### Via REST API
```bash
curl -X POST http://localhost:8080/api/executions \
  -H "Content-Type: application/json" \
  -d '{
    "graphId": "simple-sequential",
    "initialState": {"input": "test"},
    "metadata": {}
  }'
```

### Via UI
1. Open the UI in your browser
2. The execution list will appear on the left sidebar
3. Click on any execution to view its details
4. The graph visualization will show node status in real-time
5. View events, state, and tasks in the bottom panel

## Architecture

### Service Layer
- **ExecutionService**: Manages graph execution lifecycle
- **GraphService**: Provides graph topology information
- **HumanTaskService**: Handles human task operations
- **ExecutionTracker**: Tracks execution state and events
- **GraphRegistry**: Manages registered graphs
- **WebSocketNotificationService**: Handles WebSocket subscriptions

### Configuration
- **ApplicationConfig**: Bean configuration for Jackson, HumanTaskManager
- **WebSocketConfig**: WebSocket endpoint configuration

### Controllers
- **ExecutionController**: REST endpoints for executions
- **GraphController**: REST endpoints for graph topology
- **HumanTaskController**: REST endpoints for human tasks

### WebSocket
- **ExecutionWebSocketHandler**: Handles WebSocket connections and subscriptions

## Testing

### Run All Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test Coverage
- REST API integration tests (ExecutionController, GraphController, HumanTaskController)
- WebSocket integration tests
- UI smoke tests
- Service layer tests

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: langgraph-ui

logging:
  level:
    dev.langgraph: DEBUG
```

## Technology Stack

### Backend
- Spring Boot 3.3.4
- Spring WebSocket
- Jackson for JSON serialization
- Java 21

### Frontend
- React 18 (loaded via CDN)
- Cytoscape.js 3.26 for graph visualization
- Vanilla CSS for styling
- WebSocket API for real-time updates

## Graph Visualization

The graph visualization uses Cytoscape.js with the following features:
- **Node Colors** indicate status:
  - Blue: Pending
  - Yellow: Running
  - Green: Completed
  - Red: Failed
  - Gray: Skipped
- **Pan/Zoom** for navigation
- **Breadth-first layout** for optimal viewing
- **Automatic fitting** on load

## Browser Compatibility

The UI is tested on:
- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)

WebSocket support is required for real-time updates.
