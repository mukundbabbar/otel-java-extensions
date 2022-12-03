package com.mulabs.java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

    @RequestMapping(value = {"","/"})
    public String home() {
    	System.out.println("Application:Home");
        return "Hello World";
    }

    public static void main(String[] args) {
    	System.out.println("Application:Main");
        SpringApplication.run(Application.class, args);
    }

}
