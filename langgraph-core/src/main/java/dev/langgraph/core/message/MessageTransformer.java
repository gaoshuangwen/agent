package dev.langgraph.core.message;

import java.util.List;

@FunctionalInterface
public interface MessageTransformer {
    
    List<Message> transform(List<Message> messages, MessageContext context);
    
    static MessageTransformer identity() {
        return (messages, context) -> messages;
    }
    
    static MessageTransformer limitToLast(int n) {
        return (messages, context) -> {
            if (messages.size() <= n) {
                return messages;
            }
            return messages.subList(messages.size() - n, messages.size());
        };
    }
    
    static MessageTransformer filterByRole(String role) {
        return (messages, context) -> messages.stream()
                .filter(m -> m.role().equals(role))
                .toList();
    }
    
    default MessageTransformer andThen(MessageTransformer after) {
        return (messages, context) -> after.transform(transform(messages, context), context);
    }
}
