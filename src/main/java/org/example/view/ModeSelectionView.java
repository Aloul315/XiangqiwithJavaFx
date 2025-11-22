package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.scene.transform.Scale;
import org.example.data.AccountService;
import org.example.data.GameStateStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 游戏模式选择视图。
 * <p>
 * 提供单机模式、联机模式、AI对战等入口，并展示当前登录用户的个人信息（头像、昵称、战绩）。
 * 支持修改用户昵称和头像。
 * </p>
 */
public class ModeSelectionView extends StackPane {
    private static final double BASE_WIDTH = 600;
    private static final double BASE_HEIGHT = 560;

    public record ProfileUpdateRequest(String nickname, String avatarPath, String oldPassword, String newPassword, String confirmPassword) {}
    public record AccountDeletionRequest(String password) {}

    private final Label messageLabel = new Label();
    private final Circle avatarCircle = new Circle(28);
    private final Label nicknameLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label overlayMessage = new Label();
    private final TextField nicknameField = new TextField();
    private final PasswordField oldPasswordField = new PasswordField();
    private final PasswordField newPasswordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label avatarPathLabel = new Label("未选择头像");
    private final StackPane profileOverlay = new StackPane();
    private final StackPane slotOverlay = new StackPane();
    private final StackPane contentRoot = new StackPane();
    private final Scale contentScale = new Scale(1.0, 1.0);

    private final Function<ProfileUpdateRequest, AccountService.ProfileUpdateResult> onProfileUpdate;
    private final Runnable onLogout;
    private final Function<AccountDeletionRequest, AccountService.AccountDeletionResult> onAccountDeletion;
    private final Function<String, List<GameStateStore.SaveSlotInfo>> saveSlotProvider;
    private final Runnable onNewGame;
    private final Consumer<Integer> onLoadGame;

    private VBox contentBox;
    private Button humanButton;
    private HBox humanSplitBox;

    private AccountService.UserProfile currentProfile;
    private String selectedAvatarPath;

    /**
     * 构造模式选择视图。
     *
     * @param onNewGame     新游戏回调
     * @param onLoadGame    读取存档回调
     * @param onHumanVsAi   人机对战回调
     * @param onProfileUpdate 个人资料更新回调
     * @param onLogout      注销登录回调
     * @param onAccountDeletion 账号注销回调
     * @param saveSlotProvider 存档提供者
     */
    public ModeSelectionView(Runnable onNewGame,
                             Consumer<Integer> onLoadGame,
                             Runnable onHumanVsAi,
                             Function<ProfileUpdateRequest, AccountService.ProfileUpdateResult> onProfileUpdate,
                             Runnable onLogout,
                             Function<AccountDeletionRequest, AccountService.AccountDeletionResult> onAccountDeletion,
                             Function<String, List<GameStateStore.SaveSlotInfo>> saveSlotProvider) {
        Objects.requireNonNull(onNewGame, "onNewGame");
        Objects.requireNonNull(onLoadGame, "onLoadGame");
        Objects.requireNonNull(onHumanVsAi, "onHumanVsAi");
        this.onProfileUpdate = Objects.requireNonNull(onProfileUpdate, "onProfileUpdate");
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
        this.onAccountDeletion = Objects.requireNonNull(onAccountDeletion, "onAccountDeletion");
        this.saveSlotProvider = Objects.requireNonNull(saveSlotProvider, "saveSlotProvider");
        this.onNewGame = Objects.requireNonNull(onNewGame, "onNewGame");
        this.onLoadGame = Objects.requireNonNull(onLoadGame, "onLoadGame");

        setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setBackgroundTexture();

        contentRoot.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        contentRoot.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        contentRoot.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        contentScale.setPivotX(BASE_WIDTH / 2.0);
        contentScale.setPivotY(BASE_HEIGHT / 2.0);
        contentRoot.getTransforms().add(contentScale);

        StackPane card = new StackPane();
        card.setPadding(new Insets(52, 42, 52, 42));
        // Background panel removed as requested
        card.setStyle("-fx-background-color: transparent;");

        contentBox = new VBox(24);
        VBox content = contentBox;
        content.setAlignment(Pos.CENTER);

        configureAvatar();

        Label title = new Label("排阵布兵");
        title.setFont(Font.font("KaiTi", FontWeight.EXTRA_BOLD, 34));
        title.setTextFill(Color.web("#2c1b0f"));

        Label subtitle = new Label("请挑选作战模式");
        subtitle.setFont(Font.font("KaiTi", FontWeight.BOLD, 20));
        subtitle.setTextFill(Color.web("#4f2c16"));

        humanButton = primaryButton("真人对战");
        humanButton.setOnAction(evt -> showHumanSplit());

        Button aiButton = secondaryButton("人机对战");
        aiButton.setOnAction(evt -> onHumanVsAi.run());

        Button creditsButton = secondaryButton("制作者名单");
        creditsButton.setOnAction(evt -> showCreatorInfo());

        messageLabel.setFont(Font.font("KaiTi", FontWeight.BOLD, 18));
        messageLabel.setTextFill(Color.web("#721414"));
        messageLabel.setVisible(false);

        HBox avatarRow = new HBox(12, avatarCircle, nicknameLabel);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        avatarRow.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(avatarRow, title, subtitle, humanButton, aiButton, creditsButton, messageLabel);
        card.getChildren().add(content);

        buildProfileOverlay();
        buildSlotOverlay();
        contentRoot.getChildren().addAll(card, profileOverlay, slotOverlay);
        getChildren().add(contentRoot);
        StackPane.setAlignment(contentRoot, Pos.CENTER);

        widthProperty().addListener((obs, oldVal, newVal) -> updateScale());
        heightProperty().addListener((obs, oldVal, newVal) -> updateScale());
        updateScale();

        this.setOnMouseClicked(evt -> {
            if (!evt.isConsumed()) {
                resetHumanButton();
            }
        });
    }

