package org.example.model;

/**
 * 炮/砲棋子类。
 * 移动规则：直线移动，不吃子时与车相同；吃子时必须隔一个棋子（炮架）。
 */
public final class CannonPiece extends AbstractPiece {

    public CannonPiece(String name, int row, int col, boolean red) {
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

        AbstractPiece targetPiece = model.getPieceAt(targetRow, targetCol);
        boolean hasTarget = targetPiece != null;

        // 计算路径上的棋子数量
        int blockCount = 0;
        if (horizontal) {
            int start = Math.min(currentCol, targetCol) + 1;
            int end = Math.max(currentCol, targetCol);
            for (int col = start; col < end; col++) {
                if (model.getPieceAt(currentRow, col) != null) {
                    blockCount++;
                }
            }
        } else {
            int start = Math.min(currentRow, targetRow) + 1;
            int end = Math.max(currentRow, targetRow);
            for (int row = start; row < end; row++) {
                if (model.getPieceAt(row, currentCol) != null) {
                    blockCount++;
                }
            }
        }

        // 移动逻辑：路径上不能有棋子
        if (!hasTarget) {
            return blockCount == 0;
        }

        // 吃子逻辑：不能吃己方棋子
        if (targetPiece.isRed() == isRed()) {
            return false;
        }

        // 吃子逻辑：必须隔一个棋子（炮架）
        return blockCount == 1;
    }
}
