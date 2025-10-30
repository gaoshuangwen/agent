package dev.langgraph.core.message;

import java.util.*;
import java.util.stream.Collectors;

public final class MessageHistory {
    
    private final List<Message> messages;
    private final int maxSize;

    private MessageHistory(List<Message> messages, int maxSize) {
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.maxSize = maxSize;
    }

    public static MessageHistory empty() {
        return new MessageHistory(List.of(), Integer.MAX_VALUE);
    }

    public static MessageHistory of(List<Message> messages) {
        return new MessageHistory(messages, Integer.MAX_VALUE);
    }

    public static MessageHistory withMaxSize(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("Max size must be at least 1");
        }
        return new MessageHistory(List.of(), maxSize);
    }

    public MessageHistory add(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        List<Message> newMessages = new ArrayList<>(messages);
        newMessages.add(message);
        
        if (newMessages.size() > maxSize) {
            newMessages = newMessages.subList(newMessages.size() - maxSize, newMessages.size());
        }
        
        return new MessageHistory(newMessages, maxSize);
    }

    public MessageHistory addAll(List<Message> newMessages) {
        Objects.requireNonNull(newMessages, "Messages cannot be null");
        List<Message> combined = new ArrayList<>(messages);
        combined.addAll(newMessages);
        
        if (combined.size() > maxSize) {
            combined = combined.subList(combined.size() - maxSize, combined.size());
        }
        
        return new MessageHistory(combined, maxSize);
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<Message> getLastN(int n) {
        if (n <= 0) {
            return List.of();
        }
        if (n >= messages.size()) {
            return messages;
        }
        return messages.subList(messages.size() - n, messages.size());
    }

    public List<Message> getByRole(String role) {
        return messages.stream()
                .filter(m -> m.role().equals(role))
                .collect(Collectors.toList());
    }

    public Optional<Message> getLatest() {
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(messages.size() - 1));
    }

    public Optional<Message> getLatestByRole(String role) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.role().equals(role)) {
                return Optional.of(msg);
            }
        }
        return Optional.empty();
    }

    public int size() {
        return messages.size();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public int maxSize() {
        return maxSize;
    }

    @Override
    public String toString() {
        return "MessageHistory{size=" + messages.size() + ", maxSize=" + maxSize + "}";
    }
}
