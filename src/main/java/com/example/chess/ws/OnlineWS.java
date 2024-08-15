package com.example.chess.ws;

import com.example.chess.service.Play;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
@Slf4j
public class OnlineWS implements WebSocketHandler {
    BlockingQueue<WebSocketSession> onlinePool = new ArrayBlockingQueue<>(1000);
    @Resource
    Play play;

    @PostConstruct
    public void init() {
        new Thread(() -> {
            log.info("开始匹配");
            while (true) {
                try {
                    WebSocketSession u1 = onlinePool.take();
                    if (!u1.isOpen()){
                        continue;
                    }
                    WebSocketSession u2 = onlinePool.take();
                    if (!u2.isOpen()){
                        onlinePool.offer(u1);
                        continue;
                    }
                    u1.getAttributes().put("currentChessPlayer", false);
                    u2.getAttributes().put("currentChessPlayer", false);
                    play.play(u1, u2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    // 将新连接的用户添加到在线用户池
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        onlinePool.offer(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        synchronized (session) {
            //若不是当前执棋人，判定为作弊，直接判负
            if (!(Boolean) session.getAttributes().get("currentChessPlayer")) {
                session.getAttributes().put("result", false);
                session.getAttributes().put("resultReason", "作弊");
            }
            String text = message.getPayload().toString();
            if ("[surrender]".equals(text)) {
                session.getAttributes().put("result", false);
                session.getAttributes().put("resultReason", "认输");
            } else if ("[repentance]".equals(text)){
                session.getAttributes().put("repentance",true);
                session.getAttributes().put("resultReason", "悔棋");
            }
            else{
                String[] coordinate = text.split(",");
                int x = Integer.parseInt(coordinate[0]);
                int y = Integer.parseInt(coordinate[1]);
                session.getAttributes().put("x", x);
                session.getAttributes().put("y", y);
            }
            session.notify();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        synchronized (session) {
            session.getAttributes().put("result", false);
            session.getAttributes().put("resultReason", "掉线");
            session.notify();
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
