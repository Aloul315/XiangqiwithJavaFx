package org.example.view;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.layout.Priority;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.SVGPath;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.stage.FileChooser;
import javafx.animation.TranslateTransition;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.example.data.AccountService;

/**
 * 登录与注册视图组件，提供用户身份验证、账号注册及游客登录功能。
 * <p>
 * 该视图包含一个卡片式布局，左侧和右侧装饰有卡通大象动画。
 * 支持账号密码登录、记住密码、显示/隐藏密码、头像显示以及新用户注册流程。
 * </p>
 */
public class LoginView extends StackPane {

    /**
     * 登录请求的数据载体。
     *
     * @param account    用户输入的账号或昵称
     * @param password   用户输入的密码
     * @param rememberMe 是否勾选“记住密码”
     */
    public record LoginRequest(String account, String password, boolean rememberMe) {}

    /**
     * 注册请求的数据载体。
     *
     * @param account    新账号
     * @param nickname   用户昵称
     * @param password   设置的密码
     * @param avatarPath 头像文件的本地路径
     */
    public record RegistrationRequest(String account, String nickname, String password, String avatarPath) {}

    private static final double ACCOUNT_CELL_AVATAR_RADIUS = 14;

    private final ComboBox<String> accountBox = new ComboBox<>();
    private final ObservableList<String> knownAccounts = FXCollections.observableArrayList();
    private final PasswordField passwordField = new PasswordField();
    private final TextField passwordVisibleField = new TextField();
    private final CheckBox rememberBox = new CheckBox("记住密码");
    private final Label loginMessageLabel = new Label();
    private final Circle avatarCircle = new Circle(40);
    private final Label activeNicknameLabel = new Label("欢迎登录");
    // 按半径缓存默认问号头像，避免重复绘制 Canvas
    private final Map<Double, ImagePattern> placeholderCache = new HashMap<>();

    private final TextField registerAccountField = new TextField();
    private final PasswordField registerPasswordField = new PasswordField();
    private final PasswordField registerConfirmField = new PasswordField();
    private final TextField registerNicknameField = new TextField();
    private final Label registerMessageLabel = new Label();
    private final Circle registerAvatarCircle = new Circle(36);
    private final Label registerAvatarHint = new Label("未选择头像");
    private String registrationAvatarPath;

    private final StackPane contentStack = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private final Label overlayLabel = new Label();

    private VBox loginContent;
    private VBox registerContent;
    private StackPane glassCard;
    private SequentialTransition overlayTransition;
    private Timeline shakeTimeline;
    private final Function<String, AccountService.UserProfile> profileProvider;
    private String displayedAvatarPath;
    private final Runnable onGuestLogin;

