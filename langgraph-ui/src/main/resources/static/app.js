const { useState, useEffect, useRef } = React;

const API_BASE = window.location.origin + '/api';
const WS_BASE = `ws://${window.location.host}/ws`;

function App() {
    const [view, setView] = useState('graphs');
    const [graphs, setGraphs] = useState([]);
    const [selectedGraph, setSelectedGraph] = useState(null);
    const [executions, setExecutions] = useState([]);
    const [selectedExecution, setSelectedExecution] = useState(null);
    const [executionDetails, setExecutionDetails] = useState(null);
    const [graphTopology, setGraphTopology] = useState(null);
    const [activeTab, setActiveTab] = useState('events');
    const [pendingTasks, setPendingTasks] = useState([]);
    const [error, setError] = useState(null);
    const [showStartDialog, setShowStartDialog] = useState(false);
    const [showDetailDialog, setShowDetailDialog] = useState(false);
    const [selectedElement, setSelectedElement] = useState(null);
    const [initialState, setInitialState] = useState('{}');
    const [executionHistory, setExecutionHistory] = useState([]);
    const cyRef = useRef(null);
    const wsRef = useRef(null);

    useEffect(() => {
        fetchGraphs();
        fetchExecutions();
        fetchPendingTasks();
        loadExecutionHistory();
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

    useEffect(() => {
        if (selectedGraph) {
            fetchGraphTopology(selectedGraph.graphId);
        }
    }, [selectedGraph]);

    useEffect(() => {
        if (graphTopology && !executionDetails && selectedGraph) {
            renderGraph(graphTopology, {});
        }
    }, [graphTopology, selectedGraph]);

    const loadExecutionHistory = () => {
        const history = localStorage.getItem('executionHistory');
        if (history) {
            setExecutionHistory(JSON.parse(history));
        }
    };

    const saveExecutionToHistory = (execution) => {
        const history = JSON.parse(localStorage.getItem('executionHistory') || '[]');
        const newEntry = {
            executionId: execution.executionId,
            graphId: execution.graphId,
            status: execution.status,
            startTime: execution.startTime,
            endTime: execution.endTime
        };
        const filtered = history.filter(h => h.executionId !== execution.executionId);
        const updated = [newEntry, ...filtered].slice(0, 50);
        localStorage.setItem('executionHistory', JSON.stringify(updated));
        setExecutionHistory(updated);
    };

    const fetchGraphs = async () => {
        try {
            const response = await fetch(`${API_BASE}/graphs`);
            const data = await response.json();
            setGraphs(data);
        } catch (err) {
            console.error('Error fetching graphs:', err);
        }
    };

    const fetchExecutions = async () => {
        try {
            const response = await fetch(`${API_BASE}/executions`);
            const data = await response.json();
            setExecutions(data);
            data.forEach(exec => {
                if (exec.status === 'COMPLETED' || exec.status === 'FAILED') {
                    saveExecutionToHistory(exec);
                }
            });
        } catch (err) {
            console.error('Error fetching executions:', err);
        }
    };

    const fetchExecutionDetails = async (executionId) => {
        try {
            const response = await fetch(`${API_BASE}/executions/${executionId}`);
            const data = await response.json();
            setExecutionDetails(data);
            if (data.status === 'COMPLETED' || data.status === 'FAILED') {
                saveExecutionToHistory(data);
            }
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

    const startExecution = async () => {
        if (!selectedGraph) return;

        try {
            const stateObj = JSON.parse(initialState);
            const response = await fetch(`${API_BASE}/executions`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    graphId: selectedGraph.graphId,
                    initialState: stateObj,
                    metadata: {}
                })
            });
            const data = await response.json();
            
            if (data.executionId) {
                setShowStartDialog(false);
                setInitialState('{}');
                setView('executions');
                setSelectedExecution(data.executionId);
                fetchExecutions();
            } else if (data.error) {
                setError(data.error);
            }
        } catch (err) {
            console.error('Error starting execution:', err);
            setError('Failed to start execution: ' + err.message);
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

    const getNodeColor = (nodeId, status) => {
        // Special colors for start and end nodes
        if (nodeId === 'start') return '#00BCD4'; // Cyan for start
        if (nodeId === 'end') return '#E91E63'; // Pink for end
        
        // Status-based colors take precedence during execution
        if (status === 'RUNNING') return '#ffc107'; // Yellow for running
        if (status === 'COMPLETED') return '#4caf50'; // Green for completed
        if (status === 'FAILED') return '#f44336'; // Red for failed
        
        // Different colors for different node types when pending
        const nodeColors = {
            'validate': '#9C27B0',  // Purple
            'check': '#FF9800',     // Orange
            'process': '#3F51B5',   // Indigo
            'enrich': '#009688',    // Teal
            'prepare': '#795548',   // Brown
            'approval': '#FF5722',  // Deep Orange
            'approved': '#8BC34A',  // Light Green
            'rejected': '#F44336',  // Red
            'extract': '#607D8B',   // Blue Grey
            'transform': '#673AB7', // Deep Purple
            'load': '#FFC107',      // Amber
            'merge': '#CDDC39',     // Lime
            'high-priority': '#D32F2F', // Dark Red
            'normal-priority': '#1976D2', // Dark Blue
            'low-priority': '#388E3C'  // Dark Green
        };
        
        return nodeColors[nodeId] || '#2196f3'; // Default blue
    };

    const renderGraph = (topology, nodeStatuses) => {
        if (!topology || !topology.nodes) return;

        const elements = [];

        topology.nodes.forEach(node => {
            const status = nodeStatuses ? nodeStatuses[node.id] : 'PENDING';
            const baseColor = getNodeColor(node.id, status);
            
            elements.push({
                data: {
                    id: node.id,
                    label: node.name,
                    status: status,
                    nodeData: node,
                    baseColor: baseColor
                }
            });
        });

        topology.edges.forEach(edge => {
            const isConditional = edge.type.includes('Conditional');
            elements.push({
                data: {
                    id: edge.id,
                    source: edge.source,
                    target: edge.target,
                    label: edge.type,
                    edgeData: edge,
                    isConditional: isConditional
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
                        'text-wrap': 'wrap',
                        'text-max-width': '70px',
                        'background-color': 'data(baseColor)',
                        'color': '#fff',
                        'font-size': '11px',
                        'font-weight': 'bold',
                        'width': '90px',
                        'height': '90px',
                        'border-width': '3px',
                        'border-color': '#fff',
                        'text-outline-width': 2,
                        'text-outline-color': 'data(baseColor)',
                        'transition-property': 'background-color, border-color, border-width, box-shadow',
                        'transition-duration': '0.3s',
                        'box-shadow': '0 2px 4px rgba(0,0,0,0.2)'
                    }
                },
                {
                    selector: 'node[status="RUNNING"]',
                    style: {
                        'border-width': '8px',
                        'border-color': '#fff',
                        'box-shadow': '0 0 30px rgba(255, 193, 7, 0.8), 0 0 60px rgba(255, 193, 7, 0.4)'
                    }
                },
                {
                    selector: 'node[status="COMPLETED"]',
                    style: {
                        'border-color': '#fff',
                        'box-shadow': '0 0 15px rgba(76, 175, 80, 0.5)'
                    }
                },
                {
                    selector: 'node[status="FAILED"]',
                    style: {
                        'border-color': '#fff',
                        'box-shadow': '0 0 15px rgba(244, 67, 54, 0.5)'
                    }
                },
                {
                    selector: 'node:active',
                    style: {
                        'overlay-opacity': 0.2,
                        'overlay-color': '#1976d2'
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 3,
                        'line-color': function(ele) {
                            return ele.data('isConditional') ? '#FF6B6B' : '#4ECDC4';
                        },
                        'target-arrow-color': function(ele) {
                            return ele.data('isConditional') ? '#FF6B6B' : '#4ECDC4';
                        },
                        'target-arrow-shape': 'triangle',
                        'curve-style': 'bezier',
                        'label': function(ele) {
                            return ele.data('isConditional') ? '?' : '';
                        },
                        'font-size': '14px',
                        'font-weight': 'bold',
                        'text-rotation': 'autorotate',
                        'text-margin-y': -12,
                        'arrow-scale': 1.5,
                        'line-style': function(ele) {
                            return ele.data('isConditional') ? 'dashed' : 'solid';
                        }
                    }
                },
                {
                    selector: 'edge:active',
                    style: {
                        'line-color': '#1976d2',
                        'target-arrow-color': '#1976d2',
                        'width': 4
                    }
                }
            ],
            layout: {
                name: 'breadthfirst',
                directed: true,
                roots: topology.entryPoint ? '[id = "' + topology.entryPoint + '"]' : undefined,
                padding: 60,
                spacingFactor: 1.8,
                avoidOverlap: true,
                nodeDimensionsIncludeLabels: true,
                animate: false,
                fit: true,
                // Force left-to-right layout
                rankDir: 'LR'
            }
        });

        // Add click handlers
        cy.on('tap', 'node', function(evt) {
            const node = evt.target;
            const nodeId = node.data('id');
            const nodeData = node.data('nodeData');
            const status = node.data('status');
            
            setSelectedElement({
                type: 'node',
                id: nodeId,
                name: nodeData.name,
                status: status,
                metadata: nodeData.metadata,
                state: executionDetails ? executionDetails.state : null
            });
            setShowDetailDialog(true);
        });

        cy.on('tap', 'edge', function(evt) {
            const edge = evt.target;
            const edgeData = edge.data('edgeData');
            
            setSelectedElement({
                type: 'edge',
                id: edge.data('id'),
                source: edge.data('source'),
                target: edge.data('target'),
                edgeType: edgeData.type,
                metadata: edgeData.metadata,
                state: executionDetails ? executionDetails.state : null
            });
            setShowDetailDialog(true);
        });

        // Animate running nodes
        const animateRunningNodes = () => {
            cy.nodes('[status="RUNNING"]').animate({
                style: {
                    'border-width': '8px'
                },
                duration: 500,
                easing: 'ease-in-out-cubic'
            }).animate({
                style: {
                    'border-width': '4px'
                },
                duration: 500,
                easing: 'ease-in-out-cubic',
                complete: () => {
                    if (cy.nodes('[status="RUNNING"]').length > 0) {
                        setTimeout(animateRunningNodes, 100);
                    }
                }
            });
        };

        if (cy.nodes('[status="RUNNING"]').length > 0) {
            animateRunningNodes();
        }

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

    const loadHistoryExecution = (executionId) => {
        setView('executions');
        setSelectedExecution(executionId);
    };

    const clearHistory = () => {
        localStorage.removeItem('executionHistory');
        setExecutionHistory([]);
    };

    return (
        <div className="app-container">
            <div className="header">
                <h1>🔷 LangGraph UI</h1>
                <div className="header-tabs">
                    <button 
                        className={`header-tab ${view === 'graphs' ? 'active' : ''}`}
                        onClick={() => setView('graphs')}
                    >
                        Graphs
                    </button>
                    <button 
                        className={`header-tab ${view === 'executions' ? 'active' : ''}`}
                        onClick={() => setView('executions')}
                    >
                        Executions ({executions.length})
                    </button>
                    <button 
                        className={`header-tab ${view === 'history' ? 'active' : ''}`}
                        onClick={() => setView('history')}
                    >
                        History ({executionHistory.length})
                    </button>
                </div>
            </div>
            <div className="main-content">
                <div className="sidebar">
                    {view === 'graphs' ? (
                        <>
                            <h2>Available Graphs</h2>
                            {graphs.length === 0 ? (
                                <div className="empty-state">No graphs available</div>
                            ) : (
                                <ul className="graph-list">
                                    {graphs.map(graph => (
                                        <li
                                            key={graph.graphId}
                                            className={`graph-item ${selectedGraph?.graphId === graph.graphId ? 'active' : ''}`}
                                            onClick={() => setSelectedGraph(graph)}
                                        >
                                            <div className="graph-name">{graph.name}</div>
                                            <div className="graph-id">{graph.graphId}</div>
                                            <div className="graph-stats">
                                                {graph.nodes.length} nodes, {graph.edges.length} edges
                                            </div>
                                            <button 
                                                className="btn btn-primary btn-small"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    setShowStartDialog(true);
                                                }}
                                            >
                                                Start Execution
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </>
                    ) : view === 'history' ? (
                        <>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                                <h2>Execution History</h2>
                                {executionHistory.length > 0 && (
                                    <button className="btn btn-secondary btn-small" onClick={clearHistory}>
                                        Clear
                                    </button>
                                )}
                            </div>
                            {executionHistory.length === 0 ? (
                                <div className="empty-state">No execution history</div>
                            ) : (
                                <ul className="execution-list">
                                    {executionHistory.map(exec => (
                                        <li
                                            key={exec.executionId}
                                            className="execution-item"
                                            onClick={() => loadHistoryExecution(exec.executionId)}
                                        >
                                            <div>
                                                {exec.executionId.substring(0, 8)}...
                                                <span className={`status ${exec.status}`}>{exec.status}</span>
                                            </div>
                                            <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                                                {exec.graphId}
                                            </div>
                                            <div style={{ fontSize: '11px', color: '#999', marginTop: '2px' }}>
                                                {new Date(exec.startTime).toLocaleString()}
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </>
                    ) : (
                        <>
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
                        </>
                    )}
                </div>
                <div className="content-area">
                    {view === 'graphs' && !selectedGraph ? (
                        <div className="empty-state">Select a graph to view its topology</div>
                    ) : view === 'executions' && !selectedExecution ? (
                        <div className="empty-state">Select an execution to view details</div>
                    ) : view === 'history' ? (
                        <div className="empty-state">Select an execution from history to view details</div>
                    ) : (
                        <>
                            <div className="graph-container">
                                <div className="graph-info-bar">
                                    {executionDetails && (
                                        <>
                                            <span className="info-item">
                                                <strong>Status:</strong> 
                                                <span className={`status ${executionDetails.status}`}>
                                                    {executionDetails.status}
                                                </span>
                                            </span>
                                            {executionDetails.currentNode && (
                                                <span className="info-item">
                                                    <strong>Current Node:</strong> {executionDetails.currentNode}
                                                </span>
                                            )}
                                            <span className="info-item">
                                                <strong>Started:</strong> {new Date(executionDetails.startTime).toLocaleString()}
                                            </span>
                                        </>
                                    )}
                                </div>
                                <div id="cy"></div>
                            </div>
                            {view === 'executions' && (
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
                            )}
                        </>
                    )}
                </div>
            </div>
            {showStartDialog && selectedGraph && (
                <div className="modal-overlay" onClick={() => setShowStartDialog(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <h2>Start Execution</h2>
                        <p>Graph: <strong>{selectedGraph.name}</strong></p>
                        <div className="form-group">
                            <label>Initial State (JSON):</label>
                            <textarea
                                className="form-control"
                                rows="10"
                                value={initialState}
                                onChange={(e) => setInitialState(e.target.value)}
                                placeholder='{"key": "value"}'
                            />
                        </div>
                        <div className="modal-actions">
                            <button className="btn btn-primary" onClick={startExecution}>
                                Start
                            </button>
                            <button className="btn btn-secondary" onClick={() => setShowStartDialog(false)}>
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {showDetailDialog && selectedElement && (
                <div className="modal-overlay" onClick={() => setShowDetailDialog(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <h2>{selectedElement.type === 'node' ? 'Node Details' : 'Edge Details'}</h2>
                        {selectedElement.type === 'node' ? (
                            <div className="detail-content">
                                <div className="detail-row">
                                    <strong>ID:</strong> {selectedElement.id}
                                </div>
                                <div className="detail-row">
                                    <strong>Name:</strong> {selectedElement.name}
                                </div>
                                <div className="detail-row">
                                    <strong>Status:</strong> 
                                    <span className={`status ${selectedElement.status}`}>
                                        {selectedElement.status}
                                    </span>
                                </div>
                                {selectedElement.metadata && Object.keys(selectedElement.metadata).length > 0 && (
                                    <div className="detail-row">
                                        <strong>Metadata:</strong>
                                        <pre className="detail-json">
                                            {JSON.stringify(selectedElement.metadata, null, 2)}
                                        </pre>
                                    </div>
                                )}
                                {selectedElement.state && (
                                    <div className="detail-row">
                                        <strong>Current State:</strong>
                                        <pre className="detail-json">
                                            {JSON.stringify(selectedElement.state, null, 2)}
                                        </pre>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="detail-content">
                                <div className="detail-row">
                                    <strong>ID:</strong> {selectedElement.id}
                                </div>
                                <div className="detail-row">
                                    <strong>Type:</strong> {selectedElement.edgeType}
                                </div>
                                <div className="detail-row">
                                    <strong>Source:</strong> {selectedElement.source}
                                </div>
                                <div className="detail-row">
                                    <strong>Target:</strong> {selectedElement.target}
                                </div>
                                {selectedElement.metadata && Object.keys(selectedElement.metadata).length > 0 && (
                                    <div className="detail-row">
                                        <strong>Metadata:</strong>
                                        <pre className="detail-json">
                                            {JSON.stringify(selectedElement.metadata, null, 2)}
                                        </pre>
                                    </div>
                                )}
                                {selectedElement.state && (
                                    <div className="detail-row">
                                        <strong>Current State:</strong>
                                        <pre className="detail-json">
                                            {JSON.stringify(selectedElement.state, null, 2)}
                                        </pre>
                                    </div>
                                )}
                            </div>
                        )}
                        <div className="modal-actions">
                            <button className="btn btn-primary" onClick={() => setShowDetailDialog(false)}>
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {error && (
                <div className="error" onClick={() => setError(null)}>
                    {error}
                </div>
            )}
        </div>
    );
}

ReactDOM.render(<App />, document.getElementById('root'));
