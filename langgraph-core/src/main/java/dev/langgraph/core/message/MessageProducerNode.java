package dev.langgraph.core.message;

import dev.langgraph.core.NodeId;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class MessageProducerNode extends MessageNode {
    
    private final Function<MessageContext, Message> producer;

    public MessageProducerNode(NodeId id, String name, Map<String, Object> metadata,
                              Function<MessageContext, Message> producer) {
        super(id, name, metadata);
        this.producer = Objects.requireNonNull(producer, "Producer function cannot be null");
    }

    @Override
    protected MessageContext processMessage(MessageContext context) {
        Message message = producer.apply(context);
        return context.addMessage(message);
    }
}
