package dev.langgraph.core.message;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;

import java.util.Map;

public abstract class MessageNode extends AbstractNode {

    protected MessageNode(NodeId id, String name, Map<String, Object> metadata) {
        super(id, name, metadata);
    }

    @Override
    protected final State doExecute(State inputState, ExecutionContext context) throws Exception {
        MessageContext messageContext = MessageContext.fromState(inputState, context);
        MessageContext resultContext = processMessage(messageContext);
        return resultContext.toState();
    }

    protected abstract MessageContext processMessage(MessageContext context) throws Exception;
}
