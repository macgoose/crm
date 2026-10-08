package com.spimex.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        // Keep H2 automatic mixed mode reachable for local tools on multi-homed Windows hosts.
        System.setProperty(
            "h2.bindAddress",
            System.getProperty("h2.bindAddress", "127.0.0.1")
        );

        SpringApplication.run(UserServiceApplication.class, args);
    }
}
