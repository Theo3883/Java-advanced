package org.example.lab3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
public class Lab3Application {
    public static void main(String[] args) {
        SpringApplication.run(Lab3Application.class, args);
    }
}
