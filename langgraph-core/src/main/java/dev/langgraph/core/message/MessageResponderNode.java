package dev.langgraph.core.message;

import dev.langgraph.core.NodeId;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class MessageResponderNode extends MessageNode {
    
    private final Function<MessageContext, String> responder;
    private final String responseRole;

    public MessageResponderNode(NodeId id, String name, Map<String, Object> metadata,
                               String responseRole, Function<MessageContext, String> responder) {
        super(id, name, metadata);
        this.responseRole = Objects.requireNonNull(responseRole, "Response role cannot be null");
        this.responder = Objects.requireNonNull(responder, "Responder function cannot be null");
    }

    public MessageResponderNode(NodeId id, String name, Function<MessageContext, String> responder) {
        this(id, name, Map.of(), "assistant", responder);
    }

    @Override
    protected MessageContext processMessage(MessageContext context) {
        String responseContent = responder.apply(context);
        Message response = Message.of(responseRole, responseContent);
        return context.addMessage(response);
    }
}
