package com.example.gomoku;

package com.example.gomoku;

import java.util.HashMap;
import java.util.Map;

/**
 * 五目並べのAIを実装するクラスです。
 * 複数の難易度レベルと異なるアルゴリズムをサポートします。
 */
public class GomokuAI {
    // 難易度レベル
    public enum Level {
        EASY,    // 簡単（ランダム+基本評価）
        MEDIUM,  // 中級（アルファベータ2手読み）
        HARD     // 難しい（アルファベータ4手読み + 効率化）
    }
    
    // デフォルトの探索深さ
    private static final int DEFAULT_DEPTH_EASY = 1;
    private static final int DEFAULT_DEPTH_MEDIUM = 2;
    private static final int DEFAULT_DEPTH_HARD = 3;  // 4から3に削減
    
    // 評価スコア定数
    private static final int SCORE_FIVE = 100000;  // 5連（勝利）
    private static final int SCORE_OPEN_FOUR = 10000;  // 両端空きの4連
    private static final int SCORE_FOUR = 1000;    // 片端空きの4連
    private static final int SCORE_OPEN_THREE = 500;  // 両端空きの3連
    private static final int SCORE_THREE = 100;    // 片端空きの3連
    private static final int SCORE_OPEN_TWO = 50;  // 両端空きの2連
    private static final int SCORE_TWO = 10;       // 片端空きの2連
    private static final int SCORE_CENTER = 3;     // 中央に近い位置の評価
    
    // 探索範囲の制限
    private static final int MAX_MOVES_TO_CONSIDER = 15;
    private static final int VICINITY_RANGE = 2;
    
    // トランスポジションテーブル（探索結果キャッシュ）
    private Map<String, Integer> transpositionTable;
    
    // ショートカット用定数
    private static final char BLACK_STONE = GomokuPosition.BLACK_STONE;
    private static final char WHITE_STONE = GomokuPosition.WHITE_STONE;
    private static final char EMPTY = GomokuPosition.EMPTY;
    
    private final Level level;
    private int maxDepth;
    
    /**
     * コンストラクタ。
     * @param level 難易度レベル
     */
    public GomokuAI(Level level) {
        this.level = level;
        setDepthByLevel(level);
        this.transpositionTable = new HashMap<>();
    }
    
    /**
     * レベルに応じた探索深さを設定します。
     * @param level 難易度レベル
     */
    private void setDepthByLevel(Level level) {
        switch (level) {
            case EASY:
                maxDepth = DEFAULT_DEPTH_EASY;
                break;
            case MEDIUM:
                maxDepth = DEFAULT_DEPTH_MEDIUM;
                break;
            case HARD:
                maxDepth = DEFAULT_DEPTH_HARD;
                break;
            default:
                maxDepth = DEFAULT_DEPTH_EASY;
        }
    }
    
    /**
     * 最善手を取得します。
     * @param position 現在の局面
     * @return 最善手 [行, 列]
     */
    public int[] getBestMove(GomokuPosition position) {
        // 計算開始前にキャッシュをクリア
        if (level == Level.HARD) {
            transpositionTable.clear();
        }
        
        char currentPlayer = position.getCurrentPlayer();
        int[][] validMoves = position.getValidMoves();
        
        if (validMoves.length == 0) {
            return null;
        }
        
        // ボードの中央にまだ石がなければ、中央に置く（初手など）
        int boardSize = position.getBoardSize();
        int center = boardSize / 2;
        if (position.getStone(center, center) == EMPTY) {
            return new int[] {center, center};
        }
        
        // 難易度に応じたアルゴリズムの選択
        int[] bestMove;
        if (level == Level.EASY) {
            bestMove = getEasyMove(position, currentPlayer);
        } else if (level == Level.MEDIUM) {
            bestMove = getMediumMove(position, currentPlayer);
        } else {
            bestMove = getHardMove(position, currentPlayer);
        }
        
        return bestMove;
    }
    