    /**
     * 构造登录视图。
     *
     * @param onLogin         登录操作的回调函数
     * @param onRegister      注册操作的回调函数
     * @param onGuestLogin    游客登录的回调函数
     * @param profileProvider 用于根据账号获取用户资料的函数
     * @param initialAccounts 初始已知的账号列表（用于自动补全）
     */
    public LoginView(BiConsumer<LoginRequest, LoginView> onLogin,
             BiConsumer<RegistrationRequest, LoginView> onRegister,
             Runnable onGuestLogin,
             Function<String, AccountService.UserProfile> profileProvider,
             List<String> initialAccounts) {
    Objects.requireNonNull(onLogin, "onLogin");
    Objects.requireNonNull(onRegister, "onRegister");
    this.onGuestLogin = Objects.requireNonNull(onGuestLogin, "onGuestLogin");
    this.profileProvider = Objects.requireNonNull(profileProvider, "profileProvider");
    if (initialAccounts != null) {
        knownAccounts.setAll(initialAccounts);
    }

        setPrefSize(320, 520);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setStyle("-fx-background-color: #ffffff;");
        getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

        glassCard = buildCard();

        CartoonElephant leftElephant = new CartoonElephant();
        CartoonElephant rightElephant = new CartoonElephant();

        StackPane.setAlignment(leftElephant, Pos.TOP_LEFT);
        StackPane.setAlignment(rightElephant, Pos.TOP_RIGHT);
        StackPane.setMargin(leftElephant, new Insets(-35, 0, 0, -10));
        StackPane.setMargin(rightElephant, new Insets(-35, -10, 0, 0));

        leftElephant.setRotate(-15);
        rightElephant.setRotate(15);

        glassCard.getChildren().addAll(leftElephant, rightElephant);

        this.setOnMouseMoved(evt -> {
            leftElephant.lookAt(evt.getSceneX(), evt.getSceneY());
            rightElephant.lookAt(evt.getSceneX(), evt.getSceneY());
        });

        buildLoginContent(onLogin, onRegister);
        buildRegistrationContent(onRegister);
        contentStack.getChildren().setAll(loginContent);
        glassCard.getChildren().add(contentStack);

        configureOverlayLayer();
        getChildren().addAll(glassCard, overlayLayer);

        refreshAvatarForAccount(currentAccountInput());

        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            leftElephant.setCovering(newVal);
            rightElephant.setCovering(newVal);
        });
        passwordVisibleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                leftElephant.setCovering(true);
                rightElephant.setCovering(true);
            }
        });
    }

    private StackPane buildCard() {
        StackPane container = new StackPane();
        container.setPadding(new Insets(52, 28, 20, 28));
        return container;
    }

    private void buildLoginContent(BiConsumer<LoginRequest, LoginView> onLogin,
                   BiConsumer<RegistrationRequest, LoginView> onRegister) {
    VBox content = new VBox(12);
    content.setAlignment(Pos.CENTER);

    applyDefaultAvatar(avatarCircle);
    avatarCircle.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.25)));

    activeNicknameLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
    activeNicknameLabel.setTextFill(Color.web("#1f2933"));

    Text title = new Text("登录");
    title.setFill(Color.web("#1f2933"));
    title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 26));

    accountBox.setEditable(true);
    accountBox.setItems(knownAccounts);
    accountBox.setVisibleRowCount(6);
    accountBox.setPrefHeight(42);
    accountBox.setMaxWidth(Double.MAX_VALUE);
    accountBox.setPromptText("账号");
    accountBox.getStyleClass().add("account-combo");
    TextField accountEditor = accountBox.getEditor();
    accountEditor.setPromptText("账号");
    accountEditor.textProperty().addListener((obs, oldText, newText) -> refreshAvatarForAccount(newText));
    accountBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null) {
            String display = displayNameForAccount(newVal);
            if (!display.equals(accountEditor.getText())) {
                accountEditor.setText(display);
                accountEditor.positionCaret(display.length());
            }
            refreshAvatarForAccount(newVal);
        }
    });
    accountBox.setOnAction(evt -> refreshAvatarForAccount(currentAccountInput()));
    accountBox.setCellFactory(listView -> new AccountListCell(ACCOUNT_CELL_AVATAR_RADIUS));
    accountBox.setButtonCell(new AccountListCell(ACCOUNT_CELL_AVATAR_RADIUS));

    passwordField.setPromptText("密码");
    passwordField.setPrefHeight(42);
    passwordField.setMaxWidth(Double.MAX_VALUE);
    passwordField.getStyleClass().add("login-field");
    passwordField.setStyle("-fx-padding: 10 40 10 14;");

    passwordVisibleField.setPromptText("密码");
    passwordVisibleField.setPrefHeight(42);
    passwordVisibleField.setMaxWidth(Double.MAX_VALUE);
    passwordVisibleField.getStyleClass().add("login-field");
    passwordVisibleField.setStyle("-fx-padding: 10 40 10 14;");
    passwordVisibleField.setVisible(false);
    passwordVisibleField.setManaged(false);
    passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
    passwordVisibleField.setOnAction(evt -> validateAndSubmit(onLogin));

    SVGPath eyeIcon = new SVGPath();
    eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
    eyeIcon.setFill(Color.web("#64748b"));
    eyeIcon.setScaleX(0.75);
    eyeIcon.setScaleY(0.75);

    StackPane eyeButton = new StackPane(eyeIcon);
    eyeButton.setCursor(Cursor.HAND);
    eyeButton.setPrefSize(40, 42);
    eyeButton.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    eyeButton.setAlignment(Pos.CENTER);

    eyeButton.setOnMousePressed(evt -> {
        passwordVisibleField.setVisible(true);
        passwordVisibleField.setManaged(true);
        passwordField.setVisible(false);
        passwordField.setManaged(false);
        eyeIcon.setFill(Color.web("#2563eb"));
    });

    eyeButton.setOnMouseReleased(evt -> {
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        eyeIcon.setFill(Color.web("#64748b"));
        passwordField.requestFocus();
        passwordField.positionCaret(passwordField.getText().length());
    });

    StackPane passwordContainer = new StackPane(passwordField, passwordVisibleField, eyeButton);
    StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);

    rememberBox.setTextFill(Color.web("#475569"));
    rememberBox.setFont(Font.font("Microsoft YaHei", 13));

    loginMessageLabel.setTextFill(Color.web("#d14343"));
    loginMessageLabel.setFont(Font.font("Microsoft YaHei", 12));
    loginMessageLabel.setVisible(false);

    Button loginButton = new Button("登 录");
    loginButton.setPrefHeight(44);
    loginButton.setMaxWidth(Double.MAX_VALUE);
    loginButton.setStyle("-fx-background-color: linear-gradient(to right, #16a34a, #4ade80);" +
        "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 24;" +
        "-fx-cursor: hand;");
    loginButton.setOnAction(evt -> validateAndSubmit(onLogin));

    Button guestButton = new Button("游客登录");
    guestButton.setPrefHeight(44);
    guestButton.setMaxWidth(Double.MAX_VALUE);
    guestButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.6); -fx-text-fill: #1f2933;" +
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 24;" +
            "-fx-cursor: hand; -fx-border-color: rgba(31, 41, 51, 0.15); -fx-border-width: 1; -fx-border-radius: 24;");
    guestButton.setOnAction(evt -> onGuestLogin.run());

    accountEditor.setOnAction(evt -> passwordField.requestFocus());
    passwordField.setOnAction(evt -> validateAndSubmit(onLogin));

    HBox links = new HBox();
    links.setAlignment(Pos.CENTER);
    Label findPwd = buildLink("找回密码", () -> {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("找回密码");
        alert.setHeaderText(null);
        alert.setContentText("请联系管理员QQ：3566079870");
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    });
    Label register = buildLink("注册账号", this::showRegistrationForm);
    
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    
    links.getChildren().addAll(findPwd, spacer, register);

    content.getChildren().addAll(avatarCircle, activeNicknameLabel, title, accountBox, passwordContainer,
        rememberBox, loginMessageLabel, loginButton, guestButton, links);

    loginContent = content;
    }

    private void buildRegistrationContent(BiConsumer<RegistrationRequest, LoginView> onRegister) {
    VBox content = new VBox(16);
    content.setAlignment(Pos.CENTER);

    Text title = new Text("注册账号");
    title.setFill(Color.web("#1f2933"));
    title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));

    applyDefaultAvatar(registerAvatarCircle);
    registerAvatarCircle.setStroke(Color.web("#1e3a8a"));
    registerAvatarCircle.setStrokeWidth(1.6);

    registerAvatarHint.setTextFill(Color.web("#475569"));
    registerAvatarHint.setFont(Font.font("Microsoft YaHei", 12));

    Button chooseAvatarButton = new Button("选择头像");
    chooseAvatarButton.setPrefHeight(32);
    chooseAvatarButton.setStyle("-fx-background-color: linear-gradient(to right, #60a5fa, #2563eb); -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 16 0 16;");
    chooseAvatarButton.setOnAction(evt -> chooseRegistrationAvatar());

    HBox avatarRow = new HBox(12, registerAvatarCircle, chooseAvatarButton, registerAvatarHint);
    avatarRow.setAlignment(Pos.CENTER_LEFT);
    avatarRow.setMaxWidth(Double.MAX_VALUE);

    registerAccountField.setPromptText("请输入账号");
    registerNicknameField.setPromptText("请输入昵称");
    registerPasswordField.setPromptText("请输入密码（至少 6 位）");
    registerConfirmField.setPromptText("确认密码");

    registerAccountField.setPrefHeight(42);
    registerNicknameField.setPrefHeight(42);
    registerPasswordField.setPrefHeight(42);
    registerConfirmField.setPrefHeight(42);

    registerAccountField.setStyle(commonFieldStyle());
    registerNicknameField.setStyle(commonFieldStyle());
    registerPasswordField.setStyle(commonFieldStyle());
    registerConfirmField.setStyle(commonFieldStyle());

    registerMessageLabel.setTextFill(Color.web("#d14343"));
    registerMessageLabel.setFont(Font.font("Microsoft YaHei", 12));
    registerMessageLabel.setVisible(false);

    resetRegistrationAvatar();

    Button submitButton = new Button("注 册");
    submitButton.setPrefHeight(44);
    submitButton.setMaxWidth(Double.MAX_VALUE);
    submitButton.setStyle("-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
        "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 24;" +
        "-fx-cursor: hand;");
    submitButton.setOnAction(evt -> validateAndSubmitRegistration(onRegister));

    registerConfirmField.setOnAction(evt -> validateAndSubmitRegistration(onRegister));

    Label backToLogin = buildLink("返回登录", this::showLoginForm);

    content.getChildren().addAll(title, avatarRow, registerAccountField, registerNicknameField, registerPasswordField,
        registerConfirmField, registerMessageLabel, submitButton, backToLogin);

    registerContent = content;
    }

    private void validateAndSubmit(BiConsumer<LoginRequest, LoginView> onLogin) {
        String rawInput = currentAccountInput();
        String account = resolveAccountIdentifier(rawInput);
        String password = passwordField.getText();
        boolean remember = rememberBox.isSelected();

        if (account == null || account.isBlank()) {
            showError("请输入账号");
            accountBox.getEditor().requestFocus();
            return;
        }
        if (password.isBlank()) {
            showError("请输入密码");
            passwordField.requestFocus();
            return;
        }

        clearMessage();
        String display = displayNameForAccount(account);
        accountBox.getEditor().setText(display);
        accountBox.getEditor().positionCaret(display.length());
        onLogin.accept(new LoginRequest(account, password, remember), this);
    }

    private void validateAndSubmitRegistration(BiConsumer<RegistrationRequest, LoginView> onRegister) {
        String account = registerAccountField.getText().trim();
        String nickname = registerNicknameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirm = registerConfirmField.getText();

        if (account.isEmpty()) {
            showRegistrationError("账号不能为空");
            registerAccountField.requestFocus();
            return;
        }
        if (nickname.isEmpty()) {
            showRegistrationError("昵称不能为空");
            registerNicknameField.requestFocus();
            return;
        }
        if (password.length() < 6) {
            showRegistrationError("密码至少需要 6 位");
            registerPasswordField.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            showRegistrationError("两次输入的密码不一致");
            registerConfirmField.requestFocus();
            return;
        }

        clearRegistrationMessage();
    onRegister.accept(new RegistrationRequest(account, nickname, password, registrationAvatarPath), this);
    }

    /**
     * 显示错误提示信息。
     *
     * @param message 错误内容
     */
    public void showError(String message) {
        showMessage(message, Color.web("#d14343"));
    }

    /**
     * 显示成功或普通提示信息。
     *
     * @param message 提示内容
     */
    public void showInfo(String message) {
        showMessage(message, Color.web("#15803d"));
    }

    /**
     * 清除当前的提示信息。
     */
    public void clearMessage() {
        showMessage(null, null);
    }

    /**
     * 预填充账号输入框。
     *
     * @param account 要填充的账号
     */
    public void prefillAccount(String account) {
        if (account == null || account.isBlank()) {
            return;
        }
        if (!knownAccounts.contains(account)) {
            knownAccounts.add(0, account);
        }
        accountBox.getSelectionModel().select(account);
        String display = displayNameForAccount(account);
        accountBox.getEditor().setText(display);
        accountBox.getEditor().positionCaret(display.length());
        refreshAvatarForAccount(account);
    }

    /**
     * 更新已知账号列表。
     *
     * @param accounts 新的账号列表
     */
    public void updateKnownAccounts(List<String> accounts) {
        String current = accountBox.getEditor().getText();
        String resolved = resolveAccountIdentifier(current);
        if (accounts == null) {
            knownAccounts.clear();
        } else {
            knownAccounts.setAll(accounts);
        }
        if (resolved != null && !resolved.isBlank() && !knownAccounts.contains(resolved)) {
            knownAccounts.add(0, resolved);
        }
    }

    private String currentAccountInput() {
        String text = accountBox.getEditor().getText();
        return text != null ? text.trim() : "";
    }

    private void showMessage(String message, Color color) {
        if (message == null || message.isBlank()) {
            loginMessageLabel.setVisible(false);
            loginMessageLabel.setText("");
        } else {
            if (color != null) {
                loginMessageLabel.setTextFill(color);
            }
            loginMessageLabel.setText(message);
            loginMessageLabel.setVisible(true);
        }
    }

    public void showRegistrationError(String message) {
        showRegistrationMessage(message, Color.web("#d14343"));
    }

    public void showRegistrationInfo(String message) {
        showRegistrationMessage(message, Color.web("#15803d"));
    }

    public void clearRegistrationMessage() {
        showRegistrationMessage(null, null);
    }

    private void showRegistrationMessage(String message, Color color) {
        if (message == null || message.isBlank()) {
            registerMessageLabel.setVisible(false);
            registerMessageLabel.setText("");
        } else {
            if (color != null) {
                registerMessageLabel.setTextFill(color);
            }
            registerMessageLabel.setText(message);
            registerMessageLabel.setVisible(true);
        }
    }

    /**
     * 切换到注册表单界面。
     */
    public void showRegistrationForm() {
        clearMessage();
        clearRegistrationMessage();
        overlayLayer.setVisible(false);
        overlayLayer.setOpacity(0);
        contentStack.getChildren().setAll(registerContent);
    }

    /**
     * 切换到登录表单界面。
     */
    public void showLoginForm() {
        contentStack.getChildren().setAll(loginContent);
        refreshAvatarForAccount(currentAccountInput());
    }

    /**
     * 切换到登录表单界面并显示提示信息。
     *
     * @param message 要显示的提示信息
     */
    public void showLoginFormWithInfo(String message) {
        showLoginForm();
        showInfo(message);
    }

    private void refreshAvatarForAccount(String input) {
        // 根据当前输入同步头像、昵称与密码显示，便于快速登录
        String account = input != null ? input.trim() : "";
        if (account.isEmpty()) {
            resetAvatarAppearance();
            updateNicknameDisplay(null);
            passwordField.clear();
            return;
        }
        updatePasswordField(account);
        String avatarPath = resolveAvatarPath(account);
        if (avatarPath == null) {
            resetAvatarAppearance();
            updateNicknameDisplay(account);
            return;
        }

        if (Objects.equals(displayedAvatarPath, avatarPath)) {
            updateNicknameDisplay(account);
            return;
        }

        Image image = loadAvatarImage(avatarPath, avatarCircle.getRadius() * 2);
        if (image == null) {
            resetAvatarAppearance();
            updateNicknameDisplay(account);
            return;
        }
        avatarCircle.setFill(new ImagePattern(image));
        displayedAvatarPath = avatarPath;
        updateNicknameDisplay(account);
    }

    private void resetAvatarAppearance() {
        displayedAvatarPath = null;
        applyDefaultAvatar(avatarCircle);
    }

    private String resolveAccountIdentifier(String input) {
        // 允许用户输入账号或昵称，优先匹配已知账号
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        AccountService.UserProfile direct = safeLoadProfile(trimmed);
        if (direct != null) {
            return trimmed;
        }
        for (String account : knownAccounts) {
            AccountService.UserProfile profile = safeLoadProfile(account);
            if (profile == null) {
                continue;
            }
            String nickname = profile.nickname();
            if (nickname != null && nickname.trim().equals(trimmed)) {
                return account;
            }
        }
        return trimmed;
    }

    private AccountService.UserProfile safeLoadProfile(String account) {
        if (account == null) {
            return null;
        }
        String trimmed = account.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return profileProvider.apply(trimmed);
        } catch (Exception ex) {
            return null;
        }
    }

    private AccountService.UserProfile fetchProfile(String rawAccount) {
        String resolved = resolveAccountIdentifier(rawAccount);
        if (resolved == null || resolved.isBlank()) {
            return null;
        }
        return safeLoadProfile(resolved);
    }

    private String resolveNickname(String rawAccount) {
        AccountService.UserProfile profile = fetchProfile(rawAccount);
        if (profile == null) {
            return null;
        }
        String nickname = profile.nickname();
        return nickname != null && !nickname.isBlank() ? nickname.trim() : null;
    }

    private void updateNicknameDisplay(String account) {
        String nickname = resolveNickname(account);
        if (nickname != null) {
            activeNicknameLabel.setText(nickname);
        } else if (account != null && !account.isBlank()) {
            activeNicknameLabel.setText(account.trim());
        } else {
            activeNicknameLabel.setText("欢迎登录");
        }
    }

    private void updatePasswordField(String account) {
        // 若用户资料保存了明文密码则自动填充
        String password = resolvePassword(account);
        if (password != null) {
            passwordField.setText(password);
        } else {
            passwordField.clear();
        }
    }

    private String displayNameForAccount(String account) {
        String nickname = resolveNickname(account);
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        return account != null ? account.trim() : "";
    }

    private String resolveAvatarPath(String rawAccount) {
        AccountService.UserProfile profile = fetchProfile(rawAccount);
        if (profile == null) {
            return null;
        }
        String avatarPath = profile.avatarPath();
        if (avatarPath == null || avatarPath.isBlank()) {
            return null;
        }
        File file = new File(avatarPath);
        if (!file.exists()) {
            return null;
        }
        return file.getAbsolutePath();
    }

    private String resolvePassword(String rawAccount) {
        AccountService.UserProfile profile = fetchProfile(rawAccount);
        if (profile == null) {
            return null;
        }
        String stored = profile.password();
        return stored != null && !stored.isBlank() ? stored : null;
    }

    private Image loadAvatarImage(String absolutePath, double requestedSize) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return null;
        }
        File file = new File(absolutePath);
        if (!file.exists()) {
            return null;
        }
        try {
            Image image = new Image(file.toURI().toString(), requestedSize, requestedSize, true, true);
            return image.isError() ? null : image;
        } catch (Exception ex) {
            return null;
        }
    }

    private void applyAvatarPreview(Circle circle, String account) {
        if (circle == null) {
            return;
        }
        applyDefaultAvatar(circle);
        String avatarPath = resolveAvatarPath(account);
        if (avatarPath == null) {
            return;
        }
        Image image = loadAvatarImage(avatarPath, circle.getRadius() * 2);
        if (image == null) {
            return;
        }
        circle.setFill(new ImagePattern(image));
    }

    /**
     * 清空注册表单的所有输入字段。
     */
    public void clearRegistrationFields() {
        registerAccountField.clear();
        registerNicknameField.clear();
        registerPasswordField.clear();
        registerConfirmField.clear();
        resetRegistrationAvatar();
    }

    private void chooseRegistrationAvatar() {
        if (getScene() == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择头像");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        AvatarCropper.Result result = AvatarCropper.crop(file, getScene().getWindow());
        if (result == null) {
            return;
        }
        registrationAvatarPath = result.savedPath();
        registerAvatarCircle.setFill(new ImagePattern(result.preview()));
        registerAvatarHint.setText("已选择头像");
    }

    private void resetRegistrationAvatar() {
        registrationAvatarPath = null;
        applyDefaultAvatar(registerAvatarCircle);
        registerAvatarHint.setText("未选择头像");
    }

    /**
     * 显示登录失败的反馈（震动动画和错误提示）。
     *
     * @param message 错误信息
     */
    public void showLoginFailureFeedback(String message) {
        showError(message);
        playShakeAnimation();
        showOverlayMessage(message);
    }

    private final class AccountListCell extends ListCell<String> {
        private final Circle avatarCircle;
        private final Label nicknameLabel = new Label();
        private final Label accountLabel = new Label();
        private final VBox textBox;
        private final HBox container;

        private AccountListCell(double radius) {
            this.avatarCircle = new Circle(radius);
            LoginView.this.applyDefaultAvatar(avatarCircle);
            avatarCircle.setStroke(Color.color(0.12, 0.33, 0.62, 0.8));
            avatarCircle.setStrokeWidth(0.8);

            nicknameLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 13));
            nicknameLabel.setTextFill(Color.web("#0f172a"));

            accountLabel.setFont(Font.font("Microsoft YaHei", 11));
            accountLabel.setTextFill(Color.web("#475569"));

            textBox = new VBox(2, nicknameLabel, accountLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);

            container = new HBox(8, avatarCircle, textBox);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(String account, boolean empty) {
            super.updateItem(account, empty);
            if (empty || account == null || account.isBlank()) {
                setText(null);
                setGraphic(null);
                return;
            }
            String trimmed = account.trim();
            applyAvatarPreview(avatarCircle, trimmed);

            String nickname = resolveNickname(trimmed);
            if (nickname != null && !nickname.equals(trimmed)) {
                nicknameLabel.setText(nickname);
                accountLabel.setText(trimmed);
                accountLabel.setVisible(true);
                accountLabel.setManaged(true);
            } else {
                nicknameLabel.setText(trimmed);
                accountLabel.setVisible(false);
                accountLabel.setManaged(false);
            }

            setText(null);
            setGraphic(container);
        }
    }

    private void configureOverlayLayer() {
        overlayLayer.setPickOnBounds(false);
        overlayLayer.setMouseTransparent(true);
        overlayLayer.setVisible(false);
        overlayLayer.setOpacity(0);
        overlayLayer.setAlignment(Pos.CENTER);

        overlayLabel.setTextFill(Color.web("#f87171"));
        overlayLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));

        StackPane messageBox = new StackPane(overlayLabel);
        messageBox.setPadding(new Insets(14, 32, 14, 32));
        messageBox.setStyle("-fx-background-color: rgba(0,0,0,0.75); -fx-background-radius: 16;");

        overlayLayer.getChildren().add(messageBox);
    }

    private void showOverlayMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        overlayLabel.setText(message);
        overlayLayer.setVisible(true);
        overlayLayer.setOpacity(0);

        if (overlayTransition != null) {
            overlayTransition.stop();
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), overlayLayer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), overlayLayer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(evt -> overlayLayer.setVisible(false));

        overlayTransition = new SequentialTransition(fadeIn, fadeOut);
        overlayTransition.playFromStart();
    }

    private void playShakeAnimation() {
        if (glassCard == null) {
            return;
        }
        if (shakeTimeline != null) {
            shakeTimeline.stop();
        }
        glassCard.setTranslateX(0);
        shakeTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glassCard.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60), new KeyValue(glassCard.translateXProperty(), -12)),
                new KeyFrame(Duration.millis(120), new KeyValue(glassCard.translateXProperty(), 12)),
                new KeyFrame(Duration.millis(180), new KeyValue(glassCard.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(240), new KeyValue(glassCard.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(300), new KeyValue(glassCard.translateXProperty(), -6)),
                new KeyFrame(Duration.millis(360), new KeyValue(glassCard.translateXProperty(), 6)),
                new KeyFrame(Duration.millis(420), new KeyValue(glassCard.translateXProperty(), 0))
        );
        shakeTimeline.setOnFinished(evt -> glassCard.setTranslateX(0));
        shakeTimeline.playFromStart();
    }

    private void applyDefaultAvatar(Circle circle) {
        // 对无头像的圆形节点绘制黑底白问号
        if (circle == null) {
            return;
        }
        double radius = circle.getRadius();
        if (radius <= 0) {
            circle.setFill(Color.BLACK);
            return;
        }
        ImagePattern pattern = placeholderCache.computeIfAbsent(radius, this::createPlaceholderPattern);
        circle.setFill(pattern);
    }

    private ImagePattern createPlaceholderPattern(double radius) {
        double diameter = Math.max(2.0, radius * 2.0);
        int size = (int) Math.ceil(diameter);
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, size, size);
        gc.setFill(Color.BLACK);
        gc.fillOval(0, 0, size, size);
        gc.setFill(Color.WHITE);
        double fontSize = diameter * 0.62;
        gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("?", size / 2.0, size / 2.0);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(null, image);
        return new ImagePattern(image);
    }

    private static String commonFieldStyle() {
        return "-fx-background-color: rgba(15,23,42,0.06); -fx-background-radius: 16;" +
                "-fx-border-radius: 16; -fx-border-color: rgba(37,99,235,0.25); -fx-border-width: 1;" +
                "-fx-font-size: 14px; -fx-text-fill: #1f2937; -fx-prompt-text-fill: #94a3b8;";
    }

    private static Label buildLink(String text, Runnable action) {
        Label link = new Label(text);
        link.setTextFill(Color.web("#2563eb"));
        link.setFont(Font.font("Microsoft YaHei", 12));
        link.setOnMouseEntered(evt -> link.setUnderline(true));
        link.setOnMouseExited(evt -> link.setUnderline(false));
        link.setCursor(javafx.scene.Cursor.HAND);
        link.setOnMouseClicked(evt -> action.run());
        return link;
    }

    private static class CartoonElephant extends Group {
        private final TranslateTransition leftHandMove;
        private final TranslateTransition rightHandMove;
        private final Circle leftPupil;
        private final Circle rightPupil;
        private final double eyeRadius = 4.0;
        private final double pupilRadius = 1.5;
        private final double maxEyeMove = 2.0;
        private final double leftEyeX = -8;
        private final double leftEyeY = -6;
        private final double rightEyeX = 8;
        private final double rightEyeY = -6;

        public CartoonElephant() {
            // 头部
            Circle head = new Circle(0, 0, 24);
            head.setFill(Color.web("#94a3b8")); // Slate 400
            head.setStroke(Color.web("#64748b"));
            head.setStrokeWidth(1.5);

            // 耳朵
            Ellipse leftEar = new Ellipse(-26, -4, 14, 18);
            leftEar.setFill(Color.web("#cbd5e1")); // Slate 300
            leftEar.setStroke(Color.web("#94a3b8"));
            leftEar.setRotate(-20);

            Ellipse rightEar = new Ellipse(26, -4, 14, 18);
            rightEar.setFill(Color.web("#cbd5e1"));
            rightEar.setStroke(Color.web("#94a3b8"));
            rightEar.setRotate(20);

            // 眼睛 (眼白 + 瞳孔)
            Circle leftSclera = new Circle(leftEyeX, leftEyeY, eyeRadius, Color.WHITE);
            leftPupil = new Circle(leftEyeX, leftEyeY, pupilRadius, Color.web("#1e293b"));

            Circle rightSclera = new Circle(rightEyeX, rightEyeY, eyeRadius, Color.WHITE);
            rightPupil = new Circle(rightEyeX, rightEyeY, pupilRadius, Color.web("#1e293b"));

            // 鼻子
            QuadCurve trunk = new QuadCurve(-4, 8, 0, 35, 8, 28);
            trunk.setFill(Color.TRANSPARENT);
            trunk.setStroke(Color.web("#94a3b8"));
            trunk.setStrokeWidth(6);
            trunk.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

            // 手 (初始位置在下面)
            Circle leftHand = new Circle(-12, 22, 7);
            leftHand.setFill(Color.web("#94a3b8"));
            leftHand.setStroke(Color.web("#64748b"));

            Circle rightHand = new Circle(12, 22, 7);
            rightHand.setFill(Color.web("#94a3b8"));
            rightHand.setStroke(Color.web("#64748b"));

            // 组装身体 (耳朵在头后面)
            Group body = new Group(leftEar, rightEar, head, leftSclera, leftPupil, rightSclera, rightPupil, trunk);
            getChildren().addAll(body, leftHand, rightHand);

            // 动画
            leftHandMove = new TranslateTransition(Duration.millis(200), leftHand);
            rightHandMove = new TranslateTransition(Duration.millis(200), rightHand);
        }

        public void setCovering(boolean covering) {
            if (covering) {
                // 移动手去捂眼睛
                leftHandMove.setToX(4);  // 向内
                leftHandMove.setToY(-26); // 向上
                rightHandMove.setToX(-4);
                rightHandMove.setToY(-26);
            } else {
                // 放下手
                leftHandMove.setToX(0);
                leftHandMove.setToY(0);
                rightHandMove.setToX(0);
                rightHandMove.setToY(0);
            }
            leftHandMove.play();
            rightHandMove.play();
        }

        public void lookAt(double sceneX, double sceneY) {
            javafx.geometry.Point2D local = this.sceneToLocal(sceneX, sceneY);
            if (local == null) return;

            updatePupil(leftPupil, leftEyeX, leftEyeY, local.getX(), local.getY());
            updatePupil(rightPupil, rightEyeX, rightEyeY, local.getX(), local.getY());
        }

        private void updatePupil(Circle pupil, double eyeX, double eyeY, double targetX, double targetY) {
            double dx = targetX - eyeX;
            double dy = targetY - eyeY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 0) {
                double move = Math.min(distance, maxEyeMove);
                double angle = Math.atan2(dy, dx);
                pupil.setTranslateX(Math.cos(angle) * move);
                pupil.setTranslateY(Math.sin(angle) * move);
            } else {
                pupil.setTranslateX(0);
                pupil.setTranslateY(0);
            }
        }
    }
}