    private void buildSlotOverlay() {
        slotOverlay.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        slotOverlay.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        slotOverlay.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        slotOverlay.setVisible(false);
        slotOverlay.setOpacity(0);
        slotOverlay.setPickOnBounds(false);

        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(29,20,11,0.6);");
        backdrop.setPickOnBounds(true);
        backdrop.setOnMouseClicked(evt -> hideSlotSelector());

        StackPane card = new StackPane();
        card.setPadding(new Insets(24));
        card.setMaxHeight(500);
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: rgba(243,229,208,0.98); -fx-background-radius: 24;" +
            "-fx-border-color: rgba(125,82,33,0.75); -fx-border-width: 1.4; -fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(125,82,33,0.35), 26, 0.42, 0, 6);");
        card.setOnMouseClicked(evt -> evt.consume());

        VBox layout = new VBox(16);
        layout.setAlignment(Pos.TOP_CENTER);

        Label header = new Label("选择存档");
        header.setFont(Font.font("KaiTi", FontWeight.EXTRA_BOLD, 24));
        header.setTextFill(Color.web("#3b1f0f"));

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        VBox slotsBox = new VBox(10);
        slotsBox.setId("slots-container");
        slotsBox.setAlignment(Pos.TOP_CENTER);
        slotsBox.setPadding(new Insets(10));
        scroll.setContent(slotsBox);

        Button closeBtn = secondaryButton("关闭");
        closeBtn.setPrefHeight(40);
        closeBtn.setOnAction(evt -> hideSlotSelector());

        layout.getChildren().addAll(header, scroll, closeBtn);
        card.getChildren().add(layout);

