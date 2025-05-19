package com.example.gomoku;

import javax.swing.*;
import java.awt.*;
import java.awt.Cursor;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 五目並べのグラフィカルユーザーインターフェースを実装するクラスです。
 */
public class GomokuGUI extends JFrame {
    private static final int BOARD_SIZE = 15;
    private static final int CELL_SIZE = 30;
    private static final int BORDER_SIZE = 20;
    private static final int STONE_SIZE = 24;
    
    // Unicode文字を使用
    private static final char BLACK_STONE = '\u25CF'; // '●'
    private static final char WHITE_STONE = '\u25CB'; // '○'
    
    // フォント名
    private static final String FONT_NAME = "SansSerif";
    
    // 日本語対応フォント
    private static final Font UI_FONT = new Font(FONT_NAME, Font.PLAIN, 14);
    private static final Font UI_FONT_BOLD = new Font(FONT_NAME, Font.BOLD, 16);
    
    // サポートする言語
    private static final String[] SUPPORTED_LANGUAGES = {"en", "ja", "zh"};
    private static final String RESOURCE_BUNDLE_NAME = "MessageBundle";
    
    private GomokuPosition position;
    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private JPanel southPanel;
    private JComboBox<String> languageComboBox;
    private ResourceBundle messages;
    private Locale currentLocale;
    private boolean isVsComputer = false;
    private GomokuAI ai;
    private GomokuAI.Level aiLevel = GomokuAI.Level.MEDIUM; // デフォルト難易度
    
    /**
     * GUIアプリケーションを構築します。
     */
    public GomokuGUI() {
        // Unicodeサポートの設定
        System.setProperty("file.encoding", "UTF-8");
        
        // 言語選択ダイアログを表示
        showLanguageSelectionDialog();
        
        position = new GomokuPosition();
        position.setCurrentPlayer(BLACK_STONE); // Unicodeを使用
        ai = new GomokuAI(aiLevel);
        
        updateTitle();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // フォント設定
        setUIFont(UI_FONT);
        
        initComponents();
        
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    /**
     * 言語選択ダイアログを表示します。
     */
    private void showLanguageSelectionDialog() {
        // デフォルトロケールを使用
        currentLocale = Locale.getDefault();
        messages = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, currentLocale);
        
        // 選択肢を作成
        String[] languageNames = new String[SUPPORTED_LANGUAGES.length];
        for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
            Locale locale = new Locale(SUPPORTED_LANGUAGES[i]);
            ResourceBundle tempBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
            languageNames[i] = tempBundle.getString("language." + SUPPORTED_LANGUAGES[i]);
        }
        
