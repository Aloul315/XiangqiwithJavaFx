package org.example.model;

/**
 * 兵/卒棋子类。
 * 移动规则：过河前只能向前移动一步；过河后可以向前或左右移动一步。
 */
public final class SoldierPiece extends AbstractPiece {

    public SoldierPiece(String name, int row, int col, boolean red) {
        super(name, row, col, red);
    }

    @Override
    public boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model) {
        AbstractPiece targetPiece = model.getPieceAt(targetRow, targetCol);
        // 不能吃己方棋子
        if (targetPiece != null && targetPiece.isRed() == isRed()) {
            return false;
        }

        int currentRow = getRow();
        int currentCol = getCol();
        if (currentRow == targetRow && currentCol == targetCol) {
            return false;
        }

        int rowDiff = targetRow - currentRow;
        int colDiff = Math.abs(targetCol - currentCol);

        if (isRed()) {
            // 红方兵
            boolean crossed = currentRow <= 4; // 是否过河
            if (!crossed) {
                // 过河前只能向前一步
                return rowDiff == -1 && colDiff == 0;
            }
            // 过河后可以向前一步
            if (rowDiff == -1 && colDiff == 0) {
                return true;
            }
            // 过河后可以左右一步
            return rowDiff == 0 && colDiff == 1;
        } else {
            // 黑方卒
            boolean crossed = currentRow >= 5; // 是否过河
            if (!crossed) {
                // 过河前只能向前一步
                return rowDiff == 1 && colDiff == 0;
            }
            // 过河后可以向前一步
            if (rowDiff == 1 && colDiff == 0) {
                return true;
            }
            // 过河后可以左右一步
            return rowDiff == 0 && colDiff == 1;
        }
    }
}
