package org.example.model;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.Stack;

/**
 * 象棋棋盘模型类。
 * 负责管理棋盘状态、棋子位置、回合控制、胜负判定以及移动历史记录。
 * 使用 JavaFX 可观察集合和属性，以便视图层自动更新。
 */
public class ChessBoardModel implements Cloneable{
    public static final int ROWS = 10;
    public static final int COLS = 9;

    private final ObservableList<AbstractPiece> pieces;
    private final ReadOnlyBooleanWrapper redTurn = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper specialMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper specialMessageFromRed = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyObjectWrapper<AbstractPiece> captureHighlight = new ReadOnlyObjectWrapper<>(null);
    private final ReadOnlyObjectWrapper<CaptureEvent> lastCaptureEvent = new ReadOnlyObjectWrapper<>(null);
    private final ReadOnlyObjectWrapper<String> winner = new ReadOnlyObjectWrapper<>(null);
    private final Stack<MoveRecord> moveHistory = new Stack<>();

    /**
     * 默认构造函数。
     * 初始化标准棋局。
     */
    public ChessBoardModel() {
        this.pieces = FXCollections.observableArrayList(piece -> new Observable[]{
            piece.rowProperty(), piece.colProperty()
        });
        initializePieces();
    }

    /**
     * 带初始棋子的构造函数。
     *
     * @param initialPieces 初始棋子数组
     */
    public ChessBoardModel(AbstractPiece... initialPieces) {
        this.pieces = FXCollections.observableArrayList(piece -> new Observable[]{
            piece.rowProperty(), piece.colProperty()
        });
        if (initialPieces != null && initialPieces.length > 0) {
            pieces.setAll(initialPieces);
            resetState();
        } else {
            initializePieces();
        }
    }

    /**
     * 初始化标准开局的所有棋子。
     */
    private void initializePieces() {
        pieces.setAll(
                new ChariotPiece("車", 0, 0, false),
                new HorsePiece("馬", 0, 1, false),
                new ElephantPiece("象", 0, 2, false),
                new AdvisorPiece("仕", 0, 3, false),
                new GeneralPiece("將", 0, 4, false),
                new AdvisorPiece("仕", 0, 5, false),
                new ElephantPiece("象", 0, 6, false),
                new HorsePiece("馬", 0, 7, false),
                new ChariotPiece("車", 0, 8, false),
                new CannonPiece("砲", 2, 1, false),
                new CannonPiece("砲", 2, 7, false),
                new SoldierPiece("卒", 3, 0, false),
                new SoldierPiece("卒", 3, 2, false),
                new SoldierPiece("卒", 3, 4, false),
                new SoldierPiece("卒", 3, 6, false),
                new SoldierPiece("卒", 3, 8, false),

                new SoldierPiece("兵", 6, 0, true),
                new SoldierPiece("兵", 6, 2, true),
                new SoldierPiece("兵", 6, 4, true),
                new SoldierPiece("兵", 6, 6, true),
                new SoldierPiece("兵", 6, 8, true),
                new CannonPiece("炮", 7, 1, true),
                new CannonPiece("炮", 7, 7, true),
                new ChariotPiece("俥", 9, 0, true),
                new HorsePiece("傌", 9, 1, true),
                new ElephantPiece("相", 9, 2, true),
                new AdvisorPiece("仕", 9, 3, true),
                new GeneralPiece("帅", 9, 4, true),
                new AdvisorPiece("仕", 9, 5, true),
                new ElephantPiece("相", 9, 6, true),
                new HorsePiece("傌", 9, 7, true),
                new ChariotPiece("俥", 9, 8, true)
        );
        resetState();
    }

    /**
     * 重置游戏状态（回合、消息、高亮等）。
     */
    private void resetState() {
        redTurn.set(true);
        specialMessage.set("");
        specialMessageFromRed.set(true);
        captureHighlight.set(null);
        lastCaptureEvent.set(null);
        winner.set(null);
    }

    /**
     * 重新开始游戏。
     * 清空历史记录并重置棋盘。
     */
    public void restartGame() {
        moveHistory.clear();
        initializePieces();
    }

    /**
     * 获取棋子列表（只读）。
     */
    public ObservableList<AbstractPiece> getPieces() {
        return FXCollections.unmodifiableObservableList(pieces);
    }

