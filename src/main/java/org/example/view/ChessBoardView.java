package org.example.view;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.example.model.AbstractPiece;
import org.example.model.ChessBoardModel;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * 棋盘视图组件，负责渲染中国象棋棋盘、棋子、动画效果及处理用户交互。
 * <p>
 * 该类继承自 {@link StackPane}，使用 {@link Canvas} 进行高性能绘图。
 * 它监听 {@link ChessBoardModel} 的状态变化，并实时更新界面显示。
 * </p>
 */
public class ChessBoardView extends StackPane {
    private static final double DEFAULT_CELL_SIZE = 64;
    private static final double DEFAULT_MARGIN = 42;
    private static final double DEFAULT_PIECE_SIZE = DEFAULT_CELL_SIZE * 0.78;
    private static final double TOP_UI_PADDING = 10;
    private static final double BOTTOM_UI_PADDING = 10;
    private static final double SIDE_UI_PADDING = 10;
    private static final double RIGHT_SIDEBAR_WIDTH = 90;

    private final ChessBoardModel board;
    private final Map<String, Image> pieceTextures = new HashMap<>();
    private Image boardTexture;
    private final double[] columnPositions = new double[ChessBoardModel.COLS];
    private final double[] rowPositions = new double[ChessBoardModel.ROWS];
    private double cellSpacingX;
    private double cellSpacingY;
    private double marginX;
    private double marginY;
    private double pieceSize;
    private double pieceRadius;
    private double canvasWidth;
    private double canvasHeight;
    private final Canvas canvas;
    private final Pane effectsLayer;
    private final Pane motionLayer;
    private final Group boardGroup;
    private AbstractPiece selectedPiece;
    private final List<int[]> legalTargets = new ArrayList<>();
    private String specialMessage = "";
    private boolean specialMessageFromRed = true;
    private PauseTransition messageTimer;
    private AbstractPiece captureHighlight;
    private PauseTransition captureTimer;
    private String winnerLabel;
    private final Button restartButton;
    private final Button saveButton;
    private final Button exitButton;
    private final Button undoButton;
    private Animation activeKillAnimation;
    private boolean killEffectPending;
    private Node dragNode;
    private final Set<AbstractPiece> animatingPieces = new HashSet<>();
    private Timeline arrowRotationTimeline;
    private double targetArrowOffset;
    private Animation activeCaptureLaunch;
    private Node activeLaunchNode;
    private Runnable onSave;
    private Runnable onExit;
    private final VBox sideBar;
    private final javafx.scene.layout.HBox contentBox;
    private final Scale contentScale = new Scale(1.0, 1.0);

    /**
     * 构造一个新的棋盘视图。
     *
     * @param board 关联的棋盘数据模型
     */
    public ChessBoardView(ChessBoardModel board) {
        this.board = board;
        initializeTextures();

        configureGeometry();

        this.canvas = new Canvas(canvasWidth, canvasHeight);
        this.motionLayer = new Pane();
        motionLayer.setMouseTransparent(true);
        motionLayer.setPickOnBounds(false);
        motionLayer.setPrefSize(canvasWidth, canvasHeight);
        motionLayer.prefWidthProperty().bind(canvas.widthProperty());
        motionLayer.prefHeightProperty().bind(canvas.heightProperty());
        this.effectsLayer = new Pane();
        effectsLayer.setMouseTransparent(true);
        effectsLayer.setPickOnBounds(false);
        effectsLayer.setPrefSize(canvasWidth, canvasHeight);
        effectsLayer.prefWidthProperty().bind(canvas.widthProperty());
        effectsLayer.prefHeightProperty().bind(canvas.heightProperty());
        this.boardGroup = new Group(canvas, motionLayer, effectsLayer);
        
        this.restartButton = new Button("再来一局");
        restartButton.setFocusTraversable(false);
        restartButton.setVisible(false);
        restartButton.managedProperty().bind(restartButton.visibleProperty());
        restartButton.getStyleClass().add("restart-button");
        restartButton.setOnAction(evt -> {
            clearActiveKillEffect();
            clearCaptureLaunchEffect();
            board.clearLastCaptureEvent();
            stopTargetArrowRotation();
            deactivateDragPreview();
            animatingPieces.clear();
            motionLayer.getChildren().clear();
            board.restartGame();
            selectedPiece = null;
            legalTargets.clear();
            specialMessage = board.getSpecialMessage();
            captureHighlight = board.getCaptureHighlight();
            drawBoard();
            updateCursor();
        });
        this.saveButton = new Button("保存棋局");
        saveButton.setFocusTraversable(false);
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #b45309, #d97706);"
                + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 22;"
                + "-fx-padding: 6 10 6 10; -fx-cursor: hand;");
        saveButton.setTooltip(new Tooltip("保存当前棋局进度"));
        saveButton.setOnAction(evt -> {
            if (onSave != null) {
                onSave.run();
            }
        });

        this.exitButton = new Button("退出游戏");
        exitButton.setFocusTraversable(false);
        exitButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.setStyle("-fx-background-color: linear-gradient(to right, #dc2626, #b91c1c);"
                + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 22;"
                + "-fx-padding: 6 10 6 10; -fx-cursor: hand;");
        exitButton.setTooltip(new Tooltip("返回模式选择界面"));
        exitButton.setOnAction(evt -> {
            if (onExit != null) {
                onExit.run();
            }
        });

        this.undoButton = new Button("悔棋");
        undoButton.setFocusTraversable(false);
        undoButton.setMaxWidth(Double.MAX_VALUE);
        undoButton.setStyle("-fx-background-color: linear-gradient(to right, #6b7280, #4b5563);"
                + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 22;"
                + "-fx-padding: 6 10 6 10; -fx-cursor: hand;");
        undoButton.setOnAction(evt -> {
            board.undoMove();
            selectedPiece = null;
            legalTargets.clear();
            deactivateDragPreview();
            stopTargetArrowRotation();
            drawBoard();
            updateCursor();
        });

