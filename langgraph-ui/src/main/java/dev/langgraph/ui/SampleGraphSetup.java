package dev.langgraph.ui;

import dev.langgraph.core.Graph;
import dev.langgraph.core.GraphBuilder;
import dev.langgraph.core.GraphId;
import dev.langgraph.core.NodeId;
import dev.langgraph.core.human.HumanApprovalNode;
import dev.langgraph.core.human.HumanTaskManager;
import dev.langgraph.ui.service.GraphRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
public class SampleGraphSetup {

    private static final Logger logger = LoggerFactory.getLogger(SampleGraphSetup.class);
    private static final int NODE_EXECUTION_DELAY_MS = 10000; // 10 seconds per node

    @Bean
    public CommandLineRunner setupSampleGraphs(GraphRegistry registry, HumanTaskManager taskManager) {
        return args -> {
            try {
                createSimpleSequentialGraph(registry);
                createConditionalGraph(registry);
                createHumanApprovalGraph(registry, taskManager);
                createDataProcessingGraph(registry);
                logger.info("Successfully registered {} sample graphs", registry.getGraphIds().size());
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup sample graphs", e);
            }
        };
    }

    private void createSimpleSequentialGraph(GraphRegistry registry) throws Exception {
        NodeId startNode = NodeId.of("start");
        NodeId validateNode = NodeId.of("validate");
        NodeId processNode = NodeId.of("process");
        NodeId enrichNode = NodeId.of("enrich");
        NodeId endNode = NodeId.of("end");

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("simple-sequential"))
                .name("Simple Sequential Workflow")
                .metadata("description", "A sequential workflow with input validation and output formatting")
                .metadata("color", "#4CAF50")
                .addFunctionalNode(startNode, "Start", (state, ctx) -> {
                    logger.info("Starting workflow execution");
                    simulateWork("start");
                    return state
                            .with("step", "started")
                            .with("startTime", System.currentTimeMillis())
                            .with("workflowId", ctx.executionId());
                })
                .addFunctionalNode(validateNode, "Validate Input", (state, ctx) -> {
                    logger.info("Validating input data");
                    simulateWork("validate");
                    
                    // Validate and clean input
                    String input = state.get("input", "default");
                    if (input == null || input.isEmpty()) {
                        input = "default-value";
                    }
                    
                    return state
                            .with("step", "validated")
                            .with("validatedInput", input.trim().toLowerCase())
                            .with("validationStatus", "passed");
                })
                .addFunctionalNode(processNode, "Process Data", (state, ctx) -> {
                    logger.info("Processing data");
                    simulateWork("process");
                    
                    String input = state.get("validatedInput", "");
                    return state
                            .with("step", "processed")
                            .with("processedData", "processed-" + input)
                            .with("processCount", 1);
                })
                .addFunctionalNode(enrichNode, "Enrich Results", (state, ctx) -> {
                    logger.info("Enriching results");
                    simulateWork("enrich");
                    
                    String processed = state.get("processedData", "");
                    return state
                            .with("step", "enriched")
                            .with("enrichedData", Map.of(
                                    "data", processed,
                                    "metadata", Map.of("version", "1.0", "timestamp", System.currentTimeMillis())
                            ));
                })
                .addFunctionalNode(endNode, "Format Output", (state, ctx) -> {
                    logger.info("Formatting output");
                    simulateWork("end");
                    
                    // Format final output
                    Object enrichedData = state.get("enrichedData").orElse(null);
                    long startTime = state.get("startTime", 0L);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    return state
                            .with("step", "completed")
                            .with("status", "success")
                            .with("result", enrichedData)
                            .with("duration", duration)
                            .with("formattedAt", System.currentTimeMillis());
                })
                .addDirectEdge(startNode, validateNode)
                .addDirectEdge(validateNode, processNode)
                .addDirectEdge(processNode, enrichNode)
                .addDirectEdge(enrichNode, endNode)
                .entryPoint(startNode)
                .buildAndValidate();

