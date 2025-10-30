package com.langgraph.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebUIController {
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
