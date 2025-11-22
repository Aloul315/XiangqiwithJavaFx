package org.example.model;

/**
 * 象/相棋子类。
 * 移动规则：走“田”字（对角线两格），不能过河，且有“塞象眼”规则。
 */
public final class ElephantPiece extends AbstractPiece {

    public ElephantPiece(String name, int row, int col, boolean red) {
        super(name, row, col, red);
    }

    @Override
    public boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model) {
        int currentRow = getRow();
        int currentCol = getCol();

        if (currentRow == targetRow && currentCol == targetCol) {
            return false;
        }

        int rowDiff = Math.abs(targetRow - currentRow);
        int colDiff = Math.abs(targetCol - currentCol);

        // 必须走“田”字（对角线两格）
        if (rowDiff != 2 || colDiff != 2) {
            return false;
        }

        // 检查“塞象眼”
        int blockRow = currentRow + (targetRow > currentRow ? 1 : -1);
        int blockCol = currentCol + (targetCol > currentCol ? 1 : -1);
        if (model.getPieceAt(blockRow, blockCol) != null) {
            return false;
        }

        // 不能过河
        if (isRed()) {
            if (targetRow < 5) {
                return false;
            }
        } else {
            if (targetRow > 4) {
                return false;
            }
        }

        AbstractPiece targetPiece = model.getPieceAt(targetRow, targetCol);
        // 目标位置为空或为敌方棋子
        return targetPiece == null || targetPiece.isRed() != isRed();
    }
}
