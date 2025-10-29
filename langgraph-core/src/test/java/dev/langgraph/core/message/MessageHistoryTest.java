package dev.langgraph.core.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageHistoryTest {

    @Test
    void shouldCreateEmptyHistory() {
        MessageHistory history = MessageHistory.empty();
        
        assertTrue(history.isEmpty());
        assertEquals(0, history.size());
        assertEquals(Integer.MAX_VALUE, history.maxSize());
    }

    @Test
    void shouldCreateHistoryWithMessages() {
        List<Message> messages = List.of(
                Message.user("Hello"),
                Message.assistant("Hi there")
        );
        MessageHistory history = MessageHistory.of(messages);
        
        assertEquals(2, history.size());
        assertEquals(messages, history.getMessages());
    }

    @Test
    void shouldAddMessage() {
        MessageHistory history = MessageHistory.empty();
        Message message = Message.user("Hello");
        
        MessageHistory updated = history.add(message);
        
        assertEquals(0, history.size());
        assertEquals(1, updated.size());
        assertEquals(message, updated.getMessages().get(0));
    }

    @Test
    void shouldAddMultipleMessages() {
        MessageHistory history = MessageHistory.empty();
        List<Message> newMessages = List.of(
                Message.user("Message 1"),
                Message.user("Message 2")
        );
        
        MessageHistory updated = history.addAll(newMessages);
        
        assertEquals(2, updated.size());
    }

    @Test
    void shouldRespectMaxSize() {
        MessageHistory history = MessageHistory.withMaxSize(2);
        
        history = history.add(Message.user("1"));
        history = history.add(Message.user("2"));
        history = history.add(Message.user("3"));
        
        assertEquals(2, history.size());
        assertEquals("2", history.getMessages().get(0).content());
        assertEquals("3", history.getMessages().get(1).content());
    }

    @Test
    void shouldGetLastNMessages() {
        MessageHistory history = MessageHistory.of(List.of(
                Message.user("1"),
                Message.user("2"),
                Message.user("3"),
                Message.user("4")
        ));
        
        List<Message> last2 = history.getLastN(2);
        
        assertEquals(2, last2.size());
        assertEquals("3", last2.get(0).content());
        assertEquals("4", last2.get(1).content());
    }

    @Test
    void shouldGetLatestMessage() {
        MessageHistory history = MessageHistory.of(List.of(
                Message.user("First"),
                Message.user("Last")
        ));
        
        Message latest = history.getLatest().orElseThrow();
        
        assertEquals("Last", latest.content());
    }

    @Test
    void shouldReturnEmptyForLatestWhenEmpty() {
        MessageHistory history = MessageHistory.empty();
        
        assertTrue(history.getLatest().isEmpty());
    }

    @Test
    void shouldFilterByRole() {
        MessageHistory history = MessageHistory.of(List.of(
                Message.user("User 1"),
                Message.assistant("Assistant 1"),
                Message.user("User 2"),
                Message.assistant("Assistant 2")
        ));
        
        List<Message> userMessages = history.getByRole("user");
        List<Message> assistantMessages = history.getByRole("assistant");
        
        assertEquals(2, userMessages.size());
        assertEquals(2, assistantMessages.size());
    }

    @Test
    void shouldGetLatestByRole() {
        MessageHistory history = MessageHistory.of(List.of(
                Message.user("User 1"),
                Message.assistant("Assistant 1"),
                Message.user("User 2")
        ));
        
        Message latestUser = history.getLatestByRole("user").orElseThrow();
        Message latestAssistant = history.getLatestByRole("assistant").orElseThrow();
        
        assertEquals("User 2", latestUser.content());
        assertEquals("Assistant 1", latestAssistant.content());
    }

    @Test
    void shouldReturnEmptyForMissingRole() {
        MessageHistory history = MessageHistory.of(List.of(Message.user("Hello")));
        
        assertTrue(history.getLatestByRole("assistant").isEmpty());
    }

    @Test
    void shouldThrowExceptionForInvalidMaxSize() {
        assertThrows(IllegalArgumentException.class, () -> MessageHistory.withMaxSize(0));
        assertThrows(IllegalArgumentException.class, () -> MessageHistory.withMaxSize(-1));
    }
}
