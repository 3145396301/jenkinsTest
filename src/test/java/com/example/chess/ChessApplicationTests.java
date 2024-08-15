package com.example.chess;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChessApplicationTests {

    @Test
    void contextLoads() {
        int[][] ints = new int[9][9];
        for (int[] anInt : ints) {
            System.out.println(anInt);
        }
    }

}
