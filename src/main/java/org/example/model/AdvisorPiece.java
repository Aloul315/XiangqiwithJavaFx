package org.example.model;

/**
 * 士/仕棋子类。
 * 移动规则：在九宫格内沿斜线移动一步。
 */
public final class AdvisorPiece extends AbstractPiece {

    public AdvisorPiece(String name, int row, int col, boolean red) {
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

        // 必须沿斜线移动一步
        if (rowDiff != 1 || colDiff != 1) {
            return false;
        }

        // 必须在九宫格内
        if (isRed()) {
            if (targetRow < 7 || targetRow > 9 || targetCol < 3 || targetCol > 5) {
                return false;
            }
        } else {
            if (targetRow < 0 || targetRow > 2 || targetCol < 3 || targetCol > 5) {
                return false;
            }
        }

        AbstractPiece targetPiece = model.getPieceAt(targetRow, targetCol);
        // 目标位置为空或为敌方棋子
        return targetPiece == null || targetPiece.isRed() != isRed();
    }
}
