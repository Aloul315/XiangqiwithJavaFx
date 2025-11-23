package org.example.AI;

import java.util.ArrayList;
import org.example.model.AbstractPiece;
import org.example.model.ChessBoardModel;

public class MinMaxSearch {
    private static final int MAX_DEPTH = 4;
    private static final int ALPHA = Integer.MIN_VALUE;
    private static final int BETA = Integer.MAX_VALUE;
    private static boolean isRed;
    private ChessBoardModel model;
    public MinMaxSearch(boolean isRed, ChessBoardModel model){
        this.isRed = isRed;
        this.model = model;
    }

    public ChessBoardModel findBestMove(ChessBoardModel currentState) {
        ArrayList<ChessBoardModel> moves = getAllPossibleMoves(currentState);
        int bestVal = Integer.MIN_VALUE;
        ChessBoardModel bestState = null;
        
        for (ChessBoardModel child : moves) {
            int eval = minimax(child, 1, false, ALPHA, BETA);
            if (eval > bestVal) {
                bestVal = eval;
                bestState = child;
            }
        }
        return bestState;
    }

    private int evaluateBoard(ChessBoardModel modelState) {
        // 评估当前棋盘状态的函数
        return (int)(Math.random() * 100); // 示例返回值
    }
    public int minimax(ChessBoardModel nowState, int depth, boolean maximizingPlayer, int alpha, int beta) {
        if (depth == MAX_DEPTH) {
            return evaluateBoard(nowState);
        }
        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            // 遍历所有可能的走法
            ArrayList<ChessBoardModel> moves = getAllPossibleMoves(nowState);
            for (ChessBoardModel child : moves) {
                int eval = minimax(child, depth + 1, false, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; // Beta 剪枝
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            // 遍历所有可能的走法
            ArrayList<ChessBoardModel> moves = getAllPossibleMoves(nowState);
            for (ChessBoardModel child : moves) {
                int eval = minimax(child, depth + 1, true, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break; // Alpha 剪枝
                }
            }
            return minEval;
        }
    }
    private ArrayList<ChessBoardModel> getAllPossibleMoves(ChessBoardModel model){
        ArrayList<ChessBoardModel> moves = new ArrayList<>();
        //获取所有可能的走法
        for(AbstractPiece piece: model.getPieces()){
            if(piece.isRed() == isRed){
                //获取该棋子的所有可能走法
                //对于每个可能走法，生成一个新的棋盘状态并添加到moves中
                String type  = piece.getClass().getSimpleName();
                int row = piece.getRow();
                int col = piece.getCol();
                switch (type) {
                    case "GeneralPiece":
                        if(model.movePiece(piece, row + 1, col)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 1, col)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row, col + 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row, col - 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }
                        break;
                    case "AdvisorPiece":    
                        if(model.movePiece(piece, row + 1, col + 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row + 1, col - 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 1, col + 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 1, col - 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }
                        break;  
                    case "ElephantPiece":
                        if(model.movePiece(piece, row + 2, col + 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row + 2, col - 2)){
                            moves.add(model.clone());
                            model.undoMove();   
                        }else if(model.movePiece(piece, row - 2, col + 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 2, col - 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }
                        break;
                    case "HorsePiece":
                        if(model.movePiece(piece, row + 2, col + 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row + 2, col - 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 2, col + 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 2, col - 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row + 1, col + 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row + 1, col - 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 1, col + 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 1, col - 2)){
                            moves.add(model.clone());
                            model.undoMove();
                        }
                        break;
                    case "ChariotPiece":
                        for(int r = 0; r < 10; r++){
                            if(r != row && model.movePiece(piece, r, col)){
                                moves.add(model.clone());
                                model.undoMove();
                            }
                        }
                        for(int c = 0; c < 9; c++){
                            if(c != col && model.movePiece(piece, row, c)){
                                moves.add(model.clone());
                                model.undoMove();
                            }
                        }
                        break;
                    case "CannonPiece":
                        for(int r = 0; r < 10; r++){
                            if(r != row && model.movePiece(piece, r, col)){
                                moves.add(model.clone());
                                model.undoMove();
                            }
                        }
                        for(int c = 0; c < 9; c++){
                            if(c != col && model.movePiece(piece, row, c)){
                                moves.add(model.clone());
                                model.undoMove();
                            }
                        }
                        break;
                    case "SoldierPiece":
                        if(model.movePiece(piece, row + 1, col)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row - 1, col)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row, col + 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }else if(model.movePiece(piece, row, col - 1)){
                            moves.add(model.clone());
                            model.undoMove();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return moves;
    }
}
