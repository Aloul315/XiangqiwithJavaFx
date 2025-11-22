package org.example.model;

/**
 * 马/傌棋子类。
 * 移动规则：走“日”字（L形），且有“蹩马腿”规则。
 */
public final class HorsePiece extends AbstractPiece {

    public HorsePiece(String name, int row, int col, boolean red) {
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

        // 必须走“日”字
        boolean lShape = (rowDiff == 1 && colDiff == 2) || (rowDiff == 2 && colDiff == 1);
        if (!lShape) {
            return false;
        }

        // 检查“蹩马腿”
        int blockRow = currentRow;
        int blockCol = currentCol;
        if (rowDiff == 2) {
            blockRow += targetRow > currentRow ? 1 : -1;
        } else {
            blockCol += targetCol > currentCol ? 1 : -1;
        }

        if (model.getPieceAt(blockRow, blockCol) != null) {
            return false;
        }

        AbstractPiece targetPiece = model.getPieceAt(targetRow, targetCol);
        // 目标位置为空或为敌方棋子
        return targetPiece == null || targetPiece.isRed() != isRed();
    }
}
