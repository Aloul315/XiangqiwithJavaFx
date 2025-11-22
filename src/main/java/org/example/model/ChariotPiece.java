package org.example.model;

/**
 * 车/俥棋子类。
 * 移动规则：直线移动，无阻挡可移动任意步数。
 */
public final class ChariotPiece extends AbstractPiece {

    public ChariotPiece(String name, int row, int col, boolean red) {
        super(name, row, col, red);
    }

    @Override
    public boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model) {
        int currentRow = getRow();
        int currentCol = getCol();

        if (currentRow == targetRow && currentCol == targetCol) {
            return false;
        }

        boolean horizontal = currentRow == targetRow;
        boolean vertical = currentCol == targetCol;

        // 必须直线移动
        if (!horizontal && !vertical) {
            return false;
        }

        // 检查路径上是否有阻挡
        if (horizontal) {
            int start = Math.min(currentCol, targetCol) + 1;
            int end = Math.max(currentCol, targetCol);
            for (int col = start; col < end; col++) {
                if (model.getPieceAt(currentRow, col) != null) {
                    return false;
                }
            }
        } else {
            int start = Math.min(currentRow, targetRow) + 1;
            int end = Math.max(currentRow, targetRow);
            for (int row = start; row < end; row++) {
                if (model.getPieceAt(row, currentCol) != null) {
                    return false;
                }
            }
        }

        AbstractPiece targetPiece = model.getPieceAt(targetRow, targetCol);
        // 目标位置为空或为敌方棋子
        return targetPiece == null || targetPiece.isRed() != isRed();
    }
}