        registry.registerGraph("simple-sequential", graph);
    }

    private void createConditionalGraph(GraphRegistry registry) throws Exception {
        NodeId startNode = NodeId.of("start");
        NodeId validateNode = NodeId.of("validate");
        NodeId checkNode = NodeId.of("check");
        NodeId highPriorityNode = NodeId.of("high-priority");
        NodeId normalPriorityNode = NodeId.of("normal-priority");
        NodeId lowPriorityNode = NodeId.of("low-priority");
        NodeId mergeNode = NodeId.of("merge");
        NodeId endNode = NodeId.of("end");

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("conditional-workflow"))
                .name("Conditional Branching Workflow")
                .metadata("description", "A workflow with conditional routing based on priority")
                .metadata("color", "#2196F3")
                .addFunctionalNode(startNode, "Start", (state, ctx) -> {
                    logger.info("Starting conditional workflow");
                    simulateWork("start");
                    return state
                            .with("step", "started")
                            .with("startTime", System.currentTimeMillis());
                })
                .addFunctionalNode(validateNode, "Validate Input", (state, ctx) -> {
                    logger.info("Validating input");
                    simulateWork("validate");
                    
                    int value = state.get("value", 0);
                    return state
                            .with("step", "validated")
                            .with("validatedValue", Math.max(0, value));
                })
                .addFunctionalNode(checkNode, "Check Priority", (state, ctx) -> {
                    logger.info("Checking priority");
                    simulateWork("check");
                    
                    int value = state.get("validatedValue", 0);
                    String priority;
                    if (value > 80) {
                        priority = "high";
                    } else if (value > 40) {
                        priority = "normal";
                    } else {
                        priority = "low";
                    }
                    
                    return state
                            .with("step", "checked")
                            .with("priority", priority);
                })
                .addFunctionalNode(highPriorityNode, "High Priority Path", (state, ctx) -> {
                    logger.info("Processing high priority");
                    simulateWork("high-priority");
                    
                    return state
                            .with("step", "high-priority-processed")
                            .with("path", "high")
                            .with("result", "Urgent: Immediate action required");
                })
                .addFunctionalNode(normalPriorityNode, "Normal Priority Path", (state, ctx) -> {
                    logger.info("Processing normal priority");
                    simulateWork("normal-priority");
                    
                    return state
                            .with("step", "normal-priority-processed")
                            .with("path", "normal")
                            .with("result", "Standard: Process within normal timeframe");
                })
                .addFunctionalNode(lowPriorityNode, "Low Priority Path", (state, ctx) -> {
                    logger.info("Processing low priority");
                    simulateWork("low-priority");
                    
                    return state
                            .with("step", "low-priority-processed")
                            .with("path", "low")
                            .with("result", "Low: Queue for batch processing");
                })
                .addFunctionalNode(mergeNode, "Merge Results", (state, ctx) -> {
                    logger.info("Merging results");
                    simulateWork("merge");
                    
                    String path = state.get("path", "unknown");
                    String result = state.get("result", "");
                    
                    return state
                            .with("step", "merged")
                            .with("finalResult", Map.of(
                                    "path", path,
                                    "message", result,
                                    "processedAt", System.currentTimeMillis()
                            ));
                })
                .addFunctionalNode(endNode, "Format Output", (state, ctx) -> {
                    logger.info("Formatting output");
                    simulateWork("end");
                    
                    long startTime = state.get("startTime", 0L);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    return state
                            .with("step", "completed")
                            .with("status", "success")
                            .with("duration", duration);
                })
                .addDirectEdge(startNode, validateNode)
                .addDirectEdge(validateNode, checkNode)
                .addConditionalEdge(checkNode, highPriorityNode,
                        (state, ctx) -> "high".equals(state.get("priority", "")))
                .addConditionalEdge(checkNode, normalPriorityNode,
                        (state, ctx) -> "normal".equals(state.get("priority", "")))
                .addConditionalEdge(checkNode, lowPriorityNode,
                        (state, ctx) -> "low".equals(state.get("priority", "")))
                .addDirectEdge(highPriorityNode, mergeNode)
                .addDirectEdge(normalPriorityNode, mergeNode)
                .addDirectEdge(lowPriorityNode, mergeNode)
                .addDirectEdge(mergeNode, endNode)
                .entryPoint(startNode)
                .buildAndValidate();

        registry.registerGraph("conditional-workflow", graph);
    }

    private void createHumanApprovalGraph(GraphRegistry registry, HumanTaskManager taskManager) throws Exception {
        NodeId startNode = NodeId.of("start");
        NodeId prepareNode = NodeId.of("prepare");
        NodeId approvalNode = NodeId.of("approval");
        NodeId approvedNode = NodeId.of("approved");
        NodeId rejectedNode = NodeId.of("rejected");
        NodeId endNode = NodeId.of("end");

        HumanApprovalNode humanApproval = HumanApprovalNode.builder(approvalNode, "Request Approval", taskManager)
                .prompt("Please review and approve this request")
                .timeout(Duration.ofMinutes(10))
                .build();

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("human-approval"))
                .name("Human Approval Workflow")
                .metadata("description", "A workflow requiring human approval with preparation and post-processing")
                .metadata("color", "#FF9800")
                .addFunctionalNode(startNode, "Start", (state, ctx) -> {
                    logger.info("Starting approval workflow");
                    simulateWork("start");
                    
                    return state
                            .with("step", "started")
                            .with("requestId", ctx.executionId())
                            .with("startTime", System.currentTimeMillis());
                })
                .addFunctionalNode(prepareNode, "Prepare Request", (state, ctx) -> {
                    logger.info("Preparing approval request");
                    simulateWork("prepare");
                    
                    String requestData = state.get("requestData", "default-request");
                    
                    return state
                            .with("step", "prepared")
                            .with("preparedRequest", Map.of(
                                    "data", requestData,
                                    "requestedBy", "system",
                                    "requestedAt", System.currentTimeMillis()
                            ))
                            .with("needsApproval", true);
                })
                .addNode(humanApproval)
                .addFunctionalNode(approvedNode, "Process Approval", (state, ctx) -> {
                    logger.info("Processing approval");
                    simulateWork("approved");
                    
                    return state
                            .with("step", "approved")
                            .with("approvalStatus", "approved")
                            .with("approvedAt", System.currentTimeMillis());
                })
                .addFunctionalNode(rejectedNode, "Handle Rejection", (state, ctx) -> {
                    logger.info("Handling rejection");
                    simulateWork("rejected");
                    
                    String reason = state.get("reason", "No reason provided");
                    return state
                            .with("step", "rejected")
                            .with("approvalStatus", "rejected")
                            .with("rejectionReason", reason)
                            .with("rejectedAt", System.currentTimeMillis());
                })
                .addFunctionalNode(endNode, "Finalize", (state, ctx) -> {
                    logger.info("Finalizing workflow");
                    simulateWork("end");
                    
                    String status = state.get("approvalStatus", "unknown");
                    long startTime = state.get("startTime", 0L);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    return state
                            .with("step", "completed")
                            .with("finalStatus", status)
                            .with("duration", duration)
                            .with("completedAt", System.currentTimeMillis());
                })
                .addDirectEdge(startNode, prepareNode)
                .addDirectEdge(prepareNode, approvalNode)
                .addConditionalEdge(approvalNode, approvedNode,
                        (state, ctx) -> {
                            Boolean isApproved = state.get("__human_approved__", false);
                            return isApproved;
                        })
                .addConditionalEdge(approvalNode, rejectedNode,
                        (state, ctx) -> {
                            Boolean isApproved = state.get("__human_approved__", true);
                            return !isApproved;
                        })
                .addDirectEdge(approvedNode, endNode)
                .addDirectEdge(rejectedNode, endNode)
                .entryPoint(startNode)
                .buildAndValidate();

        registry.registerGraph("human-approval", graph);
    }

    private void createDataProcessingGraph(GraphRegistry registry) throws Exception {
        NodeId startNode = NodeId.of("start");
        NodeId extractNode = NodeId.of("extract");
        NodeId transformNode = NodeId.of("transform");
        NodeId validateNode = NodeId.of("validate");
        NodeId loadNode = NodeId.of("load");
        NodeId endNode = NodeId.of("end");

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("data-processing"))
                .name("Data Processing Pipeline")
                .metadata("description", "ETL pipeline with extraction, transformation, and loading")
                .metadata("color", "#9C27B0")
                .addFunctionalNode(startNode, "Initialize", (state, ctx) -> {
                    logger.info("Initializing data pipeline");
                    simulateWork("start");
                    
                    return state
                            .with("step", "initialized")
                            .with("pipelineId", ctx.executionId())
                            .with("startTime", System.currentTimeMillis());
                })
                .addFunctionalNode(extractNode, "Extract Data", (state, ctx) -> {
                    logger.info("Extracting data from source");
                    simulateWork("extract");
                    
                    return state
                            .with("step", "extracted")
                            .with("rawData", Map.of(
                                    "records", 1000,
                                    "source", "database",
                                    "extractedAt", System.currentTimeMillis()
                            ))
                            .with("recordCount", 1000);
                })
                .addFunctionalNode(transformNode, "Transform Data", (state, ctx) -> {
                    logger.info("Transforming data");
                    simulateWork("transform");
                    
                    int recordCount = state.get("recordCount", 0);
                    return state
                            .with("step", "transformed")
                            .with("transformedData", Map.of(
                                    "records", recordCount,
                                    "format", "normalized",
                                    "quality", "high"
                            ))
                            .with("transformations", 5);
                })
                .addFunctionalNode(validateNode, "Validate Quality", (state, ctx) -> {
                    logger.info("Validating data quality");
                    simulateWork("validate");
                    
                    return state
                            .with("step", "validated")
                            .with("qualityScore", 95)
                            .with("validationPassed", true);
                })
                .addFunctionalNode(loadNode, "Load to Target", (state, ctx) -> {
                    logger.info("Loading data to target");
                    simulateWork("load");
                    
                    int recordCount = state.get("recordCount", 0);
                    return state
                            .with("step", "loaded")
                            .with("loadedRecords", recordCount)
                            .with("target", "data-warehouse")
                            .with("loadedAt", System.currentTimeMillis());
                })
                .addFunctionalNode(endNode, "Finalize Pipeline", (state, ctx) -> {
                    logger.info("Finalizing pipeline");
                    simulateWork("end");
                    
                    long startTime = state.get("startTime", 0L);
                    long duration = System.currentTimeMillis() - startTime;
                    int recordCount = state.get("recordCount", 0);
                    
                    return state
                            .with("step", "completed")
                            .with("status", "success")
                            .with("summary", Map.of(
                                    "recordsProcessed", recordCount,
                                    "duration", duration,
                                    "qualityScore", 95
                            ))
                            .with("completedAt", System.currentTimeMillis());
                })
                .addDirectEdge(startNode, extractNode)
                .addDirectEdge(extractNode, transformNode)
                .addDirectEdge(transformNode, validateNode)
                .addDirectEdge(validateNode, loadNode)
                .addDirectEdge(loadNode, endNode)
                .entryPoint(startNode)
                .buildAndValidate();

        registry.registerGraph("data-processing", graph);
    }

    private void simulateWork(String nodeName) {
        try {
            logger.debug("Node '{}' is processing (simulating {} ms delay)", nodeName, NODE_EXECUTION_DELAY_MS);
            Thread.sleep(NODE_EXECUTION_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Node '{}' execution was interrupted", nodeName);
        }
    }
}
