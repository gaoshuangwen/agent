const { useState, useEffect, useRef } = React;

const API_BASE = window.location.origin + '/api';
const WS_BASE = `ws://${window.location.host}/ws`;

function App() {
    const [executions, setExecutions] = useState([]);
    const [selectedExecution, setSelectedExecution] = useState(null);
    const [executionDetails, setExecutionDetails] = useState(null);
    const [graphTopology, setGraphTopology] = useState(null);
    const [activeTab, setActiveTab] = useState('events');
    const [pendingTasks, setPendingTasks] = useState([]);
    const [error, setError] = useState(null);
    const cyRef = useRef(null);
    const wsRef = useRef(null);

    useEffect(() => {
        fetchExecutions();
        fetchPendingTasks();
        const interval = setInterval(() => {
            fetchExecutions();
            fetchPendingTasks();
        }, 5000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        if (selectedExecution) {
            fetchExecutionDetails(selectedExecution);
            connectWebSocket(selectedExecution);
        }
        return () => {
            if (wsRef.current) {
                wsRef.current.close();
            }
        };
    }, [selectedExecution]);

    useEffect(() => {
        if (executionDetails && executionDetails.graphId) {
            fetchGraphTopology(executionDetails.graphId);
        }
    }, [executionDetails]);

    useEffect(() => {
        if (graphTopology && executionDetails) {
            renderGraph(graphTopology, executionDetails.nodeStatuses);
        }
    }, [graphTopology, executionDetails]);

    const fetchExecutions = async () => {
        try {
            const response = await fetch(`${API_BASE}/executions`);
            const data = await response.json();
            setExecutions(data);
        } catch (err) {
            console.error('Error fetching executions:', err);
        }
    };

    const fetchExecutionDetails = async (executionId) => {
        try {
            const response = await fetch(`${API_BASE}/executions/${executionId}`);
            const data = await response.json();
            setExecutionDetails(data);
        } catch (err) {
            console.error('Error fetching execution details:', err);
        }
    };

    const fetchGraphTopology = async (graphId) => {
        try {
            const response = await fetch(`${API_BASE}/graphs/${graphId}`);
            const data = await response.json();
            setGraphTopology(data);
        } catch (err) {
            console.error('Error fetching graph topology:', err);
        }
    };

    const fetchPendingTasks = async () => {
        try {
            const response = await fetch(`${API_BASE}/tasks/pending`);
            const data = await response.json();
            setPendingTasks(data);
        } catch (err) {
            console.error('Error fetching pending tasks:', err);
        }
    };

    const connectWebSocket = (executionId) => {
        if (wsRef.current) {
            wsRef.current.close();
        }

        const ws = new WebSocket(`${WS_BASE}/executions`);
        
        ws.onopen = () => {
            console.log('WebSocket connected');
            ws.send(JSON.stringify({
                action: 'subscribe',
                executionId: executionId
            }));
        };

        ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            console.log('WebSocket message:', message);
            if (message.type !== 'ack') {
                fetchExecutionDetails(executionId);
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };

        ws.onclose = () => {
            console.log('WebSocket disconnected');
        };

        wsRef.current = ws;
    };

    const renderGraph = (topology, nodeStatuses) => {
        if (!topology || !topology.nodes) return;

        const elements = [];

        topology.nodes.forEach(node => {
            const status = nodeStatuses ? nodeStatuses[node.id] : 'PENDING';
            elements.push({
                data: {
                    id: node.id,
                    label: node.name,
                    status: status
                }
            });
        });

        topology.edges.forEach(edge => {
            elements.push({
                data: {
                    id: edge.id,
                    source: edge.source,
                    target: edge.target,
                    label: edge.type
                }
            });
        });

        if (cyRef.current) {
            cyRef.current.destroy();
        }

        const cy = cytoscape({
            container: document.getElementById('cy'),
            elements: elements,
            style: [
                {
                    selector: 'node',
                    style: {
                        'label': 'data(label)',
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'background-color': function(ele) {
                            const status = ele.data('status');
                            switch(status) {
                                case 'RUNNING': return '#ffc107';
                                case 'COMPLETED': return '#4caf50';
                                case 'FAILED': return '#f44336';
                                case 'PENDING': return '#2196f3';
                                default: return '#9e9e9e';
                            }
                        },
                        'color': '#fff',
                        'font-size': '12px',
                        'font-weight': 'bold',
                        'width': '80px',
                        'height': '80px',
                        'border-width': '3px',
                        'border-color': '#fff'
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 2,
                        'line-color': '#999',
                        'target-arrow-color': '#999',
                        'target-arrow-shape': 'triangle',
                        'curve-style': 'bezier',
                        'label': 'data(label)',
                        'font-size': '10px',
                        'text-rotation': 'autorotate',
                        'text-margin-y': -10
                    }
                }
            ],
            layout: {
                name: 'breadthfirst',
                directed: true,
                padding: 50,
                spacingFactor: 1.5
            }
        });

        cy.fit(50);
        cyRef.current = cy;
    };

    const approveTask = async (taskId) => {
        try {
            await fetch(`${API_BASE}/tasks/${taskId}/approve`, { method: 'POST' });
            fetchPendingTasks();
            if (selectedExecution) {
                fetchExecutionDetails(selectedExecution);
            }
        } catch (err) {
            console.error('Error approving task:', err);
            setError('Failed to approve task');
        }
    };

    const rejectTask = async (taskId) => {
        try {
            await fetch(`${API_BASE}/tasks/${taskId}/reject`, { method: 'POST' });
            fetchPendingTasks();
            if (selectedExecution) {
                fetchExecutionDetails(selectedExecution);
            }
        } catch (err) {
            console.error('Error rejecting task:', err);
            setError('Failed to reject task');
        }
    };

    return (
        <div className="app-container">
            <div className="header">
                <h1>🔷 LangGraph UI</h1>
            </div>
            <div className="main-content">
                <div className="sidebar">
                    <h2>Executions</h2>
                    {executions.length === 0 ? (
                        <div className="empty-state">No executions</div>
                    ) : (
                        <ul className="execution-list">
                            {executions.map(exec => (
                                <li
                                    key={exec.executionId}
                                    className={`execution-item ${selectedExecution === exec.executionId ? 'active' : ''}`}
                                    onClick={() => setSelectedExecution(exec.executionId)}
                                >
                                    <div>
                                        {exec.executionId.substring(0, 8)}...
                                        <span className={`status ${exec.status}`}>{exec.status}</span>
                                    </div>
                                    <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                                        {exec.graphId}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
                <div className="content-area">
                    {!selectedExecution ? (
                        <div className="empty-state">Select an execution to view details</div>
                    ) : (
                        <>
                            <div className="graph-container">
                                <div id="cy"></div>
                            </div>
                            <div className="bottom-panel">
                                <div className="panel-tabs">
                                    <button
                                        className={`panel-tab ${activeTab === 'events' ? 'active' : ''}`}
                                        onClick={() => setActiveTab('events')}
                                    >
                                        Events
                                    </button>
                                    <button
                                        className={`panel-tab ${activeTab === 'state' ? 'active' : ''}`}
                                        onClick={() => setActiveTab('state')}
                                    >
                                        State
                                    </button>
                                    <button
                                        className={`panel-tab ${activeTab === 'tasks' ? 'active' : ''}`}
                                        onClick={() => setActiveTab('tasks')}
                                    >
                                        Tasks ({pendingTasks.length})
                                    </button>
                                </div>
                                <div className="panel-content">
                                    {activeTab === 'events' && executionDetails && (
                                        <div className="event-log">
                                            {executionDetails.events && executionDetails.events.length === 0 ? (
                                                <div>No events yet</div>
                                            ) : (
                                                executionDetails.events.map((event, idx) => (
                                                    <div key={idx} className="event-item">
                                                        <span className="timestamp">
                                                            {new Date(event.timestamp).toLocaleTimeString()}
                                                        </span>
                                                        <span className="event-type">{event.eventType}</span>
                                                        {event.nodeId && <span>Node: {event.nodeId}</span>}
                                                    </div>
                                                ))
                                            )}
                                        </div>
                                    )}
                                    {activeTab === 'state' && executionDetails && (
                                        <div className="state-viewer">
                                            <pre>{JSON.stringify(executionDetails.state, null, 2)}</pre>
                                        </div>
                                    )}
                                    {activeTab === 'tasks' && (
                                        <div>
                                            {pendingTasks.length === 0 ? (
                                                <div>No pending tasks</div>
                                            ) : (
                                                <ul className="task-list">
                                                    {pendingTasks.map(task => (
                                                        <li key={task.taskId} className="task-item">
                                                            <div className="task-prompt">{task.prompt}</div>
                                                            <div style={{ fontSize: '12px', color: '#666' }}>
                                                                Type: {task.type} | Created: {new Date(task.createdAt).toLocaleString()}
                                                            </div>
                                                            <div className="task-actions">
                                                                <button
                                                                    className="btn btn-primary"
                                                                    onClick={() => approveTask(task.taskId)}
                                                                >
                                                                    Approve
                                                                </button>
                                                                <button
                                                                    className="btn btn-secondary"
                                                                    onClick={() => rejectTask(task.taskId)}
                                                                >
                                                                    Reject
                                                                </button>
                                                            </div>
                                                        </li>
                                                    ))}
                                                </ul>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </div>
            {error && (
                <div className="error" onClick={() => setError(null)}>
                    {error}
                </div>
            )}
        </div>
    );
}

ReactDOM.render(<App />, document.getElementById('root'));
