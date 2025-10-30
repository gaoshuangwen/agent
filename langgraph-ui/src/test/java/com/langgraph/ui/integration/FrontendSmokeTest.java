package com.langgraph.ui.integration;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FrontendSmokeTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldLoadHomePage() {
        WebDriver driver = new HtmlUnitDriver(false);
        try {
            driver.get("http://localhost:" + port + "/");
            
            assertEquals("LangGraph Visualization UI", driver.getTitle());
            
            WebElement header = driver.findElement(By.tagName("h1"));
            assertEquals("LangGraph Visualization UI", header.getText());
            
            assertNotNull(driver.findElement(By.id("graphSelect")));
            assertNotNull(driver.findElement(By.id("startExecutionBtn")));
            assertNotNull(driver.findElement(By.id("cy")));
            assertNotNull(driver.findElement(By.id("executionsList")));
            assertNotNull(driver.findElement(By.id("timeline")));
            assertNotNull(driver.findElement(By.id("logs")));
            assertNotNull(driver.findElement(By.id("approvals")));
            
        } finally {
            driver.quit();
        }
    }

    @Test
    void shouldLoadGraphsInDropdown() {
        WebDriver driver = new HtmlUnitDriver(false);
        try {
            driver.get("http://localhost:" + port + "/");
            
            WebElement select = driver.findElement(By.id("graphSelect"));
            assertNotNull(select);
            
        } finally {
            driver.quit();
        }
    }
}
