package com.example.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableAsync
@EnableWebSocket
public class ChessApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessApplication.class, args);
    }
}
