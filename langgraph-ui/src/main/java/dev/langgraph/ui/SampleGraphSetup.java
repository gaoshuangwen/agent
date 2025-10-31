package dev.langgraph.ui;

import dev.langgraph.core.Graph;
import dev.langgraph.core.GraphBuilder;
import dev.langgraph.core.GraphId;
import dev.langgraph.core.NodeId;
import dev.langgraph.core.human.HumanApprovalNode;
import dev.langgraph.core.human.HumanTaskManager;
import dev.langgraph.ui.service.GraphRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SampleGraphSetup {

    @Bean
    public CommandLineRunner setupSampleGraphs(GraphRegistry registry, HumanTaskManager taskManager) {
        return args -> {
            try {
                createSimpleSequentialGraph(registry);
                createConditionalGraph(registry);
                createHumanApprovalGraph(registry, taskManager);
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup sample graphs", e);
            }
        };
    }

    private void createSimpleSequentialGraph(GraphRegistry registry) throws Exception {
        NodeId start = NodeId.of("start");
        NodeId process = NodeId.of("process");
        NodeId end = NodeId.of("end");

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("simple-sequential"))
                .name("Simple Sequential Workflow")
                .metadata("description", "A simple three-step sequential workflow")
                .addFunctionalNode(start, "Start", (state, ctx) -> {
                    return state.with("step", "started")
                            .with("timestamp", System.currentTimeMillis());
                })
                .addFunctionalNode(process, "Process", (state, ctx) -> {
                    Object data = state.get("step").orElse("unknown");
                    return state.with("step", "processed")
                            .with("previousStep", data)
                            .with("processed", true);
                })
                .addFunctionalNode(end, "End", (state, ctx) -> {
                    return state.with("step", "completed")
                            .with("result", "success");
                })
                .addDirectEdge(start, process)
                .addDirectEdge(process, end)
                .entryPoint(start)
                .buildAndValidate();

        registry.registerGraph("simple-sequential", graph);
    }

    private void createConditionalGraph(GraphRegistry registry) throws Exception {
        NodeId start = NodeId.of("start");
        NodeId checkCondition = NodeId.of("check");
        NodeId pathA = NodeId.of("path-a");
        NodeId pathB = NodeId.of("path-b");
        NodeId merge = NodeId.of("merge");

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("conditional-workflow"))
                .name("Conditional Workflow")
                .metadata("description", "A workflow with conditional branching")
                .addFunctionalNode(start, "Start", (state, ctx) -> {
                    return state.with("value", Math.random() > 0.5 ? 10 : 5);
                })
                .addFunctionalNode(checkCondition, "Check Condition", (state, ctx) -> {
                    return state.with("checked", true);
                })
                .addFunctionalNode(pathA, "Path A (High Value)", (state, ctx) -> {
                    return state.with("path", "A").with("priority", "high");
                })
                .addFunctionalNode(pathB, "Path B (Low Value)", (state, ctx) -> {
                    return state.with("path", "B").with("priority", "low");
                })
                .addFunctionalNode(merge, "Merge", (state, ctx) -> {
                    return state.with("merged", true).with("final", true);
                })
                .addDirectEdge(start, checkCondition)
                .addConditionalEdge(checkCondition, pathA, 
                        (state, ctx) -> {
                            Integer value = state.get("value", 0);
                            return value > 7;
                        })
                .addConditionalEdge(checkCondition, pathB,
                        (state, ctx) -> {
                            Integer value = state.get("value", 0);
                            return value <= 7;
                        })
                .addDirectEdge(pathA, merge)
                .addDirectEdge(pathB, merge)
                .entryPoint(start)
                .buildAndValidate();

        registry.registerGraph("conditional-workflow", graph);
    }

    private void createHumanApprovalGraph(GraphRegistry registry, HumanTaskManager taskManager) throws Exception {
        NodeId start = NodeId.of("start");
        NodeId approval = NodeId.of("approval");
        NodeId approved = NodeId.of("approved");
        NodeId rejected = NodeId.of("rejected");

        HumanApprovalNode approvalNode = HumanApprovalNode.builder(approval, "Approval Required", taskManager)
                .prompt("Please review and approve this workflow")
                .timeout(Duration.ofMinutes(10))
                .build();

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("human-approval"))
                .name("Human Approval Workflow")
                .metadata("description", "A workflow requiring human approval")
                .addFunctionalNode(start, "Start", (state, ctx) -> {
                    return state.with("request", "approval-needed")
                            .with("requestedBy", "system");
                })
                .addNode(approvalNode)
                .addFunctionalNode(approved, "Approved", (state, ctx) -> {
                    return state.with("status", "approved")
                            .with("completedAt", System.currentTimeMillis());
                })
                .addFunctionalNode(rejected, "Rejected", (state, ctx) -> {
                    return state.with("status", "rejected")
                            .with("reason", "User rejected");
                })
                .addDirectEdge(start, approval)
                .addConditionalEdge(approval, approved,
                        (state, ctx) -> {
                            Boolean isApproved = state.get("__human_approved__", false);
                            return isApproved;
                        })
                .addConditionalEdge(approval, rejected,
                        (state, ctx) -> {
                            Boolean isApproved = state.get("__human_approved__", true);
                            return !isApproved;
                        })
                .entryPoint(start)
                .buildAndValidate();

        registry.registerGraph("human-approval", graph);
    }
}
