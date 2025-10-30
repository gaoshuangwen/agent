# LangGraph Visualization UI

A comprehensive Spring Boot-based visualization and monitoring UI for LangGraph execution flows.

## Features

### Backend (Spring Boot)
- **REST APIs**: Complete set of RESTful endpoints for graph execution management
- **WebSocket Support**: Real-time updates via WebSocket/STOMP protocol
- **Execution Management**: Start, monitor, and cancel graph executions
- **Telemetry Service**: Track execution events and node states
- **Human Approval Workflow**: Support for approval requests during execution
- **Graph Topology**: Define and visualize graph structures

### Frontend
- **Interactive Graph Visualization**: Pan and zoom graph view using Cytoscape.js
- **Live Node Status**: Real-time node state updates (pending, running, completed, failed)
- **Timeline View**: Track execution progress over time
- **Event Logs**: Comprehensive logging of execution events
- **Approval Management**: UI for handling human approval requests

## Architecture

```
langgraph-ui/
├── src/main/java/com/langgraph/ui/
│   ├── model/              # Domain models
│   ├── dto/                # Data transfer objects
│   ├── service/            # Business logic
│   ├── controller/         # REST controllers
│   └── config/             # Configuration (WebSocket, etc.)
├── src/main/resources/
│   ├── templates/          # Thymeleaf templates
│   ├── static/
│   │   ├── css/           # Stylesheets
│   │   └── js/            # JavaScript application
│   └── application.yml     # Application configuration
└── src/test/java/          # Tests
```

## API Endpoints

### Graph Topology
- `GET /api/graphs` - Get all available graphs
- `GET /api/graphs/{graphId}` - Get specific graph topology

### Execution Management
- `POST /api/executions` - Start a new execution
- `GET /api/executions` - Get all executions
- `GET /api/executions/{executionId}` - Get specific execution
- `DELETE /api/executions/{executionId}` - Cancel an execution

### Telemetry
- `GET /api/telemetry/events` - Get all events
- `GET /api/telemetry/executions/{executionId}/events` - Get events for specific execution

### Approvals
- `GET /api/approvals` - Get pending approval requests
- `GET /api/approvals/{approvalId}` - Get specific approval
- `POST /api/approvals/{approvalId}/approve` - Approve or reject a request

### WebSocket
- Connect: `/ws` (SockJS endpoint)
- Subscribe to all events: `/topic/events`
- Subscribe to execution events: `/topic/executions/{executionId}`

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build and Run

```bash
# Build the project
mvn clean install

# Run the application
cd langgraph-ui
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Run Tests

```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -Dtest=*IntegrationTest

# Run unit tests only
mvn test -Dtest=*ControllerTest
```

## Usage

1. **Select a Graph**: Choose from available graphs in the dropdown
2. **Start Execution**: Click "Start Execution" to begin processing
3. **Monitor Progress**: Watch node states update in real-time on the graph
4. **View Timeline**: See execution progress in the timeline panel
5. **Check Logs**: Review detailed event logs
6. **Handle Approvals**: Approve or reject pending approval requests

## Technology Stack

- **Backend**: Spring Boot 3.2, Spring WebSocket, Spring MVC
- **Frontend**: Vanilla JavaScript, Cytoscape.js, SockJS, STOMP
- **Build Tool**: Maven
- **Testing**: JUnit 5, Spring Test, Selenium (HtmlUnit)

## Sample Graphs

The application includes two sample graphs:
1. **Sample LangGraph**: A simple linear flow (Input → LLM → Output)
2. **Complex LangGraph**: A branching flow with decision nodes

## Development

### Adding New Graphs

Graphs can be added by modifying `GraphTopologyService.initializeSampleGraphs()`:

```java
GraphTopology newGraph = GraphTopology.builder()
    .graphId("my-graph")
    .name("My Graph")
    .nodes(Arrays.asList(...))
    .edges(Arrays.asList(...))
    .build();
```

### Customizing Node Appearance

Modify the Cytoscape styles in `/static/js/app.js` in the `initCytoscape()` method.

## License

This project is part of the LangGraph system.
