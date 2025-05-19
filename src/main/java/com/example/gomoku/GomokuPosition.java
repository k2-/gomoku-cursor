package com.example.gomoku;

/**
 * 五目並べの局面（ボード状態と手番）を表現するクラスです。
 */
public class GomokuPosition {
    private static final int BOARD_SIZE = 15;
    private final char[][] board;
    private char currentPlayer;
    
    // Unicode文字
    public static final char BLACK_STONE = '\u25CF'; // ● (黒石)
    public static final char WHITE_STONE = '\u25CB'; // ○ (白石)
    public static final char EMPTY = ' ';

    /**
     * 新しい局面を初期化します（空の盤面、黒番）。
     */
    public GomokuPosition() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        currentPlayer = BLACK_STONE;
    }

    /**
     * 既存の局面からコピーを作成します。
     * @param other コピー元の局面
     */
    public GomokuPosition(final GomokuPosition other) {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(other.board[i], 0, board[i], 0, BOARD_SIZE);
        }
        currentPlayer = other.currentPlayer;
    }

    /**
     * ボードをコンソールに表示します。
     */
    public void printBoard() {
        System.out.print("   ");
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.out.printf("%2d", i);
        }
        System.out.println();
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.out.printf("%2d |", i);
            for (int j = 0; j < BOARD_SIZE; j++) {
                System.out.print(" " + board[i][j]);
            }
            System.out.println(" |");
        }
    }

    /**
     * 指定した座標が有効な手かどうかを判定します。
     * @param row 行番号
     * @param col 列番号
     * @return 有効な手ならtrue
     */
    public boolean isValidMove(final int row, final int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] == EMPTY;
    }

    /**
     * 指定した座標に現在のプレイヤーの石を置きます。
     * @param row 行番号
     * @param col 列番号
     * @return 成功した場合true
     */
    public boolean makeMove(final int row, final int col) {
        if (!isValidMove(row, col)) {
            return false;
        }
        board[row][col] = currentPlayer;
        return true;
    }

    /**
     * 指定した座標で勝利条件を満たしているか判定します。
     * @param row 行番号
     * @param col 列番号
     * @return 勝利ならtrue
     */
    public boolean checkWinner(final int row, final int col) {
        int[][][] directions = {
            { {0, 1}, {0, -1} },    // 横
            { {1, 0}, {-1, 0} },    // 縦
            { {1, 1}, {-1, -1} },   // 右下がり斜め
            { {1, -1}, {-1, 1} }    // 左下がり斜め
        };
        for (int[][] dirPair : directions) {
            int count = 1;
            for (int[] dir : dirPair) {
                int r = row, c = col;
                while (true) {
                    r += dir[0];
                    c += dir[1];
                    if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                        break;
                    }
                    if (board[r][c] != currentPlayer) {
                        break;
                    }
                    count++;
                    if (count >= 5) {
                        return true;
                    }
                }
            }
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    /**
     * プレイヤーを交代します。
     */
    public void switchPlayer() {
        currentPlayer = (currentPlayer == BLACK_STONE) ? WHITE_STONE : BLACK_STONE;
    }

    /**
     * 有効な手のリストを返します。
     * @return 有効な手の配列（[行, 列]）
     */
    public int[][] getValidMoves() {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    count++;
                }
            }
        }
        int[][] moves = new int[count][2];
        int idx = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j)) {
                    moves[idx][0] = i;
                    moves[idx][1] = j;
                    idx++;
                }
            }
        }
        return moves;
    }

    /**
     * 指定した位置の評価値を計算します。
     * @param row 行番号
     * @param col 列番号
     * @param player 評価するプレイヤー
     * @return 評価値
     */
    public int evaluatePosition(final int row, final int col, final char player) {
        int[][][] directions = {
            { {0, 1}, {0, -1} },    // 横
            { {1, 0}, {-1, 0} },    // 縦
            { {1, 1}, {-1, -1} },   // 右下がり斜め
            { {1, -1}, {-1, 1} }    // 左下がり斜め
        };
        int score = 0;
        for (int[][] dirPair : directions) {
            int count = 1;
            int spaces = 0;
            for (int[] dir : dirPair) {
                int r = row, c = col;
                while (true) {
                    r += dir[0];
                    c += dir[1];
                    if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                        break;
                    }
                    if (board[r][c] == EMPTY) {
                        spaces++;
                        break;
                    }
                    if (board[r][c] != player) {
                        break;
                    }
                    count++;
                }
            }
            if (count >= 5) {
                score += 1000;
            } else if (count == 4 && spaces > 0) {
                score += 100;
            } else if (count == 3 && spaces > 0) {
                score += 10;
            } else if (count == 2 && spaces > 0) {
                score += 1;
            }
        }
        return score;
    }

    /**
     * 現在のプレイヤーを取得します。
     * @return 現在のプレイヤー
     */
    public char getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * 現在のプレイヤーを設定します。
     * @param player 設定するプレイヤー
     */
    public void setCurrentPlayer(final char player) {
        if (player == BLACK_STONE || player == WHITE_STONE) {
            currentPlayer = player;
        }
    }

    /**
     * 指定した座標に指定したプレイヤーの石を置きます。
     * @param row 行番号
     * @param col 列番号
     * @param player プレイヤー
     */
    public void setStone(final int row, final int col, final char player) {
        if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            board[row][col] = player;
        }
    }

    /**
     * 指定した座標の石を取得します。
     * @param row 行番号
     * @param col 列番号
     * @return 石（空白も含む）
     */
    public char getStone(final int row, final int col) {
        return (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) ? board[row][col] : EMPTY;
    }

    /**
     * ボードのサイズを取得します。
     * @return ボードのサイズ
     */
    public int getBoardSize() {
        return BOARD_SIZE;
    }
} 