        slotOverlay.getChildren().addAll(backdrop, card);
    }

    private void showSaveSlotSelector(Consumer<Integer> onSelect) {
        if (currentProfile == null) {
            showMessage("请先登录");
            return;
        }
        
        VBox container = (VBox) slotOverlay.lookup("#slots-container");
        container.getChildren().clear();
        
        List<GameStateStore.SaveSlotInfo> slots = saveSlotProvider.apply(currentProfile.account());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (GameStateStore.SaveSlotInfo slot : slots) {
            Button slotBtn = new Button();
            slotBtn.setMaxWidth(Double.MAX_VALUE);
            slotBtn.setPrefHeight(50);
            
            String text = "存档 " + slot.id();
            if (slot.exists()) {
                String displayName = slot.saveName() != null ? slot.saveName() : "未命名存档";
                text += " - " + displayName + "\n" + sdf.format(new Date(slot.timestamp()));
                slotBtn.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-color: #8b5a2b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center-left; -fx-padding: 5 15 5 15;");
            } else {
                text += "\n(空)";
                slotBtn.setStyle("-fx-background-color: rgba(255,255,255,0.4); -fx-border-color: #a0a0a0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center-left; -fx-padding: 5 15 5 15;");
                slotBtn.setDisable(true); // Disable empty slots for loading
            }
            
            slotBtn.setText(text);
            slotBtn.setFont(Font.font("Microsoft YaHei", 14));
            
            if (slot.exists()) {
                slotBtn.setCursor(Cursor.HAND);
                slotBtn.setOnAction(evt -> {
                    hideSlotSelector();
                    onSelect.accept(slot.id());
                });
            }
            
            container.getChildren().add(slotBtn);
        }

        slotOverlay.setVisible(true);
        slotOverlay.setOpacity(1);
        slotOverlay.setPickOnBounds(true);
    }

    private void hideSlotSelector() {
        slotOverlay.setVisible(false);
        slotOverlay.setOpacity(0);
        slotOverlay.setPickOnBounds(false);
    }

    public void showSaveSlotSelectorForSaving(Consumer<Integer> onSelect) {
        if (currentProfile == null) return;
        
        VBox container = (VBox) slotOverlay.lookup("#slots-container");
        container.getChildren().clear();
        
        List<GameStateStore.SaveSlotInfo> slots = saveSlotProvider.apply(currentProfile.account());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (GameStateStore.SaveSlotInfo slot : slots) {
            Button slotBtn = new Button();
            slotBtn.setMaxWidth(Double.MAX_VALUE);
            slotBtn.setPrefHeight(50);
            
            String text = "存档 " + slot.id();
            if (slot.exists()) {
                String displayName = slot.saveName() != null ? slot.saveName() : "未命名存档";
                text += " - " + displayName + "\n" + sdf.format(new Date(slot.timestamp()));
                slotBtn.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-color: #8b5a2b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center-left; -fx-padding: 5 15 5 15;");
            } else {
                text += "\n(空)";
                slotBtn.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-border-color: #8b5a2b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center-left; -fx-padding: 5 15 5 15; -fx-border-style: dashed;");
            }
            
            slotBtn.setText(text);
            slotBtn.setFont(Font.font("Microsoft YaHei", 14));
            slotBtn.setCursor(Cursor.HAND);
            slotBtn.setOnAction(evt -> {
                hideSlotSelector();
                onSelect.accept(slot.id());
            });
            
            container.getChildren().add(slotBtn);
        }

        slotOverlay.setVisible(true);
        slotOverlay.setOpacity(1);
        slotOverlay.setPickOnBounds(true);
    }

    private void showHumanSplit() {
        if (humanSplitBox == null) {
            humanSplitBox = new HBox(12);
            humanSplitBox.setAlignment(Pos.CENTER);

            Button newGame = primaryButton("新游戏");
            newGame.setPrefWidth(180);
            newGame.setOnAction(e -> onNewGame.run());

            Button loadGame = secondaryButton("读取存档");
            loadGame.setPrefWidth(180);
            loadGame.setOnAction(e -> showSaveSlotSelector(onLoadGame));

            humanSplitBox.getChildren().addAll(newGame, loadGame);
        }

        int index = contentBox.getChildren().indexOf(humanButton);
        if (index != -1) {
            contentBox.getChildren().set(index, humanSplitBox);
        }
    }

    private void resetHumanButton() {
        if (humanSplitBox != null && contentBox.getChildren().contains(humanSplitBox)) {
            int index = contentBox.getChildren().indexOf(humanSplitBox);
            if (index != -1) {
                contentBox.getChildren().set(index, humanButton);
            }
        }
    }

    private void showCreatorInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("制作者名单");
        alert.setHeaderText(null);
        alert.setContentText("Aloul\nLengthCheng");
        if (getScene() != null && getScene().getWindow() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.showAndWait();
    }

    public void setProfile(AccountService.UserProfile profile) {
        this.currentProfile = profile;
        this.selectedAvatarPath = profile != null ? profile.avatarPath() : null;
        updateAvatarDisplay();
        nicknameLabel.setText(profile != null ? profile.nickname() : "未登录");
    }

    public void showMessage(String text) {
        if (text == null || text.isBlank()) {
            messageLabel.setVisible(false);
            messageLabel.setText("");
        } else {
            messageLabel.setText(text);
            messageLabel.setVisible(true);
        }
    }

    private void configureAvatar() {
        avatarCircle.setFill(Color.web("#d6a354"));
        avatarCircle.setStroke(Color.web("#7a4e19"));
        avatarCircle.setStrokeWidth(2.5);
        avatarCircle.setCursor(Cursor.HAND);
        avatarCircle.setOnMouseClicked(evt -> showProfileEditor());

        nicknameLabel.setFont(Font.font("KaiTi", FontWeight.BOLD, 22));
        nicknameLabel.setTextFill(Color.web("#331b07"));
    }

    private void buildProfileOverlay() {
        profileOverlay.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        profileOverlay.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        profileOverlay.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        profileOverlay.setVisible(false);
        profileOverlay.setOpacity(0);
        profileOverlay.setPickOnBounds(false);

        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(29,20,11,0.6);");
        backdrop.setPickOnBounds(true);
        backdrop.setOnMouseClicked(evt -> hideProfileEditor());

        StackPane card = new StackPane();
        card.setPadding(new Insets(32, 36, 32, 36));
        card.setStyle("-fx-background-color: rgba(243,229,208,0.98); -fx-background-radius: 24;" +
            "-fx-border-color: rgba(125,82,33,0.75); -fx-border-width: 1.4; -fx-border-radius: 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(125,82,33,0.35), 26, 0.42, 0, 6);");
        card.setOnMouseClicked(evt -> evt.consume());

        VBox form = new VBox(18);
        form.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("编辑个人资料");
        header.setFont(Font.font("KaiTi", FontWeight.EXTRA_BOLD, 26));
        header.setTextFill(Color.web("#3b1f0f"));

        nicknameField.setPromptText("江湖称号");
        nicknameField.setPrefWidth(260);
        nicknameField.setStyle(fieldStyle());

        HBox avatarChooser = new HBox(12);
        avatarChooser.setAlignment(Pos.CENTER_LEFT);
        Button chooseAvatarButton = new Button("选择头像");
        chooseAvatarButton.setStyle(primaryMiniButtonStyle());
        chooseAvatarButton.setOnAction(evt -> chooseAvatar());
        avatarPathLabel.setTextFill(Color.web("#5c3a1d"));
        avatarChooser.getChildren().addAll(chooseAvatarButton, avatarPathLabel);

        oldPasswordField.setPromptText("原密码");
        newPasswordField.setPromptText("新密码（至少 6 位）");
        confirmPasswordField.setPromptText("确认新密码");
        oldPasswordField.setStyle(fieldStyle());
        newPasswordField.setStyle(fieldStyle());
        confirmPasswordField.setStyle(fieldStyle());

        overlayMessage.setTextFill(Color.web("#8b1a1a"));
        overlayMessage.setFont(Font.font("KaiTi", FontWeight.BOLD, 18));
        overlayMessage.setVisible(false);

        HBox accountActions = new HBox(12);
        accountActions.setAlignment(Pos.CENTER_RIGHT);
        Button logoutButton = new Button("退出登录");
        logoutButton.setStyle(secondaryButtonStyle());
        logoutButton.setOnAction(evt -> handleLogoutAction());
        Button deleteButton = new Button("注销账号");
        deleteButton.setStyle(dangerButtonStyle());
        deleteButton.setOnAction(evt -> handleAccountDeletion());
        accountActions.getChildren().addAll(logoutButton, deleteButton);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button saveButton = new Button("保存");
        saveButton.setStyle(primaryButtonStyle());
        saveButton.setOnAction(evt -> submitProfile());
        Button cancelButton = new Button("取消");
        cancelButton.setStyle(secondaryButtonStyle());
        cancelButton.setOnAction(evt -> hideProfileEditor());
        buttons.getChildren().addAll(cancelButton, saveButton);

        form.getChildren().addAll(header, nicknameField, avatarChooser,
            oldPasswordField, newPasswordField, confirmPasswordField,
            accountActions, overlayMessage, buttons);

        card.getChildren().add(form);

        profileOverlay.getChildren().setAll(backdrop, card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private void showProfileEditor() {
        if (currentProfile == null) {
            return;
        }
        nicknameField.setText(currentProfile.nickname());
        avatarPathLabel.setText(selectedAvatarPath != null ? selectedAvatarPath : "未选择头像");
        oldPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        overlayMessage.setVisible(false);
        profileOverlay.setVisible(true);
        profileOverlay.setOpacity(1);
        profileOverlay.setPickOnBounds(true);
    }

    private void hideProfileEditor() {
        profileOverlay.setVisible(false);
        profileOverlay.setOpacity(0);
        profileOverlay.setPickOnBounds(false);
    }

    private void handleLogoutAction() {
        hideProfileEditor();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void handleAccountDeletion() {
        if (currentProfile == null) {
            showOverlayMessage("请先登录");
            return;
        }
        if (onAccountDeletion == null) {
            showOverlayMessage("无法执行账号注销");
            return;
        }
        String password = oldPasswordField.getText();
        if (password == null || password.isBlank()) {
            showOverlayMessage("请输入原密码以确认注销");
            oldPasswordField.requestFocus();
            return;
        }
        AccountService.AccountDeletionResult result = onAccountDeletion.apply(new AccountDeletionRequest(password));
        if (result == null) {
            showOverlayMessage("注销账号失败");
            return;
        }
        if (!result.success()) {
            showOverlayMessage(result.message());
            return;
        }
        hideProfileEditor();
    }

    private void chooseAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择头像");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        if (getScene() == null) {
            return;
        }
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            AvatarCropper.Result result = AvatarCropper.crop(file, getScene().getWindow());
            if (result != null) {
                selectedAvatarPath = result.savedPath();
                avatarPathLabel.setText(selectedAvatarPath);
                avatarCircle.setFill(new ImagePattern(result.preview()));
            }
        }
    }

    private void submitProfile() {
        if (currentProfile == null) {
            return;
        }
        String nickname = nicknameField.getText();
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (nickname == null || nickname.trim().isEmpty()) {
            showOverlayMessage("昵称不能为空");
            nicknameField.requestFocus();
            return;
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                showOverlayMessage("新密码至少需要 6 位");
                newPasswordField.requestFocus();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showOverlayMessage("两次输入的密码不一致");
                confirmPasswordField.requestFocus();
                return;
            }
            if (oldPassword == null || oldPassword.isBlank()) {
                showOverlayMessage("请先输入原密码");
                oldPasswordField.requestFocus();
                return;
            }
        }

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                nickname,
                selectedAvatarPath,
                oldPassword,
                newPassword,
                confirmPassword);
        AccountService.ProfileUpdateResult result = onProfileUpdate.apply(request);
        if (result == null) {
            showOverlayMessage("更新资料失败");
            return;
        }
        if (!result.success()) {
            showOverlayMessage(result.message());
            return;
        }

        currentProfile = result.profile();
        selectedAvatarPath = currentProfile.avatarPath();
        updateAvatarDisplay();
        nicknameLabel.setText(currentProfile.nickname());
        hideProfileEditor();
        showMessage(result.message());
    }

    private void showOverlayMessage(String text) {
        overlayMessage.setText(text);
        overlayMessage.setTextFill(Color.web("#8b1a1a"));
        overlayMessage.setVisible(true);
    }

    private void updateAvatarDisplay() {
        if (selectedAvatarPath != null) {
            File file = new File(selectedAvatarPath);
            if (file.exists()) {
                try {
                    Image image = new Image(file.toURI().toString());
                    avatarCircle.setFill(new ImagePattern(image));
                    return;
                } catch (Exception ignored) {
                    // fallback below
                }
            }
        }
        avatarCircle.setFill(Color.web("#d6a354"));
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setPrefHeight(56);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFont(Font.font("KaiTi", FontWeight.BOLD, 22));
        button.setStyle("-fx-background-color: linear-gradient(to bottom, #f8d57c, #d29a3e);" +
                "-fx-text-fill: #3b1f05; -fx-background-radius: 30; -fx-border-color: rgba(59,31,5,0.4);" +
                "-fx-border-width: 1.6; -fx-border-radius: 30; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(84,42,14,0.35), 18, 0.32, 0, 4);");
        return button;
    }

    private static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setPrefHeight(56);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFont(Font.font("KaiTi", FontWeight.BOLD, 22));
        button.setStyle("-fx-background-color: rgba(239,221,186,0.65); -fx-text-fill: #4a2a15;" +
                "-fx-background-radius: 30; -fx-border-color: rgba(127,76,35,0.45);" +
                "-fx-border-width: 1.4; -fx-border-radius: 30; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(87,53,21,0.28), 16, 0.25, 0, 3);");
        return button;
    }

    private static String fieldStyle() {
        return "-fx-background-color: rgba(255,244,226,0.95); -fx-background-radius: 16;" +
                "-fx-border-radius: 16; -fx-border-color: rgba(126,80,37,0.45); -fx-border-width: 1.1;" +
                "-fx-padding: 8 14 8 14; -fx-text-fill: #321a09;";
    }

    private static String primaryButtonStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #f6c66b, #cc8a2c);" +
                "-fx-text-fill: #3b1f05; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
    }

    private static String secondaryButtonStyle() {
        return "-fx-background-color: rgba(217,186,140,0.62);" +
                "-fx-text-fill: #5b3922; -fx-background-radius: 20; -fx-cursor: hand;";
    }

    private static String primaryMiniButtonStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #f8d57c, #d29a3e);" +
                "-fx-text-fill: #331b07; -fx-background-radius: 18; -fx-cursor: hand;";
    }

    private static String dangerButtonStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #f87171, #dc2626);" +
                "-fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand;";
    }

    private void updateScale() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        double scale = Math.min(width / BASE_WIDTH, height / BASE_HEIGHT);
        if (!Double.isFinite(scale) || scale <= 0) {
            scale = 1.0;
        }
        contentScale.setX(scale);
        contentScale.setY(scale);
    }

    private void setBackgroundTexture() {
        setStyle("-fx-background-color: radial-gradient(center 50% 40%, radius 120%, #f4e3c2, #b9854a);");
    }
}
