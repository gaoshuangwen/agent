package dev.langgraph.core.message;

import dev.langgraph.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class MessageGraphBuilder {
    
    private final GraphBuilder graphBuilder;
    private final Map<String, NodeId> nodeRegistry = new HashMap<>();

    private MessageGraphBuilder(GraphBuilder graphBuilder) {
        this.graphBuilder = Objects.requireNonNull(graphBuilder);
    }

    public static MessageGraphBuilder newMessageGraph(String name) {
        return new MessageGraphBuilder(GraphBuilder.newGraph(name));
    }

    public MessageGraphBuilder addProducer(String nodeKey, String nodeName, 
                                          Function<MessageContext, Message> producer) {
        NodeId nodeId = NodeId.of(nodeKey);
        MessageProducerNode node = new MessageProducerNode(nodeId, nodeName, Map.of(), producer);
        graphBuilder.addNode(node);
        nodeRegistry.put(nodeKey, nodeId);
        return this;
    }

    public MessageGraphBuilder addResponder(String nodeKey, String nodeName,
                                           Function<MessageContext, String> responder) {
        NodeId nodeId = NodeId.of(nodeKey);
        MessageResponderNode node = new MessageResponderNode(nodeId, nodeName, responder);
        graphBuilder.addNode(node);
        nodeRegistry.put(nodeKey, nodeId);
        return this;
    }

    public MessageGraphBuilder addResponder(String nodeKey, String nodeName, String responseRole,
                                           Function<MessageContext, String> responder) {
        NodeId nodeId = NodeId.of(nodeKey);
        MessageResponderNode node = new MessageResponderNode(nodeId, nodeName, Map.of(), responseRole, responder);
        graphBuilder.addNode(node);
        nodeRegistry.put(nodeKey, nodeId);
        return this;
    }

    public MessageGraphBuilder addRouter(String nodeKey, String nodeName,
                                         Function<MessageContext, String> router) {
        NodeId nodeId = NodeId.of(nodeKey);
        MessageRouterNode node = new MessageRouterNode(nodeId, nodeName, Map.of(), router);
        graphBuilder.addNode(node);
        nodeRegistry.put(nodeKey, nodeId);
        return this;
    }

    public MessageGraphBuilder addTransformer(String nodeKey, String nodeName,
                                             MessageTransformer transformer) {
        NodeId nodeId = NodeId.of(nodeKey);
        MessageTransformNode node = new MessageTransformNode(nodeId, nodeName, Map.of(), transformer);
        graphBuilder.addNode(node);
        nodeRegistry.put(nodeKey, nodeId);
        return this;
    }

    public MessageGraphBuilder addEdge(String fromKey, String toKey) {
        NodeId from = getNodeId(fromKey);
        NodeId to = getNodeId(toKey);
        graphBuilder.addDirectEdge(from, to);
        return this;
    }

    public MessageGraphBuilder addConditionalEdge(String fromKey, String toKey,
                                                  BiPredicate<State, ExecutionContext> condition) {
        NodeId from = getNodeId(fromKey);
        NodeId to = getNodeId(toKey);
        graphBuilder.addConditionalEdge(from, to, condition);
        return this;
    }

    public MessageGraphBuilder addRouteEdge(String fromRouterKey, String routeName, String toKey) {
        NodeId from = getNodeId(fromRouterKey);
        NodeId to = getNodeId(toKey);
        graphBuilder.addConditionalEdge(from, to, (state, ctx) -> {
            MessageContext msgCtx = MessageContext.fromState(state, ctx);
            String route = msgCtx.getMetadata("route", "");
            return route.equals(routeName);
        });
        return this;
    }

    public MessageGraphBuilder entryPoint(String nodeKey) {
        NodeId nodeId = getNodeId(nodeKey);
        graphBuilder.entryPoint(nodeId);
        return this;
    }

    public MessageGraphBuilder metadata(String key, Object value) {
        graphBuilder.metadata(key, value);
        return this;
    }

    public Graph build() {
        return graphBuilder.build();
    }

    public Graph buildAndValidate() throws GraphValidationException {
        return graphBuilder.buildAndValidate();
    }

    private NodeId getNodeId(String key) {
        NodeId nodeId = nodeRegistry.get(key);
        if (nodeId == null) {
            throw new IllegalArgumentException("Node not found: " + key);
        }
        return nodeId;
    }
}