    /**
     * 获取指定位置的棋子。
     *
     * @param row 行坐标
     * @param col 列坐标
     * @return 该位置的棋子，如果没有则返回 null
     */
    public AbstractPiece getPieceAt(int row, int col) {
        return pieces.stream()
                .filter(piece -> piece.getRow() == row && piece.getCol() == col)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断当前是否为红方回合。
     */
    public boolean isRedTurn() {
        return redTurn.get();
    }

    /**
     * 获取红方回合属性。
     */
    public ReadOnlyBooleanProperty redTurnProperty() {
        return redTurn.getReadOnlyProperty();
    }

    /**
     * 验证坐标是否在棋盘范围内。
     */
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    /**
     * 获取特殊消息内容。
     */
    public String getSpecialMessage() {
        return specialMessage.get();
    }

    /**
     * 获取特殊消息属性。
     */
    public ReadOnlyStringProperty specialMessageProperty() {
        return specialMessage.getReadOnlyProperty();
    }

    /**
     * 判断特殊消息是否来自红方。
     */
    public boolean isSpecialMessageFromRed() {
        return specialMessageFromRed.get();
    }

    /**
     * 获取特殊消息来源属性。
     */
    public ReadOnlyBooleanProperty specialMessageFromRedProperty() {
        return specialMessageFromRed.getReadOnlyProperty();
    }

    /**
     * 清除特殊消息。
     */
    public void clearSpecialMessage() {
        specialMessage.set("");
    }

    /**
     * 设置特殊消息。
     *
     * @param message 消息内容
     * @param fromRed 是否来自红方
     */
    public void setSpecialMessage(String message, boolean fromRed) {
        if (message == null || message.isBlank()) {
            specialMessage.set("");
            return;
        }
        specialMessageFromRed.set(fromRed);
        specialMessage.set(message);
    }

    /**
     * 获取当前吃子高亮的棋子。
     */
    public AbstractPiece getCaptureHighlight() {
        return captureHighlight.get();
    }

    /**
     * 获取吃子高亮属性。
     */
    public ReadOnlyObjectProperty<AbstractPiece> captureHighlightProperty() {
        return captureHighlight.getReadOnlyProperty();
    }

    /**
     * 清除吃子高亮。
     */
    public void clearCaptureHighlight() {
        captureHighlight.set(null);
    }

    /**
     * 获取最后一次吃子事件。
     */
    public CaptureEvent getLastCaptureEvent() {
        return lastCaptureEvent.get();
    }

    /**
     * 获取最后一次吃子事件属性。
     */
    public ReadOnlyObjectProperty<CaptureEvent> lastCaptureEventProperty() {
        return lastCaptureEvent.getReadOnlyProperty();
    }

    /**
     * 清除最后一次吃子事件。
     */
    public void clearLastCaptureEvent() {
        lastCaptureEvent.set(null);
    }

    /**
     * 获取获胜者信息。
     */
    public String getWinner() {
        return winner.get();
    }

    /**
     * 获取获胜者属性。
     */
    public ReadOnlyObjectProperty<String> winnerProperty() {
        return winner.getReadOnlyProperty();
    }

    /**
     * 尝试移动棋子。
     *
     * @param piece  要移动的棋子
     * @param newRow 目标行坐标
     * @param newCol 目标列坐标
     * @return 如果移动成功返回 true，否则返回 false
     */
    public boolean movePiece(AbstractPiece piece, int newRow, int newCol) {
        Objects.requireNonNull(piece, "piece");
        specialMessage.set("");
        if (winner.get() != null) {
            return false;
        }
        if (!isValidPosition(newRow, newCol)) {
            return false;
        }

        if (piece.isRed() != redTurn.get()) {
            return false;
        }

        if (!piece.canMoveTo(newRow, newCol, this)) {
            return false;
        }

        boolean capturedEnemy = false;
        lastCaptureEvent.set(null);
        int originalRow = piece.getRow();
        int originalCol = piece.getCol();
        AbstractPiece captured = getPieceAt(newRow, newCol);
        
        // 检查是否误伤友军
        if (captured != null && captured.isRed() == piece.isRed()) {
            return false;
        }

        // 记录移动前的状态
        moveHistory.push(new MoveRecord(
            piece,
            originalRow,
            originalCol,
            newRow,
            newCol,
            captured,
            piece.getCaptureCount(),
            winner.get()
        ));

        if (captured != null) {
            int capturedRow = captured.getRow();
            int capturedCol = captured.getCol();
            pieces.remove(captured);
            piece.incrementCaptureCount();
            if (piece.getCaptureCount() >= 3) {
                specialMessageFromRed.set(piece.isRed());
                String text = piece.getName() + (piece.isRed() ? "（红方）" : "（黑方）")
                        + " 连斩第 " + piece.getCaptureCount() + " 枚敌子！";
                specialMessage.set(text);
            }
            capturedEnemy = true;
            captureHighlight.set(piece);
            if (captured instanceof GeneralPiece) {
                winner.set(piece.isRed() ? "红方" : "黑方");
            }
            lastCaptureEvent.set(new CaptureEvent(piece, captured, originalRow, originalCol, newRow, newCol, capturedRow, capturedCol));
        }

        piece.moveTo(newRow, newCol);
        redTurn.set(!redTurn.get());
        if (!capturedEnemy) {
            captureHighlight.set(null);
        }
        return true;
    }

    /**
     * 悔棋操作。
     * 撤销上一步移动，恢复被吃掉的棋子。
     */
    public void undoMove() {
        if (moveHistory.isEmpty()) {
            return;
        }
        MoveRecord record = moveHistory.pop();

        // 恢复棋子位置
        record.movedPiece.moveTo(record.fromRow, record.fromCol);

        // 恢复吃子计数
        record.movedPiece.captureCountProperty().set(record.oldCaptureCount);

        // 恢复被吃掉的棋子
        if (record.capturedPiece != null) {
            if (!pieces.contains(record.capturedPiece)) {
                pieces.add(record.capturedPiece);
            }
        }

        // 恢复回合
        redTurn.set(!redTurn.get());

        // 恢复获胜者状态
        winner.set(record.oldWinner);

        // 清除消息和高亮
        specialMessage.set("");
        captureHighlight.set(null);
        lastCaptureEvent.set(null);
    }

    /**
     * 获取最后一步移动记录。
     */
    public MoveRecord getLastMove() {
        return moveHistory.isEmpty() ? null : moveHistory.peek();
    }

    /**
     * 移动记录类（用于悔棋）。
     */
    public record MoveRecord(
            AbstractPiece movedPiece,
            int fromRow,
            int fromCol,
            int toRow,
            int toCol,
            AbstractPiece capturedPiece,
            int oldCaptureCount,
            String oldWinner
    ) {}

    /**
     * 吃子事件记录类。
     */
    public record CaptureEvent(AbstractPiece attacker,
                               AbstractPiece victim,
                               int fromRow,
                               int fromCol,
                               int toRow,
                               int toCol,
                               int victimRow,
                               int victimCol) {}

    /**
     * 查找对方将帅棋子。
     *
     * @param general 当前将帅棋子
     * @return 对方将帅棋子，如果未找到则返回 null
     */
    public AbstractPiece findOpposingGeneral(AbstractPiece general) {
        return pieces.stream()
                .filter(piece -> piece instanceof GeneralPiece && piece.isRed() != general.isRed())
                .findFirst()
                .orElse(null);
    }

    /**
     * 替换当前棋盘状态（用于加载存档）。
     *
     * @param newPieces   新棋子列表
     * @param redTurnTurn 是否轮到红方
     * @param winnerLabel 获胜者标签
     */
    public void replaceState(List<AbstractPiece> newPieces, boolean redTurnTurn, String winnerLabel) {
        moveHistory.clear();
        if (newPieces == null || newPieces.isEmpty()) {
            initializePieces();
            return;
        }
        pieces.setAll(newPieces);
        redTurn.set(redTurnTurn);
        specialMessage.set("");
        specialMessageFromRed.set(redTurnTurn);
        captureHighlight.set(null);
        lastCaptureEvent.set(null);
        winner.set(winnerLabel);
    }

    /**
     * 玩家枚举。
     */
    public enum Player {
        RED, BLACK
    }

    /**
     * 获取棋盘网格（二维数组形式）。
     *
     * @return 10x9 的棋子数组
     */
    public AbstractPiece[][] getGrid() {
        AbstractPiece[][] grid = new AbstractPiece[ROWS][COLS];
        for (AbstractPiece p : pieces) {
            grid[p.getRow()][p.getCol()] = p;
        }
        return grid;
    }

        @Override
    public ChessBoardModel clone() {
        ChessBoardModel cloned;
        try {
            cloned = (ChessBoardModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cloning not supported", e);
        }
        // 深拷贝棋子列表
        ObservableList<AbstractPiece> clonedPieces = FXCollections.observableArrayList(piece -> new Observable[]{
            piece.rowProperty(), piece.colProperty()
        });
        for (AbstractPiece piece : this.pieces) {
            clonedPieces.add(piece.clone());
        }
        cloned.pieces.clear();
        cloned.pieces.addAll(clonedPieces);

        // 重置其他属性
        cloned.redTurn.set(this.redTurn.get());
        cloned.specialMessage.set(this.specialMessage.get());
        cloned.specialMessageFromRed.set(this.specialMessageFromRed.get());
        cloned.captureHighlight.set(null);
        cloned.lastCaptureEvent.set(null);
        cloned.winner.set(this.winner.get());
        // 克隆时保留历史记录，以便 AI 返回的最佳状态中包含导致该状态的那一步移动
        cloned.moveHistory.clear();
        cloned.moveHistory.addAll(this.moveHistory);

        return cloned;
    }
}
