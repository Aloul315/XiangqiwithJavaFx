package org.example.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * 象棋棋子的抽象基类。
 * 包含棋子的基本属性（名称、阵营、位置）以及移动逻辑的抽象定义。
 * 使用 JavaFX 属性以支持 UI 绑定。
 */
public abstract class AbstractPiece {
    private final String name;
    private final boolean red;
    private final IntegerProperty row;
    private final IntegerProperty col;
    private final IntegerProperty captureCount;

    /**
     * 构造函数。
     *
     * @param name 棋子名称（如 "車"、"馬"）
     * @param row  初始行坐标
     * @param col  初始列坐标
     * @param red  是否为红方棋子
     */
    protected AbstractPiece(String name, int row, int col, boolean red) {
        this.name = name;
        this.red = red;
        this.row = new SimpleIntegerProperty(row);
        this.col = new SimpleIntegerProperty(col);
        this.captureCount = new SimpleIntegerProperty(0);
    }

    /**
     * 获取棋子名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 判断是否为红方棋子。
     */
    public boolean isRed() {
        return red;
    }

    /**
     * 获取当前行坐标。
     */
    public int getRow() {
        return row.get();
    }

    /**
     * 设置行坐标。
     */
    public void setRow(int value) {
        row.set(value);
    }

    /**
     * 获取行坐标属性（用于绑定）。
     */
    public IntegerProperty rowProperty() {
        return row;
    }

    /**
     * 获取当前列坐标。
     */
    public int getCol() {
        return col.get();
    }

    /**
     * 设置列坐标。
     */
    public void setCol(int value) {
        col.set(value);
    }

    /**
     * 获取列坐标属性（用于绑定）。
     */
    public IntegerProperty colProperty() {
        return col;
    }

    /**
     * 移动棋子到指定位置。
     *
     * @param newRow 目标行坐标
     * @param newCol 目标列坐标
     */
    public void moveTo(int newRow, int newCol) {
        setRow(newRow);
        setCol(newCol);
    }

    /**
     * 获取该棋子的吃子数量。
     */
    public int getCaptureCount() {
        return captureCount.get();
    }

    /**
     * 获取吃子数量属性。
     */
    public IntegerProperty captureCountProperty() {
        return captureCount;
    }

    /**
     * 增加吃子计数。
     */
    void incrementCaptureCount() {
        captureCount.set(captureCount.get() + 1);
    }

    /**
     * 重置吃子计数。
     */
    void resetCaptureCount() {
        captureCount.set(0);
    }

    /**
     * 判断棋子是否可以移动到目标位置。
     *
     * @param targetRow 目标行坐标
     * @param targetCol 目标列坐标
     * @param model     棋盘模型，用于获取棋盘状态
     * @return 如果移动合法返回 true，否则返回 false
     */
    public abstract boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model);
}
