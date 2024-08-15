package com.example.chess.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
@Slf4j
@Service
public class Play {
    @Async
    public void play(WebSocketSession u1, WebSocketSession u2) throws IOException, InterruptedException {
        log.debug("玩家1：{},玩家2：{}  开始对弈",u1.getRemoteAddress(),u2.getRemoteAddress());
        List<Map<String,Integer>> goRecord = new ArrayList<>();
        int[][] chessBoard = initChessBoard();
        int color = 0;
        ObjectMapper objectMapper = new ObjectMapper();
        WebSocketSession currentChessPlayer= u1;
        WebSocketSession currentWatcher = u2;
        while (true) {
             color = color%2;
            // 下棋
            currentChessPlayer.sendMessage(new TextMessage("[yourTurn]"));
            currentChessPlayer.getAttributes().put("currentChessPlayer",true);
            currentWatcher.getAttributes().put("currentChessPlayer",false);
            synchronized (currentChessPlayer) {
                // 等待下棋
                currentChessPlayer.wait(1000*30);
                // 判断是否悔棋
                Boolean regret = (Boolean) currentChessPlayer.getAttributes().get("repentance");
                if (regret!=null&&regret){
                    if (goRecord.size()>=2){
                        repentance(chessBoard,goRecord,currentChessPlayer,currentWatcher);
                        currentChessPlayer.getAttributes().remove("repentance");
                        continue;
                    }
                }
                // 若存在结果可能是认输或者作弊 直接进行胜负判定即可
                Boolean result = (Boolean) currentChessPlayer.getAttributes().get("result");
                if (result!=null){
                    sendResult(currentChessPlayer,result);
                    sendResult(currentWatcher,!result);
                    break;
                }
                result = (Boolean) currentWatcher.getAttributes().get("result");
                if (result!=null){
                    sendResult(currentWatcher,result);
                    sendResult(currentChessPlayer,!result);
                    break;
                }
                //没有结果则进行下棋
                Integer x = (Integer) currentChessPlayer.getAttributes().get("x");
                Integer y= (Integer) currentChessPlayer.getAttributes().get("y");
                //若没有下棋坐标，判定为超时，直接判负
                if (x==null||y==null){
                    currentChessPlayer.getAttributes().put("result",false);
                    currentChessPlayer.getAttributes().put("resultReason","超时");
                    sendResult(currentChessPlayer,false);
                    sendResult(currentWatcher,true);
                    break;
                }
                //清除本次下棋的坐标
                currentChessPlayer.getAttributes().remove("x");
                currentChessPlayer.getAttributes().remove("y");
                //行棋
                try {
                    playChess(chessBoard,x,y,color);
                    //记录本次行棋
                    Map<String,Integer> recordMap = new HashMap<>();
                    recordMap.put("x",x);
                    recordMap.put("y",y);
                    recordMap.put("color",color);
                    goRecord.add(recordMap);
                    //通知行棋
                    HashMap <String,Integer> map = new HashMap<>();
                    map.put("x",x);
                    map.put("y",y);
                    map.put("color",color);
                    currentWatcher.sendMessage(new TextMessage(objectMapper.writeValueAsString(map)));
                    currentChessPlayer.sendMessage(new TextMessage(objectMapper.writeValueAsString(map)));
                }catch (ArrayIndexOutOfBoundsException e){
                    currentChessPlayer.getAttributes().put("result",false);
                    currentChessPlayer.getAttributes().put("resultReason","坐标越界");
                    sendResult(currentChessPlayer,false);
                    sendResult(currentWatcher,true);
                    break;
                }catch (RuntimeException e){
                    currentChessPlayer.getAttributes().put("result",false);
                    currentChessPlayer.getAttributes().put("resultReason",e.getMessage());
                    sendResult(currentChessPlayer,false);
                    sendResult(currentWatcher,true);
                    break;
                }
                //判断本次行棋是否分出胜负
                if (isWin(chessBoard,x,y,color)){
                    sendResult(currentChessPlayer,true);
                    sendResult(currentWatcher,false);
                    break;
                }
            }
            color++;
            // 交换执棋人
            WebSocketSession temp = currentChessPlayer;
            currentChessPlayer = currentWatcher;
            currentWatcher = temp;
        }
    }
    int[][] initChessBoard(){
        int[][] ints = new int[15][15];
        for (int[] anInt : ints) {
            Arrays.fill(anInt, -1);
        }
        return ints;
    }
    void playChess (int[][] chessBoard,int x,int y,int color){
        if (chessBoard[x][y]!=-1){
            throw new RuntimeException("此处已有棋子，犯规判负");
        }
        try {
            chessBoard[x][y]=color;
        }catch (ArrayIndexOutOfBoundsException e){
            throw new RuntimeException("坐标越界，犯规判负");
        }
    }
    boolean isWin(int[][] chessBoard,int x,int y,int color){
        // 判断横向是否有五子相连
        int count = 1;
        for (int i = x-1; i >= 0; i--) {
            if (chessBoard[i][y]==color){
                count++;
            }else {
                break;
            }
        }
        for (int i = x+1; i < chessBoard.length; i++) {
            if (chessBoard[i][y]==color){
                count++;
            }else {
                break;
            }
        }
        if (count>=5){
            return true;
        }
        // 判断纵向是否有五子相连
        count = 1;
        for (int i = y-1; i >= 0; i--) {
            if (chessBoard[x][i]==color){
                count++;
            }else {
                break;
            }
        }
        for (int i = y+1; i < chessBoard[0].length; i++) {
            if (chessBoard[x][i]==color){
                count++;
            }else {
                break;
            }
        }
        if (count>=5){
            return true;
        }
        // 判断左上到右下是否有五子相连
        count = 1;
        for (int i = x-1,j=y-1; i >= 0 && j >= 0; i--,j--) {
            if (chessBoard[i][j]==color){
                count++;
            }else {
                break;
            }
        }
        for (int i = x+1,j=y+1; i < chessBoard.length && j < chessBoard[0].length; i++,j++) {
            if (chessBoard[i][j]==color){
                count++;
            }else {
                break;
            }
        }
        if (count>=5){
            return true;
        }
        // 判断右上到左下是否有五子相连
        count = 1;
        for (int i = x-1,j=y+1; i >= 0 && j < chessBoard[0].length; i--,j++) {
            if (chessBoard[i][j]==color){
                count++;
            }else {
                break;
            }
        }
        for (int i = x+1,j=y-1; i < chessBoard.length && j >= 0; i++,j--) {
            if (chessBoard[i][j]==color){
                count++;
            }else {
                break;
            }
        }
        return count >= 5;
    }
    void sendResult(WebSocketSession session,boolean result) throws IOException {
        session.sendMessage(new TextMessage(result?"[win]":"[lose]"));
        session.close();
    }
    void repentance(int[][] chessBoard,List<Map<String,Integer>> goRecord,WebSocketSession currentChessPlayer,WebSocketSession currentWatcher) throws IOException {
        Map<String,Integer> lastGo1 = goRecord.get(goRecord.size()-1);
        Map<String,Integer> lastGo2 = goRecord.get(goRecord.size()-2);
        chessBoard[lastGo1.get("x")][lastGo1.get("y")]=-1;
        chessBoard[lastGo2.get("x")][lastGo2.get("y")]=-1;
        goRecord.remove(goRecord.size()-1);
        goRecord.remove(goRecord.size()-1);
        // 通知悔棋
        currentChessPlayer.sendMessage(new TextMessage("[repentance],"+lastGo1.get("x")+","+lastGo1.get("y")+","+lastGo2.get("x")+","+lastGo2.get("y")));
        currentWatcher.sendMessage(new TextMessage("[repentance],"+lastGo1.get("x")+","+lastGo1.get("y")+","+lastGo2.get("x")+","+lastGo2.get("y")));
        //通知执棋人重新行棋
        currentChessPlayer.sendMessage(new TextMessage("[yourTurn]"));
    }
}
