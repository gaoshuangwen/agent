class LangGraphUI {
    constructor() {
        this.cy = null;
        this.stompClient = null;
        this.currentGraphId = null;
        this.currentExecutionId = null;
        this.executions = new Map();
        
        this.initCytoscape();
        this.initWebSocket();
        this.initEventHandlers();
        this.loadGraphs();
        this.loadExecutions();
        this.loadApprovals();
        
        setInterval(() => this.loadExecutions(), 5000);
        setInterval(() => this.loadApprovals(), 5000);
    }
    
    initCytoscape() {
        this.cy = cytoscape({
            container: document.getElementById('cy'),
            style: [
                {
                    selector: 'node',
                    style: {
                        'background-color': '#3498db',
                        'label': 'data(label)',
                        'color': '#fff',
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'width': '80px',
                        'height': '80px',
                        'font-size': '12px',
                        'text-wrap': 'wrap',
                        'text-max-width': '70px'
                    }
                },
                {
                    selector: 'node[type="start"]',
                    style: {
                        'background-color': '#27ae60',
                        'shape': 'round-rectangle'
                    }
                },
                {
                    selector: 'node[type="decision"]',
                    style: {
                        'background-color': '#f39c12',
                        'shape': 'diamond'
                    }
                },
                {
                    selector: 'node[type="llm"]',
                    style: {
                        'background-color': '#9b59b6'
                    }
                },
                {
                    selector: 'node.running',
                    style: {
                        'background-color': '#3498db',
                        'border-width': '4px',
                        'border-color': '#2980b9'
                    }
                },
                {
                    selector: 'node.completed',
                    style: {
                        'background-color': '#27ae60'
                    }
                },
                {
                    selector: 'node.failed',
                    style: {
                        'background-color': '#e74c3c'
                    }
                },
                {
                    selector: 'node.pending',
                    style: {
                        'background-color': '#95a5a6'
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 3,
                        'line-color': '#7f8c8d',
                        'target-arrow-color': '#7f8c8d',
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
                name: 'preset'
            },
            wheelSensitivity: 0.2
        });
    }
    
    initWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, () => {
            console.log('WebSocket connected');
            
            this.stompClient.subscribe('/topic/events', (message) => {
                const event = JSON.parse(message.body);
                this.handleEvent(event);
            });
        });
    }
    
    initEventHandlers() {
        document.getElementById('graphSelect').addEventListener('change', (e) => {
            this.currentGraphId = e.target.value;
            if (this.currentGraphId) {
                this.loadGraphTopology(this.currentGraphId);
            }
        });
        
        document.getElementById('startExecutionBtn').addEventListener('click', () => {
            if (this.currentGraphId) {
                this.startExecution(this.currentGraphId);
            } else {
                alert('Please select a graph first');
            }
        });
        
        document.getElementById('refreshBtn').addEventListener('click', () => {
            this.loadExecutions();
            this.loadApprovals();
        });
        
        document.getElementById('zoomInBtn').addEventListener('click', () => {
            this.cy.zoom(this.cy.zoom() * 1.2);
        });
        
        document.getElementById('zoomOutBtn').addEventListener('click', () => {
            this.cy.zoom(this.cy.zoom() * 0.8);
        });
        
        document.getElementById('fitBtn').addEventListener('click', () => {
            this.cy.fit();
        });
    }
    
    async loadGraphs() {
        try {
            const response = await fetch('/api/graphs');
            const graphs = await response.json();
            
            const select = document.getElementById('graphSelect');
            graphs.forEach(graph => {
                const option = document.createElement('option');
                option.value = graph.graphId;
                option.textContent = graph.name;
                select.appendChild(option);
            });
        } catch (error) {
            console.error('Error loading graphs:', error);
        }
    }
    
    async loadGraphTopology(graphId) {
        try {
            const response = await fetch(`/api/graphs/${graphId}`);
            const topology = await response.json();
            
            this.renderGraph(topology);
        } catch (error) {
            console.error('Error loading graph topology:', error);
        }
    }
    
    renderGraph(topology) {
        this.cy.elements().remove();
        
        topology.nodes.forEach(node => {
            this.cy.add({
                group: 'nodes',
                data: {
                    id: node.id,
                    label: node.name,
                    type: node.type
                },
                position: node.position || { x: 100, y: 100 }
            });
        });
        
        topology.edges.forEach(edge => {
            this.cy.add({
                group: 'edges',
                data: {
                    id: edge.id,
                    source: edge.source,
                    target: edge.target,
                    label: edge.label || ''
                }
            });
        });
        
        this.cy.fit();
    }
    
    async startExecution(graphId) {
        try {
            const response = await fetch('/api/executions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    graphId: graphId,
                    input: { message: 'Test input' }
                })
            });
            
            const execution = await response.json();
            this.currentExecutionId = execution.executionId;
            this.loadExecutions();
            
            if (this.stompClient && this.stompClient.connected) {
                this.stompClient.subscribe(`/topic/executions/${execution.executionId}`, (message) => {
                    const event = JSON.parse(message.body);
                    this.handleEvent(event);
                });
            }
        } catch (error) {
            console.error('Error starting execution:', error);
        }
    }
    
    async loadExecutions() {
        try {
            const response = await fetch('/api/executions');
            const executions = await response.json();
            
            executions.forEach(exec => {
                this.executions.set(exec.executionId, exec);
            });
            
            this.renderExecutions(executions);
        } catch (error) {
            console.error('Error loading executions:', error);
        }
    }
    
    renderExecutions(executions) {
        const container = document.getElementById('executionsList');
        
        if (executions.length === 0) {
            container.innerHTML = '<div class="empty-state">No executions yet</div>';
            return;
        }
        
        container.innerHTML = executions.map(exec => `
            <div class="execution-item ${this.currentExecutionId === exec.executionId ? 'selected' : ''}" 
                 data-execution-id="${exec.executionId}">
                <div class="execution-header">
                    <span class="execution-id">${exec.executionId.substring(0, 8)}...</span>
                    <span class="status-badge status-${exec.status.toLowerCase()}">${exec.status}</span>
                </div>
                <div style="font-size: 0.85rem; color: #7f8c8d;">
                    ${exec.graphId} • ${new Date(exec.startTime).toLocaleTimeString()}
                </div>
            </div>
        `).join('');
        
        document.querySelectorAll('.execution-item').forEach(item => {
            item.addEventListener('click', () => {
                this.selectExecution(item.dataset.executionId);
            });
        });
    }
    
    async selectExecution(executionId) {
        this.currentExecutionId = executionId;
        
        const execution = this.executions.get(executionId);
        if (execution) {
            this.updateNodeStates(execution);
            this.renderTimeline(execution);
            this.renderLogs(execution);
        }
        
        this.loadExecutions();
    }
    
    updateNodeStates(execution) {
        this.cy.nodes().removeClass('running completed failed pending');
        
        execution.nodeExecutions.forEach(nodeExec => {
            const node = this.cy.getElementById(nodeExec.nodeId);
            if (node.length > 0) {
                node.addClass(nodeExec.status.toLowerCase());
            }
        });
    }
    
    renderTimeline(execution) {
        const container = document.getElementById('timeline');
        
        if (!execution.nodeExecutions || execution.nodeExecutions.length === 0) {
            container.innerHTML = '<div class="empty-state">No timeline data</div>';
            return;
        }
        
        container.innerHTML = execution.nodeExecutions.map(nodeExec => `
            <div class="timeline-item">
                <span class="timeline-time">${new Date(nodeExec.startTime).toLocaleTimeString()}</span>
                <span class="timeline-node">${nodeExec.nodeName}</span>
                <span class="timeline-duration">${nodeExec.duration ? nodeExec.duration + 'ms' : '-'}</span>
            </div>
        `).join('');
    }
    
    renderLogs(execution) {
        const container = document.getElementById('logs');
        
        if (!execution.events || execution.events.length === 0) {
            container.innerHTML = '<div class="empty-state">No logs</div>';
            return;
        }
        
        const sortedEvents = [...execution.events].sort((a, b) => 
            new Date(b.timestamp) - new Date(a.timestamp)
        );
        
        container.innerHTML = sortedEvents.map(event => {
            let logClass = 'log-info';
            if (event.type.includes('FAILED')) {
                logClass = 'log-error';
            } else if (event.type.includes('COMPLETED')) {
                logClass = 'log-success';
            }
            
            return `
                <div class="log-entry ${logClass}">
                    <div class="log-time">${new Date(event.timestamp).toLocaleString()}</div>
                    <div class="log-message">${event.message}</div>
                </div>
            `;
        }).join('');
    }
    
    async loadApprovals() {
        try {
            const response = await fetch('/api/approvals');
            const approvals = await response.json();
            this.renderApprovals(approvals);
        } catch (error) {
            console.error('Error loading approvals:', error);
        }
    }
    
    renderApprovals(approvals) {
        const container = document.getElementById('approvals');
        
        if (approvals.length === 0) {
            container.innerHTML = '<div class="empty-state">No pending approvals</div>';
            return;
        }
        
        container.innerHTML = approvals.map(approval => `
            <div class="approval-item">
                <div class="approval-message">${approval.message}</div>
                <div class="approval-context">
                    Execution: ${approval.executionId.substring(0, 8)}... • 
                    Node: ${approval.nodeId}
                </div>
                <div class="approval-actions">
                    <button class="btn btn-small btn-success" 
                            onclick="app.approveRequest('${approval.approvalId}')">
                        Approve
                    </button>
                    <button class="btn btn-small btn-danger" 
                            onclick="app.rejectRequest('${approval.approvalId}')">
                        Reject
                    </button>
                </div>
            </div>
        `).join('');
    }
    
    async approveRequest(approvalId) {
        try {
            await fetch(`/api/approvals/${approvalId}/approve`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    approved: true,
                    response: 'Approved by user'
                })
            });
            
            this.loadApprovals();
        } catch (error) {
            console.error('Error approving request:', error);
        }
    }
    
    async rejectRequest(approvalId) {
        try {
            await fetch(`/api/approvals/${approvalId}/approve`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    approved: false,
                    response: 'Rejected by user'
                })
            });
            
            this.loadApprovals();
        } catch (error) {
            console.error('Error rejecting request:', error);
        }
    }
    
    handleEvent(event) {
        console.log('Event received:', event);
        
        if (event.executionId === this.currentExecutionId) {
            const execution = this.executions.get(event.executionId);
            if (execution) {
                if (!execution.events) {
                    execution.events = [];
                }
                execution.events.push(event);
                
                this.renderLogs(execution);
            }
        }
        
        if (event.type === 'NODE_STARTED' || event.type === 'NODE_COMPLETED' || event.type === 'NODE_FAILED') {
            const execution = this.executions.get(event.executionId);
            if (execution && event.executionId === this.currentExecutionId) {
                this.updateNodeStates(execution);
            }
        }
        
        if (event.type === 'APPROVAL_REQUESTED') {
            this.loadApprovals();
        }
    }
}

const app = new LangGraphUI();