        this.sideBar = new VBox(16);
        sideBar.setAlignment(Pos.TOP_CENTER);
        sideBar.setPadding(new Insets(20, 0, 0, 0));
        sideBar.setPrefWidth(RIGHT_SIDEBAR_WIDTH);
        sideBar.setMinWidth(RIGHT_SIDEBAR_WIDTH);
        sideBar.setMaxWidth(RIGHT_SIDEBAR_WIDTH);
        sideBar.getChildren().addAll(saveButton, undoButton, exitButton);
        sideBar.setPickOnBounds(false);

        VBox boardColumn = new VBox(10);
        boardColumn.setAlignment(Pos.CENTER);
        boardColumn.getChildren().addAll(boardGroup, restartButton);

        this.contentBox = new javafx.scene.layout.HBox(10);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getChildren().addAll(boardColumn, sideBar);
        contentBox.getTransforms().add(contentScale);
        contentBox.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> updateScale());

        Group contentGroup = new Group(contentBox);

        setPrefSize(canvasWidth + SIDE_UI_PADDING + RIGHT_SIDEBAR_WIDTH, canvasHeight + TOP_UI_PADDING + BOTTOM_UI_PADDING);
        setAlignment(Pos.CENTER);
        getChildren().add(contentGroup);
        // StackPane alignments are no longer needed as we manage layout manually
        // StackPane.setAlignment(boardGroup, Pos.CENTER);
        // StackPane.setAlignment(restartButton, Pos.BOTTOM_CENTER);
        // restartButton.setTranslateY(0);
        // StackPane.setAlignment(sideBar, Pos.TOP_RIGHT);

        setStyle("-fx-background-color: radial-gradient(radius 120%, #da4c2cff, #d2a86f);");

        setOnSave(null);
        setOnExit(null);

        initializeListeners();
        setupInteraction();
        drawBoard();

        widthProperty().addListener((obs, oldVal, newVal) -> updateScale());
        heightProperty().addListener((obs, oldVal, newVal) -> updateScale());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::updateScale);
            }
        });
        updateScale();
        updateCursor();
    }

    /**
     * 设置保存按钮的回调函数。
     *
     * @param handler 保存操作的逻辑
     */
    public void setOnSave(Runnable handler) {
        this.onSave = handler;
        saveButton.setDisable(handler == null);
        saveButton.setOpacity(handler == null ? 0.6 : 1.0);
    }

    /**
     * 设置退出按钮的回调函数。
     *
     * @param handler 退出操作的逻辑
     */
    public void setOnExit(Runnable handler) {
        this.onExit = handler;
        exitButton.setDisable(handler == null);
        exitButton.setOpacity(handler == null ? 0.6 : 1.0);
    }

    private void initializeListeners() {
        board.getPieces().forEach(this::registerPieceListeners);
        board.getPieces().addListener((ListChangeListener<AbstractPiece>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::registerPieceListeners);
                }
            }
            drawBoard();
        });
        board.redTurnProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedPiece != null && selectedPiece.isRed() != newVal) {
                selectedPiece = null;
                legalTargets.clear();
                deactivateDragPreview();
                stopTargetArrowRotation();
                updateCursor();
            }
            drawBoard();
        });
        board.specialMessageFromRedProperty().addListener((obs, oldVal, newVal) -> specialMessageFromRed = newVal);
        board.specialMessageProperty().addListener((obs, oldVal, newVal) -> {
            if (messageTimer != null) {
                messageTimer.stop();
                messageTimer = null;
            }
            if (newVal != null && !newVal.isBlank()) {
                specialMessage = newVal;
                messageTimer = new PauseTransition(Duration.seconds(2.5));
                messageTimer.setOnFinished(evt -> {
                    specialMessage = "";
                    drawBoard();
                    board.clearSpecialMessage();
                    messageTimer = null;
                });
                messageTimer.playFromStart();
            } else {
                specialMessage = "";
            }
            drawBoard();
        });
        board.captureHighlightProperty().addListener((obs, oldPiece, newPiece) -> {
            if (captureTimer != null) {
                captureTimer.stop();
                captureTimer = null;
            }
            captureHighlight = newPiece;
            if (newPiece != null) {
                captureTimer = new PauseTransition(Duration.seconds(5));
                captureTimer.setOnFinished(evt -> {
                    captureHighlight = null;
                    board.clearCaptureHighlight();
                    drawBoard();
                    captureTimer = null;
                });
                captureTimer.playFromStart();
                killEffectPending = true;
                Platform.runLater(() -> {
                    launchKillEffect(newPiece);
                    killEffectPending = false;
                });
            }
            drawBoard();
        });
        board.lastCaptureEventProperty().addListener((obs, oldEvent, newEvent) -> {
            if (newEvent != null) {
                Platform.runLater(() -> playCaptureLaunch(newEvent));
            }
        });
        board.winnerProperty().addListener((obs, oldWinner, newWinner) -> {
            if (messageTimer != null) {
                messageTimer.stop();
                messageTimer = null;
            }
            if (captureTimer != null) {
                captureTimer.stop();
                captureTimer = null;
            }
            clearActiveKillEffect();
            clearCaptureLaunchEffect();
            board.clearLastCaptureEvent();
            winnerLabel = newWinner;
            restartButton.setVisible(newWinner != null);
            if (newWinner != null) {
                selectedPiece = null;
                legalTargets.clear();
                specialMessage = "";
                captureHighlight = null;
            } else {
                selectedPiece = null;
                legalTargets.clear();
                specialMessage = board.getSpecialMessage();
                captureHighlight = board.getCaptureHighlight();
            }
            deactivateDragPreview();
            stopTargetArrowRotation();
            updateCursor();
            drawBoard();
        });
        winnerLabel = board.getWinner();
        restartButton.setVisible(winnerLabel != null);
    }

    private void drawBoard() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (boardTexture != null) {
            gc.drawImage(boardTexture, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            gc.setLineWidth(2);
            gc.setStroke(Color.SADDLEBROWN);
            drawGrid(gc);
            drawRiver(gc);
        }

        gc.setLineWidth(2);
        drawTurnIndicator(gc);
        if (selectedPiece != null) {
            drawSelection(gc, selectedPiece);
        }
        board.getPieces().forEach(piece -> {
            drawPiece(gc, piece);
            drawCaptureMarks(gc, piece);
        });
        drawLegalTargets(gc);
        drawSpecialMessage(gc);
        drawWinnerBanner(gc);
    }

    private void drawGrid(GraphicsContext gc) {
        double left = columnPositions[0];
        double right = columnPositions[ChessBoardModel.COLS - 1];
        double top = rowPositions[0];
        double bottom = rowPositions[ChessBoardModel.ROWS - 1];

        for (int row = 0; row < ChessBoardModel.ROWS; row++) {
            double y = rowToY(row);
            gc.strokeLine(left, y, right, y);
        }

        for (int col = 0; col < ChessBoardModel.COLS; col++) {
            double x = columnToX(col);
            if (col == 0 || col == ChessBoardModel.COLS - 1) {
                gc.strokeLine(x, top, x, bottom);
            } else {
                gc.strokeLine(x, top, x, rowToY(4));
                gc.strokeLine(x, rowToY(5), x, bottom);
            }
        }

        drawPalaceCross(gc, 0, 3);
        drawPalaceCross(gc, 0, 5);
        drawPalaceCross(gc, 7, 3);
        drawPalaceCross(gc, 7, 5);
    }

    private void drawPalaceCross(GraphicsContext gc, int row, int col) {
        double startX = columnToX(col);
        double startY = rowToY(row);
        double endX = columnToX(col + 2);
        double endY = rowToY(row + 2);

        gc.strokeLine(startX, startY, endX, endY);
        gc.strokeLine(startX, endY, endX, startY);
    }

    private void drawRiver(GraphicsContext gc) {
        gc.setFill(Color.SIENNA);
        Font font = Font.font("KaiTi", FontWeight.BOLD, 26);
        gc.setFont(font);

        double riverY = (rowToY(4) + rowToY(5)) / 2.0;
        drawCenteredText(gc, "楚河", columnToX(2), riverY, font, Color.SIENNA);
        drawCenteredText(gc, "汉界", columnToX(6), riverY, font, Color.SIENNA);
    }

    private void drawTurnIndicator(GraphicsContext gc) {
        if (winnerLabel != null) {
            String text = winnerLabel + " 获胜";
            Color color = winnerLabel.contains("红") ? Color.CRIMSON : Color.BLACK;
            Font font = Font.font("KaiTi", FontWeight.BOLD, 20);
            double centerX = (columnPositions[0] + columnPositions[ChessBoardModel.COLS - 1]) / 2.0;
            double centerY = (rowToY(4) + rowToY(5)) / 2.0 - cellSpacingY * 0.3;
            drawCenteredText(gc, text, centerX, centerY, font, color);
            return;
        }

        String text = board.isRedTurn() ? "轮到红方" : "轮到黑方";
        Color color = board.isRedTurn() ? Color.CRIMSON : Color.BLACK;
        Font font = Font.font("KaiTi", FontWeight.BOLD, 20);
        double centerX = (columnPositions[0] + columnPositions[ChessBoardModel.COLS - 1]) / 2.0;
        double centerY = (rowToY(4) + rowToY(5)) / 2.0 - cellSpacingY * 0.3;
        drawCenteredText(gc, text, centerX, centerY, font, color);
    }

    private void drawPiece(GraphicsContext gc, AbstractPiece piece) {
        if (animatingPieces.contains(piece)) {
            return;
        }
        double centerX = columnToX(piece.getCol());
        double centerY = rowToY(piece.getRow());
        Image texture = getPieceTexture(piece);
        if (texture != null) {
            double size = pieceSize;
            double left = centerX - size / 2;
            double top = centerY - size / 2;
            gc.drawImage(texture, left, top, size, size);
        } else {
            gc.setFill(Color.BEIGE);
            gc.fillOval(centerX - pieceRadius, centerY - pieceRadius, pieceRadius * 2, pieceRadius * 2);

            gc.setStroke(Color.DARKGOLDENROD);
            gc.strokeOval(centerX - pieceRadius, centerY - pieceRadius, pieceRadius * 2, pieceRadius * 2);

            Font font = Font.font("KaiTi", FontWeight.BOLD, 22);
            drawCenteredText(gc, piece.getName(), centerX, centerY + 6, font, piece.isRed() ? Color.CRIMSON : Color.BLACK);
        }
    }

    private void initializeTextures() {
        boardTexture = loadImage("/images/Background.png");
        String[] colors = {"Red", "Black"};
        String[] types = {"Advisor", "Cannon", "Chariot", "Elephant", "General", "Horse", "Soldier"};
        for (String color : colors) {
            for (String type : types) {
                String key = color + '_' + type;
                Image texture = loadImage("/images/" + key + ".png");
                if (texture != null) {
                    pieceTextures.put(key, texture);
                }
            }
        }
    }

    private void configureGeometry() {
        if (boardTexture != null && Math.abs(boardTexture.getWidth() - 325) < 1 && Math.abs(boardTexture.getHeight() - 403) < 1) {
            canvasWidth = boardTexture.getWidth();
            canvasHeight = boardTexture.getHeight();

            double[] texturedColumns = {21, 57, 92, 127, 162, 197, 231, 268, 305};
            double[] texturedRows = {36, 73, 109, 145, 181, 217, 253, 289, 325, 362};
            System.arraycopy(texturedColumns, 0, columnPositions, 0, texturedColumns.length);
            System.arraycopy(texturedRows, 0, rowPositions, 0, texturedRows.length);

            marginX = columnPositions[0];
            marginY = rowPositions[0];
            cellSpacingX = (columnPositions[columnPositions.length - 1] - columnPositions[0]) / (columnPositions.length - 1);
            cellSpacingY = (rowPositions[rowPositions.length - 1] - rowPositions[0]) / (rowPositions.length - 1);

            Image samplePiece = pieceTextures.getOrDefault("Red_General", pieceTextures.values().stream().findFirst().orElse(null));
            pieceSize = samplePiece != null ? Math.max(samplePiece.getWidth(), samplePiece.getHeight()) : DEFAULT_PIECE_SIZE;
        } else {
            canvasWidth = DEFAULT_CELL_SIZE * (ChessBoardModel.COLS - 1) + DEFAULT_MARGIN * 2;
            canvasHeight = DEFAULT_CELL_SIZE * (ChessBoardModel.ROWS - 1) + DEFAULT_MARGIN * 2;

            marginX = DEFAULT_MARGIN;
            marginY = DEFAULT_MARGIN;
            cellSpacingX = DEFAULT_CELL_SIZE;
            cellSpacingY = DEFAULT_CELL_SIZE;

            for (int col = 0; col < ChessBoardModel.COLS; col++) {
                columnPositions[col] = marginX + col * cellSpacingX;
            }
            for (int row = 0; row < ChessBoardModel.ROWS; row++) {
                rowPositions[row] = marginY + row * cellSpacingY;
            }

            pieceSize = DEFAULT_PIECE_SIZE;
        }

        pieceRadius = pieceSize / 2.0;
    }

    private double columnToX(int col) {
        return columnPositions[Math.max(0, Math.min(col, ChessBoardModel.COLS - 1))];
    }

    private double rowToY(int row) {
        return rowPositions[Math.max(0, Math.min(row, ChessBoardModel.ROWS - 1))];
    }

    @Override
    protected double computePrefWidth(double height) {
        return canvasWidth + SIDE_UI_PADDING;
    }

    @Override
    protected double computePrefHeight(double width) {
        return canvasHeight + TOP_UI_PADDING + BOTTOM_UI_PADDING;
    }

    private void updateScale() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double contentWidth = contentBox.getLayoutBounds().getWidth();
        double contentHeight = contentBox.getLayoutBounds().getHeight();

        if (contentWidth <= 0 || contentHeight <= 0) {
            // Fallback if layout hasn't happened yet
            contentWidth = canvasWidth + SIDE_UI_PADDING + RIGHT_SIDEBAR_WIDTH;
            contentHeight = canvasHeight + TOP_UI_PADDING + BOTTOM_UI_PADDING;
        }

        double scale = Math.min(width / contentWidth, height / contentHeight);
        if (!Double.isFinite(scale) || scale <= 0) {
            scale = 1.0;
        }

        contentScale.setX(scale);
        contentScale.setY(scale);
        contentScale.setPivotX(contentWidth / 2.0);
        contentScale.setPivotY(contentHeight / 2.0);
    }

    private int locateColumn(double x) {
        int nearest = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < columnPositions.length; i++) {
            double distance = Math.abs(columnPositions[i] - x);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = i;
            }
        }
        double tolerance = Math.max(cellSpacingX, 28) * 0.6;
        return bestDistance <= tolerance ? nearest : -1;
    }

    private int locateRow(double y) {
        int nearest = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < rowPositions.length; i++) {
            double distance = Math.abs(rowPositions[i] - y);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = i;
            }
        }
        double tolerance = Math.max(cellSpacingY, 28) * 0.6;
        return bestDistance <= tolerance ? nearest : -1;
    }

    private Image getPieceTexture(AbstractPiece piece) {
        String colorPrefix = piece.isRed() ? "Red" : "Black";
        String type = piece.getClass().getSimpleName().replace("Piece", "");
        return pieceTextures.get(colorPrefix + '_' + type);
    }

    private Image loadImage(String resourcePath) {
        URL resource = ChessBoardView.class.getResource(resourcePath);
        if (resource == null) {
            System.err.println("无法找到贴图: " + resourcePath);
            return null;
        }
        return new Image(resource.toExternalForm());
    }

    private void drawCenteredText(GraphicsContext gc, String text, double centerX, double baselineY, Font font, Color color) {
        Text layoutProbe = new Text(text);
        layoutProbe.setFont(font);
        double width = layoutProbe.getLayoutBounds().getWidth();

        gc.setFill(color);
        gc.setFont(font);
        gc.fillText(text, centerX - width / 2, baselineY);
    }

    private void registerPieceListeners(AbstractPiece piece) {
        piece.rowProperty().addListener((obs, oldVal, newVal) -> drawBoard());
        piece.colProperty().addListener((obs, oldVal, newVal) -> drawBoard());
    }

    private void setupInteraction() {
        canvas.setCursor(Cursor.OPEN_HAND);
        canvas.setOnMouseEntered(evt -> {
            if (board.getWinner() != null) {
                canvas.setCursor(Cursor.DEFAULT);
                return;
            }
            if (dragNode != null) {
                dragNode.setVisible(true);
            }
            updateCursor();
        });
        canvas.setOnMouseExited(evt -> {
            canvas.setCursor(Cursor.DEFAULT);
            if (dragNode != null) {
                dragNode.setVisible(false);
            }
        });
        canvas.setOnMouseMoved(event -> {
            if (dragNode != null) {
                updateDragPreviewPosition(event.getX(), event.getY());
            }
        });
        canvas.setOnMouseDragged(event -> {
            if (dragNode != null) {
                updateDragPreviewPosition(event.getX(), event.getY());
            }
        });
        canvas.setOnMouseClicked(event -> {
            if (board.getWinner() != null) {
                return;
            }
            if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
                if (selectedPiece != null) {
                    selectedPiece = null;
                    legalTargets.clear();
                    deactivateDragPreview();
                    stopTargetArrowRotation();
                    drawBoard();
                    updateCursor();
                }
                return;
            }
            double x = event.getX();
            double y = event.getY();

            int col = locateColumn(x);
            int row = locateRow(y);

            if (!board.isValidPosition(row, col)) {
                selectedPiece = null;
                legalTargets.clear();
                deactivateDragPreview();
                stopTargetArrowRotation();
                drawBoard();
                updateCursor();
                return;
            }

            AbstractPiece clickedPiece = board.getPieceAt(row, col);

            if (selectedPiece == null) {
                if (clickedPiece != null && clickedPiece.isRed() == board.isRedTurn()) {
                    selectedPiece = clickedPiece;
                    recomputeLegalTargets();
                    drawBoard();
                    activateDragPreview(selectedPiece, x, y);
                    updateCursor();
                }
                return;
            }

            if (clickedPiece != null && clickedPiece.isRed() == board.isRedTurn()) {
                selectedPiece = clickedPiece;
                recomputeLegalTargets();
                drawBoard();
                activateDragPreview(selectedPiece, x, y);
                updateCursor();
                return;
            }

            int oldRow = selectedPiece.getRow();
            int oldCol = selectedPiece.getCol();
            AbstractPiece movingPiece = selectedPiece;
            boolean moved = board.movePiece(movingPiece, row, col);
            if (!moved) {
                activateDragPreview(selectedPiece, x, y);
                updateCursor();
                return;
            }

            animatePieceMovement(movingPiece, oldRow, oldCol);
            selectedPiece = null;
            legalTargets.clear();
            deactivateDragPreview();
            stopTargetArrowRotation();
            drawBoard();
            updateCursor();
        });
    }

    private void drawSelection(GraphicsContext gc, AbstractPiece piece) {
        double centerX = columnToX(piece.getCol());
        double centerY = rowToY(piece.getRow());

        gc.setStroke(Color.GOLD);
        gc.setLineWidth(3);
        double radius = pieceRadius + Math.max(4, pieceRadius * 0.25);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        gc.setLineWidth(2);
    }

    private void drawSpecialMessage(GraphicsContext gc) {
        if (specialMessage == null || specialMessage.isBlank()) {
            return;
        }
        Font font = Font.font("KaiTi", FontWeight.BOLD, 24);
        Text metrics = new Text(specialMessage);
        metrics.setFont(font);
        double textWidth = metrics.getLayoutBounds().getWidth();
        double textHeight = metrics.getLayoutBounds().getHeight();

        double centerX = (columnPositions[0] + columnPositions[ChessBoardModel.COLS - 1]) / 2.0;
        double centerY = (rowPositions[0] + rowPositions[ChessBoardModel.ROWS - 1]) / 2.0;
        double rectWidth = textWidth + 32;
        double rectHeight = textHeight + 20;
        double rectX = centerX - rectWidth / 2;
        double rectY = centerY - rectHeight / 2;

        gc.setFill(Color.color(0, 0, 0, 0.7));
        gc.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 18, 18);
        gc.setStroke(specialMessageFromRed ? Color.CRIMSON : Color.DARKSLATEBLUE);
        gc.setLineWidth(3);
        gc.strokeRoundRect(rectX, rectY, rectWidth, rectHeight, 18, 18);

        gc.setFill(Color.ANTIQUEWHITE);
        gc.setFont(font);
        gc.fillText(specialMessage, centerX - textWidth / 2, centerY + textHeight / 4);
        gc.setLineWidth(2);
    }

    private void drawCaptureMarks(GraphicsContext gc, AbstractPiece piece) {
        if (captureHighlight != piece) {
            return;
        }
        if (killEffectPending || activeKillAnimation != null) {
            return;
        }
        int count = piece.getCaptureCount();
        if (count <= 0) {
            return;
        }
        double centerX = columnToX(piece.getCol());
        double baseY = rowToY(piece.getRow()) - pieceRadius - Math.max(10, pieceRadius * 0.45);
        double spacing = Math.max(10, pieceRadius * 0.6);
        double size = Math.max(4, pieceRadius * 0.35);
        double startX = centerX - (count - 1) * spacing / 2.0;

        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2);
        for (int i = 0; i < count; i++) {
            double x = startX + i * spacing;
            gc.strokeLine(x - size, baseY - size, x + size, baseY + size);
            gc.strokeLine(x - size, baseY + size, x + size, baseY - size);
        }
        gc.setLineWidth(2);
    }

    private void launchKillEffect(AbstractPiece piece) {
        clearActiveKillEffect();

        double centerX = columnToX(piece.getCol());
        double centerY = rowToY(piece.getRow());
        double baseY = centerY - pieceRadius - Math.max(12, pieceRadius * 0.5);
        double half = Math.max(14, pieceRadius * 0.75);

        StrokePath firstStroke = createStroke(centerX - half, baseY - half, centerX + half, baseY + half);
        StrokePath secondStroke = createStroke(centerX + half, baseY - half, centerX - half, baseY + half);
        secondStroke.path().setOpacity(0.0);

        Group effectGroup = new Group(firstStroke.path(), secondStroke.path());
        effectGroup.setOpacity(0.0);
        effectsLayer.getChildren().add(effectGroup);

        Timeline drawFirst = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(effectGroup.opacityProperty(), 1.0),
                        new KeyValue(firstStroke.path().strokeDashOffsetProperty(), firstStroke.length()),
                        new KeyValue(firstStroke.path().strokeWidthProperty(), 0.7)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(firstStroke.path().strokeDashOffsetProperty(), 0.0, javafx.animation.Interpolator.LINEAR),
                        new KeyValue(firstStroke.path().strokeWidthProperty(), 2.8, javafx.animation.Interpolator.EASE_OUT))
        );

        Timeline taperFirst = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(firstStroke.path().strokeWidthProperty(), 2.8)),
                    new KeyFrame(Duration.millis(160), new KeyValue(firstStroke.path().strokeWidthProperty(), 1.8))
        );

        Timeline drawSecond = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(secondStroke.path().opacityProperty(), 1.0),
                        new KeyValue(secondStroke.path().strokeDashOffsetProperty(), secondStroke.length()),
                        new KeyValue(secondStroke.path().strokeWidthProperty(), 0.7)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(secondStroke.path().strokeDashOffsetProperty(), 0.0, javafx.animation.Interpolator.LINEAR),
                        new KeyValue(secondStroke.path().strokeWidthProperty(), 2.8, javafx.animation.Interpolator.EASE_OUT))
        );

        Timeline taperSecond = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(secondStroke.path().strokeWidthProperty(), 2.8)),
                    new KeyFrame(Duration.millis(160), new KeyValue(secondStroke.path().strokeWidthProperty(), 1.8))
        );

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), effectGroup);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(260));

        SequentialTransition sequence = new SequentialTransition(
                drawFirst,
                taperFirst,
                new PauseTransition(Duration.millis(90)),
                drawSecond,
                taperSecond,
                new PauseTransition(Duration.millis(130)),
                fadeOut
        );

        sequence.setOnFinished(evt -> {
            effectsLayer.getChildren().remove(effectGroup);
            if (activeKillAnimation == sequence) {
                activeKillAnimation = null;
            }
            drawBoard();
        });

        sequence.play();
        activeKillAnimation = sequence;
    }

    private StrokePath createStroke(double startX, double startY, double endX, double endY) {
        Path path = new Path(new MoveTo(startX, startY), new LineTo(endX, endY));
        path.setStroke(Color.GOLD);
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        double length = Math.hypot(endX - startX, endY - startY);
        path.getStrokeDashArray().setAll(length, length);
        path.setStrokeDashOffset(length);
        path.setSmooth(true);
        return new StrokePath(path, length);
    }

    private void clearActiveKillEffect() {
        if (activeKillAnimation != null) {
            activeKillAnimation.stop();
            activeKillAnimation = null;
        }
        killEffectPending = false;
        effectsLayer.getChildren().clear();
    }

    private void playCaptureLaunch(ChessBoardModel.CaptureEvent event) {
        if (event == null || event.victim() == null) {
            return;
        }
        clearCaptureLaunchEffect();

        AbstractPiece victim = event.victim();
        Node victimNode = createPieceNode(victim);
        victimNode.setMouseTransparent(true);
        double startX = columnToX(event.victimCol()) - pieceSize / 2.0;
        double startY = rowToY(event.victimRow()) - pieceSize / 2.0;
        victimNode.relocate(startX, startY);
        motionLayer.getChildren().add(victimNode);
        victimNode.toFront();

        double dirX = event.toCol() - event.fromCol();
        double dirY = event.toRow() - event.fromRow();
        if (Math.abs(dirX) < 1e-6 && Math.abs(dirY) < 1e-6) {
            dirY = -1;
        }
        double vectorLength = Math.hypot(dirX, dirY);
        if (vectorLength == 0) {
            vectorLength = 1;
        }
        dirX /= vectorLength;
        dirY /= vectorLength;

        double travel = pieceSize * 3.6;
        double lift = -pieceSize * 1.45;
        double drop = pieceSize * 2.4;
        double finalRotation = dirX * -42 + dirY * 36;

        Timeline launch = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(victimNode.translateXProperty(), 0.0),
                        new KeyValue(victimNode.translateYProperty(), 0.0),
                        new KeyValue(victimNode.scaleXProperty(), 1.0),
                        new KeyValue(victimNode.scaleYProperty(), 1.0),
                        new KeyValue(victimNode.opacityProperty(), 1.0),
                        new KeyValue(victimNode.rotateProperty(), 0.0)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(victimNode.translateXProperty(), dirX * travel * 0.45, Interpolator.EASE_OUT),
                        new KeyValue(victimNode.translateYProperty(), dirY * travel * 0.45 + lift, Interpolator.EASE_OUT),
                        new KeyValue(victimNode.scaleXProperty(), 1.12, Interpolator.EASE_OUT),
                        new KeyValue(victimNode.scaleYProperty(), 1.12, Interpolator.EASE_OUT),
                        new KeyValue(victimNode.rotateProperty(), finalRotation * 0.35, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(460),
                        new KeyValue(victimNode.translateXProperty(), dirX * travel, Interpolator.EASE_IN),
                        new KeyValue(victimNode.translateYProperty(), dirY * travel + drop, Interpolator.EASE_IN),
                        new KeyValue(victimNode.scaleXProperty(), 0.55, Interpolator.EASE_IN),
                        new KeyValue(victimNode.scaleYProperty(), 0.55, Interpolator.EASE_IN),
                        new KeyValue(victimNode.opacityProperty(), 0.0, Interpolator.EASE_IN),
                        new KeyValue(victimNode.rotateProperty(), finalRotation, Interpolator.EASE_IN))
        );

        launch.setOnFinished(evt -> {
            motionLayer.getChildren().remove(victimNode);
            if (board.getLastCaptureEvent() == event) {
                board.clearLastCaptureEvent();
            }
            if (activeCaptureLaunch == launch) {
                activeCaptureLaunch = null;
                activeLaunchNode = null;
            }
        });

        launch.play();
        activeCaptureLaunch = launch;
        activeLaunchNode = victimNode;
    }

    private void clearCaptureLaunchEffect() {
        if (activeCaptureLaunch != null) {
            activeCaptureLaunch.stop();
            activeCaptureLaunch = null;
        }
        if (activeLaunchNode != null) {
            motionLayer.getChildren().remove(activeLaunchNode);
            activeLaunchNode = null;
        }
    }

    private record StrokePath(Path path, double length) {
    }

    private void drawWinnerBanner(GraphicsContext gc) {
        if (winnerLabel == null) {
            return;
        }
        String text = winnerLabel + " 获胜！";
        Font font = Font.font("KaiTi", FontWeight.EXTRA_BOLD, 40);
        Text metrics = new Text(text);
        metrics.setFont(font);
        double textWidth = metrics.getLayoutBounds().getWidth();
        double textHeight = metrics.getLayoutBounds().getHeight();

        double centerX = (columnPositions[0] + columnPositions[ChessBoardModel.COLS - 1]) / 2.0;
        double centerY = (rowPositions[0] + rowPositions[ChessBoardModel.ROWS - 1]) / 2.0;
        double rectWidth = textWidth + 48;
        double rectHeight = textHeight + 28;
        double rectX = centerX - rectWidth / 2;
        double rectY = centerY - rectHeight / 2;

        gc.setFill(Color.color(0, 0, 0, 0.75));
        gc.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 24, 24);
        gc.setStroke(winnerLabel.contains("红") ? Color.CRIMSON : Color.DARKSLATEBLUE);
        gc.setLineWidth(4);
        gc.strokeRoundRect(rectX, rectY, rectWidth, rectHeight, 24, 24);

        gc.setFill(Color.ANTIQUEWHITE);
        gc.setFont(font);
        gc.fillText(text, centerX - textWidth / 2, centerY + textHeight / 4);
        gc.setLineWidth(2);
    }

    private void activateDragPreview(AbstractPiece piece, double pointerX, double pointerY) {
        if (piece == null) {
            return;
        }
        if (dragNode != null) {
            motionLayer.getChildren().remove(dragNode);
        }
        dragNode = createPieceNode(piece);
        dragNode.setMouseTransparent(true);
        dragNode.setOpacity(0.5);
        motionLayer.getChildren().add(dragNode);
        updateDragPreviewPosition(pointerX, pointerY);
        dragNode.toFront();
    }

    private void updateDragPreviewPosition(double x, double y) {
        if (dragNode != null && selectedPiece != null) {
            double bestX = columnToX(selectedPiece.getCol());
            double bestY = rowToY(selectedPiece.getRow());
            double minDistance = Math.hypot(x - bestX, y - bestY);

            for (int[] target : legalTargets) {
                double tx = columnToX(target[1]);
                double ty = rowToY(target[0]);
                double dist = Math.hypot(x - tx, y - ty);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestX = tx;
                    bestY = ty;
                }
            }
            dragNode.relocate(bestX - pieceSize / 2.0, bestY - pieceSize / 2.0);
        }
    }

    private void deactivateDragPreview() {
        if (dragNode != null) {
            motionLayer.getChildren().remove(dragNode);
            dragNode = null;
        }
    }

    private Node createPieceNode(AbstractPiece piece) {
        Image texture = getPieceTexture(piece);
        if (texture != null) {
            ImageView view = new ImageView(texture);
            view.setFitWidth(pieceSize);
            view.setFitHeight(pieceSize);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            return view;
        }
        Circle circle = new Circle(pieceRadius * 0.95);
        circle.setFill(Color.BEIGE);
        circle.setStroke(Color.DARKGOLDENROD);
        circle.setStrokeWidth(2);
        Text text = new Text(piece.getName());
        text.setFont(Font.font("KaiTi", FontWeight.BOLD, 22));
        text.setFill(piece.isRed() ? Color.CRIMSON : Color.BLACK);
        StackPane wrapper = new StackPane(circle, text);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPrefSize(pieceSize, pieceSize);
        wrapper.setMinSize(pieceSize, pieceSize);
        wrapper.setMaxSize(pieceSize, pieceSize);
        return wrapper;
    }

    private void animatePieceMovement(AbstractPiece piece, int fromRow, int fromCol) {
        if (piece == null) {
            return;
        }
        animatingPieces.add(piece);
        Node node = createPieceNode(piece);
        node.setMouseTransparent(true);
        double startX = columnToX(fromCol) - pieceSize / 2.0;
        double startY = rowToY(fromRow) - pieceSize / 2.0;
        double endX = columnToX(piece.getCol()) - pieceSize / 2.0;
        double endY = rowToY(piece.getRow()) - pieceSize / 2.0;
        node.relocate(startX, startY);
        motionLayer.getChildren().add(node);
        if (dragNode != null) {
            dragNode.toFront();
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.layoutXProperty(), startX),
                        new KeyValue(node.layoutYProperty(), startY)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(node.layoutXProperty(), endX, Interpolator.EASE_BOTH),
                        new KeyValue(node.layoutYProperty(), endY, Interpolator.EASE_BOTH))
        );
        timeline.setOnFinished(evt -> {
            motionLayer.getChildren().remove(node);
            animatingPieces.remove(piece);
            drawBoard();
        });
        timeline.play();
    }

    private void updateCursor() {
        if (canvas == null) {
            return;
        }
        if (board.getWinner() != null) {
            canvas.setCursor(Cursor.DEFAULT);
            return;
        }
        canvas.setCursor(selectedPiece != null ? Cursor.CLOSED_HAND : Cursor.OPEN_HAND);
    }

    private void startTargetArrowRotation() {
        if (arrowRotationTimeline != null) {
            return;
        }
        targetArrowOffset = 0;
        arrowRotationTimeline = new Timeline(
                new KeyFrame(Duration.ZERO),
                new KeyFrame(Duration.millis(40), evt -> {
                    targetArrowOffset += pieceRadius * 0.04;
                    if (targetArrowOffset > pieceRadius * 0.22) {
                        targetArrowOffset = -pieceRadius * 0.22;
                    }
                    drawBoard();
                })
        );
        arrowRotationTimeline.setCycleCount(Animation.INDEFINITE);
        arrowRotationTimeline.play();
    }

    private void stopTargetArrowRotation() {
        if (arrowRotationTimeline != null) {
            arrowRotationTimeline.stop();
            arrowRotationTimeline = null;
        }
        targetArrowOffset = 0;
    }

        private void drawTargetArrow(GraphicsContext gc, double centerX, double centerY) {
        double arrowHeight = pieceRadius * 1.32;
        double arrowWidth = arrowHeight * 0.62;
        double headHeight = arrowHeight * 0.5;
        double shaftWidth = arrowWidth * 0.34;
        double halfHeight = arrowHeight / 2.0;
        double shaftTop = halfHeight - headHeight;
        double shaftHalfWidth = shaftWidth / 2.0;
        double headHalfWidth = arrowWidth / 2.0;
        gc.save();
        double verticalBias = -pieceRadius * 0.18;
        gc.translate(centerX, centerY + targetArrowOffset + verticalBias);

        LinearGradient arrowFill = new LinearGradient(
            -headHalfWidth, -halfHeight,
            headHalfWidth, halfHeight,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(1.0, 0.62, 0.62, 0.74)),
            new Stop(0.35, Color.color(1.0, 0.34, 0.34, 0.7)),
            new Stop(0.65, Color.color(0.9, 0.18, 0.18, 0.68)),
            new Stop(1.0, Color.color(0.55, 0.05, 0.05, 0.64))
        );
        gc.setFill(arrowFill);
        gc.beginPath();
        gc.moveTo(0, halfHeight);
        gc.lineTo(-headHalfWidth, shaftTop);
        gc.lineTo(-shaftHalfWidth, shaftTop);
        gc.lineTo(-shaftHalfWidth, -halfHeight);
        gc.lineTo(shaftHalfWidth, -halfHeight);
        gc.lineTo(shaftHalfWidth, shaftTop);
        gc.lineTo(headHalfWidth, shaftTop);
        gc.closePath();
        gc.fill();

    gc.setStroke(Color.color(1.0, 0.68, 0.68, 0.62));
    gc.setLineWidth(Math.max(1.2, pieceRadius * 0.07));
        gc.stroke();

    double leftAlpha = 0.46;
    double rightAlpha = 0.28;

        double[] leftXs = {-shaftHalfWidth, -shaftHalfWidth, -headHalfWidth, 0};
        double[] leftYs = {-halfHeight, shaftTop, shaftTop, halfHeight};
    gc.setFill(Color.color(0.4, 0.05, 0.05, leftAlpha));
        gc.fillPolygon(leftXs, leftYs, leftXs.length);

        double[] rightXs = {shaftHalfWidth, shaftHalfWidth, headHalfWidth, 0};
        double[] rightYs = {-halfHeight, shaftTop, shaftTop, halfHeight};
    gc.setFill(Color.color(1.0, 0.82, 0.82, rightAlpha));
        gc.fillPolygon(rightXs, rightYs, rightXs.length);

        double highlightWidth = shaftWidth * 0.36;
        double highlightHeight = arrowHeight * 0.92;
        LinearGradient highlightFill = new LinearGradient(
            0, -highlightHeight / 2.0,
            0, highlightHeight / 2.0,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(1.0, 0.92, 0.92, 0.55)),
            new Stop(0.7, Color.color(1.0, 0.6, 0.6, 0.15))
        );
        gc.setFill(highlightFill);
        gc.fillRoundRect(-highlightWidth / 2.0, -highlightHeight / 2.0, highlightWidth,
            highlightHeight, highlightWidth * 0.65, highlightWidth * 0.65);

        double rimWidth = shaftWidth * 0.18;
        gc.setFill(Color.color(1.0, 0.4, 0.4, 0.28));
        gc.fillRect(-shaftHalfWidth - rimWidth, -halfHeight, rimWidth, arrowHeight);
        gc.setFill(Color.color(0.35, 0.0, 0.0, 0.32));
        gc.fillRect(shaftHalfWidth, -halfHeight, rimWidth, arrowHeight);

        double glowRadius = shaftWidth * 1.28;
        gc.setFill(Color.color(1.0, 0.6, 0.6, 0.24));
        gc.fillOval(-glowRadius, -glowRadius, glowRadius * 2, glowRadius * 2);

        gc.restore();
        }

    private void drawLegalTargets(GraphicsContext gc) {
        if (legalTargets.isEmpty()) {
            return;
        }
        double arrowOffset = pieceRadius * 0.18;
        for (int[] target : legalTargets) {
            int row = target[0];
            int col = target[1];
            double centerX = columnToX(col);
            double centerY = rowToY(row);
            AbstractPiece occupant = board.getPieceAt(row, col);
            boolean isCapture = occupant != null && selectedPiece != null && occupant.isRed() != selectedPiece.isRed();
            if (isCapture) {
                drawTargetArrow(gc, centerX, centerY - arrowOffset);
            } else {
                gc.setStroke(Color.CORNFLOWERBLUE);
                gc.setLineWidth(2);
                double radius = pieceRadius * 0.55;
                gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                double innerRadius = radius * 0.7;
                gc.setFill(Color.color(0.3, 0.6, 1.0, 0.22));
                gc.fillOval(centerX - innerRadius, centerY - innerRadius, innerRadius * 2, innerRadius * 2);
            }
        }
        gc.setLineWidth(2);
    }

    private void recomputeLegalTargets() {
        legalTargets.clear();
        if (selectedPiece == null) {
            stopTargetArrowRotation();
            return;
        }
        boolean hasCaptureTargets = false;
        int pieceRow = selectedPiece.getRow();
        int pieceCol = selectedPiece.getCol();
        for (int row = 0; row < ChessBoardModel.ROWS; row++) {
            for (int col = 0; col < ChessBoardModel.COLS; col++) {
                if (row == pieceRow && col == pieceCol) {
                    continue;
                }
                if (!board.isValidPosition(row, col)) {
                    continue;
                }
                if (selectedPiece.canMoveTo(row, col, board)) {
                    legalTargets.add(new int[]{row, col});
                    AbstractPiece occupant = board.getPieceAt(row, col);
                    if (occupant != null && occupant.isRed() != selectedPiece.isRed()) {
                        hasCaptureTargets = true;
                    }
                }
            }
        }
        if (hasCaptureTargets) {
            startTargetArrowRotation();
        } else {
            stopTargetArrowRotation();
        }
    }
}
