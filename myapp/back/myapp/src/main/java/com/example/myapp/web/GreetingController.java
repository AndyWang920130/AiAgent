package com.example.myapp.web;

import java.util.concurrent.atomic.AtomicLong;

import com.example.myapp.web.vm.Greeting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(defaultValue = "World") String name) {
        String message = String.format(template, name);
        return new Greeting(counter.incrementAndGet(), message);
    }
}