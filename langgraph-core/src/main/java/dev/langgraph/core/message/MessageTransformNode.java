package dev.langgraph.core.message;

import dev.langgraph.core.NodeId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MessageTransformNode extends MessageNode {
    
    private final MessageTransformer transformer;

    public MessageTransformNode(NodeId id, String name, Map<String, Object> metadata,
                                MessageTransformer transformer) {
        super(id, name, metadata);
        this.transformer = Objects.requireNonNull(transformer, "Transformer cannot be null");
    }

    @Override
    protected MessageContext processMessage(MessageContext context) {
        List<Message> transformed = transformer.transform(context.history().getMessages(), context);
        return context.withHistory(MessageHistory.of(transformed));
    }
}
