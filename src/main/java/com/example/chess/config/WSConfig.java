package com.example.chess.config;

import com.example.chess.ws.OnlineWS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.File;

@Slf4j
@Component
public class WSConfig implements WebSocketConfigurer {
    @Resource
    private OnlineWS onlineWS;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("注册websocket服务");
        registry.addHandler(onlineWS,"/startOnline").setAllowedOrigins("*");

    }
}
