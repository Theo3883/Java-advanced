package org.example.lab2.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The greeting message is injected from application.properties using @Value.
 */
@RestController
public class HelloController {

    // @Value reads the property at startup; if missing, falls back to the default string
    @Value("${app.greeting:Hello, World!}")
    private String greeting;

    @GetMapping("/hello")
    public String hello() {
        return greeting;
    }
}
