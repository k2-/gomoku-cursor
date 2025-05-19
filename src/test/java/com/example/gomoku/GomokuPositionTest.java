package com.example.gomoku;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * GomokuPositionクラスのテストケース
 */
public class GomokuPositionTest {
    
    @Test
    public void testInitialBoard() {
        GomokuPosition position = new GomokuPosition();
        assertEquals(GomokuPosition.BLACK_STONE, position.getCurrentPlayer());
        
        // 全てのマスが空であることを確認
        for (int i = 0; i < position.getBoardSize(); i++) {
            for (int j = 0; j < position.getBoardSize(); j++) {
                assertEquals(GomokuPosition.EMPTY, position.getStone(i, j));
            }
        }
    }
    
    @Test
    public void testMakeMove() {
        GomokuPosition position = new GomokuPosition();
        
        // 有効な手
        assertTrue(position.makeMove(7, 7));
        assertEquals(GomokuPosition.BLACK_STONE, position.getStone(7, 7));
        
        // 同じ場所に再度置けないこと
        assertFalse(position.makeMove(7, 7));
        
        // 盤面外に置けないこと
        assertFalse(position.makeMove(-1, 7));
        assertFalse(position.makeMove(7, -1));
        assertFalse(position.makeMove(15, 7));
        assertFalse(position.makeMove(7, 15));
    }
    
    @Test
    public void testSwitchPlayer() {
        GomokuPosition position = new GomokuPosition();
        assertEquals(GomokuPosition.BLACK_STONE, position.getCurrentPlayer());
        
        position.switchPlayer();
        assertEquals(GomokuPosition.WHITE_STONE, position.getCurrentPlayer());
        
        position.switchPlayer();
        assertEquals(GomokuPosition.BLACK_STONE, position.getCurrentPlayer());
    }
    
    @Test
    public void testSetPlayer() {
        GomokuPosition position = new GomokuPosition();
        
        position.setCurrentPlayer(GomokuPosition.WHITE_STONE);
        assertEquals(GomokuPosition.WHITE_STONE, position.getCurrentPlayer());
        
        position.setCurrentPlayer(GomokuPosition.BLACK_STONE);
        assertEquals(GomokuPosition.BLACK_STONE, position.getCurrentPlayer());
        
        // 不正な値は無視されること
        position.setCurrentPlayer('X');
        assertEquals(GomokuPosition.BLACK_STONE, position.getCurrentPlayer());
    }
    
    @Test
    public void testCheckWinnerHorizontal() {
        GomokuPosition position = new GomokuPosition();
        
        // 横に5つ並べる
        for (int i = 0; i < 5; i++) {
            position.makeMove(7, 3 + i);
            if (i < 4) {
                assertFalse(position.checkWinner(7, 3 + i));
                position.switchPlayer();
                position.makeMove(8, 3 + i);
                assertFalse(position.checkWinner(8, 3 + i));
                position.switchPlayer();
            }
        }
        
        // 最後の手で勝利
        assertTrue(position.checkWinner(7, 7));
    }
    
    @Test
    public void testCheckWinnerVertical() {
        GomokuPosition position = new GomokuPosition();
        
        // 縦に5つ並べる
        for (int i = 0; i < 5; i++) {
            position.makeMove(3 + i, 7);
            if (i < 4) {
                assertFalse(position.checkWinner(3 + i, 7));
                position.switchPlayer();
                position.makeMove(3 + i, 8);
                assertFalse(position.checkWinner(3 + i, 8));
                position.switchPlayer();
            }
        }
        
        // 最後の手で勝利
        assertTrue(position.checkWinner(7, 7));
    }
    
    @Test
    public void testCheckWinnerDiagonal1() {
        GomokuPosition position = new GomokuPosition();
        
        // 右下がりの斜めに5つ並べる
        for (int i = 0; i < 5; i++) {
            position.makeMove(3 + i, 3 + i);
            if (i < 4) {
                assertFalse(position.checkWinner(3 + i, 3 + i));
                position.switchPlayer();
                position.makeMove(3 + i, 8 - i);
                assertFalse(position.checkWinner(3 + i, 8 - i));
                position.switchPlayer();
            }
        }
        
        // 最後の手で勝利
        assertTrue(position.checkWinner(7, 7));
    }
    
    @Test
    public void testCheckWinnerDiagonal2() {
        GomokuPosition position = new GomokuPosition();
        
        // 左下がりの斜めに5つ並べる
        for (int i = 0; i < 5; i++) {
            position.makeMove(3 + i, 7 - i);
            if (i < 4) {
                assertFalse(position.checkWinner(3 + i, 7 - i));
                position.switchPlayer();
                position.makeMove(3 + i, i);
                assertFalse(position.checkWinner(3 + i, i));
                position.switchPlayer();
            }
        }
        
        // 最後の手で勝利
        assertTrue(position.checkWinner(7, 3));
    }
    
    @Test
    public void testGetValidMoves() {
        GomokuPosition position = new GomokuPosition();
        int boardSize = position.getBoardSize();
        
        // 初期状態では全てのマスが有効
        int[][] validMoves = position.getValidMoves();
        assertEquals(boardSize * boardSize, validMoves.length);
        
        // いくつかの手を打つ
        position.makeMove(7, 7);
        position.makeMove(7, 8);
        position.makeMove(8, 7);
        
        // 有効な手が減少していることを確認
        validMoves = position.getValidMoves();
        assertEquals(boardSize * boardSize - 3, validMoves.length);
        
        // すべての有効な手が本当に有効かチェック
        for (int[] move : validMoves) {
            assertTrue(position.isValidMove(move[0], move[1]));
        }
    }
    
    @Test
    public void testPositionCopy() {
        GomokuPosition original = new GomokuPosition();
        
        // いくつかの手を打つ
        original.makeMove(7, 7);
        original.switchPlayer();
        original.makeMove(7, 8);
        original.switchPlayer();
        original.makeMove(8, 7);
        
        // コピーを作成
        GomokuPosition copy = new GomokuPosition(original);
        
        // 同じ内容であることを確認
        assertEquals(original.getCurrentPlayer(), copy.getCurrentPlayer());
        
        for (int i = 0; i < original.getBoardSize(); i++) {
            for (int j = 0; j < original.getBoardSize(); j++) {
                assertEquals(original.getStone(i, j), copy.getStone(i, j));
            }
        }
        
        // コピーを変更しても元に影響しないことを確認
        copy.makeMove(8, 8);
        assertEquals(GomokuPosition.EMPTY, original.getStone(8, 8));
        assertEquals(copy.getCurrentPlayer(), original.getStone(8, 8));
    }
} 