    /**
     * 簡単なレベルでの手を取得します（ランダム+簡易評価）。
     * @param position 局面
     * @param player プレイヤー
     * @return 選択された手
     */
    private int[] getEasyMove(GomokuPosition position, char player) {
        int[][] validMoves = position.getValidMoves();
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        
        // 各手の評価値を計算し、最も高い手を選択
        for (int[] move : validMoves) {
            int row = move[0];
            int col = move[1];
            
            GomokuPosition newPos = new GomokuPosition(position);
            newPos.setStone(row, col, player);
            
            int score = evaluateMove(newPos, row, col, player);
            
            // ランダム性を加える（同じ評価値の場合ランダムに選択）
            if (score > bestScore || (score == bestScore && Math.random() > 0.5)) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove;
    }
    
    /**
     * 中級レベルでの手を取得します（標準的なアルファベータ）。
     * @param position 局面
     * @param player プレイヤー
     * @return 選択された手
     */
    private int[] getMediumMove(GomokuPosition position, char player) {
        return getAlphaBetaMove(position, player, false);
    }
    
    /**
     * 上級レベルでの手を取得します（効率化されたアルファベータ）。
     * @param position 局面
     * @param player プレイヤー
     * @return 選択された手
     */
    private int[] getHardMove(GomokuPosition position, char player) {
        return getAlphaBetaMove(position, player, true);
    }
    
    /**
     * アルファベータ法による最善手を取得します。
     * @param position 局面
     * @param player プレイヤー
     * @param useOptimizations 最適化技術を使用するか
     * @return 最善手
     */
    private int[] getAlphaBetaMove(GomokuPosition position, char player, boolean useOptimizations) {
        int[][] validMoves = getSmartValidMoves(position);
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        
        char opponent = (player == BLACK_STONE) ? WHITE_STONE : BLACK_STONE;
        
        // 優先的に探索する手を特定し、考慮する手の数を制限
        int[][] priorityMoves = getPriorityMoves(position, validMoves);
        
        // 効率化のために考慮する手の数を制限
        int movesToConsider = Math.min(priorityMoves.length, useOptimizations ? MAX_MOVES_TO_CONSIDER : priorityMoves.length);
        
        // 各手についてアルファベータ探索を実行
        for (int i = 0; i < movesToConsider; i++) {
            int[] move = priorityMoves[i];
            int row = move[0];
            int col = move[1];
            
            GomokuPosition newPos = new GomokuPosition(position);
            newPos.setStone(row, col, player);
            
            // 即座に勝利する手があれば、それを選択
            if (newPos.checkWinner(row, col)) {
                return move;
            }
            
            // 相手が次に勝てる手があれば、それをブロック
            if (hasForcedMove(position, opponent)) {
                int[] forcedMove = getForcedMove(position, opponent);
                if (forcedMove != null) {
                    return forcedMove;
                }
            }
            
            // アルファベータ探索
            int score = alphaBeta(newPos, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, player, opponent, useOptimizations);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove;
    }
    
    /**
     * 相手の必勝手があるかチェックします。
     * @param position 局面
     * @param opponent 相手
     * @return 必勝手があればtrue
     */
    private boolean hasForcedMove(GomokuPosition position, char opponent) {
        // 有効な手を取得
        int[][] validMoves = position.getValidMoves();
        
        // 各手について相手の勝利チェック
        for (int[] move : validMoves) {
            GomokuPosition testPos = new GomokuPosition(position);
            testPos.setStone(move[0], move[1], opponent);
            if (testPos.checkWinner(move[0], move[1])) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 相手の必勝手を取得します。
     * @param position 局面
     * @param opponent 相手
     * @return 必勝手、なければnull
     */
    private int[] getForcedMove(GomokuPosition position, char opponent) {
        // 有効な手を取得
        int[][] validMoves = position.getValidMoves();
        
        // 各手について相手の勝利チェック
        for (int[] move : validMoves) {
            GomokuPosition testPos = new GomokuPosition(position);
            testPos.setStone(move[0], move[1], opponent);
            if (testPos.checkWinner(move[0], move[1])) {
                return move;
            }
        }
        
        return null;
    }
    
    /**
     * 探索効率向上のために、石がある周辺のみの有効な手を取得します。
     * @param position 局面
     * @return 絞り込まれた有効な手
     */
    private int[][] getSmartValidMoves(GomokuPosition position) {
        int boardSize = position.getBoardSize();
        boolean[][] consideredCells = new boolean[boardSize][boardSize];
        int count = 0;
        
        // 既存の石の周囲のセルを検討対象とする
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (position.getStone(i, j) != EMPTY) {
                    // 石の周囲を検討対象に追加
                    for (int di = -VICINITY_RANGE; di <= VICINITY_RANGE; di++) {
                        for (int dj = -VICINITY_RANGE; dj <= VICINITY_RANGE; dj++) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < boardSize && nj >= 0 && nj < boardSize && 
                                position.getStone(ni, nj) == EMPTY && !consideredCells[ni][nj]) {
                                consideredCells[ni][nj] = true;
                                count++;
                            }
                        }
                    }
                }
            }
        }
        
        // 結果の配列を作成
        int[][] smartMoves = new int[count][2];
        int index = 0;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (consideredCells[i][j]) {
                    smartMoves[index][0] = i;
                    smartMoves[index][1] = j;
                    index++;
                }
            }
        }
        
        return smartMoves;
    }
    
    /**
     * 優先的に探索すべき手を取得します。
     * （石の近くの手を優先的に評価）
     * @param position 局面
     * @param validMoves 有効な手のリスト
     * @return 優先順位付けされた手のリスト
     */
    private int[][] getPriorityMoves(GomokuPosition position, int[][] validMoves) {
        // 評価値と手の組み合わせを格納する配列
        int[][] scoreAndMoves = new int[validMoves.length][3]; // [スコア, 行, 列]
        
        for (int i = 0; i < validMoves.length; i++) {
            int row = validMoves[i][0];
            int col = validMoves[i][1];
            
            // 周囲の石の数を数えて評価
            int score = evaluateMoveProximity(position, row, col);
            
            scoreAndMoves[i][0] = score;
            scoreAndMoves[i][1] = row;
            scoreAndMoves[i][2] = col;
        }
        
        // スコア降順でソート
        java.util.Arrays.sort(scoreAndMoves, (a, b) -> Integer.compare(b[0], a[0]));
        
        // 結果を[行, 列]形式に変換
        int[][] result = new int[validMoves.length][2];
        for (int i = 0; i < validMoves.length; i++) {
            result[i][0] = scoreAndMoves[i][1];
            result[i][1] = scoreAndMoves[i][2];
        }
        
        return result;
    }
    
    /**
     * 手の周囲の石の数を評価します（探索効率化のため）。
     * @param position 局面
     * @param row 行
     * @param col 列
     * @return 評価値
     */
    private int evaluateMoveProximity(GomokuPosition position, int row, int col) {
        int boardSize = position.getBoardSize();
        int score = 0;
        int range = 2; // 探索範囲
        
        // 中央への近さのボーナス
        int centerDistance = Math.abs(row - boardSize / 2) + Math.abs(col - boardSize / 2);
        score += Math.max(0, boardSize - centerDistance) * SCORE_CENTER;
        
        // 周囲の石を評価
        for (int i = Math.max(0, row - range); i <= Math.min(boardSize - 1, row + range); i++) {
            for (int j = Math.max(0, col - range); j <= Math.min(boardSize - 1, col + range); j++) {
                if (position.getStone(i, j) != EMPTY) {
                    // 距離に応じた重み付け
                    int distance = Math.abs(row - i) + Math.abs(col - j);
                    if (distance <= range) {
                        score += (range - distance + 1) * 10;
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * 局面のハッシュキーを生成します（トランスポジションテーブル用）。
     * @param position 局面
     * @param isMaximizing 最大化ノードか
     * @param depth 探索深さ
     * @return ハッシュキー
     */
    private String getPositionKey(GomokuPosition position, boolean isMaximizing, int depth) {
        StringBuilder sb = new StringBuilder();
        int boardSize = position.getBoardSize();
        
        // 盤面状態をエンコード
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                sb.append(position.getStone(i, j));
            }
        }
        
        // 現在のプレイヤーと探索深さを追加
        sb.append(isMaximizing ? "M" : "m").append(depth);
        
        return sb.toString();
    }
    
    /**
     * アルファベータ探索を実行します。
     * @param position 局面
     * @param depth 残り探索深さ
     * @param alpha アルファ値
     * @param beta ベータ値
     * @param isMaximizing 最大化ノードか
     * @param player 自分の石
     * @param opponent 相手の石
     * @param useOptimizations 最適化を使用するか
     * @return 評価値
     */
    private int alphaBeta(GomokuPosition position, int depth, int alpha, int beta, boolean isMaximizing,
                         char player, char opponent, boolean useOptimizations) {
        
        // トランスポジションテーブルのチェック
        if (useOptimizations && level == Level.HARD) {
            String key = getPositionKey(position, isMaximizing, depth);
            if (transpositionTable.containsKey(key)) {
                return transpositionTable.get(key);
            }
        }
        
        // 終了条件：探索深さに達した場合は盤面評価
        if (depth == 0) {
            int score = evaluateBoard(position, player, opponent);
            
            // スコアをキャッシュ
            if (useOptimizations && level == Level.HARD) {
                String key = getPositionKey(position, isMaximizing, depth);
                transpositionTable.put(key, score);
            }
            
            return score;
        }
        
        // スマートに絞り込まれた有効な手
        int[][] validMoves = useOptimizations ? 
                            getSmartValidMoves(position) : 
                            position.getValidMoves();
        
        if (validMoves.length == 0) {
            return 0; // 引き分け
        }
        
        // 優先順位付けされた手リスト
        int[][] priorityMoves = getPriorityMoves(position, validMoves);
        
        // 効率化のために考慮する手の数を制限
        int movesToConsider = Math.min(priorityMoves.length, useOptimizations ? MAX_MOVES_TO_CONSIDER : priorityMoves.length);
        
        int bestScore;
        
        if (isMaximizing) {
            bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < movesToConsider; i++) {
                int[] move = priorityMoves[i];
                GomokuPosition newPos = new GomokuPosition(position);
                newPos.setStone(move[0], move[1], player);
                
                // 勝利チェック
                if (newPos.checkWinner(move[0], move[1])) {
                    bestScore = SCORE_FIVE;
                    break;
                }
                
                int score = alphaBeta(newPos, depth - 1, alpha, beta, false, player, opponent, useOptimizations);
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, score);
                
                // アルファベータ枝刈り
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < movesToConsider; i++) {
                int[] move = priorityMoves[i];
                GomokuPosition newPos = new GomokuPosition(position);
                newPos.setStone(move[0], move[1], opponent);
                
                // 勝利チェック
                if (newPos.checkWinner(move[0], move[1])) {
                    bestScore = -SCORE_FIVE;
                    break;
                }
                
                int score = alphaBeta(newPos, depth - 1, alpha, beta, true, player, opponent, useOptimizations);
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, score);
                
                // アルファベータ枝刈り
                if (beta <= alpha) {
                    break;
                }
            }
        }
        
        // スコアをキャッシュ
        if (useOptimizations && level == Level.HARD) {
            String key = getPositionKey(position, isMaximizing, depth);
            transpositionTable.put(key, bestScore);
        }
        
        return bestScore;
    }
    
    /**
     * 盤面全体の評価値を計算します。
     * @param position 局面
     * @param player 自分の石
     * @param opponent 相手の石
     * @return 評価値
     */
    private int evaluateBoard(GomokuPosition position, char player, char opponent) {
        int boardSize = position.getBoardSize();
        int playerScore = 0;
        int opponentScore = 0;
        
        // 水平方向の評価
        for (int i = 0; i < boardSize; i++) {
            playerScore += evaluateLine(position, i, 0, 0, 1, player);
            opponentScore += evaluateLine(position, i, 0, 0, 1, opponent);
        }
        
        // 垂直方向の評価
        for (int j = 0; j < boardSize; j++) {
            playerScore += evaluateLine(position, 0, j, 1, 0, player);
            opponentScore += evaluateLine(position, 0, j, 1, 0, opponent);
        }
        
        // 右下斜め方向の評価（左上から右下）
        for (int i = 0; i < boardSize; i++) {
            playerScore += evaluateLine(position, i, 0, 1, 1, player);
            opponentScore += evaluateLine(position, i, 0, 1, 1, opponent);
        }
        for (int j = 1; j < boardSize; j++) {
            playerScore += evaluateLine(position, 0, j, 1, 1, player);
            opponentScore += evaluateLine(position, 0, j, 1, 1, opponent);
        }
        
        // 左下斜め方向の評価（右上から左下）
        for (int i = 0; i < boardSize; i++) {
            playerScore += evaluateLine(position, i, boardSize - 1, 1, -1, player);
            opponentScore += evaluateLine(position, i, boardSize - 1, 1, -1, opponent);
        }
        for (int j = boardSize - 2; j >= 0; j--) {
            playerScore += evaluateLine(position, 0, j, 1, -1, player);
            opponentScore += evaluateLine(position, 0, j, 1, -1, opponent);
        }
        
        // 攻撃と防御のバランスを取る
        return playerScore - opponentScore;
    }
    
    /**
     * 一つのラインの評価値を計算します。
     * @param position 局面
     * @param startRow 開始行
     * @param startCol 開始列
     * @param rowDir 行方向の増分
     * @param colDir 列方向の増分
     * @param player 評価対象のプレイヤー
     * @return 評価値
     */
    private int evaluateLine(GomokuPosition position, int startRow, int startCol, 
                           int rowDir, int colDir, char player) {
        int boardSize = position.getBoardSize();
        int score = 0;
        int count = 0;
        int empty = 0;
        int row = startRow;
        int col = startCol;
        
        while (row >= 0 && row < boardSize && col >= 0 && col < boardSize) {
            char stone = position.getStone(row, col);
            
            if (stone == player) {
                count++;
                if (count >= 5) {
                    score += SCORE_FIVE;
                    count = 0;
                    empty = 0;
                }
            } else if (stone == EMPTY) {
                // 連続した石の後の空マス
                if (count > 0) {
                    empty++;
                    
                    // 連続した石と空マスの組み合わせを評価
                    score += evaluateSequence(count, empty);
                    
                    count = 0;
                    empty = 1; // 新しいシーケンスの開始
                } else {
                    empty++;
                }
            } else {
                // 相手の石
                if (count > 0) {
                    // 片側が塞がれた状態を評価
                    score += evaluateBlockedSequence(count, empty);
                }
                count = 0;
                empty = 0;
            }
            
            row += rowDir;
            col += colDir;
        }
        
        // 最後の連続を評価
        if (count > 0) {
            score += evaluateBlockedSequence(count, empty);
        }
        
        return score;
    }
    
    /**
     * 連続した石と空マスの組み合わせを評価します。
     * @param count 連続した石の数
     * @param empty 連続した後の空マスの数
     * @return 評価値
     */
    private int evaluateSequence(int count, int empty) {
        if (empty >= 2) {
            // 両端が空いている
            switch (count) {
                case 4: return SCORE_OPEN_FOUR;
                case 3: return SCORE_OPEN_THREE;
                case 2: return SCORE_OPEN_TWO;
                case 1: return 1;
            }
        } else if (empty == 1) {
            // 片端だけ空いている
            switch (count) {
                case 4: return SCORE_FOUR;
                case 3: return SCORE_THREE;
                case 2: return SCORE_TWO;
                case 1: return 0;
            }
        }
        return 0;
    }
    
    /**
     * 片側が塞がれた連続を評価します。
     * @param count 連続した石の数
     * @param empty 連続した後の空マスの数
     * @return 評価値
     */
    private int evaluateBlockedSequence(int count, int empty) {
        if (empty >= 1) {
            switch (count) {
                case 4: return SCORE_FOUR;
                case 3: return SCORE_THREE;
                case 2: return SCORE_TWO;
                case 1: return 0;
            }
        }
        return 0;
    }
    
    /**
     * 特定の位置の手の評価値を計算します。
     * @param position 局面
     * @param row 行
     * @param col 列
     * @param player プレイヤー
     * @return 評価値
     */
    private int evaluateMove(GomokuPosition position, int row, int col, char player) {
        int score = 0;
        int boardSize = position.getBoardSize();
        
        // 中央への近さのボーナス
        int centerDistance = Math.abs(row - boardSize / 2) + Math.abs(col - boardSize / 2);
        score += Math.max(0, boardSize - centerDistance) * SCORE_CENTER;
        
        // 8方向のチェック: 水平、垂直、2つの斜め方向
        int[][][] directions = {
            // {行の増分, 列の増分}, {逆方向}
            {{0, 1}, {0, -1}},    // 横
            {{1, 0}, {-1, 0}},    // 縦
            {{1, 1}, {-1, -1}},   // 右下がり斜め
            {{1, -1}, {-1, 1}}    // 左下がり斜め
        };
        
        for (int[][] dirPair : directions) {
            int count = 1;  // 現在の位置の石
            int openEnds = 0;
            
            for (int[] dir : dirPair) {
                int r = row;
                int c = col;
                boolean foundEmpty = false;
                
                for (int i = 0; i < 5; i++) {  // 最大5マス先まで探索
                    r += dir[0];
                    c += dir[1];
                    
                    if (r < 0 || r >= boardSize || c < 0 || c >= boardSize) {
                        break;
                    }
                    
                    if (position.getStone(r, c) == player) {
                        if (foundEmpty) break;  // 空白を挟んでの連続は別のパターン
                        count++;
                    } else if (position.getStone(r, c) == EMPTY) {
                        openEnds++;
                        foundEmpty = true;
                        break;
                    } else {
                        break;
                    }
                }
            }
            
            // 評価
            if (count >= 5) {
                score += SCORE_FIVE;  // 勝利
            } else if (count == 4) {
                score += (openEnds == 2) ? SCORE_OPEN_FOUR : (openEnds == 1 ? SCORE_FOUR : 0);
            } else if (count == 3) {
                score += (openEnds == 2) ? SCORE_OPEN_THREE : (openEnds == 1 ? SCORE_THREE : 0);
            } else if (count == 2) {
                score += (openEnds == 2) ? SCORE_OPEN_TWO : (openEnds == 1 ? SCORE_TWO : 0);
            }
        }
        
        return score;
    }
} 