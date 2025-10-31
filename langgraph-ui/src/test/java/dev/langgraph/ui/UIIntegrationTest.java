package dev.langgraph.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UIIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUIHomepageLoads() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("LangGraph UI")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cytoscape")));
    }

    @Test
    void testJavaScriptAppLoads() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("React")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cytoscape")));
    }

    @Test
    void testRootRedirectsToIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