        // デフォルト選択値を設定
        int defaultSelection = 0;
        for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
            if (SUPPORTED_LANGUAGES[i].equals(currentLocale.getLanguage())) {
                defaultSelection = i;
                break;
            }
        }
        
        // ダイアログを表示
        String dialogTitle = messages.getString("dialog.language.title");
        String dialogMessage = messages.getString("dialog.language.message");
        String selection = (String) JOptionPane.showInputDialog(
            null,
            dialogMessage,
            dialogTitle,
            JOptionPane.QUESTION_MESSAGE,
            null,
            languageNames,
            languageNames[defaultSelection]
        );
        
        // 選択された言語を設定
        if (selection != null) {
            for (int i = 0; i < languageNames.length; i++) {
                if (selection.equals(languageNames[i])) {
                    setLocale(new Locale(SUPPORTED_LANGUAGES[i]));
                    break;
                }
            }
        }
    }
    
    /**
     * ロケールを設定し、リソースを更新します。
     * @param locale 新しいロケール
     */
    @Override
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        currentLocale = locale;
        messages = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, currentLocale);
        
        // 言語切替の場合はUIを更新
        if (isVisible()) {
            updateUI();
        }
    }
    
    /**
     * UIを現在の言語で更新します。
     */
    private void updateUI() {
        // タイトルを更新
        updateTitle();
        
        // すべてのコンポーネントを削除し再初期化
        getContentPane().removeAll();
        
        // コンポーネントを再初期化
        if (boardPanel == null) {
            boardPanel = new BoardPanel();
        }
        boardPanel.setPreferredSize(new Dimension(
            BOARD_SIZE * CELL_SIZE + 2 * BORDER_SIZE,
            BOARD_SIZE * CELL_SIZE + 2 * BORDER_SIZE
        ));
        
        // 状態表示ラベル
        if (statusLabel == null) {
            statusLabel = new JLabel();
            statusLabel.setHorizontalAlignment(JLabel.CENTER);
            statusLabel.setFont(UI_FONT_BOLD);
        }
        updateStatus();
        
        // レイアウト設定
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.NORTH);
        
        // 下部パネル
        initSouthPanel();
        
        // ステータスバー
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel languageLabel = new JLabel(messages.getString("status.bar.language"));
        languageLabel.setFont(UI_FONT);
        
        if (languageComboBox == null) {
            languageComboBox = new JComboBox<>();
            languageComboBox.setFont(UI_FONT);
            
            languageComboBox.addActionListener(e -> {
                int selectedIndex = languageComboBox.getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < SUPPORTED_LANGUAGES.length) {
                    setLocale(new Locale(SUPPORTED_LANGUAGES[selectedIndex]));
                }
            });
        }
        updateLanguageComboBox();
        
        statusBar.add(languageLabel);
        statusBar.add(languageComboBox);
        
        // 南側に2つコンポーネントを追加するためのパネル
        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.add(southPanel, BorderLayout.CENTER);
        southContainer.add(statusBar, BorderLayout.SOUTH);
        add(southContainer, BorderLayout.SOUTH);
        
        // 再描画を強制
        validate();
        repaint();
    }
    
    /**
     * タイトルを更新します。
     */
    private void updateTitle() {
        setTitle(messages.getString("app.title"));
    }
    
    /**
     * 言語コンボボックスを更新します。
     */
    private void updateLanguageComboBox() {
        if (languageComboBox != null) {
            languageComboBox.removeAllItems();
            for (String lang : SUPPORTED_LANGUAGES) {
                Locale locale = new Locale(lang);
                ResourceBundle tempBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
                languageComboBox.addItem(tempBundle.getString("language." + lang));
            }
            
            // 現在の言語を選択
            for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
                if (SUPPORTED_LANGUAGES[i].equals(currentLocale.getLanguage())) {
                    languageComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    /**
     * UIマネージャのデフォルトフォントを設定します。
     * @param font 設定するフォント
     */
    private void setUIFont(Font font) {
        UIManager.put("Button.font", font);
        UIManager.put("ToggleButton.font", font);
        UIManager.put("RadioButton.font", font);
        UIManager.put("CheckBox.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("List.font", font);
        UIManager.put("MenuBar.font", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("RadioButtonMenuItem.font", font);
        UIManager.put("CheckBoxMenuItem.font", font);
        UIManager.put("Menu.font", font);
        UIManager.put("PopupMenu.font", font);
        UIManager.put("OptionPane.font", font);
        UIManager.put("Panel.font", font);
        UIManager.put("ProgressBar.font", font);
        UIManager.put("ScrollPane.font", font);
        UIManager.put("Viewport.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("PasswordField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("TextPane.font", font);
        UIManager.put("EditorPane.font", font);
        UIManager.put("TitledBorder.font", font);
        UIManager.put("ToolBar.font", font);
        UIManager.put("ToolTip.font", font);
        UIManager.put("Tree.font", font);
    }
    
    /**
     * コンポーネントを初期化します。
     */
    private void initComponents() {
        // ボード部分
        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(
            BOARD_SIZE * CELL_SIZE + 2 * BORDER_SIZE,
            BOARD_SIZE * CELL_SIZE + 2 * BORDER_SIZE
        ));
        
        // 状態表示ラベル
        statusLabel = new JLabel();
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setFont(UI_FONT_BOLD);
        updateStatus();
        
        // レイアウト設定
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.NORTH);
        
        // 下部パネル
        initSouthPanel();
        
        // ステータスバー
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel languageLabel = new JLabel(messages.getString("status.bar.language"));
        languageLabel.setFont(UI_FONT);
        
        languageComboBox = new JComboBox<>();
        languageComboBox.setFont(UI_FONT);
        updateLanguageComboBox();
        
        languageComboBox.addActionListener(e -> {
            int selectedIndex = languageComboBox.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < SUPPORTED_LANGUAGES.length) {
                setLocale(new Locale(SUPPORTED_LANGUAGES[selectedIndex]));
            }
        });
        
        statusBar.add(languageLabel);
        statusBar.add(languageComboBox);
        
        // 南側に2つコンポーネントを追加するためのパネル
        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.add(southPanel, BorderLayout.CENTER);
        southContainer.add(statusBar, BorderLayout.SOUTH);
        add(southContainer, BorderLayout.SOUTH);
    }
    
    /**
     * 下部パネルを初期化します。
     */
    private void initSouthPanel() {
        // 下部パネル
        southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(3, 1));
        
        // ゲームモード選択
        JPanel modePanel = new JPanel();
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton pvpButton = new JRadioButton(messages.getString("game.mode.pvp"), true);
        JRadioButton pvcButton = new JRadioButton(messages.getString("game.mode.pvc"));
        
        pvpButton.setFont(UI_FONT);
        pvcButton.setFont(UI_FONT);
        
        modeGroup.add(pvpButton);
        modeGroup.add(pvcButton);
        modePanel.add(pvpButton);
        modePanel.add(pvcButton);
        
        pvpButton.addActionListener(e -> {
            isVsComputer = false;
            resetGame();
        });
        
        pvcButton.addActionListener(e -> {
            isVsComputer = true;
            resetGame();
        });
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel();
        JButton resetButton = new JButton(messages.getString("game.reset"));
        resetButton.setFont(UI_FONT);
        resetButton.addActionListener(e -> resetGame());
        buttonPanel.add(resetButton);
        
        // 難易度選択パネル
        JPanel difficultyPanel = new JPanel();
        difficultyPanel.setBorder(BorderFactory.createTitledBorder(messages.getString("game.difficulty")));
        ButtonGroup difficultyGroup = new ButtonGroup();
        
        JRadioButton easyButton = new JRadioButton(messages.getString("game.difficulty.easy"), aiLevel == GomokuAI.Level.EASY);
        JRadioButton mediumButton = new JRadioButton(messages.getString("game.difficulty.medium"), aiLevel == GomokuAI.Level.MEDIUM);
        JRadioButton hardButton = new JRadioButton(messages.getString("game.difficulty.hard"), aiLevel == GomokuAI.Level.HARD);
        
        easyButton.setFont(UI_FONT);
        mediumButton.setFont(UI_FONT);
        hardButton.setFont(UI_FONT);
        
        difficultyGroup.add(easyButton);
        difficultyGroup.add(mediumButton);
        difficultyGroup.add(hardButton);
        
        difficultyPanel.add(easyButton);
        difficultyPanel.add(mediumButton);
        difficultyPanel.add(hardButton);
        
        easyButton.addActionListener(e -> {
            aiLevel = GomokuAI.Level.EASY;
            ai = new GomokuAI(aiLevel);
            if (isVsComputer) resetGame();
        });
        
        mediumButton.addActionListener(e -> {
            aiLevel = GomokuAI.Level.MEDIUM;
            ai = new GomokuAI(aiLevel);
            if (isVsComputer) resetGame();
        });
        
        hardButton.addActionListener(e -> {
            aiLevel = GomokuAI.Level.HARD;
            ai = new GomokuAI(aiLevel);
            if (isVsComputer) resetGame();
        });
        
        southPanel.add(modePanel);
        southPanel.add(difficultyPanel);
        southPanel.add(buttonPanel);
    }
    
    /**
     * ゲームをリセットします。
     */
    private void resetGame() {
        position = new GomokuPosition();
        position.setCurrentPlayer(BLACK_STONE); // Unicodeを使用
        updateStatus();
        boardPanel.repaint();
    }
    
    /**
     * ステータスラベルを更新します。
     */
    private void updateStatus() {
        statusLabel.setText(String.format(messages.getString("game.player.turn"), position.getCurrentPlayer()));
    }
    
    /**
     * 指定された位置に石を置きます。
     * @param row 行
     * @param col 列
     */
    private void placeStone(int row, int col) {
        if (!position.isValidMove(row, col)) {
            return;
        }
        
        position.makeMove(row, col);
        boardPanel.repaint();
        
        if (position.checkWinner(row, col)) {
            String winner = position.getCurrentPlayer() == BLACK_STONE ? 
                messages.getString("game.black") : messages.getString("game.white");
            JOptionPane.showMessageDialog(this, 
                winner + " " + messages.getString("game.win"), 
                messages.getString("game.over"), 
                JOptionPane.INFORMATION_MESSAGE);
            resetGame();
            return;
        }
        
        // 次のプレイヤーに切り替え
        char nextPlayer = position.getCurrentPlayer() == BLACK_STONE ? WHITE_STONE : BLACK_STONE;
        position.setCurrentPlayer(nextPlayer);
        updateStatus();
        
        // コンピュータの手番
        if (isVsComputer && position.getCurrentPlayer() == WHITE_STONE) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // カーソルを待機カーソルに変更
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    
                    // AIの思考時間を演出
                    Thread.sleep(500);
                    // 新しいAIクラスを使用して手を取得
                    int[] move = ai.getBestMove(position);
                    
                    // カーソルを元に戻す
                    setCursor(Cursor.getDefaultCursor());
                    
                    if (move != null) {
                        placeStone(move[0], move[1]);
                    }
                } catch (InterruptedException e) {
                    // 例外発生時もカーソルを元に戻す
                    setCursor(Cursor.getDefaultCursor());
                    e.printStackTrace();
                }
            });
        }
    }
    
    /**
     * 碁盤表示用のパネルクラスです。
     */
    private class BoardPanel extends JPanel {
        
        public BoardPanel() {
            setBackground(new Color(222, 184, 135)); // 碁盤の色
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isVsComputer && position.getCurrentPlayer() == WHITE_STONE) {
                        return; // コンピュータの手番ではクリック無効
                    }
                    
                    int col = (e.getX() - BORDER_SIZE + CELL_SIZE / 2) / CELL_SIZE;
                    int row = (e.getY() - BORDER_SIZE + CELL_SIZE / 2) / CELL_SIZE;
                    
                    if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                        placeStone(row, col);
                    }
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 格子線を描画
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < BOARD_SIZE; i++) {
                g2d.drawLine(
                    BORDER_SIZE + i * CELL_SIZE, 
                    BORDER_SIZE, 
                    BORDER_SIZE + i * CELL_SIZE, 
                    BORDER_SIZE + (BOARD_SIZE - 1) * CELL_SIZE
                );
                
                g2d.drawLine(
                    BORDER_SIZE, 
                    BORDER_SIZE + i * CELL_SIZE, 
                    BORDER_SIZE + (BOARD_SIZE - 1) * CELL_SIZE, 
                    BORDER_SIZE + i * CELL_SIZE
                );
            }
            
            // ハッシュ（星）を描画
            g2d.setColor(Color.BLACK);
            int[] hashPoints = {3, 7, 11};
            for (int i : hashPoints) {
                for (int j : hashPoints) {
                    g2d.fillOval(
                        BORDER_SIZE + i * CELL_SIZE - 3, 
                        BORDER_SIZE + j * CELL_SIZE - 3, 
                        6, 6
                    );
                }
            }
            
            // 石を描画
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    char stone = position.getStone(i, j);
                    if (stone != ' ') {
                        if (stone == BLACK_STONE) {
                            g2d.setColor(Color.BLACK);
                        } else {
                            g2d.setColor(Color.WHITE);
                        }
                        g2d.fillOval(
                            BORDER_SIZE + j * CELL_SIZE - STONE_SIZE / 2, 
                            BORDER_SIZE + i * CELL_SIZE - STONE_SIZE / 2, 
                            STONE_SIZE, STONE_SIZE
                        );
                        
                        // 白石には黒い輪郭線を追加
                        if (stone == WHITE_STONE) {
                            g2d.setColor(Color.BLACK);
                            g2d.drawOval(
                                BORDER_SIZE + j * CELL_SIZE - STONE_SIZE / 2, 
                                BORDER_SIZE + i * CELL_SIZE - STONE_SIZE / 2, 
                                STONE_SIZE, STONE_SIZE
                            );
                        }
                    }
                }
            }
        }
    }
    
    /**
     * メインメソッド
     */
    public static void main(String[] args) {
        try {
            // システムのルックアンドフィールを設定
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            GomokuGUI gui = new GomokuGUI();
            gui.setVisible(true);
        });
    }
} 