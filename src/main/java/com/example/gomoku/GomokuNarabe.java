package com.example.gomoku;

import java.util.Scanner;

/**
 * 五目並べゲームのコンソール版実装です。
 * ユーザーとコンピュータ間でプレイすることができます。
 */
public class GomokuNarabe {
    
    /**
     * コンソールベースの五目並べゲームを実行します。
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GomokuPosition position = new GomokuPosition();
        GomokuAI ai = new GomokuAI(GomokuAI.Level.MEDIUM);
        
        System.out.println("===== 五目並べ =====");
        System.out.println("プレイヤー: ● (黒)");
        System.out.println("コンピュータ: ○ (白)");
        System.out.println("コマンド: 'row col'（例: '7 7'）または 'quit'");
        System.out.println("===================");
        
        position.printBoard();
        
        while (true) {
            // プレイヤーの手番
            System.out.print("あなたの手を入力してください > ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("ゲームを終了します。");
                break;
            }
            
            try {
                String[] parts = input.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("無効な入力です。行と列を空白で区切って入力してください。");
                    continue;
                }
                
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                
                if (!position.isValidMove(row, col)) {
                    System.out.println("無効な手です。空いているマスを選んでください。");
                    continue;
                }
                
                position.makeMove(row, col);
                position.printBoard();
                
                // 勝敗判定
                if (position.checkWinner(row, col)) {
                    System.out.println("あなたの勝ちです！おめでとうございます！");
                    break;
                }
                
                // 次のプレイヤーに切り替え
                position.switchPlayer();
                
                // コンピュータの手番
                System.out.println("コンピュータが考えています...");
                
                int[] aiMove = ai.getBestMove(position);
                if (aiMove == null) {
                    System.out.println("引き分けです！");
                    break;
                }
                
                System.out.println("コンピュータの手: " + aiMove[0] + " " + aiMove[1]);
                position.makeMove(aiMove[0], aiMove[1]);
                position.printBoard();
                
                // 勝敗判定
                if (position.checkWinner(aiMove[0], aiMove[1])) {
                    System.out.println("コンピュータの勝ちです！残念...");
                    break;
                }
                
                // 次のプレイヤーに切り替え
                position.switchPlayer();
                
            } catch (NumberFormatException e) {
                System.out.println("無効な入力です。数字を入力してください。");
            }
        }
        
        scanner.close();
    }
} 