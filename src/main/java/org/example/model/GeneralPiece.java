package org.example.model;

/**
 * 将/帅棋子类。
 * 移动规则：在九宫格内沿直线移动一步。
 * 特殊规则：飞将（将帅不能在同一列直视对方）。
 */
public final class GeneralPiece extends AbstractPiece {

    public GeneralPiece(String name, int row, int col, boolean red) {
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

        // 必须沿直线移动一步
        if (rowDiff + colDiff != 1) {
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
        // 不能吃己方棋子
        if (targetPiece != null && targetPiece.isRed() == isRed()) {
            return false;
        }

        // 飞将规则：将帅不能正面直线相遇
        AbstractPiece opposingGeneral = model.findOpposingGeneral(this);
        if (opposingGeneral != null && targetCol == opposingGeneral.getCol()) {
            int startRow = Math.min(targetRow, opposingGeneral.getRow()) + 1;
            int endRow = Math.max(targetRow, opposingGeneral.getRow());
            for (int row = startRow; row < endRow; row++) {
                if (model.getPieceAt(row, targetCol) != null) {
                    return true; // 有阻挡，不违反飞将规则
                }
            }
            return false; // 无阻挡，违反飞将规则
        }

        return true;
    }
}
