package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.AI.MinMaxSearch;
import org.example.model.AbstractPiece;
import org.example.data.AccountService;
import org.example.data.DatabaseManager;
import org.example.data.GameStateStore;
import org.example.model.ChessBoardModel;
import org.example.view.ChessBoardView;
import org.example.view.LoginView;
import org.example.view.ModeSelectionView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * 象棋应用程序的主入口类。
 * 负责初始化 JavaFX 应用程序，设置场景（登录、模式选择、棋盘），并处理全局导航逻辑。
 */
public class XiangqiApp extends Application {

    private static final String PREF_LAST_ACCOUNT = "lastAccount";
    private static final String PREF_ACCOUNT_HISTORY = "accountHistory";
    private static final int MAX_ACCOUNT_HISTORY = 8;
    private static final double APP_WIDTH = 660;
    private static final double APP_HEIGHT = 620;
    private String currentAccount;
    private AccountService.UserProfile currentProfile;
    private DatabaseManager databaseManager;
    private AccountService accountService;
    private GameStateStore gameStateStore;
    private MinMaxSearch ai; // AI 搜索引擎
    private boolean isAiMode = false; // 当前是否为人机对战模式

    /**
     * 应用程序启动入口。
     * 初始化数据库、服务、视图，并显示登录界面。
     */
    @Override
    public void start(Stage stage) {
        ChessBoardModel board = new ChessBoardModel();
        ChessBoardView view = new ChessBoardView(board);
        Scene boardScene = new Scene(view);

        databaseManager = new DatabaseManager();
        accountService = new AccountService(databaseManager);
        gameStateStore = new GameStateStore();

        Preferences preferences = Preferences.userNodeForPackage(XiangqiApp.class);
        List<String> knownAccounts = loadKnownAccounts(preferences);

        final ModeSelectionView[] modeHolder = new ModeSelectionView[1];
        final LoginView[] loginHolder = new LoginView[1];
        final Scene[] loginSceneRef = new Scene[1];
        final Scene[] modeSceneRef = new Scene[1];

        // 初始化模式选择视图及其回调
        modeHolder[0] = new ModeSelectionView(
            () -> enterHumanVsHuman(stage, boardScene, view, board, modeHolder[0], null),
            (slot) -> enterHumanVsHuman(stage, boardScene, view, board, modeHolder[0], slot),
            () -> enterHumanVsAi(stage, boardScene, view, board, modeHolder[0]),
            this::handleProfileUpdate,
            () -> handleLogout(stage, loginSceneRef[0], loginHolder[0], board, modeHolder[0]),
            request -> handleAccountDeletion(request, stage, loginSceneRef[0], loginHolder[0], preferences, board, modeHolder[0]),
            account -> gameStateStore.getSaveSlots(account));
        ModeSelectionView modeSelectionView = modeHolder[0];
        Scene modeScene = new Scene(modeSelectionView);
        modeSceneRef[0] = modeScene;

        view.setOnSave(() -> handleSave(stage, board));
        view.setOnExit(() -> handleExit(stage, modeSceneRef[0], modeSelectionView, board));
        board.winnerProperty().addListener((obs, oldWinner, newWinner) -> {
            // 自动删除逻辑可能需要针对多存档进行重新审视。
            // 目前，为了保留历史记录，我们在获胜时不自动删除存档。
        });
        
        board.redTurnProperty().addListener((obs, oldVal, newVal) -> {
            if (isAiMode && !newVal && board.getWinner() == null) {
                // AI's turn (Black)
                makeAiMove(board);
            }
        });

        // 初始化登录视图及其回调
        loginHolder[0] = new LoginView((request, loginPane) ->
                handleLogin(request, loginPane, stage, boardScene, modeSceneRef[0], view, accountService, preferences, modeSelectionView),
                (registration, loginPane) -> handleRegistration(registration, loginPane, accountService, preferences),
                () -> handleGuestLogin(stage, modeSceneRef[0], modeSelectionView),
                accountService::getProfile,
                knownAccounts);
        LoginView loginView = loginHolder[0];

        Optional.ofNullable(preferences.get(PREF_LAST_ACCOUNT, null))
                .ifPresent(loginView::prefillAccount);

        Scene loginScene = new Scene(loginView);
        loginSceneRef[0] = loginScene;

        stage.setTitle("登录");
        stage.setScene(loginScene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * 处理用户登录请求。
     * 验证凭据，更新偏好设置，加载用户资料，并跳转到模式选择界面。
     */
    private void handleLogin(LoginView.LoginRequest request,
                             LoginView loginPane,
                             Stage stage,
                             Scene boardScene,
                             Scene modeScene,
                             ChessBoardView view,
                             AccountService accountService,
                             Preferences preferences,
                             ModeSelectionView modeSelectionView) {
        boolean authenticated;
        try {
            authenticated = accountService.authenticate(request.account(), request.password());
        } catch (IllegalStateException ex) {
            loginPane.showError(ex.getMessage());
            return;
        }

        if (!authenticated) {
            loginPane.showLoginFailureFeedback("账号或密码不正确");
            return;
        }

        if (request.rememberMe()) {
            preferences.put(PREF_LAST_ACCOUNT, request.account());
        } else {
            preferences.remove(PREF_LAST_ACCOUNT);
        }

        loginPane.clearMessage();
        currentAccount = request.account();
        currentProfile = accountService.getProfile(currentAccount);
        if (currentProfile == null) {
            currentProfile = new AccountService.UserProfile(currentAccount, currentAccount, null, null);
        }
        updateAccountHistory(preferences, currentAccount, loginPane);
        modeSelectionView.setProfile(currentProfile);
        
        // 检查是否存在存档
        boolean hasSaves = gameStateStore.getSaveSlots(currentAccount).stream().anyMatch(GameStateStore.SaveSlotInfo::exists);
        if (hasSaves) {
            modeSelectionView.showMessage("检测到存档，可选择读取存档继续对战");
        } else {
            modeSelectionView.showMessage(null);
        }
        
        stage.setScene(modeScene);
        stage.setTitle("选择对战模式");
        stage.setResizable(true);
        stage.setWidth(APP_WIDTH);
        stage.setHeight(APP_HEIGHT);
        enforceAspectRatio(stage);
        stage.centerOnScreen();
    }

    /**
     * 处理用户注册请求。
     * 创建新账户，并在成功后自动填充登录信息。
     */
    private void handleRegistration(LoginView.RegistrationRequest request,
                                    LoginView loginView,
                                    AccountService accountService,
                                    Preferences preferences) {
        AccountService.RegistrationResult result;
        try {
            result = accountService.register(request.account(), request.nickname(), request.password(), request.avatarPath());
        } catch (IllegalStateException ex) {
            loginView.showRegistrationError(ex.getMessage());
            return;
        }

        if (!result.success()) {
            loginView.showRegistrationError(result.message());
            return;
        }

        loginView.clearRegistrationFields();
        loginView.clearRegistrationMessage();
        preferences.put(PREF_LAST_ACCOUNT, request.account());
        loginView.prefillAccount(request.account());
        currentProfile = new AccountService.UserProfile(
            request.account(),
            request.nickname(),
            request.avatarPath(),
            request.password());
        loginView.showLoginFormWithInfo(result.message());
    }

    /**
     * 进入真人对战模式。
     * 如果提供了存档槽位，则尝试加载存档；否则开始新游戏。
     */
    private void enterHumanVsHuman(Stage stage,
                                   Scene boardScene,
                                   ChessBoardView view,
                                   ChessBoardModel board,
                                   ModeSelectionView modeSelectionView,
                                   Integer slot) {
        isAiMode = false;
        boolean restored = false;
        if (slot != null && currentAccount != null && gameStateStore != null) {
            try {
                Optional<GameStateStore.GameState> saved = gameStateStore.loadState(currentAccount, slot);
                if (saved.isPresent()) {
                    GameStateStore.GameState state = saved.get();
                    board.replaceState(state.pieces(), state.redTurn(), state.winner());
                    board.setSpecialMessage("已恢复存档 " + slot, board.isRedTurn());
                    restored = true;
                }
            } catch (IOException ex) {
                board.setSpecialMessage("读取存档失败，请稍后重试", board.isRedTurn());
            }
        }
        
        if (!restored) {
            board.restartGame();
            board.setSpecialMessage("祝您旗开得胜！", true);
        }
        
        stage.setScene(boardScene);
        String titleName = currentProfile != null ? currentProfile.nickname() : "真人对战";
        stage.setTitle("中国象棋演示 - " + titleName);
        modeSelectionView.showMessage(null);
    }

    /**
     * 进入人机对战模式。
     */
    private void enterHumanVsAi(Stage stage,
                                Scene boardScene,
                                ChessBoardView view,
                                ChessBoardModel board,
                                ModeSelectionView modeSelectionView) {
        isAiMode = true;
        ai = new MinMaxSearch(false, board); // AI plays Black
        board.restartGame();
        board.setSpecialMessage("人机对战开始！", true);
        
        stage.setScene(boardScene);
        String titleName = currentProfile != null ? currentProfile.nickname() : "人机对战";
        stage.setTitle("中国象棋演示 - " + titleName + " (VS AI)");
        modeSelectionView.showMessage(null);
    }

    private void makeAiMove(ChessBoardModel board) {
        new Thread(() -> {
            try {
                Thread.sleep(500); // Small delay for better UX
            } catch (InterruptedException e) {
                return;
            }
            
            ChessBoardModel bestState = ai.findBestMove(board);
            if (bestState != null) {
                ChessBoardModel.MoveRecord move = bestState.getLastMove();
                if (move != null) {
                    javafx.application.Platform.runLater(() -> {
                        AbstractPiece piece = board.getPieceAt(move.fromRow(), move.fromCol());
                        if (piece != null) {
                            board.movePiece(piece, move.toRow(), move.toCol());
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 处理保存棋局的逻辑。
     * 显示存档槽位选择对话框，并处理覆盖确认和命名。
     */
    private void handleSave(Stage stage, ChessBoardModel board) {
        if (currentAccount == null || currentAccount.isBlank()) {
            board.setSpecialMessage("请先登录账号后再保存棋局", true);
            return;
        }
        
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.setTitle("选择存档位置");
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f3e5d0;");
        
        List<GameStateStore.SaveSlotInfo> slots = gameStateStore.getSaveSlots(currentAccount);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        for (GameStateStore.SaveSlotInfo slot : slots) {
            String text = "存档 " + slot.id();
            if (slot.exists()) {
                String displayName = slot.saveName() != null ? slot.saveName() : "未命名存档";
                text += " - " + displayName + " (" + sdf.format(new Date(slot.timestamp())) + ")";
            } else {
                text += " (空)";
            }
            
            Button btn = new Button(text);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(40);
            btn.setStyle("-fx-background-color: #e6cca0; -fx-border-color: #8b5a2b; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            
            btn.setOnAction(e -> {
                if (slot.exists()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("覆盖存档");
                    alert.setHeaderText("存档 " + slot.id() + " 已存在");
                    alert.setContentText("是否覆盖当前存档？");
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return;
                    }
                }

                TextInputDialog nameDialog = new TextInputDialog("存档 " + slot.id());
                nameDialog.setTitle("存档命名");
                nameDialog.setHeaderText("请输入存档名称");
                nameDialog.setContentText("名称:");
                Optional<String> nameResult = nameDialog.showAndWait();
                
                if (nameResult.isPresent()) {
                    String saveName = nameResult.get();
                    if (saveName.isBlank()) {
                        saveName = "存档 " + slot.id();
                    }
                    try {
                        gameStateStore.saveState(currentAccount, slot.id(), board, saveName);
                        board.setSpecialMessage("棋局已保存至 " + saveName, board.isRedTurn());
                        dialog.close();
                    } catch (IOException ex) {
                        board.setSpecialMessage("保存棋局失败，请稍后重试", board.isRedTurn());
                        dialog.close();
                    }
                }
            });
            root.getChildren().add(btn);
        }
        
        Button cancelBtn = new Button("取消");
        cancelBtn.setOnAction(e -> dialog.close());
        cancelBtn.setStyle("-fx-background-color: #d1d1d1; -fx-cursor: hand;");
        root.getChildren().add(cancelBtn);
        
        Scene scene = new Scene(root, 320, 600);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * 处理退出游戏的逻辑。
     * 弹出确认对话框，确认后返回模式选择界面。
     */
    private void handleExit(Stage stage,
                            Scene modeScene,
                            ModeSelectionView modeSelectionView,
                            ChessBoardModel board) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出游戏");
        alert.setHeaderText("确定要退出当前对局吗？");
        alert.setContentText("未保存的进度将会丢失。");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            modeSelectionView.showMessage(null);
            stage.setScene(modeScene);
            stage.setTitle("选择对战模式");
        }
    }

    /**
     * 处理用户登出逻辑。
     * 清除当前会话信息，并返回登录界面。
     */
    private void handleLogout(Stage stage,
                              Scene loginScene,
                              LoginView loginView,
                              ChessBoardModel board,
                              ModeSelectionView modeSelectionView) {
        if (loginScene == null || loginView == null) {
            return;
        }
        board.restartGame();
        board.clearSpecialMessage();
        currentAccount = null;
        currentProfile = null;
        modeSelectionView.setProfile(null);
        modeSelectionView.showMessage(null);
        loginView.showLoginFormWithInfo("已退出登录");

        disableAspectRatio(stage);
        stage.setScene(loginScene);
        stage.setTitle("登录");
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    /**
     * 处理账号注销逻辑。
     * 验证密码后删除账号及其所有存档，并返回登录界面。
     */
    private AccountService.AccountDeletionResult handleAccountDeletion(ModeSelectionView.AccountDeletionRequest request,
                                                                       Stage stage,
                                                                       Scene loginScene,
                                                                       LoginView loginView,
                                                                       Preferences preferences,
                                                                       ChessBoardModel board,
                                                                       ModeSelectionView modeSelectionView) {
        if (currentAccount == null || currentAccount.isBlank()) {
            return new AccountService.AccountDeletionResult(false, "当前无登录账号");
        }
        if (request == null || request.password() == null || request.password().isBlank()) {
            return new AccountService.AccountDeletionResult(false, "请输入原密码");
        }
        AccountService.AccountDeletionResult result;
        try {
            result = accountService.deleteAccount(currentAccount, request.password());
        } catch (IllegalStateException ex) {
            return new AccountService.AccountDeletionResult(false, ex.getMessage());
        }
        if (!result.success()) {
            return result;
        }

        String deletedAccount = currentAccount;
        currentAccount = null;
        currentProfile = null;

        if (gameStateStore != null) {
            try {
                // 删除所有存档槽位
                for (int i = 1; i <= 10; i++) {
                    gameStateStore.deleteState(deletedAccount, i);
                }
            } catch (IOException ignore) {
                // 忽略清理失败，以便注销流程继续
            }
        }

        List<String> accounts = loadKnownAccounts(preferences);
        accounts.remove(deletedAccount);
        storeKnownAccounts(preferences, accounts);
        if (deletedAccount.equals(preferences.get(PREF_LAST_ACCOUNT, null))) {
            preferences.remove(PREF_LAST_ACCOUNT);
        }
        if (loginView != null) {
            loginView.updateKnownAccounts(new ArrayList<>(accounts));
        }

        board.restartGame();
        board.clearSpecialMessage();
        modeSelectionView.setProfile(null);
        modeSelectionView.showMessage(null);

        if (loginScene != null && loginView != null) {
            loginView.showLoginFormWithInfo(result.message());
            disableAspectRatio(stage);
            stage.setScene(loginScene);
            stage.setTitle("登录");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        }

        return result;
    }

    /**
     * 处理用户资料更新请求。
     */
    private AccountService.ProfileUpdateResult handleProfileUpdate(ModeSelectionView.ProfileUpdateRequest request) {
        if (currentAccount == null) {
            return new AccountService.ProfileUpdateResult(false, "请先登录", null);
        }
        AccountService.ProfileUpdateResult result = accountService.updateProfile(
                currentAccount,
                request.nickname(),
                request.avatarPath(),
                request.oldPassword(),
                request.newPassword());
        if (result.success()) {
            currentProfile = result.profile();
        }
        return result;
    }

    /**
     * 从偏好设置中加载已知账号列表。
     */
    private List<String> loadKnownAccounts(Preferences preferences) {
        String raw = preferences.get(PREF_ACCOUNT_HISTORY, "");
        List<String> accounts = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return accounts;
        }
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .forEach(entry -> {
                    if (!accounts.contains(entry)) {
                        accounts.add(entry);
                    }
                });
        return accounts;
    }

    /**
     * 将已知账号列表保存到偏好设置。
     */
    private void storeKnownAccounts(Preferences preferences, List<String> accounts) {
        String joined = String.join(",", accounts);
        preferences.put(PREF_ACCOUNT_HISTORY, joined);
    }

    /**
     * 更新账号登录历史记录。
     * 将最近登录的账号移至列表首位，并保持列表长度不超过限制。
     */
    private void updateAccountHistory(Preferences preferences, String account, LoginView loginView) {
        if (account == null || account.isBlank()) {
            return;
        }
        List<String> accounts = loadKnownAccounts(preferences);
        accounts.remove(account);
        accounts.add(0, account);
        while (accounts.size() > MAX_ACCOUNT_HISTORY) {
            accounts.remove(accounts.size() - 1);
        }
        storeKnownAccounts(preferences, accounts);
        loginView.updateKnownAccounts(new ArrayList<>(accounts));
    }

    /**
     * 处理游客登录逻辑。
     * 使用临时游客身份进入游戏，无法保存进度。
     */
    private void handleGuestLogin(Stage stage, Scene modeScene, ModeSelectionView modeSelectionView) {
        currentAccount = null;
        currentProfile = new AccountService.UserProfile("guest", "游客", null, null);

        modeSelectionView.setProfile(currentProfile);
        modeSelectionView.showMessage("游客模式：无法保存棋局进度");

        stage.setScene(modeScene);
        stage.setTitle("选择对战模式");
        stage.setResizable(true);
        stage.setWidth(APP_WIDTH);
        stage.setHeight(APP_HEIGHT);
        enforceAspectRatio(stage);
        stage.centerOnScreen();
    }

    /**
     * 强制窗口保持固定的宽高比。
     */
    private void enforceAspectRatio(Stage stage) {
        double ratio = APP_WIDTH / APP_HEIGHT;
        stage.minHeightProperty().bind(stage.widthProperty().divide(ratio));
        stage.maxHeightProperty().bind(stage.widthProperty().divide(ratio));
    }

    /**
     * 解除窗口的宽高比限制。
     */
    private void disableAspectRatio(Stage stage) {
        stage.minHeightProperty().unbind();
        stage.maxHeightProperty().unbind();
        stage.setMinHeight(0);
        stage.setMaxHeight(Double.MAX_VALUE);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
