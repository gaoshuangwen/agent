package dev.langgraph.core.message;

import dev.langgraph.core.NodeId;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class MessageRouterNode extends MessageNode {
    
    private final Function<MessageContext, String> router;

    public MessageRouterNode(NodeId id, String name, Map<String, Object> metadata,
                            Function<MessageContext, String> router) {
        super(id, name, metadata);
        this.router = Objects.requireNonNull(router, "Router function cannot be null");
    }

    @Override
    protected MessageContext processMessage(MessageContext context) {
        String route = router.apply(context);
        return context.withMetadata("route", route);
    }

    public String getRoute(MessageContext context) {
        return context.getMetadata("route", "");
    }
}
