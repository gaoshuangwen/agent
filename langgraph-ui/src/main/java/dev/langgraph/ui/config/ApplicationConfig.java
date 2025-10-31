package dev.langgraph.ui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langgraph.core.event.EventPublisher;
import dev.langgraph.core.human.HumanTaskManager;
import dev.langgraph.core.human.InMemoryHumanTaskStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public HumanTaskManager humanTaskManager() {
        return new HumanTaskManager(new InMemoryHumanTaskStore(), EventPublisher.noOp());
    }
}
