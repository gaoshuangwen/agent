package com.langgraph.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LangGraphUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangGraphUiApplication.class, args);
    }
}
